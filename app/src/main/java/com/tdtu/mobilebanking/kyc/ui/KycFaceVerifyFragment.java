package com.tdtu.mobilebanking.kyc.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.ErrorCodes;
import com.tdtu.mobilebanking.databinding.FragmentKycFaceVerifyBinding;
import com.tdtu.mobilebanking.kyc.FaceEmbeddingHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A4 Part 2 — Xác thực khuôn mặt bằng cách so embedding với dữ liệu enroll trong kyc/{uid}.
 * Liveness check: phát hiện chớp mắt (EYE_OPEN_PROB < 0.3 ít nhất 1 lần) rồi giữ mắt mở ổn định.
 * Cosine similarity >= threshold (0.75) -> verified.
 *
 * Hoạt động theo 2 chế độ:
 * 1) Fullscreen DialogFragment overlay (được gọi qua RealKycVerifier từ ConfirmTransactionFragment)
 * 2) Fragment thường trong nav_kyc (tự test luồng KYC)
 */
public class KycFaceVerifyFragment extends DialogFragment {

    private static final int PERMISSION_REQUEST = 102;
    private static final int STABLE_FRAMES_NEEDED = 6;
    private static final long TIMEOUT_MS = 30_000; // 30 giây timeout

    private FragmentKycFaceVerifyBinding binding;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private FaceDetector faceDetector;

    private List<Double> enrolledEmbedding;
    private double threshold = FaceEmbeddingHelper.DEFAULT_THRESHOLD;

    private boolean eyeBlinkDetected = false;
    private int stableFrameCount = 0;
    private boolean verifying = false;
    private boolean finished = false;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    public interface VerifyCallback {
        void onResult(boolean success);
        void onError(String errorCode);
    }

    private VerifyCallback verifyCallback;

    public void setVerifyCallback(VerifyCallback cb) {
        this.verifyCallback = cb;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getShowsDialog()) {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MobileBankingApp);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKycFaceVerifyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(opts);

        binding.btnCancel.setOnClickListener(v -> cancelVerification());

