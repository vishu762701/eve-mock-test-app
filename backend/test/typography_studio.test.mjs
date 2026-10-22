import test from "node:test";
import assert from "node:assert/strict";
import workerModule from "../src/index.ts";
import { validateConfig, createDefaultConfig } from "../src/routes/typographyPublished.ts";

const worker = workerModule.default || workerModule;

test("Worker /api/typography/published endpoint returns valid schema and default config", async () => {
  const fakeDb = {
    prepare: () => ({
      first: async () => null
    })
  };

  const res = await worker.fetch(
    new Request("https://example.test/api/typography/published"),
    { DB: fakeDb },
    {}
  );
  assert.equal(res.status, 200);

  const json = await res.json();
  assert.equal(json.success, true);
  assert.ok(json.data);
  assert.equal(json.data.schema_version, 1);
  assert.equal(json.data.font_family, "inter");
  assert.equal(json.data.preset_id, "ios26_balanced");
  assert.ok(json.data.global_styles);
  assert.equal(json.data.global_styles.headline.weight, 600);
  assert.equal(json.data.global_styles.body.weight, 400);
});

test("Retired typography admin editor route is no longer mounted", async () => {
  const fakeDb = {
    prepare: () => ({
      first: async () => null
    })
  };

  // Retired editor routes must not be exposed, including to previously authorized clients.
  const res = await worker.fetch(
    new Request("https://example.test/api/admin/typography/draft"),
    { DB: fakeDb },
    {}
  );
  assert.equal(res.status, 404);
});

test("validateConfig: accepts factory default configuration", () => {
  const defaultConfig = createDefaultConfig();
  const res = validateConfig(defaultConfig);
  assert.equal(res.valid, true);
  assert.equal(res.error, undefined);
});

test("validateConfig: rejects invalid font family", () => {
  const config = createDefaultConfig();
  config.font_family = "comic_sans";
  const res = validateConfig(config);
  assert.equal(res.valid, false);
  assert.match(res.error, /Invalid font family/);
});

test("validateConfig: rejects invalid preset id", () => {
  const config = createDefaultConfig();
  config.preset_id = "non_existent_preset";
  const res = validateConfig(config);
  assert.equal(res.valid, false);
  assert.match(res.error, /Invalid preset ID/);
});

test("validateConfig: rejects invalid role in global_styles", () => {
  const config = createDefaultConfig();
  config.global_styles.unknown_role = { text_size_sp: 16, weight: 400 };
  const res = validateConfig(config);
  assert.equal(res.valid, false);
  assert.match(res.error, /Invalid global style role/);
});

test("validateConfig: validates style rule bounds (size, weight, line-height, spacing)", () => {
  // Test invalid size < 8sp
  const config1 = createDefaultConfig();
  config1.global_styles.body = { text_size_sp: 4, weight: 400 };
  assert.equal(validateConfig(config1).valid, false);

  // Test invalid size > 60sp
  const config2 = createDefaultConfig();
  config2.global_styles.body = { text_size_sp: 72, weight: 400 };
  assert.equal(validateConfig(config2).valid, false);

  // Test invalid weight
  const config3 = createDefaultConfig();
  config3.global_styles.body = { text_size_sp: 16, weight: 450 };
  assert.equal(validateConfig(config3).valid, false);

  // Test line height smaller than text size
  const config4 = createDefaultConfig();
  config4.global_styles.body = { text_size_sp: 18, line_height_sp: 14, weight: 400 };
  assert.equal(validateConfig(config4).valid, false);

  // Test letter spacing out of bounds
  const config5 = createDefaultConfig();
  config5.global_styles.body = { text_size_sp: 16, letter_spacing_em: 0.9, weight: 400 };
  assert.equal(validateConfig(config5).valid, false);

  // Test valid overrides
  const config6 = createDefaultConfig();
  config6.global_styles.body = { text_size_sp: 17, line_height_sp: 22, weight: 400, letter_spacing_em: -0.02 };
  assert.equal(validateConfig(config6).valid, true);
});

test("validateConfig: rejects payloads exceeding 50KB limit", () => {
  const config = createDefaultConfig();
  // Artificially inflate with large nested screen overrides
  config.screen_overrides = {};
  for (let i = 0; i < 2000; i++) {
    config.screen_overrides[`screen_${i}`] = {
      [`elem_${i}`]: { text_size_sp: 16, line_height_sp: 20, weight: 400 }
    };
  }
  const res = validateConfig(config);
  assert.equal(res.valid, false);
  assert.match(res.error, /exceeds 50KB limit/);
});

test("validateConfig: validates screen and element keys", () => {
  const config = createDefaultConfig();
  config.screen_overrides = {
    "<script>bad</script>": {
      "elem": { text_size_sp: 16, weight: 400 }
    }
  };
  const res = validateConfig(config);
  assert.equal(res.valid, false);
  assert.match(res.error, /Invalid screen key format/);
});
