package com.eve.app.telegram.qr;

/**
 * ADAPTED source Java lattice placement, per-node wave springs, and gesture physics.
 * Upstream: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareView.java:208-229,2565-2787,3210-3218,3369-3407,6637-6768.
 * EVE's bounded drawable catalogue and username identity remain independent.
 * Telegram contributors retain copyright; THIRD_PARTY_NOTICES.md applies.
 */
public final class QrPatternField {
    private final float density;
    // Package-scoped only for the source-spring host regression harness.
    final NodeSprings nodes = new NodeSprings();
    private long clockOrigin = -1, lastFrame = -1, waveStart, glideStart, springStart;
    private float motionT, idleK = 1f, idleResumeAt = -1f;
    private float wanderX, wanderY, panX, panY, baseX, baseY;
    private float stretchX, stretchY, stretchBaseX, stretchBaseY, springX, springY;
    private float downX, downY, glideX, glideY, glideVX, glideVY;
    private float waveX, waveY, wave = 1f;
    private int waveId;
    private boolean waving, panning, moved, gliding, springing;
    // Reused output; avoid allocating one object per emoji, every rendered frame.
    public float x, y, scale = 1f, alpha = 1f, rotation, pulse;

    public QrPatternField(float density) {
        if (!Float.isFinite(density) || density <= 0) throw new IllegalArgumentException("density");
        this.density = density;
    }
    private float dp(float v) { return v * density; }
    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float smooth(float a, float b, float t) {
        float u = clamp((t - a) / (b - a), 0f, 1f);
        return u * u * (3f - 2f * u);
    }
    private static float rubber(float d, float len) {
        return 0.32f * len * (float) Math.tanh(d / Math.max(1f, len) * 2.5f);
    }

    public float motionSeconds() { return motionT; }
    public float offsetX() { return panX + wanderX; }
    public float offsetY() { return panY + wanderY; }
    public boolean wasDragged() { return moved; }

    public void beginWave(float x, float y, long now) {
        waveX = x; waveY = y; waveStart = now; wave = 0f; waving = true;
        if (++waveId == Integer.MAX_VALUE) { nodes.clear(); waveId = 1; }
    }
    public void cancelWave() {
        waving = false; wave = 1f; nodes.clear();
    }
    /** Original capped frame delta and two half-steps for stable damped springs. */
    public void advance(long now, boolean enabled) {
        if (clockOrigin < 0) clockOrigin = now;
        float t = Math.max(0f, (now - clockOrigin) / 1000f);
        float dt = lastFrame < 0 ? 0f : clamp((now - lastFrame) / 1000f, 0f, 0.05f);
        lastFrame = now;
        if (!enabled) {
            gliding = springing = waving = false;
            stretchX = stretchY = 0f;
            nodes.clear();
            return;
        }
        boolean held = panning || gliding || springing;
        if (held) idleResumeAt = t + 0.25f;
        float target = held || t < idleResumeAt ? 0f : 1f;
        float tau = target < idleK ? 0.07f : 0.45f;
        if (dt > 0) idleK += (target - idleK) * (1f - (float) Math.exp(-dt / tau));
        float idleDt = dt * idleK;
        if (idleDt > 0f) {
            float middle = motionT + idleDt / 2f;
            float angle = (float) Math.atan2(-5.5, -3.5) +
                1.2f * (float) Math.sin(0.025f * middle) +
                0.6f * (float) Math.sin(0.016f * middle + 1f);
            wanderX += (float) Math.cos(angle) * dp(6.2f) * idleDt;
            wanderY += (float) Math.sin(angle) * dp(6.2f) * idleDt;
            motionT += idleDt;
        }
        if (waving) {
            wave = clamp((now - waveStart) / 600f, 0f, 1f);
            if (wave >= 1f) waving = false;
        }
        float sdt = Math.min(0.033f, dt);
        if (sdt > 0f && nodes.n > 0) {
            boolean active = false;
            for (int j = 0; j < 2; j++) active |= nodes.step(sdt / 2f);
            if (!active && !waving) nodes.clear();
        }
        if (gliding) {
            float gt = Math.max(0f, (now - glideStart) / 1000f);
            float f = 0.35f * (1f - (float) Math.exp(-gt / 0.35f));
            panX = glideX + glideVX * f; panY = glideY + glideVY * f;
            if (gt > 2.8f) gliding = false;
        }
        if (springing) {
            float st = Math.max(0f, (now - springStart) / 1000f);
            float k = (float) (Math.exp(-st / 0.22f) * Math.cos(2 * Math.PI * st / 0.85f));
            stretchX = springX * k; stretchY = springY * k;
            if (st > 1.6f) { springing = false; stretchX = stretchY = 0f; }
        }
    }

