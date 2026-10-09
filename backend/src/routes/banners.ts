import { cleanupMedia } from '../services/mediaCleanup';
// ============================================================================
// Home Promotional Banners (Supabase Media Integration)
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { SupabaseStorage } from "../supabase";
import { AuthUser, Env, HomeBannerRow } from "../types";

export const bannerRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

function normalizeBannerLink(urlValue: unknown, labelValue: unknown): { url: string; label: string } | null {
  if (typeof urlValue !== "string" || typeof labelValue !== "string") return null;
  const urlText = urlValue.trim();
  const label = labelValue.replace(/[\u0000-\u001F\u007F]/g, " ").trim();
  if (!urlText) return label ? null : { url: "", label: "" };
  if (urlText.length > 2048 || label.length < 1 || label.length > 48) return null;
  if (/[\u0000-\u0020\u007F]/.test(urlText)) return null;

  try {
    const url = new URL(urlText);
    if (
      (url.protocol !== "https:" && url.protocol !== "http:") ||
      !url.hostname ||
      url.username ||
      url.password
    ) return null;
    return { url: url.href, label };
  } catch (_error) {
    return null;
  }
}

// GET /api/banners - List active banners for home carousel
bannerRoutes.get("/", async (c) => {
  const db = c.env.DB;
  const { results } = await db
    .prepare("SELECT * FROM home_banners WHERE active = 1 ORDER BY order_index ASC")
    .all<HomeBannerRow>();

  const list = (results || []).map((r) => ({
    id: r.id,
    imageUrl: r.image_url,
    storagePath: r.storage_path || null,
    order: r.order_index,
    uploadedAt: r.uploaded_at,
    uploadedBy: r.uploaded_by,
    active: Boolean(r.active),
    linkUrl: r.link_url || "",
    linkLabel: r.link_label || "",
  }));

  return c.json({ success: true, data: list });
});

