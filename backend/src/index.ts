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
