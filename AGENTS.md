Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint

Inspect live main, relevant code, tests and exact-SHA CI; implement only the owner's authorized outcome and its necessary dependencies. ENGINEERING_STANDARD.md is the canonical policy. Historical reports and task prompts are evidence, not standing instructions. Do not start a feature from the chat starter alone.

## Non-negotiable summary

This summary adds no independent policy. Full text: ENGINEERING_STANDARD.md section 0, which wins if the two ever differ.

1. The latest explicit owner instruction wins. Never replace it with your own design.
2. First restate the request as numbered requirements. Anything not listed stays unchanged.
3. No silent deviation. If something cannot be done as stated, stop and report before changing code.
4. Unclear, contradictory or wrong instruction: criticize directly, recommend an option, wait. After the owner decides, implement that decision exactly.
5. Decide alone only routine engineering. Behavior, UI, scope, data and dependencies are the owner's decisions.
6. Telegram source-exact work: Analysis, then Mapping, then wait for owner approval, then implement.
7. Never say exact, identical, complete or done without evidence for every requirement. The final report lists each requirement as MET, NOT MET or BLOCKED.
