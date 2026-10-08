const test = require('node:test');
const assert = require('node:assert/strict');
const { parseAndValidateGeminiQuestions: parse } = require('../questionValidation');
const q = { questionText: 'What is two plus two?', optionA: 'three', optionB: 'four', optionC: 'five', optionD: 'six', correctAnswer: 'B' };
test('real Firebase AI parser accepts valid fenced questions', () => assert.equal(parse('```json\n' + JSON.stringify([q]) + '\n```')[0].correctAnswer, 'B'));
for (const [name, raw] of [['malformed', 'no JSON'], ['empty', '[]'], ['null', 'null'], ['invalid answer', JSON.stringify([{ ...q, correctAnswer: '9' }])], ['duplicates', JSON.stringify([q, q])], ['invalid options', JSON.stringify([{ ...q, optionD: 'four' }])], ['missing fields', '[{}]']]) test(`real Firebase parser rejects ${name}`, () => assert.throws(() => parse(raw)));
