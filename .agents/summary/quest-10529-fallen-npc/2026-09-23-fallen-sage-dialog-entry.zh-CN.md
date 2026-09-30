# 10529/20529 倒下贤者任务对话入口循环

- 日期：2026-09-23。
- 状态：修复提交 `5bf5b70a2`；聚焦测试与生产目录/白名单门禁通过；**10529 客户端 ACCEPTED，20529 PENDING_CLIENT**。未操作服务端。

## 玩家症状和证据

- 用户提供的 10529 实机日志中，19:42:33 `SM_QUEST_ACTION 状态=3 步数=12297`，按 `var0@0`、`var1@6`、`var2@12` 解包为 `START/var0=9,var1=0,var2=3`，对应击杀 boss 后与倒下的德扎波波 806294 对话的阶段。其后反复下发 `SM_DIALOG_WINDOW targetObj=175632 questId=0 下发页=10`，点击页面正文“结束对话”后再次交互仍回到相同页面；该窗口没有进入 questId=10529 的任务上下文，未见客户端任务动作 `SETPRO10(10009)` 的追踪记录。日志未给出 targetObj=175632 的模板 ID，故不能仅以对象号断定它就是 806294；结合任务进度和用户截图定位本族交互入口。
- Aion 5.8 客户端映射 `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 显示 10529/20529 的 `SELECT10(4080) → SELECT10_1(4081) → SELECT10_1_1(4082) → SETPRO10(10009)`，最后一页确实只有“结束对话”这一任务动作；不能把文案当作通用关窗。
- `TalkEventHandler.onTalk` 在玩家首次点击 NPC 时先向 `QuestEngine.onDialog` 发 `TalkToNpc(npcId,-1)`；若无人处理才发不带 questId 的通用 page 10。原 XML 两侧倒下贤者在 s9 仅声明 `QUEST_SELECT(31)`、4081、4082 和 10009，缺少 `USE_OBJECT(-1)`，因此首次点击无法建立任务页上下文。`CM_DIALOG_SELECT` 对没有任务上下文的 NPC 选项故意采用普通对话分流（详见 `docs/quest/NPC_DIALOG_CONTEXT.zh-CN.md`），禁止全局放宽以免错绑未接取任务。
- 旧 handler `_10529Protection_Artifact_2`、`_20529Building_A_Protection_Artifact_2` 均注册倒下贤者并在 STEP_TO_10 执行给任务物品与传送；当前 XML 的 10009 交接保留这组副作用，不改变阶段和奖励归属。

## 修复合同和范围

- 在 `s9` 的 806294（10529）/806299（20529）各新增一条 `USE_OBJECT(-1) → SELECT10(4080)` 的任务页入口。保留现有 `QUEST_SELECT(31)` 入口和 `4081 → 4082 → 10009` 后续链、交付给物品及传送顺序。新入口仅限 `START/var0=9`，不会在未接受、其他阶段或其他 NPC 开启。
- 在 `JournalReportRowSplitContractTest#fallenSageInitialTalkOpensTheQuestPageWithoutAClientQuestRow` 锁定两侧唯一入口、条件、动作、after-commit 页面以及实际击杀后 packed counters（var0=9,var1=0,var2=3）的 planner 可行性；检查 31/4081/4082 各下一页，并从同一真实快照执行 10009，断言进入报告行 var0=10、保留 var2=3、任务物品交付和传送→任务状态同步→关窗的后置顺序。原有 `carrierHandoverEntersTheReportRow` 另覆盖 s9→s10 的基础交付。
- 扫描生产 XML 中“自身 after-commit 生成的 NPC，同时存在 QUEST_SELECT、但无 USE_OBJECT”得到 13 组候选；本轮只有 10529/20529 的倒下贤者双子拥有此次实机重复通用页 10 与相同交付链证据。其余 NPC 的客户端任务行是否缺失未取证，不机械增加直达路由。
- Playbook 去重：8.20 `NPC_DIALOG_ROUTE_GATE_COLLISION` 也会表现为仅有结束对话，但成因是多个 owner 的宽索引冲突；本次倒下贤者只有本任务 owner，成因是缺少初次交互 `-1` 路由。8.19 中 18600 已有 `USE_OBJECT` 与 `QUEST_SELECT` 并行入口的实现/测试参考，但根因还涉及领奖预览的物品门控。当前尚未通过客户端完整验收，不创建新代表案例。

## 已执行 / 待执行

