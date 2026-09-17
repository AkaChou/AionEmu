# 2026-09-17 奖励窗口档位页面与交付分支档位对齐专项排查

## 1. 排查维度与判定口径

在 50023 档位窗口批次之后，把“奖励窗口档位”从单任务体例升级为**引擎层契约**复核：

1. `npc-complete` 的 `<preview>` 路由必须下发**本档**（`complete-reward-index`）的客户端奖励窗口；
2. 编译器为“只写了确认路由、没写预览路由”的任务合成预览时，页面必须由**档位查表**得到；
3. 多档任务里，`REWARD` 节点的**进入窗口档位**必须与它实际**发放的档位**一致（进入窗口 = 结算档位）。

## 2. 全库扫描与证据链

扫描面：6,222 个任务 XML、真端解包 `Quest_unpacked/quest.xml`、Aion 5.8 客户端 `Dialogs/*.html`（`select_quest_rewardN` 页面）、旧版 handler 与旧 `quest_data.xml`。

- 真端 `reward_expN/reward_goldN/reward_itemN_*/selectable_reward_itemN_*` 的 N 就是档位号；客户端只声明 6 档奖励窗口
  （页面 5/6/7/8 对应第 1~4 档，页面 45/46 对应第 5/6 档）；
- 旧版 `QuestHandler.sendQuestEndDialog(env, reward)` 也使用 `sendQuestDialog(env, 5 + reward)`，即**旧引擎同样只用线性偏移**，第 5/6 档时同样会下发不存在的页面；
- 生产目录中 **5,351 个** `npc-complete` 预览块，其中 **6 个**声明了非 0 档位，全部集中在 4 个多档任务。

命中缺陷（引擎层两处 + XML 一处）：

| 位置 | 缺陷 | 玩家可见后果 | 受影响任务 |
|---|---|---|---|
| `QuestXmlBlockExpander.expandNpcComplete` | 预览路由写死 `ShowQuestDialog(5)` | 多档任务在 `REWARD` 阶段重新对话时，第 2/3 档也渲染第 1 档文案与奖励 | `1114`、`1367`(×2)、`2430`(×2)、`50023` |
| `QuestDefinitionCompiler.restoreRewardPreviewContract` | 合成预览用 `5 + rewardIndex` 线性推算 | 第 5/6 档会下发客户端不存在的页面 9/10（load fail） | 潜伏：当前依赖合成的任务档位均 ≤ 3 |
| `quests/1114.xml` | Asteros 分支 `complete-reward-index=1`、Namus 分支内联发放第 1 档，与各自进入窗口相反 | 玩家在“正直奖赏”窗口里拿到“满足愿望”的档位奖励，反之亦然 | `1114` |

`1114` 档位绑定证据：

- 旧 handler `_1114TheNymphsGown`：Asteros(203058) 分支 `sendQuestDialog(env, 5)`（第 1 档），Namus(203075) 分支 `sendQuestDialog(env, 6)`（第 2 档）；
- 客户端 `QUEST_Q1114.html`：`select_quest_reward1` 文案为“我被你的正直感动了…这是对你正直的奖赏”，`select_quest_reward2` 为“作为帮我实现梦想的答谢”；
- 真端 quest.xml：第 1 档 `reward_exp1=4367 / reward_gold1=1920`，第 2 档 `reward_exp2=3120 / reward_gold2=960`；
- 结论：Asteros = 第 1 档（GOLD 1920 / EXP 4367），Namus = 第 2 档（GOLD 960 / EXP 3120）。

`1367` 的三档窗口文案互不相同（脖子肉 / 里脊 / 火鸟大腿肉），`2430` 的三档窗口在进入时已分别声明 WINDOW1/2/3，
两者仅因预览写死第 1 档而在重开窗口时错档；`50023` 是本批次内已修任务，其 `r2` 预览在重开路径上仍回到第 1 档。

## 3. 修复内容

