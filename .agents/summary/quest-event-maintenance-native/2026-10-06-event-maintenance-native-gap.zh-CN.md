# D 立项：事件任务维护面的 native 行缺口（EventService 登录维护 / 循环重置全灭）

> 立项日期：2026-10-06（`quest` 分支）
> 上游：1157 前置元数据回退修复（commit `fb078d13c`）的同类面排查 §9.2-D。
> 结论先行：`events_config.xml` 的 **318 个事件任务里 315 个是 native 行**（`isNativeOwner=true`，不在
> 733 行生产目录），而事件维护链在两处 metadata 面 fail-closed —— **登录发放 / 循环重置 / 重登记全灭**，
> 仅 3 个 XML 保留行（50089 / 50090 / 80707）存活。本立项给出断链点、数据面证据、修复方案与验证计划。
> **状态（2026-10-06）：已实施（D1+D2）并跑测通过，见 §8；提交随本批。**



---

## 1. 现象与影响面

- 事件任务（活动期间由 `EventService.onPlayerLogin` 发放/维护的任务）在当前迁移状态下**几乎全部不工作**。
- 影响面实测（静态复现，脚本见本目录 `scan_event_native_owner.py`）：318 个事件任务中 **315 个 native
  行**在两条断链上先后被拒，2 层 fail-closed 叠加后实际生效的只有 3 个 XML 保留行（存活率 0.94%）。
- 观感症状（实机面）：活动开启期间——
  - 新角色/未接取角色**收不到活动任务**（登录不发放）；
  - 已有进度的角色**循环事件不重置**（每日/每周活动任务不刷新）；
  - 已完成行**登录时不重登记**（活动任务从追踪列表消失且不回来）。

## 2. 断链点（代码逐环可点）

### 断链 1：登录维护门（`EventService.StartOrMaintainQuests`）

```
EventService.onPlayerLogin (EventService.java:111-135)      ← 登录钩子
  → StartOrMaintainQuests (EventService.java:145-191)
      :155  QuestMetadata metadata = catalog.findMetadata(questId).orElse(null);
      :156  if (matchesEventQuestMetadata(player, metadata, eligibility))
      :193  static boolean matchesEventQuestMetadata(...)  { if (metadata == null || ...) return false; }
```

- `catalog` = `questCatalog()`，733 个 XML 保留行；**native 行（`isNativeOwner`）= empty**
  （与 1157 案的 `XMLStartCondition` 同型/同源：行已迁入原生车道，不在目录里）。
- `matchesEventQuestMetadata:195` 首行 `metadata == null → false` → **整个维护逻辑（含循环重置、
  `startEventQuest` 调用、COMPLETE 重登记）被整块跳过**。

### 断链 2：启动/建档/重置面（`QuestService.startEventQuest`）

即使修好断链 1，`:184/:187` 调用的 `QuestService.startEventQuest`（`QuestService.java:788-830`）仍会拒：

```
:789  QuestTemplate template = questsData.getQuestById(env.getQuestId());
:790  if (template == null || template.getCategory() != QuestCategory.EVENT) return false;
```

- `questsData`（`DataManager.QUEST_DATA`）在生产中**恒等于 `QuestsData.fromCatalog(733 行目录)`**：
  三处赋值（`DataManager.java:305`、`:510-511`、`XmlDataLoader.java:314-315`）全是
  `data.questData != null ? … : QuestsData.fromCatalog(...)`，而 JAXB 面 `data.questData` 已无装载源
  （`XmlDataLoader` 中**不存在** quests.xml 的解组调用，已核）。
- → native 事件行 `getQuestById` 返回 null → `false`。**QuestState 永不会被创建/重置**。

### 断链 2 的连带：`normalizeEventQuestStatus` / 循环重置全在内层，均不可达

`StartOrMaintainQuests:157-188` 的循环重置（`completeTime` 早于活动 `startDate` → 置 START、清零
`questVar` / `completeCount`，仅 `start=true` 轮）与重登记（COMPLETE → `SM_QUEST_ACTION`）都包在断链 1
的门内；`startEventQuest` 是断链 2。**两层都修才闭环。**

## 3. 数据面证据（静态复现，2026-10-06）

