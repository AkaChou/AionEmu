---
alwaysApply: false
globs: "src/main/resources/aion/data/static_data/quest_definition/**/*.xml, src/main/java/com/aionemu/gameserver/questEngine/**/*.java, src/main/java/com/aionemu/gameserver/ai/**/*.java, src/main/java/com/aionemu/gameserver/ai2/**/*.java, src/test/java/com/aionemu/gameserver/questEngine/**/*.java, src/test/java/com/aionemu/gameserver/ai/**/*.java, src/test/java/com/aionemu/gameserver/ai2/**/*.java, docs/quest/**/*.md"
---

# Quest Diagnosis and Repair Rules

## Required Reading

Before working on a quest issue, read:

- `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
- `docs/quest/WRITING_GUIDE.zh-CN.md`
- `docs/quest/client-dialog-mapping/README.zh-CN.md`

## Evidence Requirements

1. Inspect the compiled state, events, conditions, transactional actions, and `after-commit` ordering of the current XML together.
2. When the `origin/history` ref is available, compare the state, pages, and side-effect ordering with the legacy handler or retail template.
3. Refer to client evidence as the “Aion 5.8 client.” Do not assume it exists on a particular machine or at a fixed path.
4. If the task requires Aion 5.8 client pages, actions, dictionaries, packets, unpacked assets, packet captures, or other external evidence that is not available in the conversation or repository, list the missing inputs and ask the user to provide them. Do not guess or mark the quest as repaired before obtaining the evidence.
5. Validate the actual runtime path using logs, object IDs, NPC template IDs, map or instance context, and login/logout behavior as applicable.

## Implementation Boundaries

1. Production quest execution is owned by the quest XML and production catalog. Legacy handlers, client data, and logs are authoritative behavior evidence, not production owners.
2. Prefer a quest-specific XML fix and quest-specific regression coverage for a single-quest page or state problem.
3. A shared runtime or AI change must prove its impact scope and add shared regression coverage or a production-directory audit.
4. Tests must lock down the source, target, status, variables, event, conditions, transactional actions, and complete `after-commit` order. Asserting only the final state is insufficient.
5. A repair is incomplete when state is correct but page display, close behavior, spawning, following, teleportation, or another side effect remains wrong.

## Acceptance and Playbook Updates

1. Update `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md` only when an accepted representative repair establishes a new reusable problem pattern. Do not update the playbook for another quest already covered by an existing pattern.
2. Acceptance requires a correct XML/IR contract, passing focused tests, and passing production catalog and whitelist checks.
3. Changes involving client pages, NPC spawning, following, login/logout behavior, or performance also require the corresponding client or runtime validation. Ask the user for missing inputs instead of downgrading acceptance to speculation.
4. Delivery must explicitly distinguish "implementation complete" from "acceptance complete." If tests, catalog or whitelist checks, Aion 5.8 client validation, or actual runtime evidence remain incomplete, proactively mark the work as "pending acceptance" in the final response, list the outstanding commands or evidence and the responsible party, and do not report only that the issue is fixed.
5. The playbook is a repair reference, not an acceptance ledger. Deduplicate cases by reusable problem pattern, not by quest ID. Each case must record one representative quest, the player-visible symptom, root cause, repair layer, changed files, validation commands and results, reuse boundaries, and commit.
6. Treat a subsequent quest as the same problem only when its player-visible symptom, root cause, repair layer, and repair contract all match an existing case. Do not add that quest ID, update the case body, or create a duplicate case. Create a new case only when the problem or repair pattern differs materially.
7. Do not add a representative case for work in progress, partial repairs, failed tests, static inference only, or a quest that remains `EVIDENCE_REQUIRED`.
8. When one shared change affects multiple quests, accept each quest independently, but keep only one representative case for the reusable pattern.
9. Re-run the Playbook eligibility decision whenever acceptance evidence changes and immediately before staging a quest repair. Compare the player-visible symptom, root cause, repair layer, and repair contract with existing cases. A previous `pending acceptance` or deduplication decision expires when focused tests, catalog/whitelist checks, client validation, or runtime evidence changes the acceptance state.
10. When an already accepted repair establishes a new representative case, use two consecutive local commits so the Playbook can reference a stable repair hash: first commit only the repair sources/tests, then update and commit the Playbook with that repair commit hash. Do not amend the Playbook into a commit whose hash it records.
11. If the repair was committed while still pending acceptance, preserve its stable hash and make the Playbook follow-up the first commit after the final acceptance evidence arrives. Do not rewrite unrelated history that accumulated while acceptance was pending.
12. In either sequence, once the case is accepted, do not insert an unrelated commit, push, or issue an acceptance-complete handoff before the Playbook commit. The repair and Playbook commits form one completed delivery batch even when acceptance was obtained later.
13. Because `docs/*` is ignored, add the Playbook with an explicit path in the documentation commit:

   ```bash
   git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
   ```
