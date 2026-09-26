// ============================================================================
// App Content Routes (Privacy Policy, Terms of Service, Contact Us)
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AppContentRow, AuthUser, Env } from "../types";

export const appContentRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

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

// GET /api/app-content/:type - Get content
appContentRoutes.get("/:type", async (c) => {
  const type = (c.req.param("type") || "").toLowerCase();
  const db = c.env.DB;

  const row = await db.prepare("SELECT * FROM app_content WHERE id = ?").bind(type).first<AppContentRow>();

  if (row) {
    return c.json({
      success: true,
      data: {
        title: row.title,
        body: row.body,
        updatedAt: row.updated_at,
        updatedBy: row.updated_by,
        supportEmail: row.support_email || "",
        phone: row.phone || "",
        website: row.website || "",
        address: row.address || "",
      },
    });
  }

  return c.json({ success: true, data: getDefaultContent(type) });
});

// PUT /api/app-content/:type - Update content (Admin)
appContentRoutes.put("/:type", requireAdmin, async (c) => {
  const type = (c.req.param("type") || "").toLowerCase();
  const body = await c.req.json().catch(() => ({}));
  const user = c.get("user");
  const db = c.env.DB;
  const now = Date.now();

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
