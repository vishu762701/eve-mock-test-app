/*
 * Compatibility bridge for existing EVE imports.
 * Bezier math is inherited unchanged from the original Telegram Java class
 * (DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15).
 * This file is an ADAPTED EVE package facade, not an upstream verbatim import.
 * See THIRD_PARTY_NOTICES.md and docs/telegram-interpolator-bridge.md.
 */
package com.eve.app.ui.profile.popup;

import android.graphics.PointF;

public class CubicBezierInterpolator extends org.telegram.ui.Components.CubicBezierInterpolator {

    // Retain field types for callers assigning an EVE-package interpolator.
    public static final CubicBezierInterpolator DEFAULT = new CubicBezierInterpolator(0.25, 0.1, 0.25, 1);
    public static final CubicBezierInterpolator EASE_OUT = new CubicBezierInterpolator(0, 0, .58, 1);
    public static final CubicBezierInterpolator EASE_OUT_QUINT = new CubicBezierInterpolator(.23, 1, .32, 1);
    public static final CubicBezierInterpolator EASE_IN = new CubicBezierInterpolator(.42, 0, 1, 1);
    public static final CubicBezierInterpolator EASE_BOTH = new CubicBezierInterpolator(.42, 0, .58, 1);
    public static final CubicBezierInterpolator EASE_OUT_BACK = new CubicBezierInterpolator(.34, 1.56, .64, 1);

    // Emphasized/StandardDecelerate and interpolation methods are inherited
    // from the original class. No copy or replacement of its implementation.
    public CubicBezierInterpolator(PointF start, PointF end) {
        super(start, end);
    }

    public CubicBezierInterpolator(float startX, float startY, float endX, float endY) {
        super(startX, startY, endX, endY);
    }

    public CubicBezierInterpolator(double startX, double startY, double endX, double endY) {
        super(startX, startY, endX, endY);
    }
}