1. `QuestDialogPage.rewardWindowForTier(int tier)`：新增唯一的“档位 → 客户端页面”查表（0..5 → 5/6/7/8/45/46，超出返回空）。
2. `QuestXmlBlockExpander.expandNpcComplete`：预览路由改用查表页面；声明了预览却落在客户端未声明档位时以
   `NPC_COMPLETE_REWARD_WINDOW_UNSUPPORTED` 编译失败（fail-closed）。
3. `QuestDefinitionCompiler.restoreRewardPreviewContract`：合成预览改用同一张查表；无窗口可用时不再合成。
4. `quests/1114.xml`：Asteros 分支改 `complete-reward-index="0"`；Namus 分支改发放 GOLD 960 / EXP 3120 且
   `complete-quest reward-index="1"`；文件头补双语档位证据注释。

## 4. 新增全库回归门禁

`src/test/java/.../definition/QuestMovieAndDialogLoopRegressionTest.java`：

1. `rewardWindowTierMappingCoversEveryClientWindow()`：锁定档位查表（含越界返回空）。
2. `synthesisedAndDeclaredPreviewUseTheTableDrivenTierPage()`：合成预览（第 5 档 → 页面 45）与显式预览
   （第 2 档 → 页面 6）都不得再用线性推算。
3. `npcCompletePreviewsOpenTheirOwnTierWindowAcrossTheCatalog()`：端到端门禁——解析全部任务 XML，
   对每个带预览的 `npc-complete`，断言编译后 IR 中存在 `-1/1009` 预览路由且页面等于该档位页面
   （当前覆盖 5,351 个预览块）。
4. `multiTierHandInWindowsMatchTheirGrantedTier()`：多档任务中 `REWARD` 节点的进入窗口必须等于其结算档位
   （内联 `grant-reward` 命中奖励组时优先，否则回落到 `complete-reward-index`）。

## 5. 验证结果（已通过）

用户授权后执行：

```bash
mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest
```

结果：`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

- `QuestMovieAndDialogLoopRegressionTest`：19 项全绿（含本批新增 4 项门禁，逐块断言 5,351 个 npc-complete 预览）；
- `QuestDefinitionDirectoryLoaderTest`：全服可执行任务状态图 0 死胡同；
- `CompletedQuestPrerequisiteRegressionTest`：全服前置有向图 0 环；
- `QuestClientContractGateTest`：客户端契约基线 0 致命缺陷；
- `ProductionCatalogWhitelistVerificationTest`：`PRODUCTION_COMPILE_OK=6186, FAILURES=0, WHITELIST_VIOLATIONS=0`。

静态侧（未授权阶段）已完成：21 个多档任务逐条核对（`python3 .agents/summary/quest/twin-pair-scan/audit_multitier_preview.py`、
`audit_tier_window_grant_alignment.py`：0 发现）、客户端/真端证据比对、6,222 个 XML 解析通过。本会话未启动、停止或重启服务器进程。

首次授权运行曾在 `testCompile` 阶段暴露自身回归：`QuestDialogPage.fromId(int)` 被补丁误删，导致
`QuestDialogOrderAudit`、`QuestDialogXmlSyntaxTest` 编译失败；恢复该方法后重跑全绿。该过程说明门禁编译本身即是一次有效自检。

## 6. 验收边界与后续

- 完成度：**实现完成 + 门禁通过**；Aion 5.8 客户端实机验证仍待用户执行（多档任务的窗口文案与实发奖励需一次实机确认）；
- Playbook 判定：与既有案例 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`（14047，缺失页面导致 load fail）在「玩家可见症状、根因层、修复层」三处均不同（本例是引擎档位查表、症状是错档而非 load fail），属新材料；本批与 QE-024~027 保持一致，先沉淀 `[QE-028]` 记忆库模式与专项总结，代表性 case 待实机验收后再补索引条目；
- 下一维度：临时 NPC 生成后的中间态清理闭环（19 个 `spawn-npc-at-player` 无显式 despawn 的任务）。
