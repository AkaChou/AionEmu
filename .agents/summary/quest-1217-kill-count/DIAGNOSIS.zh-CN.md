# 任务 1217《해안을 더럽히는 상자 / Flotsam》——「提示 7、实际 10」诊断

- 日期：2026-10-07；性质：只读诊断（未改生产源码、未改客户端数据、未提交）
- 报障：玩家看到任务提示「击杀 7 个箱子怪」，实际要杀 10 个才推进到报告步骤
- 结论：**服务端（10）与真端表、客户端自身门控、韩文原页三方一致；显示「7」的是国服 CHS 本地化覆盖页的页面文本**（客户端数据侧残留），不是服务端编译/计数缺陷

## 1. 证据链（按数据源）

| 数据源 | 位置 | 1217 的击杀上限 |
|---|---|---|
| 真端表（生产编译源） | `<真端根>/Map/XML/Quest_SimpleHunt.xml:411`（本仓副本 `src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml:156`）`count1=10, monster1=FakeBox_19_n, Mimic_19_n` | **10** |
| 客户端自身门控 | `<客户端目录>/data/Quest/Quest.pak` → `quest_monster.csv:167`：`1217,Progress(SECTION_0<10; SECTION_5==0),,simpleQuest,,2,fakebox_19_n,mimic_19_n` | **10** |
| 客户端基础页（韩文） | `<客户端目录>/data/Dialogs/Dialogs.pak` → `QUEST_Q1217.html`：`…를 처리하라([%2]/10)` | **10** |
| 客户端 CHS 覆盖页（玩家实际看到） | `<客户端目录>/L10N/CHS/Data/data.pak` → `Dialogs/QUEST_Q1217.html`：`清理污染坎塔斯海岸的[%dic:STR_DIC_M_Mimic_19_n]([%2]/7)`；接取台词「您能帮我清除掉**7个**…其余的我会让部下们慢慢清除掉」 | **7** |
| 本仓退役 XML（历史实现） | `git show 4ede058c0^:src/main/resources/aion/data/static_data/quest_definition/quests/1217.xml`：`<counter-grid><dimension field="var0" required="10" npc-ids="210197 210085"/>` + k1..k10 | **10** |
| 本仓当前 IR（真端表编译） | `simple-hunt-reconciliation.tsv:22`：`var0@off0` / `var0:10x[210197,210085]` | **10** |

客户端 L10N 覆盖包优先于基础包，因此玩家界面读到的是 CHS 页面的 `([%2]/7)`——与报障「提示 7、实际 10」完全吻合。

## 2. 类规模（全量审计，脚本见本目录）

| 审计 | 脚本/产物 | 结果 |
|---|---|---|
| 客户端门控 vs 真端计数 | `audit_client_gate_vs_retail_counts.py` → `client-gate-vs-retail.tsv` | 1,718 个 `simpleQuest` 行 **全部 MATCH**（0 处不一致，141 个无门控） |
| 韩文基础页 vs 真端计数 | `audit_client_page_vs_retail_counts.py` → `kr-vs-retail.tsv` | 500 MATCH / **1 MISMATCH**（1750：真端 15，页面 5，且页面点名的怪是旧模板 `Ab1_1131_Specter_25_An`——旧版本残留页） |
| CHS 覆盖页 vs 真端计数 | 同上 → `chs-vs-retail.tsv` | 497 MATCH / **3 MISMATCH**（1217：10→7；1750：15→5；1840：44→43） |

即：客户端页面数字整体上就是真端计数（三处偏差都是「旧版本页/本地化改写」型残留），客户端自身门控与真端 **100% 对齐**。

## 3. 页面数字为什么不可当作合同

- 1217 韩文原页的接取台词写的是「**12**개 정도면…」，而同一版本的要求是 10——连韩文原版页面的台词数字都是散的（本次报障的 7 只是 CHS 版把台词与摘要一并改成了 7）。
- 1750 的页面（韩文、CHS 两边）都停在 5 并点名旧模板怪，真端要求 15。
- 1840 页面（CHS）写 43、韩文页写 44、真端 44。