        loadEnrolledEmbedding();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (!finished) {
            finished = true;
            deliverResult(false);
        }
    }

    private void cancelVerification() {
        if (!finished) {
            finished = true;
            deliverResult(false);
        }
        closeScreen();
    }

    private void loadEnrolledEmbedding() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            failWithError(ErrorCodes.PERMISSION_DENIED, "Chưa đăng nhập");
            return;
        }
        binding.tvGuide.setText("Đang tải dữ liệu khuôn mặt...");
        FirebaseFirestore.getInstance()
                .collection("kyc").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || binding == null) return;
                    if (!doc.exists()) {
                        failWithError(ErrorCodes.KYC_REQUIRED,
                                "Chưa đăng ký khuôn mặt. Vui lòng đăng ký trước.");
                        return;
                    }
                    Object rawObj = doc.get("embedding");
                    if (!(rawObj instanceof List<?>) || ((List<?>) rawObj).isEmpty()) {
                        failWithError(ErrorCodes.KYC_REQUIRED, "Dữ liệu khuôn mặt không hợp lệ.");
                        return;
                    }
                    enrolledEmbedding = new ArrayList<>();
                    for (Object o : (List<?>) rawObj) {
                        if (o instanceof Number) {
                            enrolledEmbedding.add(((Number) o).doubleValue());
                        }
                    }
                    if (enrolledEmbedding.isEmpty()) {
                        failWithError(ErrorCodes.KYC_REQUIRED, "Dữ liệu khuôn mặt không hợp lệ.");
                        return;
                    }
                    Double t = doc.getDouble("threshold");
                    if (t != null) threshold = t;

                    requestCameraOrStart();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    failWithError(ErrorCodes.NETWORK_ERROR, "Lỗi tải dữ liệu: " + e.getMessage());
                });
    }

    private void requestCameraOrStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            failWithError(ErrorCodes.PERMISSION_DENIED, "Cần quyền camera để xác thực.");
        }
    }

    private void startCamera() {
        if (binding == null) return;
        binding.tvGuide.setText("Nhìn thẳng vào camera và chớp mắt một lần...");
        startTimeout();

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (!isAdded() || binding == null) return;
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);
            } catch (Exception e) {
                failWithError(ErrorCodes.FACE_FAILED, "Lỗi camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeFrame(ImageProxy proxy) {
        if (finished || verifying || proxy.getImage() == null) {
            proxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                proxy.getImage(), proxy.getImageInfo().getRotationDegrees());

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (finished || verifying || !isAdded() || binding == null) return;
                    if (faces.size() == 1) {
                        Face face = faces.get(0);
                        Float leftEyeProb  = face.getLeftEyeOpenProbability();
                        Float rightEyeProb = face.getRightEyeOpenProbability();

                        if (!eyeBlinkDetected) {
                            if (leftEyeProb != null && rightEyeProb != null
                                    && leftEyeProb < 0.3f && rightEyeProb < 0.3f) {
                                eyeBlinkDetected = true;
                                stableFrameCount = 0;
                            } else {
                                updateGuide("Chớp mắt một lần để xác nhận bạn là người thật...");
                            }
                            return;
                        }

                        // Sau khi đã chớp mắt, giữ mặt mở mắt ổn định
                        boolean eyesOpen = leftEyeProb == null || rightEyeProb == null
                                || (leftEyeProb > 0.5f && rightEyeProb > 0.5f);
                        if (!eyesOpen) return;

                        stableFrameCount++;
                        if (stableFrameCount >= STABLE_FRAMES_NEEDED) {
                            verifying = true;
                            updateGuide("Đang xác thực...");
                            performVerification(face);
                        } else {
                            updateGuide("Giữ nguyên... " + stableFrameCount + "/" + STABLE_FRAMES_NEEDED);
                        }
                    } else {
                        stableFrameCount = 0;
                        updateGuide(faces.isEmpty()
                                ? "Nhìn thẳng vào camera"
                                : "Chỉ để một người trong khung hình");
                    }
                })
                .addOnCompleteListener(t -> proxy.close());
    }

    private void performVerification(Face face) {
        List<Float> liveEmb = FaceEmbeddingHelper.buildEmbedding(face);
        double similarity = FaceEmbeddingHelper.cosineSimilarity(enrolledEmbedding, liveEmb);
        boolean verified = similarity >= threshold;

        timeoutHandler.removeCallbacksAndMessages(null);
        finished = true;

        String message = verified
                ? String.format(Locale.US, "Xác thực khuôn mặt thành công! (Độ khớp: %.0f%%)", similarity * 100)
                : String.format(Locale.US, "Khuôn mặt không khớp. (Độ khớp: %.0f%%, cần >= %.0f%%)",
                        similarity * 100, threshold * 100);

        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            deliverResult(verified);
            if (getShowsDialog()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            } else {
                Bundle result = new Bundle();
                result.putBoolean("success", verified);
                result.putString("message", message);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_face_verify_to_result, result);
            }
        });
    }

    private void startTimeout() {
        timeoutRunnable = () -> {
            if (finished || !isAdded()) return;
            finished = true;
            String msg = "Hết thời gian xác thực. Vui lòng thử lại.";
            deliverResult(false);
            if (getShowsDialog()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                dismissAllowingStateLoss();
            } else if (getView() != null) {
                Bundle result = new Bundle();
                result.putBoolean("success", false);
                result.putString("message", msg);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_face_verify_to_result, result);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
    }

    private void deliverResult(boolean success) {
        if (verifyCallback != null) {
            VerifyCallback cb = verifyCallback;
            verifyCallback = null;
            cb.onResult(success);
        }
    }

    private void failWithError(String errorCode, String message) {
        if (finished) return;
        finished = true;
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            if (binding != null) binding.tvGuide.setText(message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            if (verifyCallback != null) {
                VerifyCallback cb = verifyCallback;
                verifyCallback = null;
                cb.onError(errorCode);
            }
            if (getShowsDialog()) {
                dismissAllowingStateLoss();
            }
        });
    }

    private void closeScreen() {
        if (!isAdded()) return;
        if (getShowsDialog()) {
            dismissAllowingStateLoss();
        } else if (getView() != null) {
            Navigation.findNavController(getView()).navigateUp();
        }
    }

    private void updateGuide(String text) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (isAdded() && binding != null) {
                binding.tvGuide.setText(text);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeoutHandler.removeCallbacksAndMessages(null);
        if (faceDetector != null) faceDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding = null;
    }
}
