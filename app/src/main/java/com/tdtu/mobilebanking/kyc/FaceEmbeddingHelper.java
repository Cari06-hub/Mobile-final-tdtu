package com.tdtu.mobilebanking.kyc;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper tạo pseudo-embedding 128 chiều từ ML Kit Face landmarks và tính Cosine Similarity.
 * Dùng chung cho KycFaceEnrollFragment và KycFaceVerifyFragment.
 */
public final class FaceEmbeddingHelper {

    public static final String MODEL_NAME = "mlkit-landmark-v1";
    public static final double DEFAULT_THRESHOLD = 0.75;
    private static final int EMBEDDING_DIM = 128;

    private FaceEmbeddingHelper() {}

    /**
     * Tạo vector 128 chiều từ 9 landmarks (18 giá trị) + 3 góc Euler (3 giá trị), pad bằng 0.
     */
    public static List<Float> buildEmbedding(Face face) {
        List<Float> emb = new ArrayList<>(EMBEDDING_DIM);
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
                emb.add(0f);
                emb.add(0f);
            }
        }
        emb.add(face.getHeadEulerAngleX() / 180f);
        emb.add(face.getHeadEulerAngleY() / 180f);
        emb.add(face.getHeadEulerAngleZ() / 180f);
        while (emb.size() < EMBEDDING_DIM) {
            emb.add(0f);
        }
        return emb.subList(0, EMBEDDING_DIM);
    }

    /** Tính cosine similarity trong khoảng [-1, 1] giữa embedding đã enroll và frame hiện tại. */
    public static double cosineSimilarity(List<Double> enrolled, List<Float> live) {
        int n = Math.min(enrolled.size(), live.size());
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < n; i++) {
            double a = enrolled.get(i);
            double b = live.get(i);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
