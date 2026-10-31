// ============================================================================
// Supabase Storage REST Helper (Bucket: eve-media)
// ============================================================================

import { Env } from "./types";

export class SupabaseStorage {
  private env: Env;

  constructor(env: Env) {
    this.env = env;
  }

  getPublicUrl(path: string): string {
    const cleanPath = path.startsWith("/") ? path.slice(1) : path;
    return `${this.env.SUPABASE_PROJECT_URL}/storage/v1/object/public/${this.env.SUPABASE_BUCKET_NAME}/${cleanPath}`;
  }

  async uploadFile(path: string, data: ArrayBuffer | Uint8Array, contentType: string): Promise<string> {
    if (!this.env.SUPABASE_SERVICE_ROLE_KEY) {
      throw new Error(
        "SUPABASE_SERVICE_ROLE_KEY is not configured on the Worker. Please set it as a Cloudflare Worker secret."
      );
    }

    const cleanKey = this.env.SUPABASE_SERVICE_ROLE_KEY.trim();
    const cleanPath = path.startsWith("/") ? path.slice(1) : path;
    const url = `${this.env.SUPABASE_PROJECT_URL}/storage/v1/object/${this.env.SUPABASE_BUCKET_NAME}/${cleanPath}`;

    const res = await fetch(url, {
      method: "POST",
      headers: {
        apikey: cleanKey,
        Authorization: `Bearer ${cleanKey}`,
        "Content-Type": contentType,
        "x-upsert": "true",
      },
      body: data,
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Supabase Storage upload failed (${res.status}): ${errText}`);
    }

    return this.getPublicUrl(cleanPath);
  }

  /** Official Storage DELETE /object/:bucketName accepts { prefixes }; max 1000.
   * https://supabase.com/docs/guides/storage/management/delete-objects */
  async deleteFiles(paths: string[]): Promise<void> {
    if (!paths.length) return;
    const key = this.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
    if (!key) throw new Error("Storage credentials are unavailable");
    const unique = [...new Set(paths.map(path => path.replace(/^\/+/, "")).filter(Boolean))];
    for (let offset = 0; offset < unique.length; offset += 1000) {
      const response = await fetch(`${this.env.SUPABASE_PROJECT_URL}/storage/v1/object/${this.env.SUPABASE_BUCKET_NAME}`, {
        method: "DELETE", signal: AbortSignal.timeout(20000),
        headers: { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
        body: JSON.stringify({ prefixes: unique.slice(offset, offset + 1000) }),
      });
      if (!response.ok) throw new Error("Storage cleanup could not be completed");
    }
  }

  async deleteFile(path: string): Promise<void> {
    if (!this.env.SUPABASE_SERVICE_ROLE_KEY) {
      throw new Error(
        "SUPABASE_SERVICE_ROLE_KEY is not configured on the Worker. Please set it as a Cloudflare Worker secret."
      );
    }

    const cleanKey = this.env.SUPABASE_SERVICE_ROLE_KEY.trim();
    const cleanPath = path.startsWith("/") ? path.slice(1) : path;
    const url = `${this.env.SUPABASE_PROJECT_URL}/storage/v1/object/${this.env.SUPABASE_BUCKET_NAME}/${cleanPath}`;

    const res = await fetch(url, {
      method: "DELETE",
      headers: {
        apikey: cleanKey,
        Authorization: `Bearer ${cleanKey}`,
      },
    });

    if (!res.ok && res.status !== 404) {
      const errText = await res.text();
      throw new Error(`Supabase Storage delete failed (${res.status}): ${errText}`);
    }
  }
}
