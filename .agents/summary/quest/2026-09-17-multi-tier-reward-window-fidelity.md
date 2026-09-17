# 2026-09-17 多档奖励组声明与档位奖励窗口一致性专项排查

## 1. 排查维度与证据链

在任务掉落步数批次提交后，继续沿“奖励结算”维度做全库排查，重点核对三个面：

1. **服务端编译期校验**：`QuestXmlBlockExpander.rewardGroup()` 对 `complete-reward-index` 的越界处理（多组时严格校验、单组时按兼容契约放行）；
2. **客户端真端文案**：`data_unpacked/Dialogs/50000_59999/quest_q50023.html` 中 `select_quest_reward1` / `select_quest_reward2` 两页文案；
3. **全库体例比对**：扫描 21 个声明多个 `<reward-groups>` 的任务，核对“第 N 档入口 → `SHOW_SELECT_QUEST_REWARD_WINDOWn`”的映射。

排查结论：
- **奖励组索引越界并非现实缺陷**：编译器对多组任务已强制校验 `complete-reward-index < rewardGroups.size()`，单组任务走兼容契约（`groups.size() == 1` 时索引仅作持久化状态、不参与选组）。全库扫描确认除 `50023` 外没有任何任务使用单组 + 非零完成奖励索引。
- **发现真实缺陷（客户端可见）**：`50023` 两档兑换共用 `SHOW_SELECT_QUEST_REWARD_WINDOW1`。玩家交出 **3 个** 线索走档位 2 时，客户端渲染的是 `select_quest_reward1` 文案“收到了 1 个线索”（真端档位 2 文案为“您有 3 个线索啊”），档位文案与实际兑换数量不符。
- **发现潜在体例风险**：`50023` 把两档奖励平铺在单个 `<rewards>` 中，只能靠 `fixed-reward-indices` 硬索引区分档位，与其余 20 个多档任务（如 `80296` 的 `reward-group action=SETPROn index=k page=WINDOWn` 体例）不一致。

## 2. 修复内容（quest 50023，天族事件兑换）

1. **元数据按档位声明**：由单个 `<rewards>`（平铺小盒 + 大盒）改为两组 `<reward-groups>`：
   - group 0 = 小盒 `188051780`（1 个线索档）
   - group 1 = 大盒 `188051782`（3 个线索档）
2. **档位 2 入口下发对应奖励窗口**：`started -> r2` 的两条路径（`SELECTED_QUEST_REWARD2` 与 `CHECK_USER_HAS_QUEST_ITEM` 3 个线索）由 `SHOW_SELECT_QUEST_REWARD_WINDOW1` 改为 `SHOW_SELECT_QUEST_REWARD_WINDOW2`；档位 1 保持 `WINDOW1`。
3. **档位 2 结算按自身组解析**：`npc-complete source="r2"` 保持 `complete-reward-index="1"`，固定奖励索引改为 `0`（指向 group 1 自身的大盒，而非依赖单组平铺的硬索引 1）。
4. 档位 1 结算保持 `complete-reward-index="0"` + `fixed-reward-indices="0"`（group 0 小盒）。

修复后两档奖励发放、持久化奖励索引、客户端窗口文案三者一致。

## 3. 新增全库回归门禁

`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java`：

1. **`multiTierQuestsNeverDeclareDeadRewardGroups()`（全库死档门禁）**
   对声明多个 `<reward-groups>` 的任务，要求每一档都存在可被发放的完成路径：
   - 该档索引被 `CompleteQuest.rewardIndex()` 引用，或
   - 存在一条完成路径，其显式 `grant-reward` 的 `(kind,id,amount)` 多重集合与某一档声明内容完全一致。
   这样既覆盖 `npc-complete` 选组体例，也覆盖 `1122`/`2513` 这类“按档位手写 `grant-reward` + `complete-quest reward-index=0`”的合法体例（该体例奖励发放正确，只是持久化索引沿用 0）。
2. **`quest50023TiersUseTheirOwnClientRewardWindow()`（专项）**
   断言 `50023` 声明 2 组、group 内容分别为小盒/大盒，且档位 1（1 个线索）下发 `WINDOW1`、档位 2（3 个线索）下发 `WINDOW2`。

## 4. 验证结果

```bash
mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest
```

- `QuestMovieAndDialogLoopRegressionTest`：13 项全绿（含新增两项门禁）；
- `QuestDefinitionDirectoryLoaderTest`：全服 6,186 个可执行任务状态图 0 死胡同；
- `CompletedQuestPrerequisiteRegressionTest`：全服前置有向图 0 环；
- `QuestClientContractGateTest`：客户端契约基线 0 致命缺陷；
- `ProductionCatalogWhitelistVerificationTest`：`PRODUCTION_COMPILE_OK=6186, FAILURES=0, WHITELIST_VIOLATIONS=0`。

## 5. 架构沉淀

新增架构模式 `[QE-026]` 二十四、多档奖励组声明与档位奖励窗口一致性 (REWARD_GROUP_TIER_FIDELITY)，同步至
`patterns/quest-engine.md`、`systemPatterns.md`、`symptom-index.md`，并通过 `sync_memory_bank.py` / `check_memory_bank.py` 校验。