- 修复前静态探针：10529 的 `s9/806294/USE_OBJECT → SELECT10` 路由数为 0，复现首次点击回退到通用 page 10 的入口缺口；20529 同形。修复后两侧各 1 条；客户端动作图和 XML 目标阶段/后置顺序静态核对通过。
- `xmllint --noout --schema src/main/resources/aion/data/static_data/quest/definitions/quest_definition.xsd` 对两份 XML 均通过；`git diff --check` 通过；IDEA 的 Java 测试文件错误检查为 0。这些检查不代替编译或游戏实机验证。
- 2026-09-24 用户授权后执行 `mvn -B test -Dtest='JournalReportRowSplitContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest'`：20 例通过、失败 0、错误 0、跳过 0、BUILD SUCCESS；分别为 9/1/10 例。当前工作区生产门禁输出 `PRODUCTION_COMPILE_OK=2441`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。该计数是本次工作区运行结果，不与旧目录规模混用。
- 验收前预期：首次点击倒下的德扎波波应下发 `questId=10529/page=4080`，依次 4081、4082；点击页内结束对话应上行 `10009`，下发 `START/var0=10`、发物品并传送，随后到代理人报告。20529 为独立客户端验收；若以后仍只见 questId=0/page10，需确认服务器实际载入的定义版本和该 NPC 的模板 ID。

## 2026-09-24 代理人报告阶段续查（待验收）

- 用户提供的 10529 客户端截图显示任务书高亮“带上陷入沉睡的德扎波波，向代理人维达报告”，称在 NPC 806075 处“没有任务”。截图证明客户端已显示报告行，但未附该次 NPC 交互的发包日志，不能仅据截图断言服务器下发了哪一页。
- 两侧 `s10/START/var0=10` 的代理人 806075/806079 原先只有 `QUEST_SELECT(31)` 入口；`TalkEventHandler.onTalk` 首次交互先发 `USE_OBJECT(-1)`，没有任务选择行时会回退到通用页。修复前静态探针在 10529 报告阶段返回 0 条直接入口（红）；现已为两侧 `s10` 各补 `USE_OBJECT(-1) → SELECT11(6500)`，保留原有 31/6501/6502/10255 与 `reward` 态的领奖预览路由，不放宽全局普通 NPC 对话分流。
- 客户端 active HTML 映射：`SELECT11(6500) → SELECT11_1(6501) → SELECT11_1_1(6502) → SET_SUCCEED(10255)`。旧 handler 两侧也有代理人在 var0=10 时直接发任务页的入口，但旧流程把倒下贤者交接提前置于 REWARD；当前定义的独立 `START` 报告行须先建立任务上下文才可完成该链。
- 新增 `JournalReportRowSplitContractTest#agentInitialTalkOpensTheReportPageWithoutAClientQuestRow`，从 `START/var0=10,var1=0,var2=3` 检查入口 planner 不改进度、对话链及最终 `REWARD/var0=11` 事务/提交后同步。静态回归探针两侧各 1 条报告入口并覆盖 31/6501/6502/10255；两份 XML 通过 XSD、`git diff --check` 通过、IDEA 测试文件错误检查为空。
- 2026-09-24 聚焦及生产门禁已按授权通过；用户随后明确回复“客户端验证成功，提交”，按规则确认 **10529 整条任务客户端 ACCEPTED**，并授权本地提交。无成功后的逐包轨迹/启动日志，记录为 `not captured`；20529 仅共享静态/测试合同，仍为 PENDING_CLIENT。代理人预期入口为 `questId=10529/page=6500`，最后动作 `10255` 应进 `REWARD/var0=11`。完整字段见 `.agents/summary/quest-acceptance/10529-2026-09-24-client-accepted.md`；Playbook 新指纹 `ACTIVE_NPC_DIALOG_MISSING_DIRECT_ENTRY` 与已有宽索引门控案例去重。
- Playbook 门禁 `python3 .agents/summary/quest/check_quest_repair_playbook.py` 通过：72 个结构化指纹、60 个代表提交、78 个代表测试、49 个详细案例。门禁首次运行暴露既有 23918 测试引用已迁到 `ChainEliteLadderContractTest`；仅将 Playbook 中两处失效引用指向现存方法，不更改该族实现。`.agents/memory-bank/patterns/quest-engine.md` 及派生索引当前有并行改动，本提交不覆盖或同步这些并行文件；可复用的指纹先收录在本次 Playbook 8.49，长期模式卡待并行工作完成后安全同步。
