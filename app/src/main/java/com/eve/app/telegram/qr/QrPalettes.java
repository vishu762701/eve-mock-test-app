package com.eve.app.telegram.qr;

/** EVE bridge to the complete pinned upstream offline palette, retaining its swatch order. */
public final class QrPalettes {
    public static int count() { return Backdrops.NAMES.length; }
    public static String name(int position) { return Backdrops.NAMES[index(position)]; }
    public static int center(int position) {
        return "Neon Blue".equals(name(position)) ? 0xFF3FB1FF : Backdrops.CENTER[index(position)];
    }
    public static int edge(int position) {
        if ("Neon Blue".equals(name(position))) return 0xFF166ACE;
        double[] lab = new double[3];
        androidx.core.graphics.ColorUtils.colorToLAB(Backdrops.EDGE[index(position)], lab);
        lab[0] = Math.max(0, lab[0] - 3);
        lab[1] *= 1.03;
        lab[2] *= 1.03;
        return androidx.core.graphics.ColorUtils.LABToColor(lab[0], lab[1], lab[2]);
    }
    public static int ink(int position) {
        return "Neon Blue".equals(name(position)) ? 0xFF0075F1
                : QrPaletteInk.contrastMatchedQr(center(position), edge(position));
    }
    public static int pattern(int position) { return Backdrops.PATTERN[index(position)]; }
    private static int index(int position) {
        if (position < 0 || position >= count()) throw new IndexOutOfBoundsException("QR palette " + position);
        return Backdrops.orderToIndex(position);
    }
}
