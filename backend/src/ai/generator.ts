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

export async function generateQuestions(
  env: Env,
  examName: string,
  syllabus: string,
  targetCount: number,
  customPrompt: string
): Promise<GeneratedQuestionItem[]> {
  const apiKey = env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured on Cloudflare Worker.");
  }

  const model = env.GEMINI_MODEL || "gemini-1.5-flash-8b";
  const CHUNK_SIZE = 25;
  const numChunks = Math.ceil(targetCount / CHUNK_SIZE);
  const collected: GeneratedQuestionItem[] = [];
  const seenTexts = new Set<string>();

  for (let c = 0; c < numChunks; c++) {
    const countForChunk = Math.min(CHUNK_SIZE, targetCount - collected.length);
    if (countForChunk <= 0) break;

    const prompt = buildPrompt(examName, syllabus, countForChunk, customPrompt);
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;

    const body = {
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        responseMimeType: "application/json",
      },
    };

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
      throw new Error(`Gemini API error (${res.status}): ${errText}`);
    }

    const data = (await res.json()) as any;
    const rawJson = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!rawJson) {
      throw new Error("Empty candidate received from Gemini API");
    }

    const parsed = parseAndValidateQuestions(rawJson);
    for (const q of parsed) {
      const norm = q.questionText.trim().toLowerCase();
      if (!seenTexts.has(norm)) {
        seenTexts.add(norm);
        collected.push(q);
      }
    }
  }

  if (collected.length === 0) {
    throw new Error(`Failed to generate valid questions for ${examName}.`);
  }

  return collected;
}