// POST /api/banners - Upload banner image & record metadata (Admin)
bannerRoutes.post("/", requireAdmin, async (c) => {
  const user = c.get("user");
  const contentType = c.req.header("content-type") || "";
  const db = c.env.DB;

  let imageBytes: ArrayBuffer;
  let ext = "jpg";
  let mime = "image/jpeg";
  let linkUrl: unknown = "";
  let linkLabel: unknown = "";

  if (contentType.includes("multipart/form-data")) {
    const formData = await c.req.formData();
    const fileValue: unknown = formData.get("file");
    if (
      !fileValue ||
      typeof fileValue !== "object" ||
      !("arrayBuffer" in fileValue) ||
      typeof fileValue.arrayBuffer !== "function"
    ) {
      return c.json({ success: false, error: "No image file provided" }, 400);
    }
    const file = fileValue as File;
    imageBytes = await file.arrayBuffer();
    mime = file.type || "image/jpeg";
    ext = mime.includes("png") ? "png" : "jpg";
    linkUrl = formData.get("linkUrl") || "";
    linkLabel = formData.get("linkLabel") || "";
  } else if (contentType.includes("image/")) {
    mime = contentType.split(";")[0];
    ext = mime.includes("png") ? "png" : "jpg";
    imageBytes = await c.req.arrayBuffer();
  } else {
    // Alternatively accept base64 payload
    const parsedBody = await c.req.json().catch(() => ({}));
    const body = parsedBody && typeof parsedBody === "object" && !Array.isArray(parsedBody)
      ? parsedBody as Record<string, unknown>
      : {};
    if (typeof body.base64 !== "string" || !body.base64) {
      return c.json({ success: false, error: "Image file or base64 required" }, 400);
    }
    const binary = atob(body.base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    imageBytes = bytes.buffer;
    if (body.mimeType) {
      mime = String(body.mimeType);
      ext = mime.includes("png") ? "png" : "jpg";
    }
    linkUrl = body.linkUrl ?? "";
    linkLabel = body.linkLabel ?? "";
  }

  // Validate size <= 5MB
  if (imageBytes.byteLength > 5 * 1024 * 1024) {
    return c.json({ success: false, error: "Banner image exceeds 5MB limit" }, 400);
  }

  const link = normalizeBannerLink(linkUrl, linkLabel);
  if (!link) {
    return c.json({ success: false, error: "Provide a valid HTTP(S) link and a label of 1 to 48 characters" }, 400);
  }

  const id = crypto.randomUUID();
  const storage = new SupabaseStorage(c.env);
  const storagePath = `banners/${id}.${ext}`;
  const imageUrl = await storage.uploadFile(storagePath, imageBytes, mime);

  // Compute next order
  const maxOrderRow = await db
    .prepare("SELECT MAX(order_index) as max_order FROM home_banners")
    .first<{ max_order: number | null }>();
  const nextOrder = (maxOrderRow?.max_order ?? -1) + 1;
  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO home_banners (
         id, image_url, storage_path, order_index, uploaded_at, uploaded_by, active, link_url, link_label
       ) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)`
    )
    .bind(id, imageUrl, storagePath, nextOrder, now, user.email, link.url, link.label)
    .run();

  return c.json(
    {
      success: true,
      data: {
        id,
        imageUrl,
        storagePath,
        order: nextOrder,
        uploadedAt: now,
        uploadedBy: user.email,
        active: true,
        linkUrl: link.url,
        linkLabel: link.label,
      },
    },
    201
  );
});

// PUT /api/banners/:id/link - Set or clear an existing banner's optional CTA.
bannerRoutes.put("/:id/link", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({})) as Record<string, unknown>;
  const link = normalizeBannerLink(body.linkUrl, body.linkLabel);
  if (!link) {
    return c.json({ success: false, error: "Provide a valid HTTP(S) link and a label of 1 to 48 characters, or clear both fields" }, 400);
  }

  const db = c.env.DB;
  const current = await db.prepare("SELECT id FROM home_banners WHERE id = ?").bind(id).first<{ id: string }>();
  if (!current) return c.json({ success: false, error: "Banner not found" }, 404);

  await db.prepare("UPDATE home_banners SET link_url = ?, link_label = ? WHERE id = ?")
    .bind(link.url, link.label, id)
    .run();
  return c.json({ success: true, data: { linkUrl: link.url, linkLabel: link.label } });
});

// DELETE /api/banners/:id - Delete banner and purge file (Admin)
bannerRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  const banner = await db
    .prepare("SELECT storage_path FROM home_banners WHERE id = ?")
    .bind(id)
    .first<HomeBannerRow>();

  if (!banner) return c.json({ success: false, error: 'Banner not found' }, 404);
  await db.prepare("DELETE FROM home_banners WHERE id = ?").bind(id).run();
  if (banner.storage_path && !await cleanupMedia(c.env, banner.storage_path, c.res.headers.get('X-Request-ID'))) {
    return c.json({ success: true, message: 'Banner removed. Media cleanup was not completed; check System Monitor.' });
  }
  return c.json({ success: true });
});

// PUT /api/banners/:id/reorder - Swap order with adjacent banner (Admin)
bannerRoutes.put("/:id/reorder", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));
  const moveUp = Boolean(body.moveUp);
  const db = c.env.DB;

  const { results: banners } = await db
    .prepare("SELECT id, order_index FROM home_banners ORDER BY order_index ASC")
    .all<{ id: string; order_index: number }>();

  if (!banners || banners.length < 2) {
    return c.json({ success: true });
  }

  const index = banners.findIndex((b) => b.id === id);
  if (index === -1) {
    return c.json({ success: false, error: "Banner not found" }, 404);
  }

  const targetIndex = moveUp ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= banners.length) {
    return c.json({ success: true });
  }

  const current = banners[index];
  const target = banners[targetIndex];

  await db.batch([
    db.prepare("UPDATE home_banners SET order_index = ? WHERE id = ?").bind(target.order_index, current.id),
    db.prepare("UPDATE home_banners SET order_index = ? WHERE id = ?").bind(current.order_index, target.id),
  ]);

  return c.json({ success: true });
});