脚本：`scan_event_native_owner.py`（`python3` 直接跑，口径与生产 Java 静态文件同源）。
`isNativeOwner` 复现口径 =（七族注册集 − catalog 733）∪（retention 台账 `RETAIL_TABLE ∧ family=DataDriven`）。

### 3.1 318 个事件任务的归属分布

| 类别 | 数量 | 明细 | 现有链路 |
|---|---|---|---|
| `RETAIL_TABLE` / SimpleTalk | 182 | 50007/51007/80022…80953 | **死**（断链 1+2） |
| `RETAIL_TABLE` / SimpleHunt | 16 | 80212-80229 段 | **死**（断链 1+2） |
| `RETAIL_TABLE` / DataDriven | 113 | 50073-50094、80900-80938 段等 | **死**（断链 1+2） |
| 不在 retention 台账 | 4 | 50011 / 50012 / 51011 / 51012 | **死**（同上；但 quest.xml 有行、SimpleTalk 表有行 → `isNativeOwner=true`） |
| `XML_RETENTION`（在 catalog） | 3 | 50089 / 50090 / 80707 | **活**（走通 typed 链路） |

- 断言核验：routed ∩ catalog = ∅ ✓；315（上界）+ 3（catalog）= 318，0 行未覆盖 ✓。
- `nativeMetadata` 可达性：**318/318 在 `quest.xml`（10035 行）都有行**（含那 4 个「不在台账」的行）——
  即修复后断链 1 的回退面 315 行全部有真端元数据可查（`clean()` 过滤待测试面确认，编译链与 catalog 同源）。
- 运行期修正项：DataDriven 面在 `DataDrivenNativeRuntime.create` 里按行裁定可路由性（空壳 stub 例外），
  静态数为**上界**；±个别行差异以测试面为准（不影响量级结论）。

### 3.2 category 分布（影响断链 2 修复分支的设计）

`quest.xml` 的 `category1`（→ `QuestMetadata.category()`，经 `RetailQuestMetadataCompiler:322`
`toUpperCase`；`QuestTemplate.fromMetadata:609` 经 `QuestCategory.valueOf` 映射）：

| category | 数量 | 备注 |
|---|---|---|
| `EVENT` | 286 | 含 3 个 XML 保留行 |
| `MISSION` | 20 | 80900-80919（全 DataDriven native） |
| `SEEN_MARKER` | 6 | 80927-80932（全 DataDriven native） |
| `PUBLIC` | 6 | 80933-80938（全 DataDriven native） |

**关键结论**：`startEventQuest` 现有 typed 路径的 `category != EVENT → false` 门若照搬到 native 分支，
**32 个非 event category 的 native 事件任务（80900-80938 段，在 maintainable 清单里）仍会死**。
「事件任务」的语义来源是 `events_config.xml` 的活动 `maintainable` 清单（运维配置），不是 quest.xml 的
`category1`（客户端 UI 分组）——真端反汇编源码中未见 category 参与事件维护（`server58-source` 全树搜
`events_config/maintainable/EventService` 无命中；该清单由真端配置文件驱动，不以反汇编形态可得）。

## 4. 修复方案

### D1（主链，推荐本次实施）：恢复 315 个 native 事件任务的维护闭环

1. `EventService.StartOrMaintainQuests:155` —— metadata 获取加 `nativeMetadata` 回退（与
   `QuestState.canRepeat()` / `CM_QUEST_SHARE` / `XMLStartCondition` 同款）：
   ```java
   QuestMetadata metadata = catalog.findMetadata(questId).orElse(null);
   if (metadata == null) {
       metadata = GameEngineServices.questEngine().nativeMetadata(questId).orElse(null);
   }
   ```
   `matchesEventQuestMetadata` 本体不动（各轴 + `eligibility.matchesCanonicalStartConditions` 对
   `QuestMetadata` 参数化判定，native 行完全适用——已逐行核对 `PlayerQuestStartEligibilityPort:146-196`：
   `prerequisitesMet` 只读 QuestState；`startConditionGroupsMet` 只读行内条件）。
2. `QuestService.startEventQuest:788-830` —— 对 `questsData.getQuestById == null` 的行回退
   `nativeMetadata` 并改用 `QuestMetadata` 轴驱动（typed 路径行为不变）：
   - category 门：**native 分支不查 category**（调用方 `EventService` 由活动清单授权；typed 分支保留原判）；
   - 等级/种族/职业/性别门：`minLevel()`（999 特判）/ `maxLevel()` / `permitsRace(name)` /
     `permittedClasses()` / `permittedGender()`；
   - QuestState 创建/重置：`metadata.repeatPolicy().maxRepeatCount()`（与 `template.getMaxRepeatCount()`
     同语义，`fromMetadata:651-653` 已核）；
   - 收尾 `updateZone()/updateNearbyQuests()` 保持。
