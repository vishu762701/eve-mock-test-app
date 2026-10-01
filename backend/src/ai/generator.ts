// ============================================================================
// Gemini AI Question Generation Engine
// ============================================================================

import { Env } from "../types";

export interface GeneratedQuestionItem {
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: string;
  explanation: string;
}

export function incrementTestNumber(current: string): string {
  const str = String(current || "Test 1").trim();
  const match = str.match(/(\d+)$/);
  if (match) {
    const num = parseInt(match[1], 10);
    const prefix = str.substring(0, match.index);
    return `${prefix}${num + 1}`;
  }
  return `${str} 2`;
}

export function getIstTimeAndDate(): { todayDate: string; currentTime: string } {
  const now = new Date();
  const dateFormatter = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata" }); // YYYY-MM-DD
  const todayDate = dateFormatter.format(now);

  const timeFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Kolkata",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  const currentTime = timeFormatter.format(now); // "HH:mm"
  return { todayDate, currentTime };
}

function buildPrompt(
  examName: string,
  syllabusText: string,
  questionCount: number,
  customPromptNotes: string
): string {
  return `You are an expert question-setter for ${examName}, a well-known Indian competitive/entrance exam.
Generate exactly ${questionCount} unique, high-quality multiple-choice questions strictly adhering to this exam pattern and syllabus:
${syllabusText || "General competitive exam topics including General Awareness, Reasoning, Quantitative Aptitude, and English Comprehension."}
Admin instructions & focus areas: ${customPromptNotes || "Ensure standard exam difficulty and balanced topic coverage."}

Rules:
1. Each question must match the real competitive exam format and difficulty level.
2. Factually verify each question and step-by-step solve before matching to options.
3. Every question must have 4 distinct, plausible options (A, B, C, D).
4. Provide a detailed, pedagogical explanation (3-4 sentences) for why the correct answer is right.
5. Output ONLY a valid JSON array of objects with the exact schema below. No markdown fences, no surrounding prose.

JSON Schema:
[
  {
    "questionText": "...",
    "optionA": "...",
    "optionB": "...",
    "optionC": "...",
    "optionD": "...",
    "correctAnswer": "A",
    "explanation": "..."
  }
]`;
}

function parseAndValidateQuestions(rawText: string): GeneratedQuestionItem[] {
  let cleaned = rawText.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.replace(/^```json\s*/, "").replace(/\s*```$/, "");
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```\s*/, "").replace(/\s*```$/, "");
  }

  let parsed: any;
  try {
    parsed = JSON.parse(cleaned);
  } catch (err: any) {
    throw new Error(`Invalid JSON from Gemini: ${err.message}`);
  }

  const list: any[] = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.questions)
    ? parsed.questions
    : [];

  const validQuestions: GeneratedQuestionItem[] = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  for (const q of list) {
    const questionText = String(q.questionText || q.question || "").trim();
    const optionA = String(q.optionA || q.a || "").trim();
    const optionB = String(q.optionB || q.b || "").trim();
    const optionC = String(q.optionC || q.c || "").trim();
    const optionD = String(q.optionD || q.d || "").trim();
    let correctAnswer = String(q.correctAnswer || q.answer || "").trim().toUpperCase();

    if (!validAnswers.has(correctAnswer)) {
      if (correctAnswer === "1" || correctAnswer === optionA.toUpperCase()) correctAnswer = "A";
      else if (correctAnswer === "2" || correctAnswer === optionB.toUpperCase()) correctAnswer = "B";
      else if (correctAnswer === "3" || correctAnswer === optionC.toUpperCase()) correctAnswer = "C";
      else if (correctAnswer === "4" || correctAnswer === optionD.toUpperCase()) correctAnswer = "D";
      else correctAnswer = "A";
    }

    const explanation = String(q.explanation || "").trim();

    if (!questionText || !optionA || !optionB || !optionC || !optionD) {
      continue;
    }

    const uniqueOptions = new Set([
      optionA.toLowerCase(),
      optionB.toLowerCase(),
      optionC.toLowerCase(),
      optionD.toLowerCase(),
    ]);
    if (uniqueOptions.size < 4) {
      continue;
    }

    validQuestions.push({
      questionText,
      optionA,
      optionB,
      optionC,
      optionD,
      correctAnswer,
      explanation: explanation || `Option ${correctAnswer} is the correct answer.`,
    });
  }

  return validQuestions;
}

