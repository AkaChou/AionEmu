# P0c-28：80290 窗口包备料（风暴期静态切片）——armour 自愈边编译通道 + 风暴归因

> 日期：2026-09-26 ｜ 切片：P0c-28 ｜ lane：静默 43+ 分钟但 05:56-57 六编译器在飞态未落（无窗口）｜ 前序：P0c-27

> **性质**：生产视图全程不可读（missing=664 持稳）。本片完成 80290 翻转前的一切静态备料
> （数据表 + 装载类 + 裁定 + 翻转脚本 + 装载探针绿），窗口内只剩 4 行发射边 + 翻转 + 门禁。

## 发现

1. **风暴归因收敛**：overlay 直探 missing 首行 5000 = `missing retail overlay quest 5000`
   （注册表静默丢弃败行；驱动 per-quest catch → rejections RUNTIME_FAILURE，异常细节被吞）。
   结合 1351/1526 overlay 编译健康 → **SimpleTalk 路径无恙，损坏局限于 lane 的
   CombineTask(574)+CollectItem(~90) 两条编译器路径**（其 05:56-57 在飞态）。失败签名追索到
   RUNTIME_FAILURE 吞没为止——修复属 lane owner，本 lane 不触碰。
2. **家族真实构成修正**：80291/80295 = XML_RETENTION **SCRIPTED**（script 登记，他 lane 处置），
   80292/80293 = NO_TABLE，四行中只有 80294 真端驱动 + 80290 待翻转。判官
   `staleRewardRowsAreHealedOnEnterWorld` 的 weapon 两侧绿来自其**在册 XML 的边**，非编译边。
3. **通道设计修正（推翻 P0c-26 的"登记入 normalization 表"思路）**：`RetailNonIrAxisGateTest`
   不变量 3 = 登记行必须真端定义**零 EnterWorld 路由**（登记=不编译判例，P0c-6/11）——把
   8029x 登记进测试侧表会让门禁与判官**直接对撞**。正确设计：**已编译的自愈边 = 真端形状**
   （80291/80295 weapon 方向链路径边 = 直接先例），走新主资源登记表
   `quest_legacy_heal_rows.tsv` + 单步 build() 发射；测试侧表保持互斥不登记。

## 已落地（本片净变更）

- **`quest_legacy_heal_rows.tsv`**（main + target/classes）：armour 两行登记
  `80294/80290  stale=1  reward=0`；头部记与测试侧表的互斥口径。
- **`RetailLegacySaveHealRows.java`**（新装载类，`forQuest`/`load`，编译过）：
  惰性单次装载，解析失败 fail-closed。
- **装载探针绿**（`RetailLegacyHealRegistryProbeTest.java.txt` 存档，临时源已删）：
  解析 2 行 + 80290/80294 服务 + 未登记 null——窗口前消除"发射通道首次读表失败 =
  全 SimpleTalk 编译中断"的风险。
- **裁定表 `p0c28-80290-heal-channel-flip.tsv`** + b1 接线；**翻转脚本
  `p0c28_retire_80290_row.py`**（p0c27 手术式同款，语法核过，**未运行**——通道先行）。
- prod 判官/门禁零改动（Daevanion 判官按现样即契约）。

## 窗口内执行序（80290 包）

1. 复核 `RetailSimpleTalkDefinitionCompiler.java` mtime（lane 静默）→ 在 `build()` 末
   （return 前）插发射边（草案见下）；编译。
2. `python3 p0c28_retire_80290_row.py` + `verify_retirement.py`（预期 1430/1430/4794/6224）。
3. `DurableDaevanionWeaponRewardRowContractTest` 全 7 方法（生产视图恢复后；80290 翻转 +
   80294 补边 → `staleRewardRowsAreHealedOnEnterWorld` 四行全绿 = Daevanion 既有债清偿）。
4. T2 80290 80294 + SimpleTalk 族门 + NonIrAxis 门（8029x 不入其表，应不受扰）+
   Ownership/verify；并入 P0c-24/25/26/27 合并复跑清单。

发射边草案（判官逐字段对齐：条件 [StatusIs(REWARD), QuestVariableIs(var0, staleRow)] /
动作 [SetVariable(var0, rewardRow)] / afterCommit [SyncQuestState(LEVEL_AND_VISIBILITY_REFRESH)] /
priority null / sourceNode null / target "reward"）：

```java
// P0c-28：旧存档自愈边（armour 方向，登记驱动）——单步行族 XML 时代存档停在 1 基行号、
// 真端投影在 0 基末行，不修复则领奖书页落在不存在的行（判例 80290/80294；链路径 weapon
// 方向同源通道在 buildChain journalRowRepair）。登记与投影不一致即拒绝编译（fail-closed）。
// P0c-28: registry-driven legacy-save heal edge (armour direction) — XML-era saves stop on
// 1-based rows while the retail projection sits on the 0-based last row; the chain path's
// weapon-direction sibling lives in buildChain. Registry/projection mismatch rejects the row.
RetailLegacySaveHealRows.HealEdge heal = RetailLegacySaveHealRows.forQuest(entry.questId());
if (heal != null) {
    if (heal.rewardRow() != rewardRow.get("var0")) {
        throw new IllegalStateException("legacy heal registry row " + entry.questId()
            + " disagrees with the journal projection " + rewardRow);
    }
    transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
        List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
            new QuestCondition.QuestVariableIs("var0", heal.staleRow())),
        List.of(new QuestAction.SetVariable("var0", heal.rewardRow())), "reward",
        List.of(new AfterCommitAction.SyncQuestState(
            QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), null, null));
}
```

## 判例（勿重推）

- **已编译的自愈边 = 真端形状，不入测试侧 normalization 登记表**：该表门禁要求登记行零
  EnterWorld 路由（登记=不编译）——两类自愈边（编译边/移除登记）必须分表，混登即门禁对撞。
- **overlay 注册表静默丢弃编译败行**（lenient 视图"missing X"=编译失败被吞）——风暴归因不能
  只看 overlay 缺行，需结合驱动 rejections 机制与源文件 mtime 窗口。
- **投影核对先于通道**：armour summary_rows=1→lastRowIndex=0=契约 rewardRow、weapon 2→1——
  登记表行内 rewardRow 与投影的一致性由编译期 guard fail-closed 兜底。
- judge-abort 掩蔽第三例的完整解：80291/80295（XML SCRIPTED）绿=XML 边、非编译边——判官
  通过原因必须区分"XML 供给"与"真端编译供给"，翻转裁决只对后者有效。

## 未验证（PENDING，同因：lane 风暴）

- 发射边落地的全部窗口验证（上列窗口内执行序）。
- P0c-24/25/26/27 合并复跑清单不变。

## 下一步

- 轮询风暴（lane 静默 ≠ 风暴愈：在飞态冻结在树里，等 lane 落地其编译器重构）。
- 窗开即执行：发射边 → 翻转 → Daevanion 7 方法 + T2 ×6 任务 + 族门 + T3 债池 diff。
