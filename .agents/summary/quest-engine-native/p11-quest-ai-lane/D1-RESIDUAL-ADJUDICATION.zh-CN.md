# D1 残余裁决：Quest-AI 集外对话位 / 真端无注册任务（85 件）

- 日期：2026-10-03；性质：**只读取证 + 证据登记**（零生产数据/代码变更）。
- 上游：`P11-PREREQ-QUEST-AI-LANE.zh-CN.md` §6（D1 落地）§6.4（残余面）。
- 工具：`audit_residual_bindings.py`；产物：`d1-residual-adjudication.tsv`（85 行 × 16 列全轴明细）。

## 1. 一页结论

D1 冻结面之外还有 **85 件**任务落在「Quest-AI 绑定面」证据缺口里（67 件集外对话位 + 18 件真端无注册）。
本批用 5 条独立轴逐件对拍，**85/85 全部至少命中一条轴，0 件对话 NPC 不存在于真端 npcs.xml**；
只剩 **5 件**退化到「仅客户端页 + 真端 quest.xml 行」的单轴证据，登记为待验收项。

| 判定 | 件数 | 证据 | 处置 |
|---|---|---|---|
| `LEGACY_BACKED` | 43 | 遗留/客户端对话契约（`origin/history` compact quest scripts）的 start ∪ end ∪ progress NPC 覆盖全部 XML 对话位 | 采信 |
| `DD_BACKED` | 22 | 真端 `data_driven_quest.xml` 行的 `value0_acquire_` / `reward_npc_name` 解析出的 npc id ∩ XML 对话位 ≠ ∅ | 采信（真端设计数据背书） |
| `SCRIPTED_REGISTRY` | 10 | `retail-xml-retention.xml` 的 `reason=SCRIPTED` + `evidence=registry=FUN_180cab5xx/FUN_180caf3xx…`（真端按 **quest id** 的直驱脚本口） | 采信（对话位由 quest-id 直驱脚本承担，不走 NPC 名字面） |
| `PLAIN_NPC_WITH_CLIENT_PAGES` | 3 | 对话位是**无 `quest_ai_name`** 的通用 NPC（如 701466/701467 巧克力塔、202549 ShugoL）+ 客户端 active 页 | 采信（通用任务对话 + quest-id 直驱） |
| `DD_REWARD_DEFERRED` | 2 | 15690 / 25690：`ADJUDICATED:RETAIL_REWARD_NPC_UNRESOLVED`（真端 `reward_npc_name` 服务端解析不到） | 已登记，沿用既有裁定 |
| `CLIENT_PAGES_ONLY` | 5 | **1003 / 2005 / 2230 / 2288 / 14013**：真端 `quest.xml` 有行 + 客户端 active 页（9–19 页）+ NPC 存在且带 `quest_ai_name`，但该 name 在 ScriptDLL 字面零命中；无 legacy / DD / scripted 轴 | **待逐件取证（本批唯一开口）** |

轴分布（85 件分母）：NPC 存在性 = 85/85 存在（0 缺失）；legacy = COVERED 43 / PARTIAL 6 / CONFLICT 1 / NO_ROW 35；
DD = MATCH 35 / MISMATCH 2 / NO_ROW 48；客户端 active 对话页 = 77/85 有。

## 2. 方法与输入

| 轴 | 输入（外部数据根按名引用） | 口径 |
|---|---|---|
| 1 NPC 存在性 / `quest_ai_name` | `<真端根>/Map/XML/npcs.xml` | id 命中 = 存在；有 `<quest_ai_name>` = 真端声明为 Quest-AI NPC |
| 2 遗留/客户端对话契约 | `<客户端解包根>` 同源的 headless 抽取仓 `AionEmu-headless` → `data/client-dialog-mapping/legacy-quest-dialog-contracts.csv`（sha256 `85016518…86b6adb7`，与 `quest_client_handin_npc_sets.xml` 同源） | `start_npc_ids ∪ end_npc_ids ∪ progress_npc_ids ⊇ XML 对话位` |
| 3 真端 DD 设计数据 | 仓内 `quest/retail/data_driven_quest.xml` | `value0_acquire_` / `reward_npc_name` 名字 → npcs.xml（name ∪ quest_ai_name，大小写不敏感）解析出 id |
| 4 客户端页 | 同上 `AionEmu-headless` → `data/client-dialog-mapping/quest-dialog-pages.csv`（Aion 5.8 `Quest.pak` HTML，`source_variant=active`） | 命中 = 客户端确有该任务的对话页 |
| 5 quest-id 直驱脚本 | 仓内 `retail-xml-retention.xml`（`reason`/`evidence` 列） | `reason=SCRIPTED ∧ evidence=registry=FUN_…` |

复现：

```bash
python3 .agents/summary/quest-engine-native/p11-quest-ai-lane/audit_residual_bindings.py
```

## 3. 关键事实

1. **零「凭空对话位」**：85 件的全部 XML 对话 NPC 都存在于真端 `npcs.xml`（id 级），没有一件引用不存在的 NPC。
2. **无注册 ≠ 无脚本（口径边界）**：D1 的「注册面」= 字面 `FUN_180cb5920(L"name", questId)` 调用（18787 处）。
   同一全局注册表 `DAT_1847204c8` 还有**数据驱动入口**（`FUN_180c44720` 于 `ScriptDLL64.c:2075210/2075774/…`
   与 `:2684218/2684237/…`，key = quest id、对象携带名字），所以「字面零命中」不等于「真端无脚本」——
   `SCRIPTED_REGISTRY` 轴（10 件）正是这一形态的落地证据。
3. **1 件 legacy `CONFLICT` 是假警报**：30800 的 XML 对话位 834987（`IDTM_M_NPC_Shugo_Manager`）与契约声明的
   834986（`IDTM_L_NPC_Shugo_Manager`）**共享同一 `quest_ai_name`**（`IDTM_L_NPC_Shugo_Manager`），
   真端名字键（`_wcsicmp` 大小写不敏感）语义下两者绑同一脚本；DD 行同时解析出这两个 id（`DD_MATCH`）。
4. **18 件「真端无注册」全部有第二证据面**：13 件 DD 行接取/交付名命中（如 10527→806075、20527→806079、
   50125→205958、51125→205960），5 件遗留契约覆盖（1195→203098 Spatalos、21030→799252、50038/50040/50041→832815）。
5. **待验 5 件的形态**：`retail-none`（真端无族表/DD 行）+ 真端 `quest.xml` 有行 + 客户端 active 页；
   其中 203081（Ozzz）在遗留契约里是 1121 的对话位、203540（Mijou）是 2117/2121/2125 的对话位、
   203129（Leto）是 1197 的对话位；203621（Sjania，2230/2288）在遗留契约中无其它引用。
   即：**NPC 本身是既有对话位，但「本任务 ↔ 该 NPC」这一条绑定缺真端/客户端背书**。

## 4. 与门的关系

- 常设门 `QuestAiDialogBindingGateTest` 的冻结清单**不变**（跨界 21 任务/25 引用；真端无注册 18 件）：
  本裁决只**登记解释**，不构成放宽——新增跨界引用/未注册任务仍然即红。
- 「集外对话位」这一轴（67 件）**未入门**：它是本批一次性普查 + 本文件登记；若日后要常设化，
  应连同 §1 的 6 类判定一起冻结（否则任何新增通用 NPC 对话位都会误触）。
- 待验 5 件（1003 / 2005 / 2230 / 2288 / 14013）建议入口 = 客户端真机走链 + 真端 NPC 侧脚本（若存在于其它模块），
  取证完成前保持 XML 车道现状（不删、不改绑定）。
