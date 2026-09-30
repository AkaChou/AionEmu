# client-dialog-mapping 与无头客户端迁出记录（2026-09-30）

本仓库不再包含客户端页面/动作映射数据与无头客户端对拍层；它们整体迁到本地独立项目
`AionEmu-headless`（仓库外，未纳入本仓库 git）。本记录是**在库证据入口**：原
`docs/quest/client-dialog-mapping/*.csv` 的引用统一改指本文件。

- 迁出前版本：搬运时的 HEAD `edac52b6c`；随后并发车道提交 `3bc4b6e23`（只动真端编译器与任务 XML，未触及本目录）
- 迁出内容：数据 15 个文件 + 代码 48 个文件，共 63 个文件
- 迁出后本仓库保留：`src/main/java/**` 全部生产代码、`src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`、
  `src/main/resources/quest/quest_client_*.tsv`（运行时冻结表）、进程内运行时夹具（`QuestE2eRuntime`/`QuestE2eWorldFixture`/
  `QuestProtocolLoop`/`ClientActionOutcome`/`ClientActionBridge` 等）及其不依赖客户端数据的测试。

## 迁出的数据

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `docs/quest/client-dialog-mapping/README.zh-CN.md` | 11041 | `b1e81dd1a0d6b04f…` |
| `docs/quest/client-dialog-mapping/client-html-pages.csv` | 276770 | `c6fa80b446b3c3a5…` |
| `docs/quest/client-dialog-mapping/client-hyperlinks.csv` | 178458 | `c332af87a10a194f…` |
| `docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv` | 493766 | `86dabb2b69d7f116…` |
| `docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv` | 2142423 | `85016518d757e07f…` |
| `docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv` | 2215428 | `2a8bfb995d837b3b…` |
| `docs/quest/client-dialog-mapping/mapping-summary.json` | 1594 | `0bd7401463b3403d…` |
| `docs/quest/client-dialog-mapping/page-action-map.csv` | 113859 | `56a8dbdec5d10895…` |
| `docs/quest/client-dialog-mapping/parse-errors.csv` | 21 | `d92cf0777fa7e14b…` |
| `docs/quest/client-dialog-mapping/parse-recoveries.csv` | 12587 | `23a2217e33eda6b2…` |
| `docs/quest/client-dialog-mapping/quest-action-summary.csv` | 25950 | `0af6e9f4ea5e931c…` |
| `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` | 11749334 | `3db5abbe0426d4ff…` |
| `docs/quest/client-dialog-mapping/quest-dialog-pages.csv` | 14238719 | `a4b878f712724f40…` |
| `docs/quest/client-dialog-mapping/same-id-map.csv` | 490041 | `759a8521ec37322c…` |
| `docs/quest/client-dialog-mapping/same-symbol-map.csv` | 561317 | `237f75c86166542f…` |

