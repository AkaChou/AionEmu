# 哈拉梅尔 18504/18505 接取门禁归因与真端 10 位大计数器驱动退役纪要

## 一、 背景与报障现象

玩家反馈在测试角色 `Tt` 与哈拉梅尔入口 NPC 泽菲罗斯（Zephyros，NPC ID: 203166）交互时：
* 任务 `18504`（报障中误写为 `18054`）《消灭全部怪物》（击杀 65 只怪）与 `18505`《巡视头目》在 NPC 对话框中点击“接受”（动作 1002）后，服务端直接下发页 0 关窗，任务未能成功接取；
* 用户询问：**18504 能不能由真端驱动，真端官方底层是怎么做的？排查类似任务并一起退役。**

---

## 二、 报障根因排查

1. **强前置条件未满足（`Q18507` 门禁拦截）**：
   * 真端 `quest.xml` 与官方解包数据明确配置：
     * `18504`：`<finished_quest_cond1>Q18507</finished_quest_cond1>`
     * `18505`：`<finished_quest_cond1>Q18507</finished_quest_cond1>`
   * `18507` 为哈拉梅尔最终 Boss 击杀任务《하메룬을 잡아라》（消灭哈梅伦）；
   * 测试角色在未完成 18507 时，服务端的 `QuestCondition.StartEligible()` 前置审查生效，判定拒绝接取并关窗，这是符合真端设计的正常机制。
2. **手写 XML 历史遗留缺陷**：
   * 此前 18504 因击杀数 65 超过了模拟器此前写死的 6 位计数器上限（63），被打上 `RETAIL_COUNTER_EXCEEDS_6BIT` 误判为不能真端驱动，保留了手写 XML `18504.xml`；
   * 手写 XML 存在微观多余页码（1011 -> 1007 -> 4 -> 1002）以及“多打一只怪”（var0=65 时仍需再击杀一次才翻 REWARD）的双重逻辑缺陷。

---

## 三、 真端官方底层机制证实（反编译 `ScriptDLL64.c`）

真端官方代码对击杀计数的实现如下：
1. **普通狩猎（$\le 63$ 只）**：使用 `FUN_180cb13b0`，占用 6 位位宽（掩码 `0x3f`）；
2. **大容量狩猎（$> 63$ 只）**：官方底层专门开辟了 **`FUN_180cb14e0`**（**10 位位宽大容量计数器**，掩码 `0x3ff`，上限 1023）！
3. `18504` 在 `ScriptDLL64.c` 中的注册代码：
   ```c
   FUN_180cb14e0(0x4848, param_2, 1, 0x41, 0x41, 1);
   ```
   * `0x4848` = 18504；`0x41` = 65 只怪；
   * 槽位位移 `(slot - 1) * 10`，掩码 `0x3ff`；
   * 计数达到 65 时直接触发虚表调用 `*(plVar1 + 0x100)` 跃迁至 REWARD。
4. **怪物匹配**：真端以 `IDNovice_02`、`IDNovice_04`、`IDNovice_05`、`IDNovice_07` 等子区域前缀登记，副本内任意匹配该前缀的怪均计入进度。

---

## 四、 类似大计数任务普查与退役清单

全库普查击杀数 $> 63$ 的任务，8 个单槽大计数任务已彻底退役手写 XML：

| 任务 ID | 家族 | 目标数量 | 任务名称 | 处理结论 |
| :--- | :--- | :--- | :--- | :--- |
| **18504** | SimpleHunt | 65 | [天族] 消灭哈梅伦全部怪物 | **退役 `18504.xml`，真端 10 位计数器驱动** |
| **28504** | SimpleHunt | 65 | [魔族] 消灭哈梅伦全部怪物 | **退役 `28504.xml`，真端 10 位计数器驱动** |
| **11456** | SimpleHunt | 100 | [天族] 英吉斯温扫荡 | **退役 `11456.xml`，真端 10 位计数器驱动** |
| **21456** | SimpleHunt | 100 | [魔族] 格尔克马洛斯扫荡 | **退役 `21456.xml`，真端 10 位计数器驱动** |
| **80214** | SimpleHunt | 100 | [活动] 怪物扫荡 | **退役 `80214.xml`，真端 10 位计数器驱动** |
| **80223** | SimpleHunt | 100 | [活动] 怪物扫荡 | **退役 `80223.xml`，真端 10 位计数器驱动** |
| **80691** | SimpleHunt | 500 | [活动] 皇帝金库夺还 | **退役 `80691.xml`，真端 10 位计数器驱动** |
| **80817** | DataDriven | 100 | [活动] 杀死疾病宿主（100 只骆驼） | **退役 `80817.xml`，真端规范形驱动** |
| *1842..1844* | SimpleHunt | 80 + 1 | 要塞双槽任务（天族） | 双槽复合形态，维持 XML 合法保留 |
| *2843..2845* | SimpleHunt | 80 + 1 | 要塞双槽任务（魔族） | 双槽复合形态，维持 XML 合法保留 |

---

## 五、 代码改造要点

1. **`RetailHuntCounterLayout.java`**：新增 `WIDE_SECTION_BITS = 10` 与 `WIDE_SECTION_MASK = 1023`。
2. **`RetailSimpleHuntDefinitionCompiler.java`**：
   * `precheck`：放宽单槽任务上限至 `WIDE_SECTION_MASK`；
   * `buildCanonicalCounterQuest`：为单槽大计数任务构建规范形生命周期（`unaccepted -> started -> reward -> complete`），自增收口值精确等于目标值，杜绝多杀怪缺陷。
3. **`RetailSimpleHuntPlan.java`**：变体展开严格限制在 `18504` 与 `28504` 作用域内，保护 DataDriven 家族既有 14 个任务的指纹不漂移。
4. **`RetailDataDrivenDefinitionCompiler.java`**：支持 LevelUpLogIn 类任务解耦，不生成虚假 NPC 55 路由，消除控制台警告。
5. **资产退役**：
   * `git rm` 删除了 8 个手写 XML；
   * `quest_definition_catalog.xml` 移除了 8 个 `<definition>` 条目；
   * `retail-xml-retention.tsv`、`retail-simplehunt-compiler-rejects.tsv`、`retail-data-driven-drift.tsv` 同步更新。

---

## 六、 门禁验证结果

全量门禁执行：
```bash
mvn test -Dtest=QuestHaramelSubsequentQuestsProductionFlowTest,RetailSimpleHuntFamilyGateTest,RetailDataDrivenGateTest,QuestProductionStartupGateTest
```
**结果：Tests run: 14, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。**
