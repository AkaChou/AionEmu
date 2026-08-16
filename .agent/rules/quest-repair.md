---
alwaysApply: false
globs: "src/main/resources/aion/data/static_data/quest_definition/**/*.xml, src/main/java/com/aionemu/gameserver/questEngine/**/*.java, src/main/java/com/aionemu/gameserver/ai/**/*.java, src/main/java/com/aionemu/gameserver/ai2/**/*.java, src/test/java/com/aionemu/gameserver/questEngine/**/*.java, src/test/java/com/aionemu/gameserver/ai/**/*.java, src/test/java/com/aionemu/gameserver/ai2/**/*.java, docs/quest/**/*.md"
---

# 任务排查与修复规则

## 必读入口

处理任务问题前读取：

- `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
- `docs/quest/WRITING_GUIDE.zh-CN.md`
- `docs/quest/client-dialog-mapping/README.zh-CN.md`

## 证据要求

1. 同时核对当前 XML 编译后的状态、事件、条件、事务动作和 `after-commit` 顺序。
2. 如果 `origin/history` ref 可用，对照旧 handler 或正式模板的状态、页面和副作用时序。
3. 客户端证据统一称为“Aion 5.8 客户端”，不要假设它位于某台机器或固定目录。
4. 当前任务需要 Aion 5.8 客户端页面、动作、字典、数据包、解包产物、抓包或其他外部证据，而会话和仓库中没有提供时，明确列出缺失项并询问用户提供；在取得证据前不得猜测或把任务标记为已修复。
5. 结合运行日志、对象 ID、NPC 模板 ID、地图/实例和登录登出行为验证实际路径。

## 实现边界

1. 生产任务的执行 owner 是任务 XML 和 production catalog；旧 handler、客户端数据和日志只作为权威行为证据。
2. 单任务页面或状态问题优先修 XML，并增加任务专用回归测试。
3. 共享 runtime 或 AI 问题必须证明受影响范围，并增加共享测试或生产目录级审计。
4. 测试必须锁定 source、target、status、变量、事件、条件、事务动作及 `after-commit` 完整顺序，不能只断言最终状态。
5. 状态正确但页面、关闭、生成、跟随或传送副作用错误，仍属于未完成修复。

## 验收与 Playbook

1. 每修复完一个任务，只有正确性验收完成后，才能更新 `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`。
2. 完成验收至少包括 XML/IR 合同正确、相关 focused tests 通过、production catalog/whitelist 通过。
3. 涉及客户端页面、NPC 生成、跟随、登录登出或性能时，还必须完成对应的客户端或运行时验证；缺少必要输入时向用户询问，不得降级为推测验收。
4. Playbook 案例记录任务 ID、任务名称、玩家症状、根因、修改文件、验证命令与结果、残余风险和 commit hash；一个任务只记录一次已验收结论。
5. 修复中、部分修复、测试失败、只有静态推断或仍为 `EVIDENCE_REQUIRED` 的任务，不得写入“已修复”案例。
6. 一个共享修复涉及多个任务时逐任务验收，只记录实际通过的任务。
7. 任务修复和 Playbook 更新应在同一交付批次中完成。`docs/*` 被忽略时，提交文档使用明确路径：

   ```bash
   rtk git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
   ```
