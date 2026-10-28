package com.digitaltimeline.core;

/** Small, dependency-free vector helpers used by embeddings and the search index. */
public final class VectorMath {
    private VectorMath() {}

    public static float dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector size mismatch: " + a.length + " vs " + b.length);
        }
        float sum = 0f;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    public static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / Math.sqrt(na * nb);
    }

    /** Mean of several equally-sized vectors (the "centroid"). */
    public static float[] centroid(Iterable<float[]> vectors, int dims) {
        double[] acc = new double[dims];
        int n = 0;
        for (float[] v : vectors) {
            if (v == null || v.length != dims) continue;
            for (int i = 0; i < dims; i++) acc[i] += v[i];
            n++;
        }
        float[] out = new float[dims];
        if (n == 0) return out;
        for (int i = 0; i < dims; i++) out[i] = (float) (acc[i] / n);
        return out;
    }
}
