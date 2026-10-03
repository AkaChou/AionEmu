# P5D 步 2 报告：ItemPlay 中继链/步物品接线 + 相机闸门推进 + 旧存档自愈（实现面）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：把 P5D 步 1 坐实的真端形态**接成运行时面**——接取步 0 → 中继 `talk_npcK`（表序、动作
> `10000+K-1`、页 `select(K+1)`、步 K 发/扣）→ 用道具（真端相机闸门 `step == relayCount`，推进到
> `relayCount + 1`）→ 交付节点（REWARD）。**接线 ≠ 激活**：owner 仍为 XML 保留的中继行的激活随
> retention 重裁（与 QE-112 在飞切片共文件）另批落地。
> 日期：2026-10-01。分支：`quest`。前置：`p5d/P5D-STEP1-REPORT.zh-CN.md`（真端形态证据）、
> `p3/P3-STEP3-REPORT.zh-CN.md`（talk 族同轴中继面）。
> 批门：族门 12/12 + 行集门 3/3；族门 + tablelane **135/135**（步 1 基线 132 → +3 例）；
> 聚焦套件 **1699 / 162F+142E / 108 类**，对步 1 基线 **ADDED 0 / REMOVED 0 / changed triplets 0**。

---

## 1. 真端证据（本批闭合的那条）

| 事实 | 证据 |
|---|---|
| 行主 thunk = 本行相机：取 (状态, 步) 后带**步号闸门**推进 | `ScriptDLL64.c:2301868`（18213 = quest 0x4725）：`if (resStatus == '\x03' && resStep == 2) set(0x4725, 3, 0)`；`:2301832`（13704 = 0x3588）= `0 → 1`；`:2301796`（13400 = 0x3458）= `1 → 2` |
| 43 行全量：`闸门步 = relayCount`、`目标步 = relayCount + 1` | `p5d/tools/itemplay-shape-audit.py` 的 `camera-guard`/`camera-target` 列：**41 行 0 例外**；无相机的两行 = 80255/80256（advance 轴不存在，已冻结） |
| 客户端页契约同轴 | `client_dialog_contract.tsv`：9623 = `1011/1352/1693`（2 中继）、13400 = `1011/1352/1353/1354`（1 中继）、50048 = `1011/1352`（1 中继）、13704 = `1011`（直交形） |
| 旧编译器（真端+客户端派生契约）同形 | 已删的 `RetailSimpleItemPlayDefinitionCompiler`（`git show db1492b80^:...`）：`var0` = 1 位；`var0 == 0 → var0 = 1` 才 `started → reward` |
| 步号写在 `var0` 且**只有一个位段** | XML 保留 5 行（18213/28213/39713/49713/50048）逐行：`bit-field name="var0" offset="0" width=2..6` 各 1 段 ⇒ 整值写步号不覆盖其它字段 |
| 可接取轴：`minlevel_permitted = 999` = **停用形** | 真端 `quest.xml`（本车道 `NativeQuestStartPort` 同口径：`Quest::CanAcquireQuest` 对 `level < minlevel` 恒拒）与客户端 `quest.xml` 对 13400/23400 **双向 999** ⇒ 43 行 = **停用 27 行 / 可接取 16 行** |
| 50048 派生定义（仍在生产）同形 | `quests/50048.xml`：接取 → `s0`(var0=0) → 中继 `833460` `SETPRO1` 置 `var0=1` → 扣物品 + `var0=2` → `NPC_REPORT` ⇒ 交付步 = `relayCount + 1` |

---

## 2. 落地（实现面）

| # | 面 | 实现 |
|---|---|---|
| 1 | **中继链** | `SimpleItemPlayHandler` 增 `RelayStep(questId, step, npcId)` + `relaysByNpcId` / `relayCountByQuestId`；构造期按 `talk_npcK` **表序**解析中继 NPC（非唯一解析 ⇒ 该行不可路由） |
| 2 | **第 K 步发/扣** | `stepGiveByQuestId` / `stepRemoveByQuestId`（位置保留，缺位 null）；访问器 `stepGiveItem/stepRemoveItem`；声明了却解析不出 ⇒ 不可路由（不半接线） |
| 3 | **步页/动作** | `pageForStep(K)` = `1352/1693/2034`（真端 SELECT2..4）；中继动作 = `10000 + K - 1`，只有 `步号 == K - 1` 才推进到 K（乱序/重复零副作用），步进即发/扣 |
| 4 | **相机闸门推进** | `onItemUse`：`步号 != relayCount` ⇒ 零副作用；`步号 == relayCount` ⇒ 步号写 `relayCount + 1` + `REWARD`（旧形 `var0 == 0 → 1` 的真端等价物） |
| 5 | **旧存档自愈** | 新增 `onEnterWorld(Player)`：REWARD 态而步号仍为 0 的存档补到 `relayCount + 1`（P5 起的 native 车道推进时未写步号）；`QuestEngine.onEnterWorld` 接线（与 talk 族自愈并列，best-effort） |
| 6 | **注册面** | `installInterest` 增中继 NPC 的 `addOnTalkEvent`（仅对路由行注册，激活行不变） |
| 7 | **访问器** | `relayCount(int)` / `relaysForNpc(int)` / `relaysForQuest(int)` / `pageForStep(int)`（门与证据面用） |

