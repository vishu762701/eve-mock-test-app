// ============================================================================
// Home Promotional Banners (Supabase Media Integration)
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { SupabaseStorage } from "../supabase";
import { AuthUser, Env, HomeBannerRow } from "../types";

export const bannerRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

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

  if (contentType.includes("multipart/form-data")) {
    const formData = await c.req.formData();
    const file = formData.get("file") as File | null;
    if (!file) {
      return c.json({ success: false, error: "No image file provided" }, 400);
    }
    imageBytes = await file.arrayBuffer();
    mime = file.type || "image/jpeg";
    ext = mime.includes("png") ? "png" : "jpg";
  } else if (contentType.includes("image/")) {
    mime = contentType.split(";")[0];
    ext = mime.includes("png") ? "png" : "jpg";
    imageBytes = await c.req.arrayBuffer();
  } else {
    // Alternatively accept base64 payload
    const body = await c.req.json().catch(() => ({}));
    if (!body.base64) {
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
  }

  // Validate size <= 5MB
  if (imageBytes.byteLength > 5 * 1024 * 1024) {
    return c.json({ success: false, error: "Banner image exceeds 5MB limit" }, 400);
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
      `INSERT INTO home_banners (id, image_url, storage_path, order_index, uploaded_at, uploaded_by, active)
       VALUES (?, ?, ?, ?, ?, ?, 1)`
    )
    .bind(id, imageUrl, storagePath, nextOrder, now, user.email)
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
      },
    },
    201
  );
});

// DELETE /api/banners/:id - Delete banner and purge file (Admin)
bannerRoutes.delete("/:id", requireAdmin, async (c) => {
  const id = c.req.param("id");
  const db = c.env.DB;

  const banner = await db
    .prepare("SELECT storage_path FROM home_banners WHERE id = ?")
    .bind(id)
    .first<HomeBannerRow>();

  if (banner && banner.storage_path) {
    try {
      const storage = new SupabaseStorage(c.env);
      await storage.deleteFile(banner.storage_path);
    } catch (_e) {}
  }

  await db.prepare("DELETE FROM home_banners WHERE id = ?").bind(id).run();
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
