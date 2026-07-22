# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和结算数据核对的结果。

## Smoldering Fire Temple（302000000）

### 已完成

- 真端普通/Master 条件出生、Pattern、奖励门控、结算恢复和静态去重已完成，并在独立窗口提交。

## Dark Poeta（300040000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idlf1/world_N.xml` 为 UTF-16LE 真端来源；普通创建 ID `39/66/122/179` 使用 `spawn_page=1`，SP/Master 创建 ID `1001/1002` 使用 `spawn_page=2`。
- 真端条件出生共有 73 条、20 个条件变量；现有生成数据漏掉来源编号 `#3/#4/#5/#18/#19/#28`，并遗漏 `vanq`、`aboss_die`、`sboss_die`、`SpecialServer_Cond`。
- `IDLF1_Temp_01_Sp`、`IDLF1_Temp_08_Sp`、`IDLF1_Temp_09_Sp`、`IDLF1_Vanq_A_Sp` Pattern 已存在并分别推进缺失变量；`SpecialServer_Cond` 按出生页面在 Handler 初始化。
- `206478` 的三条真端出生包含同一九点 sensory polygon；现有条件加载器和 Pattern AI 数据结构已支持该字段，不需要新增 dynamic area。

### 已完成

- 补入 6 条真端条件出生，使用 `10457..10462`，保留页面、位置、初始延迟、重生延迟、战斗状态反出生和 sensory polygon。
- 规范化真端 #5/#18 的多余右括号，避免表达式解析失败。
- Dark Poeta Handler 按 `spawn_page=2` 写入 `specialserver_cond=1`，普通页面写入 `0`，使新建和恢复路径一致。
- 保留现有 Handler 的 runtime 状态、条件引擎、Pattern、分数、掉落和结算 ownership；未删除无明确真端替代的逻辑。

### 验证范围

- `DarkPoetaRetailMigrationTest` 锁定 20 个变量、73 条条件、来源编号、页面、NPC 数量、sensory polygon、表达式规范化和页面初始化。
- 运行条件表达式解析、条件出生、Pattern AI、Handler 恢复和 XML schema 专项测试；GM 实测和线上副本压测不在本窗口范围内。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表全部区域完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。