**未改动**：接取面（`NativeQuestStartPort` 接取即 `setVar(0)` = 真端步 0）、交付/预览/领奖面、`item_check`
（本族 0 行声明 ⇒ 保持 fail-closed）、`cutsceneid1` 行（触发列缺失型休眠 §10.3-#21）。

---

## 3. 接线 ≠ 激活（本批的边界）

| 行集 | 面是否就绪 | 是否上线 | 原因 |
|---|---|---|---|
| 6 路由行（直交形） | ✅（新增闸门 + 步号写回） | ✅ | owner `RETAIL_TABLE` |
| 18213/28213 | ✅（中继 2 步 + 发扣 + 页 + 闸门全就绪） | ❌ | owner `XML_RETENTION`；激活 = retention 重裁 + 删 XML（**与 QE-112 在飞切片共文件 ⇒ 另批**） |
| 18828/28828 | 直交形（无中继） | ❌ | **双保险 fail-closed**：接取/交付名 `HousingManager_Li/Da` 静态数据无解，且 `con_quest` 目标 18829/28829 **不在本表**（跨表目标 ⇒ 本车道不发明路由） |
| 50048 | 数据面就绪但中继名无解 | ❌ | `NPC_event_devasday_shugoseller` 仅存在于真端表、静态数据零命中（与 `NPC_event_devasday_shugo` 同类）⇒ 名字轴 fail-closed |
| 9623/13054/23054/23562/13400/23400 | — | ❌ | **停用形（`minlevel_permitted = 999`）**：不可接取 ⇒ 长尾缺口无运行期影响（13400/23400 的过场 859/860 由此不需要接线，§10.3-#21 闭环） |
| 80255/80256、39713/49713、28 行不在生产 | ❌（真端形态本身不成立/不在生产） | ❌ | 见 §10.3-#16① 冻结 |

**证据面双向冻结**：行集门把未解析名集合改成**逐元素相等**断言（新增或消失都必须显式改表）；
本批新增登记 `NPC_event_devasday_shugoseller`（50048 的 `talk_npc1`），其余 5 个名字不变。

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 族门（ItemPlay） | `mvn -o test -Dtest='SimpleItemPlayNativeFamilyGateTest'` | **12/12**（步 1 基线 9 → +3：中继接线面 / 相机闸门 / 自愈） |
| 行集门 | `mvn -o test -Dtest='ItemPlayFamilyRowInventoryGateTest'` | **4/4**（新增可接取轴用例：逐行从 `quest.xml` 独立复算两桶 + 过场行必须落停用形 + 可接取中继行集冻结） |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest'` | **135/135**（步 1 基线 132 → +3 例） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 / 162F+142E / 108 类**，对步 1 基线 **ADDED 0 / REMOVED 0 / changed triplets 0** |

证据：`gates/2026-10-01-p5d2-family-tablane.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 未入仓）、
`gates/2026-10-01-focused-run-p5d2-red-classes.tsv`、`gates/2026-10-01-focused-run-p5d2-delta.tsv`（入仓）。

---

## 5. 残余与下一步

1. **P5D 步 3（激活批，等 QE-112 落地）**：retention 重裁 **18213/28213**（转 `RETAIL_TABLE` 并删 XML），
   同时补端到端中继用例（现在只能测到接线面与闸门，因为路由集不含中继行）；50048 与 18828/28828 保持
   `XML_RETENTION` 但**理由应改成真端形态结论**（50048 = 中继名无解；18828/28828 = 名字无解 + `con_quest`
   目标跨表 18829/28829 不在本表），而不是笼统的「talk_npc 链轴 / con_quest 前置轴」。
2. **保持冻结**：80255/80256（无相机）、39713/49713（接取哨兵）、28 行不在生产。
3. 客户端验收 `PENDING_CLIENT`（6 路由行的用物流程本批新增步号写回，建议随步 3 一并复测 19048 家族）。
