package com.eve.app.telegram.qr;

import android.graphics.Color;

/** Unchanged colour/contrast solver extracted from the pinned QrShareView.java:1304-1400. */
final class QrPaletteInk {
    private static final double QR_CONTRAST = 4.4;

    private static double lin(double v) {return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);}

    private static double gam(double v) {return v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;}

    /** sRGB int → OKLab {L, a, b}. */
    private static void toOkLab(int c, double[] out) {
        final double r = lin(Color.red(c) / 255.0), g = lin(Color.green(c) / 255.0), b = lin(Color.blue(c) / 255.0);
        final double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        final double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        final double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        out[0] = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
        out[1] = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
        out[2] = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
    }

    /** OKLab → linear sRGB (unclamped) into out. */
    private static void okLabToLinear(double L, double a, double b, double[] out) {
        double l = L + 0.3963377774 * a + 0.2158037573 * b;
        double m = L - 0.1055613458 * a - 0.0638541728 * b;
        double s = L - 0.0894841775 * a - 1.2914855480 * b;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        out[0] = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
        out[1] = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
        out[2] = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
    }

    private static boolean inGamut(double[] rgb) {
        final double e = 1e-4;
        return rgb[0] >= -e && rgb[0] <= 1 + e && rgb[1] >= -e && rgb[1] <= 1 + e && rgb[2] >= -e && rgb[2] <= 1 + e;
    }

    /** The largest chroma ≤ c that is inside sRGB at (L, h); the linear rgb of it is left in rgb. */
    private static double fitChroma(double L, double c, double h, double[] rgb) {
        final double ca = Math.cos(h), sa = Math.sin(h);
        okLabToLinear(L, c * ca, c * sa, rgb);
        if (inGamut(rgb)) return c;
        double lo = 0, hi = c;
        for (int k = 0; k < 14; k++) {
            final double mid = (lo + hi) / 2;
            okLabToLinear(L, mid * ca, mid * sa, rgb);
            if (inGamut(rgb)) lo = mid;
            else hi = mid;
        }
        okLabToLinear(L, lo * ca, lo * sa, rgb);
        return lo;
    }

    private static double contrastLinear(double[] rgb) {
        final double y = 0.2126 * clamp01(rgb[0]) + 0.7152 * clamp01(rgb[1]) + 0.0722 * clamp01(rgb[2]);
        return 1.05 / (y + 0.05);
    }

    private static double clamp01(double v) {return v < 0 ? 0 : v > 1 ? 1 : v;}

    static double contrastWhite(int c) {
        final double y = 0.2126 * lin(Color.red(c) / 255.0) + 0.7152 * lin(Color.green(c) / 255.0) + 0.0722 * lin(Color.blue(c) / 255.0);
        return 1.05 / (y + 0.05);
    }

    /**
     * The QR ink for a backdrop: the edge's OKLCH hue (the centre/edge mix for near-neutral edges), chroma edge·1.25+0.02
     * (max 0.20, reduced to fit sRGB at each lightness), lightness solved so the contrast on white is 4.4:1.
     * Near-neutral backdrops get a slightly hue-tinted grey at the same contrast.
     */
    static int contrastMatchedQr(int center, int edge) {return contrastMatchedQr(center, edge, true);}

    static int contrastMatchedQr(int center, int edge, boolean cap) {
        final double[] e = new double[3], m = new double[3], rgb = new double[3];
        toOkLab(edge, e);
        toOkLab(center, m);
        final double lCap = e[0] < 0.45 ? e[0] - 0.03 : 2.0; // dark backdrops only: never lighter than the edge
        final double edgeC = Math.hypot(e[1], e[2]);
        double h, c;
        if (edgeC < 0.03) {
            final double a = m[1] * 0.4 + e[1] * 0.6, b = m[2] * 0.4 + e[2] * 0.6;
            h = Math.atan2(b, a);
            c = Math.min(0.012, Math.hypot(a, b)); // a hint of the hue, still a grey
        } else {
            h = Math.atan2(e[2], e[1]);
            c = Math.min(edgeC * 1.25 + 0.02, 0.20);
        }
        double lo = 0.2, hi = 0.9; // contrast falls as L rises
        for (int k = 0; k < 12; k++) {
            final double mid = (lo + hi) / 2;
            fitChroma(mid, c, h, rgb);
            if (contrastLinear(rgb) > QR_CONTRAST) lo = mid;
            else hi = mid;
        }
        // pick the end of the final bracket closest to the target (the bracket is ~0.0002 wide)
        double L = (lo + hi) / 2;
        if (cap && L > lCap)
            L = Math.max(0.05, lCap); // dark backdrops: the code stays darker (contrast rises above 4.4)
        fitChroma(L, c, h, rgb);
        return Color.rgb((int) Math.round(clamp01(gam(clamp01(rgb[0]))) * 255), (int) Math.round(clamp01(gam(clamp01(rgb[1]))) * 255), (int) Math.round(clamp01(gam(clamp01(rgb[2]))) * 255));
    }

}
