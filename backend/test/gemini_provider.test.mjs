import test from "node:test";
import assert from "node:assert/strict";

// Mirror types & classification logic directly from backend/src/ai/generator.ts for Node test runner
const GeminiErrorCode = {
  CONFIGURATION_MISSING: "CONFIGURATION_MISSING",
  AUTH_ACCOUNT_INVALID: "AUTH_ACCOUNT_INVALID",
  AUTH_INVALID: "AUTH_INVALID",
  PERMISSION_DENIED: "PERMISSION_DENIED",
  QUOTA_EXCEEDED: "QUOTA_EXCEEDED",
  RATE_LIMITED: "RATE_LIMITED",
  MODEL_NOT_FOUND: "MODEL_NOT_FOUND",
  TEMPORARY_PROVIDER_ERROR: "TEMPORARY_PROVIDER_ERROR",
  NETWORK_ERROR: "NETWORK_ERROR",
  INVALID_GEMINI_RESPONSE: "INVALID_GEMINI_RESPONSE",
  INVALID_GENERATED_QUESTIONS: "INVALID_GENERATED_QUESTIONS",
};

class GeminiProviderError extends Error {
  constructor(code, httpStatus, userFacingMessage, internalDetails) {
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

function classifyGeminiError(status, errText) {
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
