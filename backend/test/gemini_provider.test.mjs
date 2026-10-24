import test from "node:test";
import assert from "node:assert/strict";

// Mirror types & classification logic directly from backend/src/ai/generator.ts for Node test runner
const GeminiErrorCode = {
  CONFIGURATION_MISSING: "CONFIGURATION_MISSING",
  AUTH_ACCOUNT_INVALID: "AUTH_ACCOUNT_INVALID",
  AUTH_INVALID: "AUTH_INVALID",
  PERMISSION_DENIED: "PERMISSION_DENIED",
  FAILED_PRECONDITION: "FAILED_PRECONDITION",
  QUOTA_EXCEEDED: "QUOTA_EXCEEDED",
  RATE_LIMITED: "RATE_LIMITED",
  MODEL_NOT_FOUND: "MODEL_NOT_FOUND",
  TEMPORARY_PROVIDER_ERROR: "TEMPORARY_PROVIDER_ERROR",
  NETWORK_ERROR: "NETWORK_ERROR",
  INVALID_GEMINI_RESPONSE: "INVALID_GEMINI_RESPONSE",
  INVALID_GENERATED_QUESTIONS: "INVALID_GENERATED_QUESTIONS",
  SAFETY_BLOCKED: "SAFETY_BLOCKED",
  TRUNCATED_OUTPUT: "TRUNCATED_OUTPUT",
};

