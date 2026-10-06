// ============================================================================
// App Content Routes (Privacy Policy, Terms of Service, Contact Us)
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AppContentRow, AuthUser, Env } from "../types";

export const appContentRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const HOME_HERO_ACTIONS = new Set(["open_practice", "open_pyq", "browse_exams"]);

function serializeContent(row: AppContentRow | null, type: string, includeDraft: boolean) {
  if (!row) {
    return type === "home_hero"
      ? { ...getDefaultContent(type), enabled: false, ctaLabel: "", ctaAction: "" }
      : getDefaultContent(type);
  }

  const enabled = Boolean(row.enabled);
  const visible = type !== "home_hero" || includeDraft || enabled;
  return {
    title: visible ? row.title : "",
    body: visible ? row.body : "",
    updatedAt: row.updated_at,
    updatedBy: includeDraft ? row.updated_by : "",
    supportEmail: row.support_email || "",
    phone: row.phone || "",
    website: row.website || "",
    address: row.address || "",
    ...(type === "home_hero"
      ? {
          enabled,
          ctaLabel: visible ? row.cta_label || "" : "",
          ctaAction: visible ? row.cta_action || "" : "",
        }
      : {}),
  };
}

function sanitizeHomeText(value: unknown, field: string, maxLength: number): string | null {
  if (typeof value !== "string") return null;
  const plainText = value
    .replace(/<[^>]*>/g, "")
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, "")
    .trim();
  if (plainText.length > maxLength) return null;
  if (field === "ctaLabel") return plainText.replace(/[\r\n\t]+/g, " ").trim();
  return plainText;
}

function getDefaultContent(type: string) {
  switch (type) {
    case "privacy":
      return {
        title: "Privacy Policy",
        body: `Eve ("the App", "we", "us") is a free mock test / exam preparation app for Government and entrance exams in India. This Privacy Policy explains what information the App collects and how it is used.

Information We Collect:
• Google Sign-In: When you sign in, we receive your name, email address, and profile photo from your Google account via Firebase Authentication.
• Test Attempts: We store your exam attempts, scores, selected answers, and attempt dates so you can view your test history.
• Device / Notification Token: To send you optional notifications about new exams or study reminders, we store a notification token linked to your account.

How We Use Your Information:
• To let you sign in and securely identify your account.
• To show your personal test history, scores, and exam progress.
• To send optional notifications if you enable them.

Data Storage & Sharing:
All data is stored securely using Cloudflare Workers, Cloudflare D1, and Supabase Storage. We do not run third-party advertising trackers or sell your personal data.

Children's Privacy:
The App is intended for students preparing for competitive exams and is not directed at children under 13.

Contact Us:
For questions or data deletion requests, contact us at pronlike9@gmail.com.`,
        updatedAt: 1727000000000,
        updatedBy: "admin",
        supportEmail: "pronlike9@gmail.com",
        phone: "",
        website: "https://vishu762701.github.io/eve-mock-test-app",
        address: "India",
      };
    case "terms":
      return {
        title: "Terms of Service",
        body: `Terms of Service for Eve

1. Acceptance of Terms:
By downloading, accessing, or using the Eve app, you agree to be bound by these Terms of Service. If you do not agree, please do not use the app.

2. Description of Service:
Eve provides free practice mock tests, past question papers, and study resources for competitive and entrance exams in India. All services are offered free of charge.

3. User Conduct:
You agree to use the app only for lawful study and preparation purposes. You may not attempt to reverse engineer, disrupt, or bypass authentication or scoring systems.

4. Intellectual Property:
Question papers, practice questions, and study material are provided for educational purposes. App trademarks and logos belong to Eve.

5. Disclaimer of Warranties:
The app is provided on an "as is" and "as available" basis without warranties of any kind. While we strive for accuracy, Eve does not guarantee that question answers are error-free or that competitive exam patterns will not change.

6. Changes to Terms:
We may update these Terms periodically. Continued use of the app signifies acceptance of updated terms.`,
        updatedAt: 1727000000000,
        updatedBy: "admin",
        supportEmail: "pronlike9@gmail.com",
        phone: "",
        website: "https://vishu762701.github.io/eve-mock-test-app",
        address: "India",
      };
    case "contact":
      return {
        title: "Contact Us",
        body: "Have questions, feedback, or need help with your exam preparation? Reach out to our support team through any of the channels below.",
        updatedAt: 1727000000000,
        updatedBy: "admin",
        supportEmail: "pronlike9@gmail.com",
        phone: "",
        website: "https://vishu762701.github.io/eve-mock-test-app",
        address: "India",
      };
    case "home_hero":
      return {
        title: "",
        body: "",
        updatedAt: 0,
        updatedBy: "",
        supportEmail: "",
        phone: "",
        website: "",
        address: "",
      };
    default:
      return {
        title: "",
        body: "",
        updatedAt: 0,
        updatedBy: "admin",
        supportEmail: "",
        phone: "",
        website: "",
        address: "",
      };
  }
}

