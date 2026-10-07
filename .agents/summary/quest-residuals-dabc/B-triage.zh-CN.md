# 遗留问题 B 组专项：36500/46500 阵营日任族 17 个（领奖行越界与轴口径）

- 日期：2026-10-07
- 触发：QE-054 全量收口 AUDIT §8 遗留问题 B（"started 无 var0 的表车道混合型，reward=1、retail={0}，非本模式"）。
- 结论：**36512 真错已修（reward 2→1 + 自愈边）；其余 16 个维持不修（在界内、镜像对称、无实机报障）；新增行内界门禁。**

## 1. 族形态与 "started 无 var0" 的来源

- 17 个 = 天族 36500/36504/36505/36506/36510/36511/36512/36516 + 魔族 46500/46504/46505/46506/46510/46511/46512/46516/46517；
  retention owner=XML_RETENTION、reason=SCRIPTED（有 XML），同时登记在真端 `quest.xml`/`npcfactions_quest.xml`（"混合型"指此）。
- `started` 无 var0 是**有意设计**：`cfddeead8`（2026-08-21，COUNTER_SOURCE_PROJECTION_NO_LOCK）把 135 个实时计数任务的
  START 投影移除，避免 quest 计数自环在首次自增后被 source 投影全等匹配挡死。
- 客户端任务书（HTML 实读）：全族 2 行 —— 行 0 = "前往感应区，找到并消灭 X ([%2]/1)"，行 1 = "向 E_365xx 报告"。

## 2. 显示模型校准（用于判定"投影越界"）

实机已验收样本反推：**客户端按 `row[axis]` 显示**——
- 4338（客户端 7 行，已验收 reward=5）= 行 5「把 quest_4338b 扔到岩浆中」，即"最后玩法步"行，非末行「向 Gundalpun 报告」；
- 10525（8 行，QE-054 修 reward=6）= 行 6「使用 quest_10525d」；
- 1361 实机空白案例：投影 2 > 末行 1 → 整块空白（越界即空白）。

## 3. 真端脚本证据（ScriptDLL64.c）

- 36500 `FUN_180edaad0` / 36512 `FUN_180edaa90` 同形：`0xd0` 读 (status,var) → `status==3 && var==0` 时
  `0x100(quest,0,0)`（状态推进**不写轴**）→ `0x110(quest, 0, 0, 1, 1, ...)`（轴 0→0）。
  即**真端从未写轴（轴恒 0）**，与 crosscheck 的 retail={0} 一致。
- 同形对照：4338 的 `0x110(0x10f2, 4, 5, ...)` 写 5 → 其已验收 reward=5。故 0x110(quest, from, to, …) 的 3/4 参位 = from/to 成立。
- 槽位注册：`0x3070(..., questId, 4, 0xffffffff, 0)`（kind=4，与 10525 族 kind=3 逐值注册不同形）。

## 4. 判定

| 组 | 现状 | 判定 | 依据 |
|---|---|---|---|
| 36512 | reward=**2**，客户端 2 行（越界） | **真错，已修** | 越界 = 空白形态（1361 先例）；镜像 46512=1；同族其余 16 全为 1 |
| 其余 16 | reward=1（=末行"报告"行，在界内） | **不改** | 在界内且镜像对称；0（真端轴）会把显示翻回"击杀"行（row[axis]），16 个任务改语义需一次客户端观测，禁止无证据机械改（QE-054 边界） |

## 5. 修复明细（36512）

- `quests/36512.xml`：reward 节点 var0 2→1；s1→reward 击杀边写值 2→1；
  新增无 source 进世界自愈边 `REWARD/var0=2 → set 1`（LEVEL_AND_VISIBILITY_REFRESH），修复已落盘的越界旧档。
- `xmllint + quest_definition.xsd` validates 1/1。
- 新门禁 `FactionDailyRewardRowInRangeContractTest`（definition 包）：
  ① 17 个 reward 行 ≤ 客户端行数-1（读 `quest_client_summary_rows.tsv`，缺登记即红）；
  ② 8 对镜像 reward 行相等；③ 36512 的 reward=1、两条推进边写 1、自愈边结构。

## 6. 残留与后续

- 真端轴恒 0 与族内 1 的口径差：若未来要按真端 0 对齐（16 个 + 36512），需先做一次客户端观测
  （REWARD 态任务书显示"击杀(1/1)"行还是"报告"行）再决定；本次不做。
- 门禁只在 XML 定义侧锁行内界；native 车道不涉及本族（XML_RETENTION）。
