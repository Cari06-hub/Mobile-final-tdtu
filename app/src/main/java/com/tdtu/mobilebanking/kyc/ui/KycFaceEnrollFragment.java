package com.tdtu.mobilebanking.kyc.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentKycFaceEnrollBinding;
import com.tdtu.mobilebanking.kyc.FaceEmbeddingHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A4 — Officer or customer enrols face for eKYC using ML Kit face detection */
public class KycFaceEnrollFragment extends Fragment {

    private static final int PERMISSION_REQUEST = 100;
    private static final int STABLE_FRAMES_NEEDED = 15;
    private static final long MAX_PHOTO_BYTES = 40 * 1024; // 40 KB

    private FragmentKycFaceEnrollBinding binding;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private FaceDetector faceDetector;

    private int stableFrameCount = 0;
    private boolean capturing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKycFaceEnrollBinding.inflate(inflater, container, false);
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

        binding.btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

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
            Toast.makeText(requireContext(), "Cần quyền camera để thực hiện eKYC", Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCamera();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Lỗi camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCamera() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, analysis);
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeFrame(ImageProxy proxy) {
        if (capturing || proxy.getImage() == null) {
            proxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!isAdded()) return;
                    if (faces.size() == 1) {
                        Face face = faces.get(0);
                        int frameArea = proxy.getWidth() * proxy.getHeight();
                        int faceArea = face.getBoundingBox().width() * face.getBoundingBox().height();
                        if (faceArea > frameArea * 0.15) {
                            stableFrameCount++;
                            requireActivity().runOnUiThread(() ->
                                    binding.tvGuide.setText("Giữ yên... " + stableFrameCount + "/" + STABLE_FRAMES_NEEDED));
                            if (stableFrameCount >= STABLE_FRAMES_NEEDED && !capturing) {
                                capturing = true;
                                capturePhoto(face);
                            }
                        } else {
                            stableFrameCount = 0;
                            requireActivity().runOnUiThread(() ->
                                    binding.tvGuide.setText("Đưa khuôn mặt vào gần hơn"));
                        }
                    } else {
                        stableFrameCount = 0;
                        requireActivity().runOnUiThread(() ->
                                binding.tvGuide.setText(faces.isEmpty()
                                        ? "Nhìn thẳng vào camera" : "Chỉ để một người trong khung hình"));
                    }
                })
                .addOnCompleteListener(t -> proxy.close());
    }

    private void capturePhoto(Face face) {
        requireActivity().runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvGuide.setText("Đang xử lý...");
        });

        File outputFile = new File(requireContext().getCacheDir(), "kyc_enroll.jpg");
        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Bitmap bmp = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                        String base64 = compressToBase64(bmp);
                        List<Float> embedding = FaceEmbeddingHelper.buildEmbedding(face);
                        saveToFirestore(base64, embedding);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        capturing = false;
                        stableFrameCount = 0;
                        requireActivity().runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvGuide.setText("Lỗi chụp ảnh, thử lại");
                        });
                    }
                });
    }

    /** Compress bitmap to JPEG under 40 KB, return as Base64 string */
    private String compressToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 50;
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        if (baos.toByteArray().length > MAX_PHOTO_BYTES) {
            baos.reset();
            bmp.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void saveToFirestore(String photoBase64, List<Float> embedding) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        Map<String, Object> kycData = new HashMap<>();
        kycData.put("embedding", embedding);
        kycData.put("photoBase64", photoBase64);
        kycData.put("enrolledAt", FieldValue.serverTimestamp());
        kycData.put("enrolledBy", uid);
        kycData.put("modelName", FaceEmbeddingHelper.MODEL_NAME);
        kycData.put("threshold", FaceEmbeddingHelper.DEFAULT_THRESHOLD);

        batch.set(db.collection("kyc").document(uid), kycData);
        batch.update(db.collection("users").document(uid), "kycStatus", "VERIFIED");

        batch.commit()
                .addOnSuccessListener(u -> {
                    if (!isAdded() || binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Bundle args = new Bundle();
                    args.putBoolean("success", true);
                    args.putString("message", "Xác thực eKYC (CCCD & Khuôn mặt) thành công!");
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_face_enroll_to_result, args);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    capturing = false;
                    stableFrameCount = 0;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvGuide.setText("Lỗi lưu dữ liệu, thử lại");
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (faceDetector != null) faceDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding = null;
    }
}