## 4. 技术风险说明（若按页面改成 7）

客户端行阶梯/门控数据 `quest_monster.csv` 写死 `SECTION_0<10`（见 QE-125：hunt 行阶梯以客户端行驱动）。服务端若提前到 7 推进，客户端自身的 hunt 门控仍认为该段未满，存在「服务端已进报告步、客户端仍显示击杀步」的显示不一致风险；同时该改动会偏离真端表与仓库真端数据的一致性口径。

## 5. 处置（用户裁定：按客户端页面的 7 收口）

### 5.1 已执行（服务端）

- `src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml` 任务 1217：
  `count1` 10 → **7**，并在行内加双语偏差注释（指向本文件）。
- 静态校验：`xmllint --schema Quest_SimpleHunt.xsd` 通过；Python `ElementTree` 解析通过
  （1863 行不变，1217 解析为 `count1=7, monster1=FakeBox_19_n, Mimic_19_n`）；`git diff --check` 无空白问题。
- 影响面（服务端）：`NativeQuestTableLoader` → `SimpleHuntHandler`（目标/计数）、
  `RetailHuntCounterLayout.goal/isComplete`（完成值 = 7，第 7 杀写完成态进 REWARD）、
  `CameraRegistry`（fullValue 由同一行派生，自动跟随）。测试侧无硬编码 10：
  `QuestSimpleHuntRetailContractTest` 对 handler 拥有（native 车道）的退役行提前放行，
  `RetailSimpleHuntTableTest` 断言的是 1102/1517/1365，均不受影响。
- 聚焦门禁（2026-10-07，IDEA MCP，用户授权）：`SimpleHuntNativeFamilyGateTest` 5/5、
  `RetailSimpleHuntTableTest` 3/3、`RetailOwnershipGateTest` 5/5、
  `QuestSimpleHuntRetailContractTest` 1/1、`RetailTableSchemaGateTest` 2/2，全部通过。

### 5.2 待定（客户端）

- CHS 页面本身就是 7，**无需改页面**。
- 但客户端自身门控 `<客户端目录>/data/Quest/Quest.pak → quest_monster.csv:167` 仍是
  `Progress(SECTION_0<10; SECTION_5==0)`（=真端值、=客户端任务书进度合同）。按仓库既有认知
  （QE-012 客户端步骤校验、headless `QuestMonsterProgressContractAuditTest`「客户端在 SECTION_0==1
  时才计数对应行」），客户端任务书的行可见性由该门控决定；服务端停在 7 时该门控仍成立，
  **存在「服务端已进报告步、客户端任务书仍停在击杀行」的实机风险**。
  ⇒ 若实机复测出现该现象，补一条 CPK-001 口径的客户端单条目补丁：
  `data/Quest/Quest.pak` 内 `quest_monster.csv` 的 1217 行 `SECTION_0<10` → `SECTION_0<7`
  （以客户端现用文件为基准、单条目替换、出货前核对差异条目数 = 1）。

### 5.3 同型页面偏差不同批处理的理由

CHS 页面对真端 count 的偏差只有 3 例：1217（10→7）、1750（15→5）、1840（44→43）。
1750 的页面（韩文/CHS 两版）都停在对不上真端的旧模板怪 `Ab1_1131_Specter_25_An`（真端点的是
`Ab1_1132_Specter_27_An`），是旧版本页残留，按页面改会与真端目标集冲突；1840 是「All」聚合口径的
差 1（页面 43 vs 门控/真端 44），与客户端自身门控冲突。两者都不按页面口径改，保持真端值。

## 6. 未执行项

- 未启动、停止或重启服务端进程（改动需用户重启后才生效）。
- 未修改/未重打包客户端 pak（用户选择先实机复测；若任务书仍停在击杀行，再按 §5.2 做单条目补丁）。
- 实机复测（杀 7 只箱子怪是否推进到报告步、任务书是否同步）由用户执行。
- 未提交 git（提交需用户授权；提交时本目录证据与 `Quest_SimpleHunt.xml` 同批入库）。