class GeminiProviderError extends Error {
  constructor(code, httpStatus, userFacingMessage, options) {
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

function redactSecrets(text, keysToRedact = []) {
  if (!text) return "";
  let result = text.replace(/AIza[0-9A-Za-z_-]{20,}/g, "[REDACTED_KEY]");
  for (const k of keysToRedact) {
    if (k && k.length > 5) {
      result = result.split(k).join("[REDACTED_KEY]");
    }
  }
  return result;
}

function parseGeminiErrorBody(rawText) {
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
    // Non-JSON
  }
  return {};
}

function getGeminiApiKeys(env) {
  const keys = [];
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

function getModelChain(env) {
  const primary = (env.GEMINI_MODEL || "gemini-3.5-flash-lite").trim();
  const fallbacksStr = env.GEMINI_FALLBACK_MODELS || "gemini-3.1-flash-lite,gemini-3.5-flash";
  const fallbacks = fallbacksStr.split(/[,;\s]+/).map((m) => m.trim()).filter(Boolean);

  const rawList = [primary, ...fallbacks];
  const isExcluded = (m) => /gemini-2\.[05]/i.test(m);

  const filtered = rawList.filter((m) => m.length > 0 && !isExcluded(m));
  const unique = Array.from(new Set(filtered));

  return unique.length > 0 ? unique : ["gemini-3.5-flash-lite"];
}

function classifyGeminiError(status, errText, meta) {
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

function parseAndValidateQuestions(rawText) {
  if (!rawText || !rawText.trim()) {
    throw new Error("Empty text from AI provider");
  }

  let text = rawText.trim();
  const fenceMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/);
  if (fenceMatch) {
    text = fenceMatch[1].trim();
  }

  let parsed = null;
  try {
    parsed = JSON.parse(text);
  } catch {
    const firstBracket = text.indexOf("[");
    const lastBracket = text.lastIndexOf("]");
    if (firstBracket !== -1 && lastBracket > firstBracket) {
      try {
        parsed = JSON.parse(text.substring(firstBracket, lastBracket + 1));
      } catch {}
    }

    if (!parsed) {
      const firstBrace = text.indexOf("{");
      const lastBrace = text.lastIndexOf("}");
      if (firstBrace !== -1 && lastBrace > firstBrace) {
        try {
          parsed = JSON.parse(text.substring(firstBrace, lastBrace + 1));
        } catch {}
      }
    }
  }

  if (!parsed) {
    throw new Error(`Failed to extract valid JSON: ${text.slice(0, 150)}`);
  }

  const list = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.questions)
    ? parsed.questions
    : Array.isArray(parsed.data)
    ? parsed.data
    : [];

  const validQuestions = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  for (const q of list) {
    if (!q || typeof q !== "object") continue;

    const questionText = String(q.questionText || q.question || "").trim();
    const optionA = String(q.optionA || q.a || q.options?.A || q.options?.[0] || "").trim();
    const optionB = String(q.optionB || q.b || q.options?.B || q.options?.[1] || "").trim();
    const optionC = String(q.optionC || q.c || q.options?.C || q.options?.[2] || "").trim();
    const optionD = String(q.optionD || q.d || q.options?.D || q.options?.[3] || "").trim();

    let correctAnswer = String(q.correctAnswer || q.answer || q.correct_answer || "").trim().toUpperCase();

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

test("Gemini API key extraction gathers and deduplicates multiple secret slots", () => {
  const env1 = {
    GEMINI_API_KEY: "key-main",
    GEMINI_API_KEY_1: "key-slot-1",
    GEMINI_API_KEY_2: "key-slot-2",
  };
  assert.deepEqual(getGeminiApiKeys(env1), ["key-main", "key-slot-1", "key-slot-2"]);

  const env2 = {
    GEMINI_API_KEYS: "key-a, key-b; key-c\nkey-d",
    GEMINI_API_KEY_1: "key-a", // duplicate
  };
  assert.deepEqual(getGeminiApiKeys(env2), ["key-a", "key-b", "key-c", "key-d"]);

  const envEmpty = {};
  assert.deepEqual(getGeminiApiKeys(envEmpty), []);
});

test("Gemini error classification taxonomy", () => {
  // 1. ACCOUNT_STATE_INVALID
  const rawProvider401 = JSON.stringify({
    error: {
      code: 401,
      message: "The bound service account is deleted or disabled. The service account bound to the API key must be active.",
      status: "ACCOUNT_STATE_INVALID",
    },
  });
  const err1 = classifyGeminiError(401, rawProvider401);
  assert.equal(err1.code, GeminiErrorCode.AUTH_ACCOUNT_INVALID);
  assert.equal(err1.httpStatus, 401);
  assert.equal(err1.isCredentialFailure, true);
  assert.ok(!err1.userFacingMessage.includes("service account")); // Raw provider details masked

  // 2. Generic 401
  const err2 = classifyGeminiError(401, "Unauthorized");
  assert.equal(err2.code, GeminiErrorCode.AUTH_INVALID);
  assert.equal(err2.isCredentialFailure, true);

  // 3. API_KEY_INVALID in 400
  const err3 = classifyGeminiError(400, "API_KEY_INVALID");
  assert.equal(err3.code, GeminiErrorCode.AUTH_INVALID);
  assert.equal(err3.isCredentialFailure, true);

  // 4. 403 Forbidden
  const err4 = classifyGeminiError(403, "Permission Denied");
  assert.equal(err4.code, GeminiErrorCode.PERMISSION_DENIED);
  assert.equal(err4.isCredentialFailure, true);

  // 5. 404 Model Not Found
  const err5 = classifyGeminiError(404, "Model not found");
  assert.equal(err5.code, GeminiErrorCode.MODEL_NOT_FOUND);
  assert.equal(err5.isCredentialFailure, false);

  // 6. 429 Quota Exceeded
  const err6 = classifyGeminiError(429, "RESOURCE_EXHAUSTED: quota exceeded");
  assert.equal(err6.code, GeminiErrorCode.QUOTA_EXCEEDED);
  assert.ok(err6.userFacingMessage.includes("quota"));

  // 7. 429 Rate Limited
  const err7 = classifyGeminiError(429, "Too Many Requests");
  assert.equal(err7.code, GeminiErrorCode.RATE_LIMITED);

  // 8. 503 Provider Error
  const err8 = classifyGeminiError(503, "Service Unavailable");
  assert.equal(err8.code, GeminiErrorCode.TEMPORARY_PROVIDER_ERROR);

  // 9. FAILED_PRECONDITION (region/billing)
  const failedPreconditionRegion = JSON.stringify({
    error: {
      code: 400,
      message: "User location is not supported for the API use.",
      status: "FAILED_PRECONDITION",
    },
  });
  const err9 = classifyGeminiError(400, failedPreconditionRegion);
  assert.equal(err9.code, GeminiErrorCode.FAILED_PRECONDITION);
  assert.equal(err9.isPreconditionFailure, true);
  assert.ok(err9.userFacingMessage.includes("region") || err9.userFacingMessage.includes("FAILED_PRECONDITION"));

  const failedPreconditionBilling = JSON.stringify({
    error: {
      code: 400,
      message: "Billing has not been enabled for this project.",
      status: "FAILED_PRECONDITION",
    },
  });
  const err10 = classifyGeminiError(400, failedPreconditionBilling);
  assert.equal(err10.code, GeminiErrorCode.FAILED_PRECONDITION);
  assert.equal(err10.isPreconditionFailure, true);
  assert.ok(err10.userFacingMessage.includes("billing"));
});

test("Simulated multi-credential rotation skips invalid credential and uses backup", async () => {
  const keys = ["dead-key-1", "valid-key-2"];
  const calls = [];

  async function mockFetch(apiKey) {
    calls.push(apiKey);
    if (apiKey === "dead-key-1") {
      return {
        ok: false,
        status: 401,
        text: async () => '{"error": {"status": "ACCOUNT_STATE_INVALID", "message": "disabled"}}',
      };
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({
        candidates: [
          {
            content: {
              parts: [
                {
                  text: JSON.stringify([
                    {
                      questionText: "Sample question?",
                      optionA: "A",
                      optionB: "B",
                      optionC: "C",
                      optionD: "D",
                      correctAnswer: "A",
                      explanation: "Explanation",
                    },
                  ]),
                },
              ],
            },
          },
        ],
      }),
    };
  }

  let finalSuccess = false;
  let usedKey = null;

  for (let idx = 0; idx < keys.length; idx++) {
    const key = keys[idx];
    const res = await mockFetch(key);
    if (!res.ok) {
      const errText = await res.text();
      const classified = classifyGeminiError(res.status, errText);
      if (classified.isCredentialFailure) {
        continue; // Rotate to next slot
      }
    } else {
      finalSuccess = true;
      usedKey = key;
      break;
    }
  }

  assert.equal(finalSuccess, true);
  assert.equal(usedKey, "valid-key-2");
  assert.deepEqual(calls, ["dead-key-1", "valid-key-2"]);
});

test("API keys are never exposed in user facing error messages", () => {
  const secretKey = "AIzaSySecretKey999";
  const rawError = `Gemini request failed with key ${secretKey}: ACCOUNT_STATE_INVALID`;
  const classified = classifyGeminiError(401, rawError);

  assert.ok(!classified.userFacingMessage.includes(secretKey));
  assert.ok(!classified.userFacingMessage.includes("ACCOUNT_STATE_INVALID"));
  assert.equal(
    classified.userFacingMessage,
    "AI generation is temporarily unavailable because the AI service credentials are inactive or invalid."
  );
});

test("Model chain excludes 2.0 and 2.5 models and honors fallbacks", () => {
  const envWith2 = {
    GEMINI_MODEL: "gemini-2.0-flash",
    GEMINI_FALLBACK_MODELS: "gemini-3.5-flash-lite, gemini-2.5-pro, gemini-3.1-flash-lite",
  };
  const chain = getModelChain(envWith2);
  assert.ok(!chain.includes("gemini-2.0-flash"), "gemini-2.0 must be excluded");
  assert.ok(!chain.includes("gemini-2.5-pro"), "gemini-2.5 must be excluded");
  assert.deepEqual(chain, ["gemini-3.5-flash-lite", "gemini-3.1-flash-lite"]);

  const envDefault = {};
  const defaultChain = getModelChain(envDefault);
  assert.deepEqual(defaultChain, ["gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.5-flash"]);
});

test("Tolerant parseAndValidateQuestions handles markdown fences, surrounding prose, and object wrapper", () => {
  const markdownSample = `Here is your requested exam question:
\`\`\`json
[
  {
    "questionText": "What is the capital of India?",
    "optionA": "Mumbai",
    "optionB": "New Delhi",
    "optionC": "Kolkata",
    "optionD": "Chennai",
    "correctAnswer": "2",
    "explanation": "New Delhi is the official capital."
  }
]
\`\`\`
Good luck with your exam!`;

  const parsed = parseAndValidateQuestions(markdownSample);
  assert.equal(parsed.length, 1);
  assert.equal(parsed[0].questionText, "What is the capital of India?");
  assert.equal(parsed[0].correctAnswer, "B"); // Normalized from "2" to "B"

  const objectWrapperSample = `Preamble text
{
  "questions": [
    {
      "question": "Which planet is known as the Red Planet?",
      "a": "Venus",
      "b": "Mars",
      "c": "Jupiter",
      "d": "Saturn",
      "answer": "B",
      "explanation": "Mars has iron oxide on its surface."
    }
  ]
}
Postamble text`;

  const parsedObj = parseAndValidateQuestions(objectWrapperSample);
  assert.equal(parsedObj.length, 1);
  assert.equal(parsedObj[0].questionText, "Which planet is known as the Red Planet?");
  assert.equal(parsedObj[0].optionB, "Mars");
  assert.equal(parsedObj[0].correctAnswer, "B");
});

// Full generateQuestions engine for mocked fetch tests
async function generateQuestions(env, examName, syllabus, targetCount, customPrompt, fetchFn = fetch) {
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
  const collected = [];
  const seenTexts = new Set();

  const startTime = Date.now();
  const TIME_BUDGET_MS = 120_000;
  const correlationId = "test-correlation-id";

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

    let chunkQuestions = [];
    let chunkSuccess = false;
    let lastError = null;

    modelLoop: for (let mIdx = 0; mIdx < modelChain.length; mIdx++) {
      const currentModel = modelChain[mIdx];
      let useResponseMimeType = true;
      let didRetryWithoutMimeType = false;
      let currentChunkRequestCount = neededCount;

      attemptLoop: for (let attempt = 1; attempt <= 3; attempt++) {
        const apiKey = candidateKeys[keyIndex % candidateKeys.length];
        const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(currentModel)}:generateContent`;

        const body = {
          contents: [{ parts: [{ text: "prompt" }] }],
        };
        if (useResponseMimeType) {
          body.generationConfig = { responseMimeType: "application/json" };
        }

        try {
          const res = await fetchFn(endpoint, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "x-goog-api-key": apiKey,
            },
            body: JSON.stringify(body),
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

            if (providerErr.isPreconditionFailure) {
              throw providerErr;
            }

            if (providerErr.isCredentialFailure) {
              keyIndex++;
              if (candidateKeys.length > 1) {
                continue attemptLoop;
              } else {
                break attemptLoop;
              }
            }

            if (res.status === 400 || res.status === 404) {
              if (useResponseMimeType && !didRetryWithoutMimeType) {
                useResponseMimeType = false;
                didRetryWithoutMimeType = true;
                continue attemptLoop;
              }
              break attemptLoop;
            }

            if (providerErr.isTransient && attempt < 3) {
              continue attemptLoop;
            }

            break attemptLoop;
          }

          const data = await res.json();
          if (data.promptFeedback?.blockReason) {
            throw new GeminiProviderError(
              GeminiErrorCode.SAFETY_BLOCKED,
              400,
              `Blocked by AI safety filters (${data.promptFeedback.blockReason}).`,
              { correlationId, model: currentModel }
            );
          }

          const candidate = data.candidates?.[0];
          if (!candidate) {
            lastError = new GeminiProviderError(GeminiErrorCode.INVALID_GEMINI_RESPONSE, 500, "Empty candidates");
            continue attemptLoop;
          }

          if (candidate.finishReason === "SAFETY") {
            throw new GeminiProviderError(GeminiErrorCode.SAFETY_BLOCKED, 400, "Blocked by safety filters.");
          }

          if (candidate.finishReason === "MAX_TOKENS") {
            if (currentChunkRequestCount > 1) {
              currentChunkRequestCount = Math.max(1, Math.floor(currentChunkRequestCount / 2));
              continue attemptLoop;
            }
          }

          const rawText = candidate.content?.parts?.[0]?.text;
          const parsed = parseAndValidateQuestions(rawText);
          if (parsed.length === 0) {
            lastError = new GeminiProviderError(GeminiErrorCode.INVALID_GENERATED_QUESTIONS, 500, "No valid questions");
            continue attemptLoop;
          }

          chunkQuestions = parsed;
          chunkSuccess = true;
          break modelLoop;
        } catch (err) {
          if (err instanceof GeminiProviderError) {
            lastError = err;
            if (err.isPreconditionFailure) throw err;
          } else {
            lastError = err;
          }
        }
      }
    }

    if (!chunkSuccess) {
      if (collected.length > 0) break;
      throw lastError || new Error("Failed to generate questions");
    }

    let addedFromChunk = 0;
    for (const q of chunkQuestions) {
      const norm = q.questionText.trim().toLowerCase();
      if (!seenTexts.has(norm)) {
        seenTexts.add(norm);
        collected.push(q);
        addedFromChunk++;
        if (collected.length >= targetCount) break;
      }
    }

    if (collected.length < targetCount) {
      if (addedFromChunk === 0 || extraRounds >= MAX_EXTRA_ROUNDS) break;
      extraRounds++;
    }
  }

  if (collected.length === 0) {
    throw new GeminiProviderError(GeminiErrorCode.INVALID_GENERATED_QUESTIONS, 500, "No questions collected");
  }

  return collected;
}

function createDummyQuestion(idx) {
  return {
    questionText: `Question ${idx}: What is the test answer?`,
    optionA: "Alpha",
    optionB: "Beta",
    optionC: "Gamma",
    optionD: "Delta",
    correctAnswer: "A",
    explanation: "Alpha is correct",
  };
}

test("Mock fetch test: 400 -> no-JSON-mode retry -> fallback model", async () => {
  const calls = [];
  const mockFetch = async (url, opts) => {
    const body = JSON.parse(opts.body);
    calls.push({ url, hasResponseMime: Boolean(body.generationConfig?.responseMimeType) });

    if (url.includes("gemini-3.5-flash-lite")) {
      // Primary model fails on both json and non-json mode
      return {
        ok: false,
        status: 400,
        text: async () => JSON.stringify({ error: { code: 400, status: "INVALID_ARGUMENT", message: "Bad mode" } }),
      };
    }

    if (url.includes("gemini-3.1-flash-lite")) {
      // Fallback model succeeds
      return {
        ok: true,
        status: 200,
        json: async () => ({
          candidates: [
            {
              content: {
                parts: [{ text: JSON.stringify([createDummyQuestion(1)]) }],
              },
            },
          ],
        }),
      };
    }

    return { ok: false, status: 500, text: async () => "error" };
  };

  const env = {
    GEMINI_API_KEY: "test-key",
    GEMINI_MODEL: "gemini-3.5-flash-lite",
    GEMINI_FALLBACK_MODELS: "gemini-3.1-flash-lite",
  };

  const res = await generateQuestions(env, "Math", "Algebra", 1, "Notes", mockFetch);
  assert.equal(res.length, 1);
  assert.equal(res[0].questionText, "Question 1: What is the test answer?");

  // Verify call sequence:
  // 1. Primary with responseMimeType
  // 2. Primary without responseMimeType
  // 3. Fallback model
  assert.equal(calls.length, 3);
  assert.ok(calls[0].url.includes("gemini-3.5-flash-lite") && calls[0].hasResponseMime === true);
  assert.ok(calls[1].url.includes("gemini-3.5-flash-lite") && calls[1].hasResponseMime === false);
  assert.ok(calls[2].url.includes("gemini-3.1-flash-lite"));
});

test("Mock fetch test: 429 retry/backoff", async () => {
  let attempt = 0;
  const mockFetch = async () => {
    attempt++;
    if (attempt === 1) {
      return {
        ok: false,
        status: 429,
        text: async () => JSON.stringify({ error: { code: 429, status: "RESOURCE_EXHAUSTED", message: "Rate limit" } }),
      };
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({
        candidates: [
          {
            content: { parts: [{ text: JSON.stringify([createDummyQuestion(1)]) }] },
          },
        ],
      }),
    };
  };

  const env = { GEMINI_API_KEY: "test-key" };
  const res = await generateQuestions(env, "Math", "", 1, "", mockFetch);
  assert.equal(res.length, 1);
  assert.equal(attempt, 2);
});

test("Mock fetch test: MAX_TOKENS retry", async () => {
  let attempt = 0;
  const mockFetch = async () => {
    attempt++;
    if (attempt === 1) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          candidates: [{ finishReason: "MAX_TOKENS" }],
        }),
      };
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({
        candidates: [
          {
            finishReason: "STOP",
            content: { parts: [{ text: JSON.stringify([createDummyQuestion(1)]) }] },
          },
        ],
      }),
    };
  };

  const env = { GEMINI_API_KEY: "test-key" };
  const res = await generateQuestions(env, "Math", "", 1, "", mockFetch);
  assert.equal(res.length, 1);
  assert.equal(attempt, 2);
});

test("Mock fetch test: key redaction prevents key leaks", () => {
  const rawKey = "AIzaSyTestSecretKey456789";
  const errText = `Invalid argument when calling with key ${rawKey} in query param`;
  const err = classifyGeminiError(400, errText, { keysToRedact: [rawKey] });

  assert.ok(!err.providerMessage.includes(rawKey));
  assert.ok(err.providerMessage.includes("[REDACTED_KEY]"));
  assert.ok(!err.userFacingMessage.includes(rawKey));
});

test("Mock fetch test: FAILED_PRECONDITION stops early without cycling models", async () => {
  let callCount = 0;
  const mockFetch = async () => {
    callCount++;
    return {
      ok: false,
      status: 400,
      text: async () =>
        JSON.stringify({
          error: {
            code: 400,
            status: "FAILED_PRECONDITION",
            message: "User location is not supported for the API use.",
          },
        }),
    };
  };

  const env = {
    GEMINI_API_KEY: "test-key",
    GEMINI_MODEL: "gemini-3.5-flash-lite",
    GEMINI_FALLBACK_MODELS: "gemini-3.1-flash-lite,gemini-3.5-flash",
  };

  await assert.rejects(
    async () => {
      await generateQuestions(env, "Exam", "", 5, "", mockFetch);
    },
    (err) => {
      assert.ok(err.isPreconditionFailure);
      assert.equal(err.code, GeminiErrorCode.FAILED_PRECONDITION);
      assert.ok(err.userFacingMessage.includes("region"));
      return true;
    }
  );

  // Exactly 1 call made! Did not attempt any fallback models!
  assert.equal(callCount, 1);
});

test("Mock fetch test: remainder top-up reaches the target count", async () => {
  let round = 0;
  const mockFetch = async () => {
    round++;
    if (round === 1) {
      // Requested 5, but returns only 3 questions
      return {
        ok: true,
        status: 200,
        json: async () => ({
          candidates: [
            {
              content: {
                parts: [
                  {
                    text: JSON.stringify([
                      createDummyQuestion(1),
                      createDummyQuestion(2),
                      createDummyQuestion(3),
                    ]),
                  },
                ],
              },
            },
          ],
        }),
      };
    }
    // Second round returns 2 more questions to meet target of 5
    return {
      ok: true,
      status: 200,
      json: async () => ({
        candidates: [
          {
            content: {
              parts: [
                {
                  text: JSON.stringify([
                    createDummyQuestion(4),
                    createDummyQuestion(5),
                  ]),
                },
              ],
            },
          },
        ],
      }),
    };
  };

  const env = { GEMINI_API_KEY: "test-key" };
  const res = await generateQuestions(env, "Exam", "", 5, "", mockFetch);
  assert.equal(res.length, 5);
  assert.equal(round, 2);
});
