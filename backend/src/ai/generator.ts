// ============================================================================
// Gemini AI Question Generation Engine (Self-Healing & Tolerant)
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

export function redactSecrets(text: string, keysToRedact: string[] = []): string {
  if (!text) return "";
  let result = text.replace(/AIza[0-9A-Za-z_-]{20,}/g, "[REDACTED_KEY]");
  for (const k of keysToRedact) {
    if (k && k.length > 5) {
      result = result.split(k).join("[REDACTED_KEY]");
    }
  }
  return result;
}

export interface ParsedGeminiErrorBody {
  code?: number;
  message?: string;
  status?: string;
}

export function parseGeminiErrorBody(rawText: string): ParsedGeminiErrorBody {
  try {
    const data = JSON.parse(rawText);
    if (data && typeof data === "object") {
      if (data.error && typeof data.error === "object") {
        return {
          code: typeof data.error.code === "number" ? data.error.code : undefined,
          message: typeof data.error.message === "string" ? data.error.message : undefined,
          status: typeof data.error.status === "string" ? data.error.status : undefined,
        };
      }
    }
  } catch {
    // Non-JSON error payload
  }
  return {};
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

/**
 * Tolerant JSON parser for Gemini outputs:
 * 1. Strips markdown code fences (```json ... ``` or ``` ... ```)
 * 2. Extracts outermost JSON array [...] or object { questions: [...] }
 * 3. Ignores preamble or trailing text
 * 4. Normalizes answers and verifies 4 distinct options
 */
export function parseAndValidateQuestions(rawText: string): GeneratedQuestionItem[] {
  if (!rawText || !rawText.trim()) {
    throw new Error("Empty text from AI provider");
  }

  let text = rawText.trim();

  // Strip code fences if present
  const fenceMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/);
  if (fenceMatch) {
    text = fenceMatch[1].trim();
  }

  let parsed: any = null;
  try {
    parsed = JSON.parse(text);
  } catch {
    // Extract outermost JSON array [...]
    const firstBracket = text.indexOf("[");
    const lastBracket = text.lastIndexOf("]");
    if (firstBracket !== -1 && lastBracket > firstBracket) {
      try {
        parsed = JSON.parse(text.substring(firstBracket, lastBracket + 1));
      } catch {
        // Fall through
      }
    }

    // Extract outermost JSON object {...} (e.g. { questions: [...] })
    if (!parsed) {
      const firstBrace = text.indexOf("{");
      const lastBrace = text.lastIndexOf("}");
      if (firstBrace !== -1 && lastBrace > firstBrace) {
        try {
          parsed = JSON.parse(text.substring(firstBrace, lastBrace + 1));
        } catch {
          // Fall through
        }
      }
    }
  }

  if (!parsed) {
    throw new Error("Failed to extract valid JSON from Gemini output");
  }

  const list: any[] = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.questions)
    ? parsed.questions
    : Array.isArray(parsed.data)
    ? parsed.data
    : [];

  if (!Array.isArray(list) || list.length === 0) {
    throw new Error("Gemini response did not contain an array of questions");
  }

  const validQuestions: GeneratedQuestionItem[] = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  const field = (value: unknown): string => {
    if (value === undefined || value === null) return "";
    if (typeof value !== "string") throw new Error("Question text and options must be strings");
    return value.trim();
  };
  const seen = new Set<string>();
  for (const q of list) {
    if (!q || typeof q !== "object") throw new Error("Invalid question object");

    const questionText = field(q.questionText || q.question_text || q.question || "");
    const optionA = field(q.optionA || q.option_a || q.a || q.options?.A || q.options?.[0] || "");
    const optionB = field(q.optionB || q.option_b || q.b || q.options?.B || q.options?.[1] || "");
    const optionC = field(q.optionC || q.option_c || q.c || q.options?.C || q.options?.[2] || "");
    const optionD = field(q.optionD || q.option_d || q.d || q.options?.D || q.options?.[3] || "");

    let correctAnswer = String(q.correctAnswer || q.answer || q.correct_answer || "").trim().toUpperCase();

    if (!validAnswers.has(correctAnswer)) {
      if (correctAnswer === "1" || correctAnswer === optionA.toUpperCase()) correctAnswer = "A";
      else if (correctAnswer === "2" || correctAnswer === optionB.toUpperCase()) correctAnswer = "B";
      else if (correctAnswer === "3" || correctAnswer === optionC.toUpperCase()) correctAnswer = "C";
      else if (correctAnswer === "4" || correctAnswer === optionD.toUpperCase()) correctAnswer = "D";
      else throw new Error("Invalid correct answer; expected A, B, C or D");
    }

    const explanation = String(q.explanation || "").trim();

    if (!questionText || !optionA || !optionB || !optionC || !optionD) {
      throw new Error("Missing question text or option");
    }

    const uniqueOptions = new Set([
      optionA.toLowerCase(),
      optionB.toLowerCase(),
      optionC.toLowerCase(),
      optionD.toLowerCase(),
    ]);
    if (uniqueOptions.size < 4) {
      throw new Error("Question options must be distinct");
    }

    const normalized = questionText.toLowerCase().replace(/\s+/g, " ");
    if (seen.has(normalized)) throw new Error("Duplicate question in AI response");
    seen.add(normalized);
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
  FAILED_PRECONDITION = "FAILED_PRECONDITION",
  QUOTA_EXCEEDED = "QUOTA_EXCEEDED",
  RATE_LIMITED = "RATE_LIMITED",
  MODEL_NOT_FOUND = "MODEL_NOT_FOUND",
  TEMPORARY_PROVIDER_ERROR = "TEMPORARY_PROVIDER_ERROR",
  NETWORK_ERROR = "NETWORK_ERROR",
  INVALID_GEMINI_RESPONSE = "INVALID_GEMINI_RESPONSE",
  INVALID_GENERATED_QUESTIONS = "INVALID_GENERATED_QUESTIONS",
  SAFETY_BLOCKED = "SAFETY_BLOCKED",
  TRUNCATED_OUTPUT = "TRUNCATED_OUTPUT",
}

