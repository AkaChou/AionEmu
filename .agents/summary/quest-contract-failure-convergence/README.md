# 全量门禁 12 项失败收敛（2026-09-16）

- 状态: 静态门禁全绿（`mvn -B test` → 3277 用例 / 0 failures / 0 errors / 2 skipped / BUILD SUCCESS）；3329 已于 2026-09-16 客户端验收（正常完成链路 trace，见 `quest-acceptance/3329-2026-09-16-client-accepted.md`），28301/80805 客户端验收待做
- 日期: 2026-09-16
- 前置: `ecae0bb4f` 之后的全量套件仍有 12 项失败，本批逐项收敛

## 一、数据/AI/风轨期望漂移（4 项，改测试）

| 测试 | 根因 | 修复 |
|---|---|---|
| `RetailAiDefinitionLoaderTest.loadsCompleteRetailDefinitions` | `a7da0ad67` 把 LF4_M 区域迁到 LF4（210050000），同名 `LF4_Dramata_OutArea` 已在目标世界，去重后 134→133；`5ccb10261` 补 9 条塔洛克条件刷怪（4430→4439）；`a7da0ad67` 删除 9 个重复风箱（288→279） | 期望更新为 133 / 4439 / 279，并补注释 |
| `RetailAiDefinitionLoaderTest.loadsOnlyCompleteWorldScopedRetailWaypoints` | `5ccb10261` 新增 1 条完整 `retail:300190000:idelim_path_1f_sheluk_keynm_52_ae_1` 路径模板（3206→3207） | 期望更新为 3207，并新增该模板存在断言 |
| `AI2EngineRetailSelectionTest.selectsCompleteRetailPatternsButPreservesScriptedActionItemProtocols` | 烟雾 AI 已由 `kinquid_debuff` 更名为 `Elim_SmogEffect`（`KinquidDebuffAI2.@AIName`），旧名只存在于测试保留名单 | 名单改为 `Elim_SmogEffect` |
| `WindstreamDefinitionLoaderTest.loadsRetailWindstreamsWithCanonicalAndCompatibilityMapIds` | `a7da0ad67` 把 LF4_M 路线同时映射到可玩世界 210050000，但 group 77（77001）本属 LF4（210050000）；旧断言把 LF4 自身路线误判为泄漏 | 改为断言 210050000 存在、210130000 不存在 |

## 二、审计自测与实现不同步（2 项，改测试）

- `reportsAnUnconditionalRewardRouteWhenTheSourceHasANextStage`：审计固定输出 `progress-targets=[...]`（`QuestPrematureRewardRouteAudit.java:59`，生产门禁同格式），测试仍断言 `[stage]` → 更新期望。
- `reportsEveryUnconditionalChoiceOnTheSameClientPage`：实现只对“同来源+同 NPC+同奖励节点”的重复选择判定 `MULTIPLE_UNGUARDED_REWARD_CHOICES`，与相邻用例 `allowsDistinctRewardBranchesOnTheSameClientPage` 的自相矛盾 → 把 fixture 从 reward0/reward1 改为同一 `reward` 节点，保留用例意图。

## 三、编队分组（1 项，改测试）

`InstanceWalkerFormationsPositionGroupingTest.separatesExtraSoloUnitFromTheobomosKrallOffsetFormation`：`groupCandidates` 只与组锚点比较，组顺序取决于 spawn 文档 spot 顺序；当前结果为 {2,1}，用例却按 {1,2} 断言。改为断言尺寸集合 `[1,2]`，保留“孤立单位不并入小队”的不变量。

## 四、任务契约回滚/修正（5 项）

| 测试 | 权威契约 | 修复 |
|---|---|---|
| `LegacyTemplateMirrorRouteRegressionTest`（1722 / 2332） | legacy handler `var+1`、客户端 CSV、`94636797a` 守卫均要求 `s2->s3` 且 after-commit 含 `PACKET_ONLY`；2332 的材料按钮同时存在回显自环 | 期望改为 `s3` + `SyncQuestState(PACKET_ONLY)`；路由断言改为先按目标节点过滤再要求唯一 |
| `MissionItemConsumptionBatchRegressionTest`（1367） | `94636797a` 已把 1367 拆成三条带材料条件的 `started->reward0/1/2`，每条扣除整套收集物 | 断言改为逐条校验三个奖励分支 |
| `Quest3935ClientDialogAlignmentTest` | `s4` 是带誓约石条件的最终交付阶段，不是中间阶段（同测试另一用例明确要求该路由存在） | 从禁止集合移除 `s4` |
| `QuestDraupnirNpcVariantContractTest`（80805） | 同族 80734 使用 `kill-chain` + k1..k6；原 `counter` 把“第 6 杀”条件挂在无 var0 投影的 `started` 上，规划器永远不可规划 | 80805 改为 `kill-chain nodes="started k1 k2 k3 k4 k5 reward"` + 单事件双 NPC |
| `QuestClientContractGateTest`（3329/28301 SETPRO） | 审计禁止无条件下 `SETPRO*` 直领奖；但客户端按钮必须有落点 | 删除 3329 的 `a0b0->reward`、28301 的三条 `started->reward`；补 close-dialog 回退（3329 SETPRO1 为“结束对话”；28301 SETPRO2 依 legacy handler 仅 var==7 有效，其余阶段结束对话） |

## 五、保留的既有缺陷（23 条，写入 baseline）

`src/test/resources/quest/quest-client-contract-baseline.tsv` 记录 23 条 `BUTTON_WITHOUT_ROUTE`，来自后续任务批次
（`106a69bda`、`de7c36d9d`、`10a2e7e57`、`0823653a7`、`2f0752248`），涉及任务
1197、1198、2289、2367、2411、2443、2448、2922、3088、3936、3937、3940、4940、4941、80008、80009。
这些是“客户端可见按钮无服务端路由”的真实缺口，本批**未修复**，仅按门禁设计保留；新增指纹仍会立即红灯。

## 六、门禁结果

- `mvn -B -DskipTests compile`（touch 后全量重编译）: 4718 源文件、release 25、BUILD SUCCESS
- 聚焦: 数据/AI/风轨 10/10、审计 4/4、编队 7/7、任务契约 44/44（含 `QuestClientContractGateTest`）
- 全量 `mvn -B test`: **3277 / 0 failures / 0 errors / 2 skipped / BUILD SUCCESS**
- 2026-09-16 17:55–17:59 提交前重跑（工作区含并行 Taloc/drop 测试）: **3289 / 0 failures / 0 errors / 2 skipped / BUILD SUCCESS**
- 未执行 `mvn clean`（IDEA 启动的服务进程在跑）；构建日志在仓库外 `/tmp/aion-*.log`

## 七、剩余风险

- 任务 XML 三处（3329/28301/80805）：3329 已由用户于 2026-09-16 客户端验收成功（正常完成链路 trace 17:50:59–17:52:07；提前 SETPRO1 子场景无日志片段，仅用户口述确认，见 `quest-acceptance/3329-2026-09-16-client-accepted.md`）；28301 拾取动力装置与 k7 领奖、80805 六杀进度与奖励弹窗仍待 Aion 5.8 客户端验收。
- baseline 中 23 条既有缺口待另立任务修复。
