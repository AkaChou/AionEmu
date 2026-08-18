---
alwaysApply: false
globs: "src/main/resources/aion/data/static_data/quest_definition/**/*.xml, src/main/java/com/aionemu/gameserver/questEngine/**/*.java, src/main/java/com/aionemu/gameserver/ai/**/*.java, src/main/java/com/aionemu/gameserver/ai2/**/*.java, src/test/java/com/aionemu/gameserver/questEngine/**/*.java, src/test/java/com/aionemu/gameserver/ai/**/*.java, src/test/java/com/aionemu/gameserver/ai2/**/*.java, docs/quest/**/*.md"
---

# Quest Diagnosis and Repair Rules

## Required Reading

Before working on a quest issue, read:

- `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
- `docs/quest/repair-playbook/PATTERNS.zh-CN.md`
- `docs/quest/repair-playbook/CASES.zh-CN.md`
- `docs/quest/WRITING_GUIDE.zh-CN.md`
- `docs/quest/client-dialog-mapping/README.zh-CN.md`

When `CASES.zh-CN.md` links a matched case to `docs/quest/repair-playbook/cases/*.md`, read that case shard before designing the repair.

## Evidence Requirements

1. Before designing or editing a repair, match the player symptom, compiled IR, owner shape, and side-effect contract against the Playbook pattern fingerprints. Read the best representative commit's complete diff and test, then record the matched and differing contract fields; reading only the one-line case index is insufficient.
2. Inspect the compiled state, events, conditions, transactional actions, and `after-commit` ordering of the current XML together.
3. When the `origin/history` ref is available, compare the state, pages, and side-effect ordering with the legacy handler or retail template.
4. Refer to client evidence as the “Aion 5.8 client.” Do not assume it exists on a particular machine or at a fixed path.
5. If the task requires Aion 5.8 client pages, actions, dictionaries, packets, unpacked assets, packet captures, or other external evidence that is not available in the conversation or repository, list the missing inputs and ask the user to provide them. Do not guess or mark the quest as repaired before obtaining the evidence.
6. Validate the actual runtime path using logs, object IDs, NPC template IDs, map or instance context, and login/logout behavior as applicable.

## Implementation Boundaries

1. Production quest execution is owned by the quest XML and production catalog. Legacy handlers, client data, and logs are authoritative behavior evidence, not production owners.
2. Prefer a quest-specific XML fix and quest-specific regression coverage for a single-quest page or state problem.
3. A shared runtime or AI change must prove its impact scope and add shared regression coverage or a production-directory audit.
4. Tests must lock down the source, target, status, variables, event, conditions, transactional actions, and complete `after-commit` order. Asserting only the final state is insufficient.
5. A repair is incomplete when state is correct but page display, close behavior, spawning, following, teleportation, or another side effect remains wrong.

## Acceptance and Playbook Updates

1. Update `docs/quest/repair-playbook/PATTERNS.zh-CN.md` and `CASES.zh-CN.md` only when an accepted representative repair establishes a new reusable problem pattern. Do not update the Playbook for another quest already covered by an existing pattern; keep stable process rules in `QUEST_REPAIR_PLAYBOOK.zh-CN.md`.
2. Acceptance requires a correct XML/IR contract, passing focused tests, and passing production catalog and whitelist checks. Do not hand an XML change to the user for client retesting until a quest-specific compiler test and the production catalog/whitelist gates pass; without build authorization, keep the repair `PENDING` and list the unexecuted commands.
3. Changes involving client pages, NPC spawning, following, login/logout behavior, or performance also require the corresponding client or runtime validation. Ask the user for missing inputs instead of downgrading acceptance to speculation.
4. An unambiguous user message that identifies the current quest and states that client validation or client acceptance is complete is authoritative confirmation that the whole quest is playable, unless the user explicitly limits the statement to one branch or step. Treat it as `CLIENT_ACCEPTED`; do not ask the user to repeat the same route or provide screenshots. The message is also explicit standing authorization to finish the local submission workflow without waiting for another "commit" command: preserve unrelated dirty files, commit the scoped repair if it is still uncommitted or reuse its existing pending-acceptance commit, record the acceptance, re-run Pattern eligibility, and complete the evidence/Playbook commit. An existing Pattern gets a repair commit plus a scoped acceptance-record commit; a new Pattern gets a repair commit plus one Playbook/acceptance-record commit. It does not authorize a push, a server lifecycle action, or unrelated changes. Known compiler/catalog failures still have to be resolved before an acceptance-complete handoff.
5. Record completed client or runtime validation with the fields in `.agent/summary/quest-acceptance/README.zh-CN.md`. The record must identify the repository commit and relevant working-tree state, Aion 5.8 client/data provenance, NPC/object and map/instance context, source state/action/page, expected and actual response, startup health, available logs/protocol/artifact hashes, acceptance status, and remaining risks. When rule 4 is triggered, quote or faithfully summarize the user's confirmation and mark unavailable technical artifacts as `not captured`; their absence does not invalidate the user's gameplay acceptance.
6. Delivery must explicitly distinguish "implementation complete" from "acceptance complete." If tests, catalog or whitelist checks, Aion 5.8 client validation, or actual runtime evidence remain incomplete, proactively mark the work as "pending acceptance" in the final response, list the outstanding commands or evidence and the responsible party, and do not report only that the issue is fixed.
7. If startup logs contain `Can't initialize typed quest engine`, `QuestCompilationException`, `AMBIGUOUS_TRANSITION`, or a production catalog compile failure, stop client-level diagnosis and return to XML expansion and compiler evidence. The agent must not start, stop, or restart the user-managed server to perform this check.
8. The playbook is a repair reference, not an acceptance ledger. Deduplicate cases by reusable problem pattern, not by quest ID. Each new or materially revised representative case must record one representative quest, a stable pattern ID, searchable symptom keywords, the IR/owner fingerprint, first checkpoint, player-visible symptom, root cause, repair layer, changed files, representative test, validation commands and results, reuse boundaries, and commit.
9. For duplicate-owner repairs, a test that proves the surviving XML/runtime route is not sufficient by itself. The representative evidence must also identify the commit diff or source audit that removes the displaced owner, and runtime validation must record object or side-effect counts when the actual path is available.
10. Treat a subsequent quest as the same problem only when its player-visible symptom, root cause, repair layer, and repair contract all match an existing case. Do not add that quest ID, update the case body, or create a duplicate case. Create a new case only when the problem or repair pattern differs materially.
11. Do not add a representative case for work in progress, partial repairs, failed tests, static inference only, or a quest that remains `EVIDENCE_REQUIRED`.
12. When one shared change affects multiple quests, accept each quest independently, but keep only one representative case for the reusable pattern.
13. Re-run the Playbook eligibility decision whenever acceptance evidence changes and immediately before staging a quest repair. Compare the player-visible symptom, root cause, repair layer, and repair contract with existing cases. A previous `pending acceptance` or deduplication decision expires when focused tests, catalog/whitelist checks, client validation, or runtime evidence changes the acceptance state.
14. When an already accepted repair establishes a new representative case, use two consecutive local commits so the Playbook can reference a stable repair hash: first commit only the repair sources/tests, then update and commit the Playbook with that repair commit hash. Do not amend the Playbook into a commit whose hash it records.
15. If the repair was committed while still pending acceptance, preserve its stable hash and make the Playbook follow-up the first commit after the final acceptance evidence arrives. Do not rewrite unrelated history that accumulated while acceptance was pending.
16. In either sequence, once the case is accepted, do not insert an unrelated commit, push, or issue an acceptance-complete handoff before the Playbook commit. The repair and Playbook commits form one completed delivery batch even when acceptance was obtained later.
17. After changing Playbook patterns or representative cases, run `python3 scripts/quest/check_quest_repair_playbook.py` and require every indexed or detailed representative commit to resolve and have at least one structured Pattern fingerprint with an existing `TestClass#method` reference.
18. Because `docs/*` is ignored, add every changed Playbook document with explicit paths in the documentation commit:

   ```bash
   git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
   git add -f docs/quest/repair-playbook/PATTERNS.zh-CN.md
   git add -f docs/quest/repair-playbook/CASES.zh-CN.md
   ```