export class GeminiProviderError extends Error {
  code: GeminiErrorCode;
  httpStatus: number;
  userFacingMessage: string;
  isCredentialFailure: boolean;
  isPreconditionFailure: boolean;
  isTransient: boolean;
  providerStatus?: string;
  providerMessage?: string;
  correlationId?: string;
  model?: string;
  attempt?: number;

  constructor(
    code: GeminiErrorCode,
    httpStatus: number,
    userFacingMessage: string,
    options?: {
      providerStatus?: string;
      providerMessage?: string;
      correlationId?: string;
      model?: string;
      attempt?: number;
      isCredentialFailure?: boolean;
      isPreconditionFailure?: boolean;
      isTransient?: boolean;
    }
  ) {
    super(userFacingMessage);
    this.name = "GeminiProviderError";
    this.code = code;
    this.httpStatus = httpStatus;
    this.userFacingMessage = userFacingMessage;
    this.providerStatus = options?.providerStatus;
    this.providerMessage = options?.providerMessage;
    this.correlationId = options?.correlationId;
    this.model = options?.model;
    this.attempt = options?.attempt;
    this.isCredentialFailure =
      options?.isCredentialFailure ??
      (code === GeminiErrorCode.AUTH_ACCOUNT_INVALID ||
        code === GeminiErrorCode.AUTH_INVALID ||
        code === GeminiErrorCode.PERMISSION_DENIED);
    this.isPreconditionFailure =
      options?.isPreconditionFailure ?? (code === GeminiErrorCode.FAILED_PRECONDITION);
    this.isTransient =
      options?.isTransient ??
      (code === GeminiErrorCode.RATE_LIMITED ||
        code === GeminiErrorCode.TEMPORARY_PROVIDER_ERROR ||
        code === GeminiErrorCode.NETWORK_ERROR ||
        httpStatus === 429 ||
        (httpStatus >= 500 && httpStatus <= 599));
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

/**
 * Resolves the candidate model fallback chain:
 * Primary: env.GEMINI_MODEL (default: gemini-3.5-flash-lite)
 * Fallbacks: env.GEMINI_FALLBACK_MODELS (default: "gemini-3.1-flash-lite,gemini-3.5-flash")
 * Explicitly excludes 2.0 / 2.5 models as instructed.
 */
export function getModelChain(env: Env): string[] {
  const primary = (env.GEMINI_MODEL || "gemini-3.5-flash-lite").trim();
  const fallbacksStr = env.GEMINI_FALLBACK_MODELS || "gemini-3.1-flash-lite,gemini-3.5-flash";
  const fallbacks = fallbacksStr.split(/[,;\s]+/).map((m) => m.trim()).filter(Boolean);

  const rawList = [primary, ...fallbacks];
  // Do NOT use 2.0/2.5 models
  const isExcluded = (m: string) => /gemini-2\.[05]/i.test(m);

  const filtered = rawList.filter((m) => m.length > 0 && !isExcluded(m));
  const unique = Array.from(new Set(filtered));

  return unique.length > 0 ? unique : ["gemini-3.5-flash-lite"];
}

/**
 * Classifies Gemini errors by provider status first, then HTTP status code.
 * Sanitizes and caps providerMessage to 300 characters, redacting any API keys.
 */
export function classifyGeminiError(
  status: number,
  errText: string,
  meta?: { correlationId?: string; model?: string; attempt?: number; keysToRedact?: string[] }
): GeminiProviderError {
  const parsed = parseGeminiErrorBody(errText);
  const rawMsg = parsed.message || errText || "";
  const sanitizedMsg = redactSecrets(rawMsg, meta?.keysToRedact).slice(0, 300);
  const providerStatus = (parsed.status || "").toUpperCase().trim();
  const lowerMsg = sanitizedMsg.toLowerCase();

  const baseOptions = {
    providerStatus: providerStatus || undefined,
    providerMessage: sanitizedMsg || undefined,
    correlationId: meta?.correlationId,
    model: meta?.model,
    attempt: meta?.attempt,
  };

  // 1. Classify by provider status first
  if (providerStatus === "FAILED_PRECONDITION") {
    let msg = "AI generation is unavailable due to account precondition requirements (e.g. region availability or billing).";
    if (lowerMsg.includes("location") || lowerMsg.includes("region") || lowerMsg.includes("country")) {
      msg = "AI generation is not supported in the current service region (FAILED_PRECONDITION).";
    } else if (lowerMsg.includes("billing")) {
      msg = "AI generation requires billing to be enabled on the Google Cloud project (FAILED_PRECONDITION).";
    }
    return new GeminiProviderError(
      GeminiErrorCode.FAILED_PRECONDITION,
      status || 400,
      msg,
      { ...baseOptions, isPreconditionFailure: true }
    );
  }

  if (providerStatus === "INVALID_ARGUMENT") {
    if (
      lowerMsg.includes("api_key_invalid") ||
      lowerMsg.includes("api key not valid") ||
      (lowerMsg.includes("invalid") && lowerMsg.includes("key"))
    ) {
      return new GeminiProviderError(
        GeminiErrorCode.AUTH_INVALID,
        status || 400,
        "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid.",
        { ...baseOptions, isCredentialFailure: true }
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.INVALID_GEMINI_RESPONSE,
      status || 400,
      "AI generation request was invalid or rejected by provider.",
      baseOptions
    );
  }

  if (providerStatus === "UNAUTHENTICATED") {
    return new GeminiProviderError(
      GeminiErrorCode.AUTH_INVALID,
      status || 401,
      "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid.",
      { ...baseOptions, isCredentialFailure: true }
    );
  }

  if (providerStatus === "PERMISSION_DENIED") {
    return new GeminiProviderError(
      GeminiErrorCode.PERMISSION_DENIED,
      status || 403,
      "AI generation is temporarily unavailable due to insufficient permissions on the AI service.",
      { ...baseOptions, isCredentialFailure: true }
    );
  }

  if (providerStatus === "RESOURCE_EXHAUSTED") {
    if (lowerMsg.includes("quota")) {
      return new GeminiProviderError(
        GeminiErrorCode.QUOTA_EXCEEDED,
        status || 429,
        "AI generation quota has been reached. Please try again later.",
        { ...baseOptions, isTransient: true }
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.RATE_LIMITED,
      status || 429,
      "AI generation is currently rate-limited. Please wait a moment and try again.",
      { ...baseOptions, isTransient: true }
    );
  }

  if (providerStatus === "NOT_FOUND") {
    return new GeminiProviderError(
      GeminiErrorCode.MODEL_NOT_FOUND,
      status || 404,
      "The configured AI generation model is currently unavailable.",
      baseOptions
    );
  }

  // 2. Classify by HTTP status code fallback
  if (status === 401) {
    if (
      lowerMsg.includes("account_state_invalid") ||
      lowerMsg.includes("deleted or disabled") ||
      lowerMsg.includes("service account")
    ) {
      return new GeminiProviderError(
        GeminiErrorCode.AUTH_ACCOUNT_INVALID,
        401,
        "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid.",
        { ...baseOptions, isCredentialFailure: true }
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.AUTH_INVALID,
      401,
      "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid.",
      { ...baseOptions, isCredentialFailure: true }
    );
  }

  if (status === 400) {
    if (
      lowerMsg.includes("api_key_invalid") ||
      lowerMsg.includes("api key not valid") ||
      (lowerMsg.includes("invalid") && lowerMsg.includes("key"))
    ) {
      return new GeminiProviderError(
        GeminiErrorCode.AUTH_INVALID,
        400,
        "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid.",
        { ...baseOptions, isCredentialFailure: true }
      );
    }
    if (lowerMsg.includes("failed_precondition") || lowerMsg.includes("location") || lowerMsg.includes("billing")) {
      return new GeminiProviderError(
        GeminiErrorCode.FAILED_PRECONDITION,
        400,
        "AI generation is unavailable due to account precondition requirements (e.g. region availability or billing).",
        { ...baseOptions, isPreconditionFailure: true }
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.INVALID_GEMINI_RESPONSE,
      400,
      "AI generation request was invalid or rejected by provider.",
      baseOptions
    );
  }

  if (status === 403) {
    return new GeminiProviderError(
      GeminiErrorCode.PERMISSION_DENIED,
      403,
      "AI generation is temporarily unavailable due to insufficient permissions on the AI service.",
      { ...baseOptions, isCredentialFailure: true }
    );
  }

  if (status === 404) {
    return new GeminiProviderError(
      GeminiErrorCode.MODEL_NOT_FOUND,
      404,
      "The configured AI generation model is currently unavailable.",
      baseOptions
    );
  }

  if (status === 429) {
    if (lowerMsg.includes("resource_exhausted") || lowerMsg.includes("quota")) {
      return new GeminiProviderError(
        GeminiErrorCode.QUOTA_EXCEEDED,
        429,
        "AI generation quota has been reached. Please try again later.",
        { ...baseOptions, isTransient: true }
      );
    }
    return new GeminiProviderError(
      GeminiErrorCode.RATE_LIMITED,
      429,
      "AI generation is currently rate-limited. Please wait a moment and try again.",
      { ...baseOptions, isTransient: true }
    );
  }

  if (status >= 500 && status <= 599) {
    return new GeminiProviderError(
      GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
      status,
      "AI service is temporarily unavailable. Please try again shortly.",
      { ...baseOptions, isTransient: true }
    );
  }

  return new GeminiProviderError(
    GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
    status,
    "AI service is temporarily unavailable. Please try again shortly.",
    baseOptions
  );
}

function parseRetryAfterMs(headerVal: string | null): number {
  if (!headerVal) return 0;
  const numSec = parseFloat(headerVal);
  if (!isNaN(numSec) && numSec > 0) {
    return Math.min(10000, Math.round(numSec * 1000));
  }
  const dateMs = Date.parse(headerVal);
  if (!isNaN(dateMs)) {
    const diff = dateMs - Date.now();
    return diff > 0 ? Math.min(10000, diff) : 0;
  }
  return 0;
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Self-healing AI question generation engine.
 * - Chunk size: 10
 * - Exponential backoff + jitter for transient failures (429, 5xx, network)
 * - Model fallback chain
 * - Tolerant parsing and retry without responseMimeType on 400/404
 * - Immediate stop on FAILED_PRECONDITION
 * - Max 120s overall budget
 * - Remainder requesting (up to 2 extra rounds) for target question count
 */
export async function generateQuestions(
  env: Env,
  examName: string,
  syllabus: string,
  targetCount: number,
  customPrompt: string,
  fetchFn: typeof fetch = fetch
): Promise<GeneratedQuestionItem[]> {
  if (!Number.isInteger(targetCount) || targetCount < 1 || targetCount > 200) {
    throw new Error("questionCount must be an integer between 1 and 200");
  }
  const candidateKeys = getGeminiApiKeys(env);
  if (candidateKeys.length === 0) {
    throw new GeminiProviderError(
      GeminiErrorCode.CONFIGURATION_MISSING,
      500,
      "AI generation is not configured correctly. Please contact the administrator."
    );
  }

  const modelChain = getModelChain(env);
  const CHUNK_SIZE = 10;
  const collected: GeneratedQuestionItem[] = [];
  const seenTexts = new Set<string>();

  const startTime = Date.now();
  const TIME_BUDGET_MS = 120_000; // 120 seconds
  const correlationId = crypto.randomUUID();

  let keyIndex = 0;
  let extraRounds = 0;
  const MAX_EXTRA_ROUNDS = 2;

  while (collected.length < targetCount) {
    if (Date.now() - startTime >= TIME_BUDGET_MS) {
      throw new GeminiProviderError(
        GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
        504,
        "AI question generation timed out after 120 seconds. Please try again.",
        { correlationId }
      );
    }

    const neededCount = Math.min(CHUNK_SIZE, targetCount - collected.length);
    if (neededCount <= 0) break;

    let chunkQuestions: GeneratedQuestionItem[] = [];
    let chunkSuccess = false;
    let lastError: GeminiProviderError | Error | null = null;

    modelLoop: for (let mIdx = 0; mIdx < modelChain.length; mIdx++) {
      const currentModel = modelChain[mIdx];
      let useResponseMimeType = true;
      let didRetryWithoutMimeType = false;
      let currentChunkRequestCount = neededCount;

      attemptLoop: for (let attempt = 1; attempt <= 3; attempt++) {
        if (Date.now() - startTime >= TIME_BUDGET_MS) {
          throw new GeminiProviderError(
            GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
            504,
            "AI question generation timed out after 120 seconds. Please try again.",
            { correlationId, model: currentModel, attempt }
          );
        }

        const apiKey = candidateKeys[keyIndex % candidateKeys.length];
        const prompt = buildPrompt(examName, syllabus, currentChunkRequestCount, customPrompt);
        const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(currentModel)}:generateContent`;

        const body: any = {
          contents: [{ parts: [{ text: prompt }] }],
        };
        if (useResponseMimeType) {
          body.generationConfig = {
            responseMimeType: "application/json",
          };
        }

        try {
          const res = await fetchFn(endpoint, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "x-goog-api-key": apiKey,
            },
            body: JSON.stringify(body),
            signal: AbortSignal.timeout(Math.max(1, Math.min(30_000, TIME_BUDGET_MS - (Date.now() - startTime)))),
          });

          if (!res.ok) {
            const errText = await res.text();
            const providerErr = classifyGeminiError(res.status, errText, {
              correlationId,
              model: currentModel,
              attempt,
              keysToRedact: candidateKeys,
            });
            lastError = providerErr;

            console.warn(
              `[Gemini Diagnostics] Slot ${keyIndex % candidateKeys.length} HTTP ${res.status} [${providerErr.code}], model: ${currentModel}, attempt: ${attempt}, correlationId: ${correlationId}`
            );

            // 1. FAILED_PRECONDITION (region/billing): stop immediately, surface exact reason
            if (providerErr.isPreconditionFailure) {
              throw providerErr;
            }

            // 2. Credential error (401/403/invalid key): advance to next key slot
            if (providerErr.isCredentialFailure) {
              keyIndex++;
              // If there are other keys available, continue attempt with the new key
              if (candidateKeys.length > 1) {
                continue attemptLoop;
              } else {
                // Only 1 key and it is dead -> try next model or fail
                break attemptLoop;
              }
            }

            // 3. 400 INVALID_ARGUMENT or 404: retry once without responseMimeType, then try next model
            if (res.status === 400 || res.status === 404) {
              if (useResponseMimeType && !didRetryWithoutMimeType) {
                useResponseMimeType = false;
                didRetryWithoutMimeType = true;
                continue attemptLoop;
              }
              // Already tried without responseMimeType or other 400/404 -> switch to next model
              break attemptLoop;
            }

            // 4. Transient error (429, 5xx): retry with exponential backoff + jitter, honoring Retry-After
            if (providerErr.isTransient && attempt < 3) {
              const retryAfterMs = parseRetryAfterMs(res.headers.get("Retry-After"));
              const backoffMs = Math.min(5000, 500 * Math.pow(2, attempt - 1) + Math.random() * 300);
              const waitMs = Math.min(Math.max(retryAfterMs, backoffMs), Math.max(0, TIME_BUDGET_MS - (Date.now() - startTime)));
              await sleep(waitMs);
              continue attemptLoop;
            }

            // If not retryable or exhausted 3 attempts on this model, proceed to next model
            break attemptLoop;
          }

          const data = (await res.json()) as any;

          // Check for promptFeedback blockReason (e.g. SAFETY)
          if (data.promptFeedback?.blockReason) {
            const blockReason = data.promptFeedback.blockReason;
            throw new GeminiProviderError(
              GeminiErrorCode.SAFETY_BLOCKED,
              400,
              `The question generation was blocked by AI safety filters (${blockReason}). Please refine the syllabus or prompts.`,
              { correlationId, model: currentModel }
            );
          }

          const candidate = data.candidates?.[0];
          if (!candidate) {
            lastError = new GeminiProviderError(
              GeminiErrorCode.INVALID_GEMINI_RESPONSE,
              500,
              "AI service returned an empty candidate list.",
              { correlationId, model: currentModel, attempt }
            );
            continue attemptLoop;
          }

          // Handle candidate finishReason
          if (candidate.finishReason === "SAFETY") {
            throw new GeminiProviderError(
              GeminiErrorCode.SAFETY_BLOCKED,
              400,
              "The question generation was blocked by AI safety filters.",
              { correlationId, model: currentModel }
            );
          }

          if (candidate.finishReason === "MAX_TOKENS") {
            // Retry with a smaller chunk size if possible
            if (currentChunkRequestCount > 4) {
              currentChunkRequestCount = Math.max(3, Math.floor(currentChunkRequestCount / 2));
              continue attemptLoop;
            }
          }

          const rawText = candidate.content?.parts?.map((part: any) => part.text || "").join("");
          if (!rawText) {
            lastError = new GeminiProviderError(
              GeminiErrorCode.INVALID_GEMINI_RESPONSE,
              500,
              "AI service returned an empty response part.",
              { correlationId, model: currentModel, attempt }
            );
            continue attemptLoop;
          }

          let parsed: GeneratedQuestionItem[];
          try { parsed = parseAndValidateQuestions(rawText); } catch {
            throw new GeminiProviderError(GeminiErrorCode.INVALID_GENERATED_QUESTIONS, 502, "AI returned invalid questions. Please retry.", { correlationId, model: currentModel, attempt, isTransient: true });
          }
          if (parsed.length === 0) {
            lastError = new GeminiProviderError(
              GeminiErrorCode.INVALID_GENERATED_QUESTIONS,
              500,
              "Failed to produce valid multiple-choice questions from AI output.",
              { correlationId, model: currentModel, attempt }
            );
            continue attemptLoop;
          }

          chunkQuestions = parsed;
          chunkSuccess = true;
          break modelLoop;
        } catch (err: any) {
          if (err instanceof GeminiProviderError) {
            lastError = err;
            if (err.isPreconditionFailure) {
              throw err;
            }
          } else {
            lastError = new GeminiProviderError(
              GeminiErrorCode.NETWORK_ERROR,
              503,
              "Unable to connect to AI generation service. Please try again.",
              {
                providerMessage: redactSecrets(err.message, candidateKeys).slice(0, 300),
                correlationId,
                model: currentModel,
                attempt,
                isTransient: true,
              }
            );
          }

          // Transient network failure: backoff and retry up to 3 attempts
          if (attempt < 3) {
            const backoffMs = Math.min(5000, 500 * Math.pow(2, attempt - 1) + Math.random() * 300);
            await sleep(backoffMs);
            continue attemptLoop;
          }
        }
      }
    }

    if (!chunkSuccess) {
      // If we failed to get questions for this chunk, stop loop
      throw lastError || new GeminiProviderError(
        GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
        500,
        "Failed to generate questions with all configured models and credentials.",
        { correlationId }
      );
    }

    let addedFromChunk = 0;
    for (const q of chunkQuestions) {
      const norm = q.questionText.trim().toLowerCase().replace(/\s+/g, " ");
      if (!seenTexts.has(norm)) {
        seenTexts.add(norm);
        collected.push(q);
        addedFromChunk++;
        if (collected.length >= targetCount) break;
      }
    }

    // If chunk returned fewer valid questions than asked and we still haven't met target,
    // allow up to MAX_EXTRA_ROUNDS to request the remainder
    if (collected.length < targetCount && addedFromChunk < neededCount) {
      if (addedFromChunk === 0 || extraRounds >= MAX_EXTRA_ROUNDS) {
        break;
      }
      extraRounds++;
    }
  }

  if (collected.length !== targetCount) {
    throw new GeminiProviderError(
      GeminiErrorCode.INVALID_GENERATED_QUESTIONS,
      500,
      `AI produced ${collected.length} of ${targetCount} required questions. No test was published.`,
      { correlationId, isTransient: true }
    );
  }

  return collected;
}
