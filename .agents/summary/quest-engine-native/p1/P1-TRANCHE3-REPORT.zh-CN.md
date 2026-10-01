# P1 第三横切报告：quest.xml 元数据行装载（纯新增，零切换）

日期：2026-10-01　前置：第一/第二横切（codec/相机/注册表/解析器/SimpleHunt 装载器/owner 解析器）

## 范围

- 新增 `tablelane/NativeQuestXmlTable.java`：入仓 `quest.xml`（10035 行，根 `<quests>`、行 `<quest>`、
  id 子元素）的安全 DOM 装载 + 类型化访问器（`text/integer/bool/list`）。**数据层零语义合成**——
  起始条件、奖励组、职业可选奖励、重复策略的语义提取属切换批内 handler 的工作；本类只保证
  「行到字段」忠实。声明书 §3 清单已加行。
- 门测试 `NativeQuestXmlTableTest` 5 例；tablelane 套件 **46/46 绿**。

## 数据级事实（建模依据）

1. **直接子元素实测全单值**：全表扫描零个同标签重复的直接子元素；多值模型（首现序 List）保留为
   防数据演化的契约，用合成流验证。
2. **重复字段都在嵌套容器里**（`fighter_selectable_item` ×3 在 `*_selectable_reward` 容器内，
   70+ 行有之）——容器字段以 textContent 拼接透出，结构化访问（奖励组/职业奖励提取）归切换批；
   修正了首版 javadoc 把嵌套容器误写成直接子元素多值的表述。
3. 行 85 的 `class_permitted` = 17 职业令牌（首版夹具误写 24，实读纠正）。
4. 修红 1 轮：新测试方法漏声明 `parse` 的 `IOException`（编译错，未产生假红）。

## 与既有组件的关系

- 同 `NativeQuestTableLoader` 模式（classpath 资源、安全 DocumentBuilder、内部实体子集允许/外部
  全拒、重复 id fail-fast、缺行 `NATIVE_TABLE_ROW_MISSING`）。
- 为切换批提供：handler 读取起始条件（`StartEligible`）、奖励组档数（→奖励窗映射）、职业可选
  奖励（8..23 确认动作载荷）、重复上限的直接基础；同时是 P8 删除 `quest_name_string_ids.tsv`
  后 name/desc 直读的落点。
