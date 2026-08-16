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

1. Update `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md` for a repaired quest only after correctness acceptance is complete.
2. Acceptance requires a correct XML/IR contract, passing focused tests, and passing production catalog and whitelist checks.
3. Changes involving client pages, NPC spawning, following, login/logout behavior, or performance also require the corresponding client or runtime validation. Ask the user for missing inputs instead of downgrading acceptance to speculation.
4. 交付时必须明确区分“实现完成”和“验收完成”。若测试、catalog/whitelist、Aion 5.8 客户端或实际运行时证据尚未完成，必须在最终回复中主动提醒用户验收，标记为“待验收”，列出剩余命令/证据和验收责任方，不得只报告“已修复”。
5. Playbook 案例按可复用的“问题模式”去重，不按任务 ID 重复建立。首个案例必须记录代表任务、玩家可见症状、根因、修复层、修改文件、验证命令和结果、残余风险、commit，以及独立通过验收的“已验收任务 ID”列表。
6. 后续任务仅当玩家可见症状、根因、修复层和验收合同均与已有案例相同时，才视为同一问题；该任务独立验收通过后，只把任务 ID 追加到原案例的“已验收任务 ID”，不得复制案例正文或新增重复案例。任一项不同时必须新建案例。
7. Do not add a case or append a quest ID to the repaired-case section for work in progress, partial repairs, failed tests, static inference only, or a quest that remains `EVIDENCE_REQUIRED`.
8. When one shared change affects multiple quests, accept each quest independently and append only those that actually pass.
9. Deliver a quest repair and its playbook update in the same batch. Because `docs/*` is ignored, add the playbook with an explicit path when committing it:

   ```bash
   git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
   ```
