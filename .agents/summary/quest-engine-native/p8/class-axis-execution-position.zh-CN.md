# 原生行职业轴执行位取证（P8 第四批，2026-10-02）

对应：P8 第一刀登记「原生行职业轴执行位（`QuestRetailClassGateTest` 豁免面：真端 class 限制是否为
服务端闸门）」。结论：**真端职业轴 = 服务端接取闸门，非纯显示元数据**；生产原生接取口已在同一
执行位执行该闸，豁免面注释里的「职业轴 = 显示元数据」表述被本取证推翻并已改写。

## 1. 真端装载面：token 列表 → 32 位职业掩码

quest.xml 行装载走 schema 驱动的通用装载器（`QuestDesignData.cpp` @140d1aa50 只直读
quests/quest/id/name/desc，其余列经描述符表 `FUN_140db3f70(param_2, 0x32898)` 装入行结构——
数据表反编译源不可读），但转换器本身有同源铁证：**`FUN_140da4100` = 职业词表 → 32 位掩码**，
同一函数被 `SkillLearn.cpp:310`（`SkillLearn::class`）与
`ItemCosmeticInfo.cpp:78`（`ItemCosmeticInfo::Set(), class_permitted` → `*(uint32_t*)(+0x110)`）
调用，语义（token 列表 → 位掩码）完全一致。

## 2. 真端消费面：CanAcquireQuest 第一道闸（先于等级）

`Quest_CanAcquireQuest`（Quest.cpp @140d5a960，即 §10.3-#18 邻近清单三值判定 2/1/0 的同一函数）
的判定序：

1. `(quest_design+0x2c >> (player+0xe7 & 0x1f)) & 1 == 0` → 消息 `0x13d85f` 中止（**职业位测试**）；
2. `minlevel_permitted`（+0x24）→ `0x13d85b` / +1 容差 `0x13d85c`；`maxlevel`（+0x28）→ `0x13d85c`；
3. `(quest+0x34 & 1 << (vtable+0x398 getter & 0x1f)) == 0` → `0x13d864`（race 侧掩码之一）；
4. `max_repeat_count`（+0x18）vs `UserQuestData_GetQuestFinishCount` → `0x13d878`；
5. `quest+0x38 != -1` vs `player+0x52fc`（gender int）→ `0x13d86c`；
6. `(quest+0x30 >> (player+0x734 & 0x1f)) & 1` → `0x13d861/0x13d862`（race 另一位）。

player+0xe7 = **玩家职业枚举**：`EquipmentItem.cpp:2540`（`class < 2` 基础职业分支）、
`Item.cpp:2748`（道具职业限制同形掩码测试）、`CDisassemItemInfo.cpp:294`（同形）。
⇒ quest_design+0x2c = `class_permitted` 掩码，**职业位测试是接取第一道闸，先于等级**。

## 3. 生产执行位完整性（native 行全接取面已执行该闸）

| 接取面 | 执行位 | 类轴来源 |
|---|---|---|
| lane 家族接取（Talk/Collect/Hunt/ItemPlay/UseItem 对话面）| `NativeQuestStartPort.start` → `evaluateNpcAcquire` → `eligibilityVerdict` | quest.xml `class_permitted` 直读 + `RetailQuestMetadataCompiler.permittedClassNames`（minLevel≥10 基础展开），失败 = `CLASS_BLOCKED` |
| DD 运行时接取（三入口）| 同上（`DataDrivenNativeRuntime` :1108/:1134/:1182 全走 `NativeQuestStartPort.start`）| 同上 |
| 阵营轮换（§10.3-#25 发放车道）| `NativeFactionRotation.eligible`（候选池构造期，:71-78）| 同一映射函数直读同一列 |
| 附近任务清单（SM_NEARBY_QUESTS）| `zoneVerdict` → `eligibilityVerdict` | 同上 |
| 系统发放 `grant()` | 无类轴（状态面专用）| 调用方（NpcFactions）已在池构造期用 `factionRotationEligible`（含类轴）过滤——与真端 `InitFactionQuest` 前置 `CheckQuestAcquireCondition` 同构 |

port 级行为锁已有：`NativeQuestStartPortTest.classRestrictedRowsFollowTheRetailTokenMapping`
（1913 行：fighter⇒GLADIATOR 放行、knight⇒TEMPLAR 放行、WARRIOR `CLASS_BLOCKED` 且不建档）。

## 4. 门禁重锚（QuestRetailClassGateTest）

- 豁免面**保留**（native 行无 typed 目录载体，目录对拍只服务 XML 行），注释改为取证结论；
- 新增 `nativeClassAxisIsEnforcedThroughTheSharedPortWordlist`：对合同快照**每一行**（人口 =
  评审快照 100%），从 `NativeQuestXmlTable` 直读真端行原文，断言
  `permittedClassNames(原文, minlevel)` == 门禁展开口径（≥16 token 通配 = 空集边界，实测合同
  token 数分布 1..13、无 15/16 token 行，边界在评审人口上不咬合）——把「评审快照 ↔ 真端行原文
  ↔ port 词表」三点钉死在同一个映射函数上，任何一端漂移（快照陈旧 / quest.xml 重入仓漂移 /
  词表映射改动）都会红。
- 口径注记：门禁 m1 的 `retailAliveClasses`（死条目删除形）服务 XML 目录对拍；port 词表 =
  `retailExpandedClasses`（基础展开形，即分歧台账的 RETAIL_PRIORITY 裁决）——新断言用后者。

## 5. 证据命令

```bash
grep -n 'CanAcquireQuest' /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/Quest/Quest.cpp
grep -rn 'FUN_140da4100' .../classes/Skill/SkillLearn.cpp .../classes/Item/ItemCosmeticInfo.cpp
grep -rn 'param_3 + 0xe7' .../classes/Item/EquipmentItem.cpp .../classes/Item/Item.cpp
mvn -o test -Dtest='QuestRetailClassGateTest,NativeQuestStartPortTest' -DfailIfNoTests=false
```