export enum GeminiErrorCode {
  CONFIGURATION_MISSING = "CONFIGURATION_MISSING",
  AUTH_ACCOUNT_INVALID = "AUTH_ACCOUNT_INVALID",
  AUTH_INVALID = "AUTH_INVALID",
  PERMISSION_DENIED = "PERMISSION_DENIED",
  QUOTA_EXCEEDED = "QUOTA_EXCEEDED",
  RATE_LIMITED = "RATE_LIMITED",
  MODEL_NOT_FOUND = "MODEL_NOT_FOUND",
  TEMPORARY_PROVIDER_ERROR = "TEMPORARY_PROVIDER_ERROR",
  NETWORK_ERROR = "NETWORK_ERROR",
  INVALID_GEMINI_RESPONSE = "INVALID_GEMINI_RESPONSE",
  INVALID_GENERATED_QUESTIONS = "INVALID_GENERATED_QUESTIONS",
}

export class GeminiProviderError extends Error {
  code: GeminiErrorCode;
  httpStatus: number;
  userFacingMessage: string;
  isCredentialFailure: boolean;

  constructor(
    code: GeminiErrorCode,
    httpStatus: number,
    userFacingMessage: string,
    internalDetails?: string
  ) {
    super(userFacingMessage);
    this.name = "GeminiProviderError";
    this.code = code;
    this.httpStatus = httpStatus;
    this.userFacingMessage = userFacingMessage;
    this.isCredentialFailure =
      code === GeminiErrorCode.AUTH_ACCOUNT_INVALID ||
      code === GeminiErrorCode.AUTH_INVALID ||
      code === GeminiErrorCode.PERMISSION_DENIED;
  }
}

export function getGeminiApiKeys(env: Env): string[] {
  const keys: string[] = [];
  if (env.GEMINI_API_KEY) keys.push(env.GEMINI_API_KEY.trim());
  if (env.GEMINI_API_KEY_1) keys.push(env.GEMINI_API_KEY_1.trim());
  if (env.GEMINI_API_KEY_2) keys.push(env.GEMINI_API_KEY_2.trim());
  if (env.GEMINI_API_KEY_3) keys.push(env.GEMINI_API_KEY_3.trim());
  if (env.GEMINI_API_KEY_4) keys.push(env.GEMINI_API_KEY_4.trim());
  if (env.GEMINI_API_KEYS) {
    const list = env.GEMINI_API_KEYS.split(/[,;\s]+/).map((k) => k.trim()).filter(Boolean);
    keys.push(...list);
  }
  return Array.from(new Set(keys.filter((k) => k.length > 0)));
}