    public void onDown(float x, float y) {
        gliding = springing = false;
        panning = true; moved = false; downX = x; downY = y;
        baseX = panX; baseY = panY;
        stretchBaseX = stretchX; stretchBaseY = stretchY;
    }
    public void onMove(float x, float y, float w, float h) {
        if (!panning) return;
        float dx = x - downX, dy = y - downY;
        if (!moved && Math.hypot(dx, dy) > dp(8f)) moved = true;
        if (moved) {
            panX = baseX + dx; panY = baseY + dy;
            stretchX = stretchBaseX + rubber(dx, w);
            stretchY = stretchBaseY + rubber(dy, h);
        }
    }
    public void onUp(float vx, float vy, long now) {
        if (!panning) return;
        panning = false;
        if (moved) {
            float v = (float) Math.hypot(vx, vy), max = dp(2600f);
            if (v > max) { vx *= max / v; vy *= max / v; }
            glideX = panX; glideY = panY; glideVX = vx; glideVY = vy;
            glideStart = now; gliding = true;
        }
        releaseStretch(now);
    }
    public void onCancel(long now) { panning = false; releaseStretch(now); }
    private void releaseStretch(long now) {
        if (stretchX == 0f && stretchY == 0f) return;
        springX = stretchX; springY = stretchY; springStart = now; springing = true;
    }

    /**
     * Source placeInner: wave-hit impulse, radial shaping, exponential 90dp
     * edge squeeze and 12dp edge fade. Source shine/tilt not imported.
     */
    public boolean place(float sx, float sy, float size, int w, int h,
                         float cardCY, int pop, int row, int col, float mass,
                         boolean live) {
        if (w <= 0 || h <= 0 || mass <= 0f || !Float.isFinite(sx) ||
            !Float.isFinite(sy) || !Float.isFinite(size)) return false;
        float band = dp(90f), margin = band * 1.3f;
        if (sx < -margin || sx > w + margin || sy < -margin || sy > h + margin) return false;
        long key = nodeKey(pop, row, col);
        int slot = nodes.find(key);
        if (live && waving && (slot < 0 || nodes.hit[slot] != waveId)) {
            float maxR = (float) Math.hypot(Math.max(waveX, w - waveX),
                Math.max(waveY, h - waveY)) + dp(60);
            float ddx = sx - waveX, ddy = sy - waveY;
            float d = (float) Math.hypot(ddx, ddy);
            if (d <= maxR * (1f - (float) Math.pow(1f - wave, 2.4f))) {
                if (slot < 0) slot = nodes.obtain(key);
                if (slot >= 0) {
                    nodes.hit[slot] = waveId;
                    float inv = 1f / mass;
                    float dx = d > 1f ? ddx / d : 0f, dy = d > 1f ? ddy / d : -1f;
                    nodes.vx[slot] += dx * dp(360f) * inv;
                    nodes.vy[slot] += dy * dp(360f) * inv;
                    nodes.vr[slot] += (dx >= 0f ? 1f : -1f) * 180f * inv;
                    nodes.vs[slot] += 1.8f;
                }
            }
        }
        if (slot >= 0) {
            sx += nodes.x[slot]; sy += nodes.y[slot];
            rotation = nodes.r[slot]; pulse = nodes.s[slot];
        } else { rotation = pulse = 0f; }
        float rcx = w / 2f, rdx = sx - rcx, rdy = sy - cardCY;
        float rr = 0f;
        if (rdx > .5f) rr = Math.max(rr, rdx / Math.max(1f, w - rcx));
        else if (rdx < -.5f) rr = Math.max(rr, -rdx / Math.max(1f, rcx));
        if (rdy > .5f) rr = Math.max(rr, rdy / Math.max(1f, h - cardCY));
        else if (rdy < -.5f) rr = Math.max(rr, -rdy / Math.max(1f, cardCY));
        rr = Math.min(1f, rr);
        float radialScale = 1f - .22f * smooth(.55f, 1f, rr);
        float radialAlpha = 1f - .2f * smooth(.8f, 1f, rr);
        float k = 1f;
        if (sx < band && sx <= w - sx) {
            float g = (float) Math.exp(sx / band - 1f);
            sx = band * g; k = g;
        } else if (w - sx < band) {
            float g = (float) Math.exp((w - sx) / band - 1f);
            sx = w - band * g; k = g;
        }
        if (sy < band && sy <= h - sy) {
            float g = (float) Math.exp(sy / band - 1f);
            sy = band * g; k = Math.min(k, g);
        } else if (h - sy < band) {
            float g = (float) Math.exp((h - sy) / band - 1f);
            sy = h - band * g; k = Math.min(k, g);
        }
        alpha = radialAlpha * smooth(.08f, .25f, k);
        if (alpha <= .004f) return false;
        float sized = size * radialScale;
        float base = sized < dp(10f) ? dp(10f) / Math.max(1f, size) : radialScale;
        scale = pop == 1 ? k * base :
            Math.max(k * base, Math.min(size, dp(9f)) / Math.max(1f, size));
        float half = size * scale / 2f;
        float ed = Math.min(Math.min(sx - half, w - sx - half),
                            Math.min(sy - half, h - sy - half));
        alpha *= smooth(0f, dp(12f), ed);
        if (alpha <= .004f) return false;
        x = sx; y = sy; return true;
    }

