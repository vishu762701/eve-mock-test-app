import test from "node:test";
import assert from "node:assert/strict";
import workerModule from "../src/index.ts";
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

test("Worker /api/admin/typography routes require authorization", async () => {
  const fakeDb = {
    prepare: () => ({
      first: async () => null
    })
  };

  // Without auth header, should fail with 401
  const res = await worker.fetch(
    new Request("https://example.test/api/admin/typography/draft"),
    { DB: fakeDb },
    {}
  );
  assert.equal(res.status, 401);
});