## 迁出的代码

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/ItemCollectingDialogProtocolAlignmentTest.java` | 16096 | `d7c191eb5016c586…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest11031And11032RetailFlowTest.java` | 17673 | `efed7930ea9c3c1d…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest13704ClientDialogAlignmentTest.java` | 5716 | `27ab928d043dcf7b…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest13708ClientDialogAlignmentTest.java` | 5735 | `5f86cd87b3e694f6…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest13951And23951RewardOwnerTest.java` | 6065 | `181ca713445f987c…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1466ClientDialogAlignmentTest.java` | 9181 | `b7ad4f2d26f1a0d4…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest15334And25334And30800ClientDialogAlignmentTest.java` | 6764 | `04ba8d6caab7a3b3…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest19048And23704And23708And29048ClientDialogAlignmentTest.java` | 6934 | `412387d92eaf0504…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest2393And3722ItemPlayRewardOwnerTest.java` | 7447 | `c8a1e191030d0e61…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest25670ClientDialogAlignmentTest.java` | 5872 | `455db8acb332ec4f…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest28821DialogRouteRegressionTest.java` | 2977 | `402068b504bb57a4…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest28931ClientDialogAlignmentTest.java` | 7822 | `3bdbae8f92398e69…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest3100ClientDialogAlignmentTest.java` | 8204 | `a4aa509253e6f45b…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest4914ClientDialogAlignmentTest.java` | 4370 | `f252204099d8d9f7…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestBClassRouteContractTest.java` | 23534 | `7e3eeeec76c6e42a…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestClientContractGateTest.java` | 7720 | `92627dea288c0280…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialogOrderAudit.java` | 30593 | `ca5b1896ad7a1054…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialogOrderAuditTest.java` | 26479 | `235355e9a86b1237…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialogSequenceAudit.java` | 10257 | `1a538b58774b3b03…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialogSequenceAuditTest.java` | 7503 | `e26059f85cbd72cd…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMonsterProgressContractAuditTest.java` | 88470 | `d54a2a31e9772b65…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieContinuationGateTest.java` | 9822 | `4cecae2d20f7b73a…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPrematureRewardRouteAudit.java` | 6121 | `15caa5ce50aeac4f…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPrematureRewardRouteAuditTest.java` | 5608 | `6d99367069ef0299…` |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/ReportToManyLegacyFlowRegressionTest.java` | 10590 | `29926ba0148a9404…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/ClientTaskScopeAuditTest.java` | 5457 | `661c158cb94323f4…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/HandoverContinuationContract.java` | 3980 | `812b1f9c579301d0…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/LegacyQuestEvidenceOracle.java` | 7288 | `10c6dc45762b9c72…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestE2eAudit.java` | 2584 | `6a9e4479dc2192e4…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestE2eAuditRow.java` | 1738 | `0a834ce4d923e6f7…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestE2eBatchAudit.java` | 32266 | `955cc73fa56563a9…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestE2eInfrastructureTest.java` | 44133 | `e7e96094fa399115…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestE2eReportWriter.java` | 5491 | `e85f4b230749f22a…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestEquippedStartProductionFlowTest.java` | 7119 | `b6060db76c0a1afc…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestExclusiveSiblingAttributionTest.java` | 4322 | `c75bed65359b09bf…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestGoldenJourneyTest.java` | 11807 | `0fe3db4cc9f3af3a…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestHandoverContinuationAuditTest.java` | 10774 | `bc517e7171f3df10…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestPageButtonAuditTest.java` | 3172 | `06d22ab97422d969…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestProductionJourneyAudit.java` | 12041 | `23186a23124dfe9d…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestProductionJourneyTest.java` | 12338 | `5d2a2a157c4dedad…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestWorldReachabilityOracle.java` | 7105 | `0331966bae358d11…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestWorldReachabilityOracleTest.java` | 3802 | `0e9ebd2a8722cf55…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/RetailQuestContractTest.java` | 33100 | `2bc3a9077f8204e3…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/client/ClientResourceOracle.java` | 10278 | `9ee9872fdacbab5b…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/client/QuestHeadlessClient.java` | 7282 | `b26191b5450a1a08…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/journey/QuestJourneyRunner.java` | 14403 | `41fcb63a171ed97d…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/journey/QuestProductionJourneyExecutor.java` | 17261 | `77fa052b3a22624e…` |
| `src/test/java/com/aionemu/gameserver/questEngine/e2e/journey/QuestProductionJourneyPlanner.java` | 33771 | `32644f2f2592ce86…` |

## 影响与边界

- 本仓库**不再运行**客户端页/按钮对拍与真端生命周期黑盒门（`QuestClientContractGateTest`、`QuestMovieContinuationGateTest`、
  `QuestMonsterProgressContractAuditTest`、`QuestDialogOrderAudit*`、`QuestBClassRouteContractTest`、
  `ReportToManyLegacyFlowRegressionTest`、`RetailQuestContractTest`、`QuestGoldenJourneyTest`、`QuestHandoverContinuationAuditTest` 等）。
  这些断言随代码迁出，需要在 `AionEmu-headless` 对当前 server 修订运行。
- 本仓库保留的运行时表（`client_dialog_contract.tsv`、`quest_client_*.tsv`）**内容未变**，仍由 `Retail*GateTest` 覆盖其真端侧行为；
  但它们与客户端快照的来源哈希不再由本仓库校验。
- 迁出项目当前状态：只做了本地搬运（未建 pom、未跑测试），需要时再补 `test-jar` 依赖并跑通。
