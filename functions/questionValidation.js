function parseAndValidateGeminiQuestions(rawJson) {
  let cleaned = rawJson.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.replace(/^```json\s*/, "").replace(/\s*```$/, "");
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```\s*/, "").replace(/\s*```$/, "");
  }

  let parsed;
  try {
    parsed = JSON.parse(cleaned);
  } catch (err) {
    throw new Error("Invalid JSON output from Gemini");
  }

  const list = Array.isArray(parsed)
    ? parsed
    : (Array.isArray(parsed?.questions) ? parsed.questions : []);
  if (!list.length) throw new Error("Empty generated question array");

  const validQuestions = [];
  const validAnswers = new Set(["A", "B", "C", "D"]);

  const field = (value) => {
    if (value == null) return "";
    if (typeof value !== "string") throw new Error("Question text and options must be strings");
    return value.trim();
  };
  const seen = new Set();
  for (const q of list) {
    if (!q || typeof q !== "object") throw new Error("Invalid question object");
    const questionText = field(q.questionText || q.question || "");
    const optionA = field(q.optionA || q.a || "");
    const optionB = field(q.optionB || q.b || "");
    const optionC = field(q.optionC || q.c || "");
    const optionD = field(q.optionD || q.d || "");
    let correctAnswer = String(q.correctAnswer || q.answer || "").trim().toUpperCase();

    if (!validAnswers.has(correctAnswer)) {
      if (correctAnswer === "1" || correctAnswer === optionA.toUpperCase()) correctAnswer = "A";
      else if (correctAnswer === "2" || correctAnswer === optionB.toUpperCase()) correctAnswer = "B";
      else if (correctAnswer === "3" || correctAnswer === optionC.toUpperCase()) correctAnswer = "C";
      else if (correctAnswer === "4" || correctAnswer === optionD.toUpperCase()) correctAnswer = "D";
      else throw new Error("Invalid generated answer");
    }

    const explanation = String(q.explanation || "").trim();

    // Check non-empty
    if (!questionText || !optionA || !optionB || !optionC || !optionD) {
      throw new Error("Missing or invalid question fields");
    }

    // Check no duplicate choices among options
    const uniqueOptions = new Set([
      optionA.toLowerCase(),
      optionB.toLowerCase(),
      optionC.toLowerCase(),
      optionD.toLowerCase(),
    ]);
    if (uniqueOptions.size < 4) {
      throw new Error("Missing or invalid question fields");
    }

    const textKey = questionText.toLowerCase().replace(/\s+/g, " ");
    if (seen.has(textKey)) throw new Error("Duplicate generated question");
    seen.add(textKey);
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

module.exports = { parseAndValidateGeminiQuestions };