3. `EventService:147-148` eligibility 构造的 loader 一并加 native 回退（一致性；只影响
   `repeatCompletionMatches` 的前置行查询与 `normalQuestCount` 的分类计数，后者对 native 行从
   「null→计为普通任务」修正为「按真实 category 判定」）。

### D2（关联点，建议跟踪）：`repeatCompletionMatches` 的 `==` 与真端 ≥ 语义同型偏差

`PlayerQuestStartEligibilityPort:191-196`：`state.getCompleteCount() == prerequisite.repeatPolicy().maxRepeatCount()`
与 1157 修复前的 `XMLStartCondition` 同一偏差（真端 `IsFinishedQuestWithBranch` = `required <= count`）。
计数溢出（完成次数 > 上限）时误拒。修复 = `<` 语义（对齐 1157 的修复）。**影响面在 typed 车道全部
snapshot 路径**，与 D1 主链无关，可独立提交。`maxRepeat ∈ {1, 255}` 已短路，无影响。

## 5. 验证计划

1. 单测（IDEA MCP 跑测）：
   - `startEventQuest` native 分支：构造 QuestState 断言创建/重置/等级 999 特判（静态 `questsData`
     经反射注入或走 `DataManager` 初始化面，参照 `NativeNearbyQuestAxisGateTest` 的 Objenesis 基建）；
   - `EventService` 维护门：native 行 metadata 回退后 `matchesEventQuestMetadata` 通过
     （同包测试可直接调 package-private static）；
   - 回归：`NativeNearbyQuestAxisGateTest` 8/8、`CMQuestShareCanonicalMetadataTest`、
     `RetailQuestStateTest`、`PlayerQuestStartEligibilityPortTest`（存量 1 红不回归即可）。
2. 抽样行：80029（SimpleTalk）/ 80212（SimpleHunt）/ 80900（DataDriven, MISSION category）/
   50089（XML_RETENTION，回归不回归）/ 50011（不在台账的边缘行）。
3. 实机验收（用户执行）：活动开启期间登录 → 事件任务出现在任务列表；已有进度的循环活动任务
   登录后重置/重登记。验收清单在实施后随该任务补。
4. 记账：`pending` 项——DataDriven 逐行裁定与静态上界的差异核对；`clean()` 过滤对 315 行的实际成功率。

## 6. 风险与边界

- **不影响 typed 车道**：断链 1 回退只在 catalog 缺行时生效；断链 2 native 分支只在 `getQuestById == null`
  时进入。typed 行行为逐字不变。
- **`EventService` 无 native 替代面**：tablelane 侧不存在事件维护的等价实现（已核 260 事件行/七族
  无 EventQuestRefresh 类处理）——不修则永灭，不存在「本来就该由 native 车道做」的分流设计。
- **范围控制**：D1 两处改动 + 一行 loader 回退；D2 独立跟踪，不与 D1 掺提（若用户要一并修，照 1157
  的 `<` 语义）。
- 事件任务 QuestState 创建后，任务执行面依赖各行自身的 handler/DD 运行期（native 行已有各自执行面）；
  本立项只恢复**登录维护/建档/重置**这一段，不扩到执行面。

## 7. 待决点（用户已裁决：全部实施，见 §8）

1. ~~D1 是否实施~~（裁决：实施 2026-10-06）。
2. ~~D2 是否随 D1 一并修~~（裁决：一并修）。
3. 实机验收窗口：需活动开启期间（events_config 有活跃活动）才能全链观察；无活动窗口时以单测+抽查行为准（**待用户实机**）。

## 8. 实施与验证记录（2026-10-06）

### 8.1 D1（主链，两处 + 一行 loader 回退）

1. `EventService.StartOrMaintainQuests`（:146-155）：metadata 获取加 `nativeMetadata` 回退；eligibility 构造的
   loader 同步加回退（`catalog 缺行 → nativeMetadata`）。
