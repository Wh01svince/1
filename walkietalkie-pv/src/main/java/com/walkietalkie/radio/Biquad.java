package com.walkietalkie.radio;

/** RBJ cookbook biquad filter (transposed direct form II). */
final class Biquad {
    private final double b0, b1, b2, a1, a2;
    private double z1, z2;

    private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
        this.b0 = b0 / a0; this.b1 = b1 / a0; this.b2 = b2 / a0;
        this.a1 = a1 / a0; this.a2 = a2 / a0;
    }

    static Biquad lowPass(double fs, double f0, double q) {
        double w = 2 * Math.PI * f0 / fs, c = Math.cos(w), alpha = Math.sin(w) / (2 * q);
        return new Biquad((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha);
    }

    static Biquad highPass(double fs, double f0, double q) {
        double w = 2 * Math.PI * f0 / fs, c = Math.cos(w), alpha = Math.sin(w) / (2 * q);
        return new Biquad((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha);
    }

    float process(float x) {
        double y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return (float) y;
    }
}
