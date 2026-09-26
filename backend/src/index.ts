// ============================================================================
// Eve Mock Test App — Cloudflare Worker Backend API
// ============================================================================

import { Hono } from "hono";
import { cors } from "hono/cors";
import { handleScheduledTestGeneration } from "./cron/scheduledTestGeneration";
import { authMiddleware } from "./middleware/authMiddleware";
import { rateLimit } from "./middleware/rateLimiter";
import { adminRoutes } from "./routes/admin";
import { appContentRoutes } from "./routes/appContent";
import { attemptRoutes } from "./routes/attempts";
import { authRoutes } from "./routes/auth";
import { bannerRoutes } from "./routes/banners";
import { bookmarkRoutes } from "./routes/bookmarks";
import { broadcastRoutes } from "./routes/broadcasts";
import { examRoutes } from "./routes/exams";
import { feedbackRoutes } from "./routes/feedback";
import { generatedTestRoutes } from "./routes/generatedTests";
import { leaderboardRoutes } from "./routes/leaderboard";
import { pinRoutes } from "./routes/pins";
import { pollRoutes } from "./routes/polls";
import { questionRoutes } from "./routes/questions";
import { SupabaseStorage } from "./supabase";
import { AuthUser, Env } from "./types";

const app = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// 1. CORS Middleware
app.use(
  "*",
  cors({
    origin: "*",
    allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowHeaders: ["Authorization", "Content-Type", "x-file-name"],
    maxAge: 86400,
  })
);

// 2. Health check (Public allowlist)
app.get("/api/health", (c) => {
  return c.json({
    status: "ok",
    app: "Eve Mock Test API",
    version: "2.0.0",
    timestamp: Date.now(),
  });
});

// Diagnostic / Verification Endpoints (Public Allowlist)
app.get("/api/health/d1", async (c) => {
  try {
    const db = c.env.DB;
    const { results: tables } = await db
      .prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE '_cf_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'd1_%' ORDER BY name;")
      .all<{ name: string }>();

    const { results: indexes } = await db
      .prepare("SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%';")
      .all<{ name: string }>();

    const { results: triggers } = await db
      .prepare("SELECT name FROM sqlite_master WHERE type='trigger';")
      .all<{ name: string }>();

    // Test safe write & immediate cleanup
    const testId = `diag-${Date.now()}`;
    await db.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").bind(testId, Date.now()).run();
    await db.prepare("DELETE FROM admins WHERE email = ?").bind(testId).run();

    return c.json({
      success: true,
      status: "connected",
      tableCount: (tables || []).length,
      tables: (tables || []).map((t) => t.name),
      indexCount: (indexes || []).length,
      triggerCount: (triggers || []).length,
      readWriteTest: "PASS",
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

app.get("/api/health/storage", async (c) => {
  if (!c.env.SUPABASE_SERVICE_ROLE_KEY) {
    return c.json({
      success: false,
      error: "SUPABASE_SERVICE_ROLE_KEY is not configured on the Worker",
    }, 500);
  }

  try {
    const storage = new SupabaseStorage(c.env);
    const testId = `verify-${Date.now()}`;

    // 1. Banner upload & delete verification
    const bannerPath = `banners/test-${testId}.jpg`;
    const dummyBanner = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46]);
    const bannerUrl = await storage.uploadFile(bannerPath, dummyBanner, "image/jpeg");
    await storage.deleteFile(bannerPath);

    // 2. Syllabus upload & delete verification
    const syllabusPath = `syllabi/test-${testId}.pdf`;
    const dummyPdf = new TextEncoder().encode("%PDF-1.4\n%Test Eve Syllabus\n%%EOF");
    const syllabusUrl = await storage.uploadFile(syllabusPath, dummyPdf, "application/pdf");
    await storage.deleteFile(syllabusPath);

    return c.json({
      success: true,
      connection: "PASS",
      bannerUpload: "PASS",
      bannerDelete: "PASS",
      syllabusUpload: "PASS",
      syllabusDelete: "PASS",
      sampleBannerUrl: bannerUrl,
      sampleSyllabusUrl: syllabusUrl,
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 3. Rate limiting middleware (120 req / minute per IP or UID)
app.use("/api/*", rateLimit(120, 60));

// 4. Authentication Middleware
app.use("/api/*", authMiddleware);

// 5. Mount Domain Routers
app.route("/api/auth", authRoutes);
app.route("/api/users", authRoutes);
app.route("/api/exams", examRoutes);
app.route("/api/questions", questionRoutes);
app.route("/api/attempts", attemptRoutes);
app.route("/api/leaderboard", leaderboardRoutes);
app.route("/api/broadcasts", broadcastRoutes);
app.route("/api/feedback", feedbackRoutes);
app.route("/api/banners", bannerRoutes);
app.route("/api/pins", pinRoutes);
app.route("/api/bookmarks", bookmarkRoutes);
app.route("/api/polls", pollRoutes);
app.route("/api/app-content", appContentRoutes);
app.route("/api/admin", adminRoutes);
app.route("/api/generated-tests", generatedTestRoutes);

// 6. Global Error Handling & 404
app.notFound((c) => {
  return c.json({ success: false, error: `Route not found: ${c.req.method} ${c.req.path}` }, 404);
});

app.onError((err, c) => {
  console.error("Unhandled Worker error:", err);
  return c.json({ success: false, error: err.message || "Internal server error" }, 500);
});

export default {
  fetch: app.fetch,
  scheduled: handleScheduledTestGeneration,
};