2. `QuestService.startEventQuest`（:788 起）：拆分为 typed 分支（原逻辑逐字保留）与 native 分支
   （`questsData.getQuestById == null` 时回退 `nativeMetadata`，以 metadata 轴复现等级/种族/职业/性别门 +
   `repeatPolicy().maxRepeatCount()` 的建档/重置判定）；native 分支不套 `category1` 门（事件语义 = 活动清单）。

### 8.2 D2（关联点）

3. `PlayerQuestStartEligibilityPort.repeatCompletionMatches`：`==` → `>=`（对齐真端 `required <= count`，
   与 1157 的 `XMLStartCondition` 修复同源；`maxRepeat ∈ {1,255}` 短路不变）。

### 8.3 已验证（IDEA MCP，2026-10-06）

| 测试类 | 结果 |
|---|---|
| **`EventQuestNativeMaintenanceTest`（新增 6 用例）** | **6/6 绿**：80022 建档 / 80900（mission category）不拦 / 等级门仍生效 / 未知行 fail-closed / 超限不重置且未达上限重置 / `StartOrMaintainQuests` 登录维护联动建档 |
| `PlayerQuestStartEligibilityPortTest` | 16/17（1 红 = `daevanionAuxiliarySlots…` 编译面组数**存量红**，与 D2 运行面零交集） |
| `NativeNearbyQuestAxisGateTest` | 8/8 绿（native 活基准回归） |
| `EventServiceCanonicalMetadataTest` | 2/2 绿（维护门各轴语义未变） |
| `EventServiceTest` | 2/2 绿 |
| `QuestPrerequisiteRetailContractTest` | 2/2 绿（1157 面回归） |
| `RetailQuestStateTest` | 2/2 绿 |
| `CMQuestShareCanonicalMetadataTest` | 1/1 绿 |

测试夹具注意点（供后续复用）：`startEventQuest` 收尾 `updateZone()`（final）→ 需真实构造
`ZoneUpdateService` 装入 `GameMovementLoopServices.resolvedZoneUpdateService` 反射缓存；`QuestService.questsData`
（static）注入空 `QuestsData`（与生产「native 行取不到模板」形状等价）；裸引擎
（`QuestEngine.setInstanceProvider`）即可服务 `nativeMetadata`（不依赖 typed 目录）。

### 8.4 沉淀与提交状态

- Memory Bank：`patterns/quest-engine.md` 新增 **QE-151**（`METADATA_DUAL_SOURCE_NATIVE_FALLBACK`，1157+A/B/C+D
  四面归一）；`systemPatterns.md` 路由已加；sync + verify 全绿（`MEMORY_BANK_VERIFY_OK STEPS=3`）。
- **提交口径**：memory-bank 三文件（quest-engine.md / systemPatterns.md / 派生索引）当前含**并行会话（DD/13403）
  未提交改动**（quest-engine.md 的 QE-150 修订 13/13 行、systemPatterns.md 同路由行），无法干净分离 hunk
  → 本批提交**不含 memory-bank**，待并行会话提交后补提（与 1157 的 QE-150 推迟同处置）。
- 实机验收：待用户（活动开启期间登录观察事件任务发放/重置）。

---

## 附：证据文件索引

| 文件 | 作用 |
|---|---|
| `scan_event_native_owner.py`（本目录） | 318 事件任务 owner/category 静态复现扫描 |
| `EventService.java:111-212`（已读全文） | 断链 1 现场（登录钩子、维护门、reset 逻辑、检查周期 5min） |
| `QuestService.java:788-830` | 断链 2 现场（startEventQuest） |
| `QuestsData.java:126-137` + `DataManager.java:305,510-511` + `XmlDataLoader.java:314-315` | `questsData` 恒等于 733 行目录的三处赋值 |
| `PlayerQuestStartEligibilityPort.java:146-196` | eligibility 无状态直判核对（对 native metadata 适用）+ D2 偏差行 |
| `retail-xml-retention.xml` / `quest.xml` / `DataDrivenNativeRuntime.java:436-460` | 数据面（retention 台账 / 真端全量行 / DD 切换集=台账 RETAIL_TABLE∧DataDriven） |
| `.agents/summary/quest-1157-prereq-fallback/2026-10-06-1157-prereq-catalog-fallback.zh-CN.md §9.2-D` | 上游排查登记（本条立项的触发点） |
