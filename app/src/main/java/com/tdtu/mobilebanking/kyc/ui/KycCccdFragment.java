package com.tdtu.mobilebanking.kyc.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentKycCccdBinding;

import java.text.Normalizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A7 — Scan CCCD (12-digit Vietnamese ID card) with ML Kit OCR to verify customer identity */
public class KycCccdFragment extends Fragment {

    private static final int PERMISSION_REQUEST = 101;
    private static final Pattern CCCD_PATTERN = Pattern.compile("\\b(\\d{12})\\b");
    private static final Pattern DOB_PATTERN = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");

    private FragmentKycCccdBinding binding;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private TextRecognizer recognizer;
    private FirebaseFirestore db;

    // Extracted values
    private String scannedCccd = null;
    private String scannedName = null;
    private String scannedDob = null;
    private boolean scanDone = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKycCccdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        binding.cardResult.setVisibility(View.GONE);
        binding.btnConfirm.setOnClickListener(v -> verify(view));
        binding.btnRescan.setOnClickListener(v -> resetScan());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        if (code == PERMISSION_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(requireContext(), "Cần quyền camera", Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
        }
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).addListener(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
                bindCamera();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Lỗi camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCamera() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeFrame(ImageProxy proxy) {
        if (scanDone || proxy.getImage() == null) { proxy.close(); return; }

        InputImage image = InputImage.fromMediaImage(
                proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        recognizer.process(image)
                .addOnSuccessListener(this::parseOcrResult)
                .addOnCompleteListener(t -> proxy.close());
    }

    private void parseOcrResult(Text visionText) {
        if (scanDone) return;
        String fullText = visionText.getText();

        // Extract CCCD number
        if (scannedCccd == null) {
            Matcher m = CCCD_PATTERN.matcher(fullText);
            if (m.find()) scannedCccd = m.group(1);
        }

        // Extract DOB
        if (scannedDob == null) {
            Matcher m = DOB_PATTERN.matcher(fullText);
            if (m.find()) scannedDob = m.group(1);
        }

        // Extract full name: look for line after "Ho va ten" / "Full name"
        if (scannedName == null) {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String upper = line.getText().toUpperCase();
                    if (upper.contains("HO VA TEN") || upper.contains("FULL NAME")
                            || upper.contains("HỌ VÀ TÊN")) {
                        // Name is usually on the same line after the label or next line
                        String[] parts = line.getText().split(":", 2);
                        if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                            scannedName = parts[1].trim();
                        }
                    }
                }
            }
        }

        // If all 3 found → show result
        if (scannedCccd != null && scannedDob != null) {
            scanDone = true;
            requireActivity().runOnUiThread(this::showResult);
        }
    }

    private void showResult() {
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding.tvScannedCccd.setText("Số CCCD: " + (scannedCccd != null ? scannedCccd : "—"));
        binding.tvScannedName.setText("Họ tên: " + (scannedName != null ? scannedName : "—"));
        binding.tvScannedDob.setText("Ngày sinh: " + (scannedDob != null ? scannedDob : "—"));
        binding.cardResult.setVisibility(View.VISIBLE);
        binding.tvGuide.setText("Kiểm tra thông tin và xác nhận");
    }

    private void verify(View view) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnConfirm.setEnabled(false);

        db.collection("users").document(uid).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnConfirm.setEnabled(true);

                    boolean cccdMatch = scannedCccd != null
                            && scannedCccd.equals(doc.getString("cccd"));
                    boolean dobMatch = scannedDob != null
                            && scannedDob.equals(doc.getString("dob"));
                    boolean nameMatch = scannedName == null // name optional
                            || normalize(scannedName).equals(normalize(doc.getString("fullName")));

                    if (cccdMatch && dobMatch && nameMatch) {
                        // CCCD khớp → tiếp tục sang bước đăng ký khuôn mặt (sẽ set kycStatus = VERIFIED)
                        Navigation.findNavController(view)
                                .navigate(R.id.action_cccd_to_face_enroll);
                    } else {
                        Bundle args = new Bundle();
                        args.putBoolean("success", false);
                        args.putString("message", "Thông tin CCCD không khớp hồ sơ. Vui lòng liên hệ nhân viên.");
                        Navigation.findNavController(view)
                                .navigate(R.id.action_kycCccd_to_kycResult, args);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnConfirm.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetScan() {
        scannedCccd = null;
        scannedName = null;
        scannedDob = null;
        scanDone = false;
        binding.cardResult.setVisibility(View.GONE);
        binding.tvGuide.setText("Đưa mặt trước CCCD vào khung hình");
        startCamera();
    }

    /** Normalise Vietnamese string for comparison (remove diacritics, uppercase) */
    private String normalize(String s) {
        if (s == null) return "";
        String nfd = Normalizer.normalize(s.trim().toUpperCase(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").replace("Đ", "D");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding = null;
    }
}