export function classifyGeminiError(status: number, errText: string): GeminiProviderError {
  const lower = (errText || "").toLowerCase();

  if (status === 401) {
    if (
      lower.includes("account_state_invalid") ||
      lower.includes("deleted or disabled") ||
      lower.includes("service account")
    ) {
      return new GeminiProviderError(
        GeminiErrorCode.AUTH_ACCOUNT_INVALID,
        401,
        "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid."
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.AUTH_INVALID,
      401,
      "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid."
    );
  }

  if (status === 400) {
    if (lower.includes("api_key_invalid") || (lower.includes("invalid_argument") && lower.includes("key"))) {
      return new GeminiProviderError(
        GeminiErrorCode.AUTH_INVALID,
        400,
        "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid."
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.INVALID_GEMINI_RESPONSE,
      400,
      "AI generation request was invalid or rejected by provider."
    );
  }

  if (status === 403) {
    return new GeminiProviderError(
      GeminiErrorCode.PERMISSION_DENIED,
      403,
      "AI generation is temporarily unavailable due to insufficient permissions on the AI service."
    );
  }

  if (status === 404) {
    return new GeminiProviderError(
      GeminiErrorCode.MODEL_NOT_FOUND,
      404,
      "The configured AI generation model is currently unavailable."
    );
  }

  if (status === 429) {
    if (lower.includes("resource_exhausted") || lower.includes("quota")) {
      return new GeminiProviderError(
        GeminiErrorCode.QUOTA_EXCEEDED,
        429,
        "AI generation quota has been reached. Please try again later."
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.RATE_LIMITED,
      429,
      "AI generation is currently rate-limited. Please wait a moment and try again."
    );
  }

  if (status >= 500 && status <= 599) {
    return new GeminiProviderError(
      GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
      status,
      "AI service is temporarily unavailable. Please try again shortly."
    );
  }

  return new GeminiProviderError(
    GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
    status,
    "AI service is temporarily unavailable. Please try again shortly."
  );
}

export async function generateQuestions(
  env: Env,
  examName: string,
  syllabus: string,
  targetCount: number,
  customPrompt: string
): Promise<GeneratedQuestionItem[]> {
  const candidateKeys = getGeminiApiKeys(env);
  if (candidateKeys.length === 0) {
    throw new GeminiProviderError(
      GeminiErrorCode.CONFIGURATION_MISSING,
      500,
      "AI generation is not configured correctly. Please contact the administrator."
    );
  }

  const authoritativeModel = (env.GEMINI_MODEL || "gemini-3.5-flash-lite").trim();
  const CHUNK_SIZE = 25;
  const numChunks = Math.max(1, Math.ceil(targetCount / CHUNK_SIZE));
  const collected: GeneratedQuestionItem[] = [];
  const seenTexts = new Set<string>();

  for (let c = 0; c < numChunks; c++) {
    const countForChunk = Math.min(CHUNK_SIZE, targetCount - collected.length);
    if (countForChunk <= 0) break;

    const prompt = buildPrompt(examName, syllabus, countForChunk, customPrompt);
    const body = {
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        responseMimeType: "application/json",
      },
    };

    let chunkSuccess = false;
    let lastError: GeminiProviderError | Error | null = null;
    const correlationId = crypto.randomUUID();

    keyLoop: for (let keyIdx = 0; keyIdx < candidateKeys.length; keyIdx++) {
      const apiKey = candidateKeys[keyIdx];
      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(authoritativeModel)}:generateContent`;

      try {
        const res = await fetch(endpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey,
          },
          body: JSON.stringify(body),
        });

        if (!res.ok) {
          const errText = await res.text();
          const providerErr = classifyGeminiError(res.status, errText);
          lastError = providerErr;

          // Safe diagnostic logging: Never log the API key
          console.warn(
            `[Gemini Diagnostics] Slot ${keyIdx} HTTP ${res.status} [${providerErr.code}], model: ${authoritativeModel}, correlationId: ${correlationId}`
          );

          if (providerErr.isCredentialFailure) {
            // Immediately skip dead credential slot and try next slot
            continue keyLoop;
          }
          continue keyLoop;
        }

        const data = (await res.json()) as any;
        const rawJson = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (!rawJson) {
          lastError = new GeminiProviderError(
            GeminiErrorCode.INVALID_GEMINI_RESPONSE,
            500,
            "AI service returned an empty response."
          );
          continue keyLoop;
        }

        const parsed = parseAndValidateQuestions(rawJson);
        if (parsed.length === 0) {
          lastError = new GeminiProviderError(
            GeminiErrorCode.INVALID_GENERATED_QUESTIONS,
            500,
            "Failed to produce valid multiple-choice questions."
          );
          continue keyLoop;
        }

        for (const q of parsed) {
          const norm = q.questionText.trim().toLowerCase();
          if (!seenTexts.has(norm)) {
            seenTexts.add(norm);
            collected.push(q);
          }
        }
        chunkSuccess = true;
        break keyLoop;
      } catch (err: any) {
        if (err instanceof GeminiProviderError) {
          lastError = err;
        } else {
          lastError = new GeminiProviderError(
            GeminiErrorCode.NETWORK_ERROR,
            503,
            "Unable to connect to AI generation service. Please try again."
          );
        }
      }
    }

    if (!chunkSuccess) {
      throw lastError || new GeminiProviderError(
        GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
        500,
        "Failed to generate questions with all configured credentials."
      );
    }
  }

  if (collected.length === 0) {
    throw new GeminiProviderError(
      GeminiErrorCode.INVALID_GENERATED_QUESTIONS,
      500,
      `Failed to generate valid questions for ${examName}.`
    );
  }

  return collected;
}