function normalizeContentType(type: string): string {
  const t = type.toLowerCase();
  if (t === "privacy_policy" || t === "privacy") return "privacy";
  if (t === "terms_of_service" || t === "terms") return "terms";
  if (t === "contact_us" || t === "contact") return "contact";
  return t;
}

// GET /api/app-content/home_hero/admin - Read a draft for the admin editor.
appContentRoutes.get("/home_hero/admin", requireAdmin, async (c) => {
  const row = await c.env.DB
    .prepare("SELECT * FROM app_content WHERE id = ?")
    .bind("home_hero")
    .first<AppContentRow>();
  return c.json({ success: true, data: serializeContent(row, "home_hero", true) });
});

// GET /api/app-content/:type - Public content reads. Disabled Home hero drafts are redacted.
appContentRoutes.get("/:type", async (c) => {
  const rawType = (c.req.param("type") || "").toLowerCase();
  const type = normalizeContentType(rawType);
  const db = c.env.DB;

  const row = await db.prepare("SELECT * FROM app_content WHERE id = ?").bind(type).first<AppContentRow>();

  return c.json({ success: true, data: serializeContent(row, type, false) });
});

// PUT /api/app-content/:type - Update content (Admin)
appContentRoutes.put("/:type", requireAdmin, async (c) => {
  const rawType = (c.req.param("type") || "").toLowerCase();
  const type = normalizeContentType(rawType);
  const parsedBody = await c.req.json().catch(() => ({}));
  const body = parsedBody && typeof parsedBody === "object" && !Array.isArray(parsedBody)
    ? parsedBody as Record<string, unknown>
    : {};
  const user = c.get("user");
  const db = c.env.DB;
  const now = Date.now();

  if (type === "home_hero") {
    const enabled = body.enabled;
    if (typeof enabled !== "boolean") {
      return c.json({ success: false, error: "enabled must be a boolean" }, 400);
    }

    const title = sanitizeHomeText(body.title, "title", 80);
    const textBody = sanitizeHomeText(body.body, "body", 600);
    const ctaLabel = sanitizeHomeText(body.ctaLabel ?? "", "ctaLabel", 32);
    const ctaAction = sanitizeHomeText(body.ctaAction ?? "", "ctaAction", 32);
    if (title === null || textBody === null || ctaLabel === null || ctaAction === null) {
      return c.json({ success: false, error: "Home hero fields are invalid or too long" }, 400);
    }
    if (enabled && (!title || !textBody)) {
      return c.json({ success: false, error: "A published Home hero needs a title and body" }, 400);
    }
    if (Boolean(ctaLabel) !== Boolean(ctaAction)) {
      return c.json({ success: false, error: "CTA label and action must be provided together" }, 400);
    }
    if (ctaAction && !HOME_HERO_ACTIONS.has(ctaAction)) {
      return c.json({ success: false, error: "CTA action is not supported" }, 400);
    }

    await db
      .prepare(
        `INSERT INTO app_content (
           id, title, body, updated_at, updated_by, support_email, phone, website, address,
           enabled, cta_label, cta_action
         ) VALUES (?, ?, ?, ?, ?, '', '', '', '', ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           title = excluded.title,
           body = excluded.body,
           updated_at = excluded.updated_at,
           updated_by = excluded.updated_by,
           enabled = excluded.enabled,
           cta_label = excluded.cta_label,
           cta_action = excluded.cta_action`
      )
      .bind("home_hero", title, textBody, now, user.email, enabled ? 1 : 0, ctaLabel, ctaAction)
      .run();

    return c.json({ success: true });
  }

  const title = String(body.title || "").trim();
  const textBody = String(body.body || "").trim();
  const supportEmail = String(body.supportEmail || "").trim();
  const phone = String(body.phone || "").trim();
  const website = String(body.website || "").trim();
  const address = String(body.address || "").trim();

  await db
    .prepare(
      `INSERT INTO app_content (id, title, body, updated_at, updated_by, support_email, phone, website, address)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         title = excluded.title,
         body = excluded.body,
         updated_at = excluded.updated_at,
         updated_by = excluded.updated_by,
         support_email = excluded.support_email,
         phone = excluded.phone,
         website = excluded.website,
         address = excluded.address`
    )
    .bind(type, title, textBody, now, user.email, supportEmail, phone, website, address)
    .run();

  return c.json({ success: true });
});
