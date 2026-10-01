import { ExamRow } from "../types";

export type PublishStatus = "live" | "paused";

export async function resolvePublishStatus(
  db: D1Database,
  exam: { publish_mode?: string; [key: string]: any } | ExamRow
): Promise<PublishStatus> {
  const examMode = (exam.publish_mode || "").trim().toLowerCase();
  if (examMode === "live") return "live";
  if (examMode === "paused") return "paused";

  try {
    const row = await db
      .prepare("SELECT body FROM app_content WHERE id = 'app_config'")
      .first<{ body: string }>();

    if (row && row.body) {
      const config = JSON.parse(row.body);
      const defaultMode = (config.default_publish_mode || "").trim().toLowerCase();
      if (defaultMode === "live") return "live";
      if (defaultMode === "paused") return "paused";
    }
  } catch (_e) {
    // Missing or invalid config -> fallback to paused
  }

  return "paused";
}