    public void clear() {
        nodes.clear();
        panning = moved = gliding = springing = waving = false;
        clockOrigin = lastFrame = -1;
        motionT = wanderX = wanderY = panX = panY = stretchX = stretchY = 0f;
        idleK = 1f; idleResumeAt = -1f; wave = 1f;
    }
    static long nodeKey(int pop, int r, int c) {
        return ((long) pop << 60) ^ (((long) r & 0x3FFFFFFFL) << 30) ^
            ((long) c & 0x3FFFFFFFL);
    }

    /** Original NodeSprings, open addressing and k=220, damping=0.62. */
    static final class NodeSprings {
        static final int CAP = 4096;
        final long[] keys = new long[CAP];
        final boolean[] used = new boolean[CAP];
        final float[] x = new float[CAP], y = new float[CAP], vx = new float[CAP],
            vy = new float[CAP], r = new float[CAP], vr = new float[CAP],
            s = new float[CAP], vs = new float[CAP];
        final int[] hit = new int[CAP], list = new int[CAP];
        int n;
        int find(long key) {
            if (n == 0) return -1;
            int i = (int) (mix(key) & (CAP - 1));
            while (used[i]) {
                if (keys[i] == key) return i;
                i = (i + 1) & (CAP - 1);
            }
            return -1;
        }
        int obtain(long key) {
            if (n >= CAP * 3 / 4) return -1;
            int i = (int) (mix(key) & (CAP - 1));
            while (used[i]) {
                if (keys[i] == key) return i;
                i = (i + 1) & (CAP - 1);
            }
            used[i] = true; keys[i] = key; hit[i] = -1;
            x[i] = y[i] = vx[i] = vy[i] = r[i] = vr[i] = s[i] = vs[i] = 0f;
            list[n++] = i;
            return i;
        }
        private static long mix(long k) {
            k ^= k >>> 33;
            k *= 0xff51afd7ed558ccdL;
            k ^= k >>> 33;
            return k;
        }
        boolean step(float dt) {
            final float k = 220f, c = 2f * .62f * (float) Math.sqrt(220f);
            boolean moving = false;
            for (int q = 0; q < n; q++) {
                int i = list[q];
                if (x[i] == 0 && y[i] == 0 && vx[i] == 0 && vy[i] == 0 &&
                    r[i] == 0 && vr[i] == 0 && s[i] == 0 && vs[i] == 0) continue;
                vx[i] += (-k * x[i] - c * vx[i]) * dt; x[i] += vx[i] * dt;
                vy[i] += (-k * y[i] - c * vy[i]) * dt; y[i] += vy[i] * dt;
                vr[i] += (-k * r[i] - c * vr[i]) * dt; r[i] += vr[i] * dt;
                vs[i] += (-k * s[i] - c * vs[i]) * dt; s[i] += vs[i] * dt;
                if (Math.abs(x[i]) + Math.abs(y[i]) + Math.abs(vx[i]) +
                    Math.abs(vy[i]) < .05f &&
                    Math.abs(r[i]) + Math.abs(vr[i]) < .02f &&
                    Math.abs(s[i]) + Math.abs(vs[i]) < .0005f) {
                    x[i] = y[i] = vx[i] = vy[i] = r[i] = vr[i] = s[i] = vs[i] = 0f;
                } else moving = true;
            }
            return moving;
        }
        void clear() {
            for (int q = 0; q < n; q++) used[list[q]] = false;
            n = 0;
        }
    }
}
