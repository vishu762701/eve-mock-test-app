import { Env } from "../types";
import {
  getGeminiApiKeys,
  getModelChain,
  classifyGeminiError,
  GeminiProviderError,
  GeminiErrorCode,
} from "./generator";
import { trackGeminiCall } from "../util/usageTracker";

export interface ReformatExplanationInput {
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: string;
  explanation: string;
}

export const MAX_QUESTION_TEXT_LEN = 1000;
export const MAX_OPTION_TEXT_LEN = 500;
export const MAX_EXPLANATION_TEXT_LEN = 2000;

export function validateReformatInput(raw: any): {
  valid: boolean;
  error?: string;
  input?: ReformatExplanationInput;
} {
  if (!raw || typeof raw !== "object") {
    return { valid: false, error: "Request body must be a JSON object" };
  }

  const questionText = typeof raw.questionText === "string" ? raw.questionText.trim() : "";
  const optionA = typeof raw.optionA === "string" ? raw.optionA.trim() : "";
  const optionB = typeof raw.optionB === "string" ? raw.optionB.trim() : "";
  const optionC = typeof raw.optionC === "string" ? raw.optionC.trim() : "";
  const optionD = typeof raw.optionD === "string" ? raw.optionD.trim() : "";
  const explanation = typeof raw.explanation === "string" ? raw.explanation.trim() : "";

  let rawCorrect = typeof raw.correctAnswer === "string" ? raw.correctAnswer.trim().toUpperCase() : "A";
  if (rawCorrect === "1") rawCorrect = "A";
  else if (rawCorrect === "2") rawCorrect = "B";
  else if (rawCorrect === "3") rawCorrect = "C";
  else if (rawCorrect === "4") rawCorrect = "D";

  const validOptions = new Set(["A", "B", "C", "D"]);
  const correctAnswer = validOptions.has(rawCorrect) ? rawCorrect : "A";

  if (!questionText && !explanation) {
    return { valid: false, error: "Either questionText or explanation must be provided" };
  }

  if (questionText.length > MAX_QUESTION_TEXT_LEN) {
    return { valid: false, error: `questionText exceeds maximum length of ${MAX_QUESTION_TEXT_LEN} characters` };
  }
  if (optionA.length > MAX_OPTION_TEXT_LEN) {
    return { valid: false, error: `optionA exceeds maximum length of ${MAX_OPTION_TEXT_LEN} characters` };
  }
  if (optionB.length > MAX_OPTION_TEXT_LEN) {
    return { valid: false, error: `optionB exceeds maximum length of ${MAX_OPTION_TEXT_LEN} characters` };
  }
  if (optionC.length > MAX_OPTION_TEXT_LEN) {
    return { valid: false, error: `optionC exceeds maximum length of ${MAX_OPTION_TEXT_LEN} characters` };
  }
  if (optionD.length > MAX_OPTION_TEXT_LEN) {
    return { valid: false, error: `optionD exceeds maximum length of ${MAX_OPTION_TEXT_LEN} characters` };
  }
  if (explanation.length > MAX_EXPLANATION_TEXT_LEN) {
    return { valid: false, error: `explanation exceeds maximum length of ${MAX_EXPLANATION_TEXT_LEN} characters` };
  }

  return {
    valid: true,
    input: {
      questionText,
      optionA,
      optionB,
      optionC,
      optionD,
      correctAnswer,
      explanation,
    },
  };
}

export function buildReformatPrompt(input: ReformatExplanationInput): string {
  const qText = input.questionText.trim();
  const optA = input.optionA.trim();
  const optB = input.optionB.trim();
  const optC = input.optionC.trim();
  const optD = input.optionD.trim();
  const correct = input.correctAnswer.trim().toUpperCase();
  const exp = input.explanation.trim();

  return `You are an expert educational content writer and teacher for Indian competitive exams.
Restructure the following question's explanation into the exact "Key Points" step-by-step format:
1. Start with a bold one-line summary stating the correct answer (e.g. "**The correct answer is Option ${correct}.**").
2. Followed by a "Key Points:" section header.
3. Followed by short bullet points, each bold-labeling the concept before explaining it (e.g. "• **Concept Name:** Detailed reasoning...", "• **Given Values:** ...", "• **Step-by-step Calculation:** ...").
Never output a single unformatted paragraph.

Question: ${qText}
Option A: ${optA}
Option B: ${optB}
Option C: ${optC}
Option D: ${optD}
Correct Answer: Option ${correct}
Original Explanation: ${exp || "Option " + correct + " is the correct answer."}

Output ONLY the reformatted explanation text in the exact Key Points format:`;
}

export async function reformatExplanationWithAi(
  env: Env,
  input: ReformatExplanationInput,
  fetchFn: typeof fetch = fetch
): Promise<string> {
  const candidateKeys = getGeminiApiKeys(env);
  if (candidateKeys.length === 0) {
    if (env.DB) {
      await trackGeminiCall(env.DB, false).catch(() => {});
    }
    throw new GeminiProviderError(
      GeminiErrorCode.CONFIGURATION_MISSING,
      500,
      "AI service is not configured. Missing API keys."
    );
  }

  const modelChain = getModelChain(env);
  const prompt = buildReformatPrompt(input);
  const correlationId = crypto.randomUUID();
  let lastError: any = null;

  for (let mIdx = 0; mIdx < modelChain.length; mIdx++) {
    const model = modelChain[mIdx];
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;

    for (let kIdx = 0; kIdx < candidateKeys.length; kIdx++) {
      const apiKey = candidateKeys[kIdx];

      try {
        const res = await fetchFn(endpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey,
          },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
          }),
          signal: AbortSignal.timeout(20_000),
        });

        if (!res.ok) {
          const errText = await res.text();
          const classified = classifyGeminiError(res.status, errText, {
            correlationId,
            model,
            keysToRedact: candidateKeys,
          });
          lastError = classified;
          if (env.DB) {
            await trackGeminiCall(env.DB, false).catch(() => {});
          }
          if (classified.isPreconditionFailure) {
            throw classified;
          }
          continue; // rotate to next key
        }

        const data = (await res.json()) as any;
        if (data.promptFeedback?.blockReason || data.candidates?.[0]?.finishReason === "SAFETY") {
          if (env.DB) {
            await trackGeminiCall(env.DB, false).catch(() => {});
          }
          throw new GeminiProviderError(
            GeminiErrorCode.SAFETY_BLOCKED,
            400,
            "Reformatting was blocked by AI safety filters."
          );
        }

        const text = data.candidates?.[0]?.content?.parts?.map((p: any) => p.text || "").join("").trim();
        if (!text) {
          if (env.DB) {
            await trackGeminiCall(env.DB, false).catch(() => {});
          }
          lastError = new GeminiProviderError(
            GeminiErrorCode.INVALID_GEMINI_RESPONSE,
            502,
            "AI returned empty explanation text."
          );
          continue;
        }

        if (env.DB) {
          await trackGeminiCall(env.DB, true).catch(() => {});
        }
        return text;
      } catch (err: any) {
        if (err instanceof GeminiProviderError && err.isPreconditionFailure) {
          throw err;
        }
        lastError = err;
        if (env.DB) {
          await trackGeminiCall(env.DB, false).catch(() => {});
        }
      }
    }
  }

  throw lastError || new GeminiProviderError(
    GeminiErrorCode.TEMPORARY_PROVIDER_ERROR,
    502,
    "Failed to reformat explanation with configured AI models."
  );
}
