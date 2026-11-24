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
import com.google.mlkit.vision.face.FaceLandmark;
import com.tdtu.mobilebanking.databinding.FragmentKycFaceEnrollBinding;

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
                        List<Float> embedding = buildEmbedding(face);
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

    /**
     * Build a 128-dim pseudo-embedding from ML Kit face landmarks.
     * Coordinates are normalised to [0,1] relative to image dimensions.
     * Remaining dims are padded with 0.
     */
    private List<Float> buildEmbedding(Face face) {
        List<Float> emb = new ArrayList<>();
        int[] landmarkTypes = {
                FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE,
                FaceLandmark.NOSE_BASE, FaceLandmark.MOUTH_LEFT,
                FaceLandmark.MOUTH_RIGHT, FaceLandmark.LEFT_EAR,
                FaceLandmark.RIGHT_EAR, FaceLandmark.LEFT_CHEEK,
                FaceLandmark.RIGHT_CHEEK
        };
        for (int type : landmarkTypes) {
            FaceLandmark lm = face.getLandmark(type);
            if (lm != null) {
                emb.add(lm.getPosition().x / 1000f);
                emb.add(lm.getPosition().y / 1000f);
            } else {
                emb.add(0f); emb.add(0f);
            }
        }
        // Add head Euler angles
        emb.add(face.getHeadEulerAngleX() / 180f);
        emb.add(face.getHeadEulerAngleY() / 180f);
        emb.add(face.getHeadEulerAngleZ() / 180f);
        // Pad to 128 dims
        while (emb.size() < 128) emb.add(0f);
        return emb.subList(0, 128);
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
        kycData.put("modelName", "mlkit-landmark-v1");
        kycData.put("threshold", 0.75);

        batch.set(db.collection("kyc").document(uid), kycData);
        batch.update(db.collection("users").document(uid), "kycStatus", "VERIFIED");

        batch.commit()
                .addOnSuccessListener(u -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Đăng ký khuôn mặt thành công!", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
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
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding = null;
    }
}
