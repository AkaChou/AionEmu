# 10522/20522 创造力引导任务领奖态轴收口

- 日期：2026-10-09
- 触发：用户实机报障「10522 注入创造力后任务列表步骤为空」（2026-10-08 晚 trace + 2026-10-09 GM A/B 定谳）
- 状态：**FIX_APPLIED / GATES_GREEN（4/4）**，待用户实机复测

## 根因

batch8（bd6c9e224，2026-09-21）按「quest_summary 末行索引抬行」批次规则让引擎外写入方
`CM_CREATIVITY_POINTS#checkQuestCompletion` 先写 `var0=1` 再置 REWARD。该任务没有玩法内
进度步，真端「0x100 状态推进不写轴」——REWARD 轴必须停在接取值 0。客户端任务书按 0 基行
匹配（quest_q10522.html 的 `[%0]` 行 = 步 0），REWARD/var0=1 两行全落空 ⇒ 步骤空白。

用户 A/B 实证：`//quest set 10522 reward 0` 后任务书恢复；注入流程（REWARD/1）空白。
与 c6662f637 宝珠四任务（30211/30213/30311/30313「REWARD 必须停在落盘值 0」）及
AUDIT §D 10525/20525 自愈边反转同族；10522/20522 当时被 fullscan 判 NO_RETAIL_SETPROGRESS
留后续，本次实机暴露收口。

## 修复（6 文件）

1. `CM_CREATIVITY_POINTS.java`：删两处 `setQuestVarById(0, 1)`，只 `setStatus(REWARD)`。
   注意：注释英文句避免 `word (` 形状（`match fail (mirror...` 会被
   `audit_external_reward_advance.py` 的 METHOD_DECL_RE 误判为方法声明，产生 `#fail` 幽灵写入方行）。
2. `quests/10522.xml` / `quests/20522.xml`：reward 节点投影 `var0` 1→0；自愈边反转——
   捕获批次 8 污染档（REWARD + var0=1）→ 显式 `<set-variable field="var0" value="0"/>` + sync
   （与 10525 先例同形，落盘纠正而非仅投影下发）。
3. `external-reward-advance-baseline.tsv`：重跑审计脚本（writerStep=0/projection=0/stale=[1]）；
   脚本对 XML 已退役的 15545/25545 无生成路径（`definitions/quests/<id>.xml` 不存在即跳过），
   两行系手工维护，补回后保持 10 行与 `EXPECTED_QUEST_IDS` 对齐。
4. `Quest10522AutoStartDialogTest` / `Quest20522AutoStartDialogTest`：reward 断言 var0=0；
   自愈边断言改条件 var0=1 + `SetVariable("var0", 0)`。

## 验证

- IDEA MCP（2026-10-09 授权）：Quest10522AutoStartDialogTest 1/1、
  Quest20522AutoStartDialogTest 1/1、ExternalRewardAdvanceReentryContractTest 2/2，全绿。
- 实机复测（待用户）：重启服务端后完整链——接取（START/0，任务书显示「使用创造力点数」）→
  注入创造力（REWARD/0）→ 任务书保持有步骤 → 维达/Feregran 领奖；
  batch8 污染档（REWARD/var0=1）重登自愈归 0。

## 排除链备查

- 接取包形态：同批 10110/10525（f_mission=0）与 10522（f_mission=1）逐字节同形（addQuest START/0），前两者正常。
- f_mission=1 单因素：10101（f_mission=1）2026-10-08 20:49 接取 2 秒后推步、整链走通。
- batch8 回归面：10522.xml 与写入方自 bd6c9e224 后无逻辑变更（仅路径迁移/lint）。
- 「步 0 全任务空白」：同批 10110/10525 步 0 正常，排除。
