# 130 个在表任务全景排查与首批真端驱动改造退役纪要

## 一、 背景与排查底数
根据真端驱动原则，推进“规范形生命周期”（Canonical Lifecycle）：彻底废弃微观页码阶梯与 TSV 补丁，直接按真端规范模型驱动。
对真端 8 大模板表内留存的 130 个任务进行全面排查与代码审计：
- **真端全量定义完备、可直接真端驱动（15 个）**：
  - 1842, 1843, 1844, 2843, 2844, 2845（克罗坦、德基萨斯、拉米伦深层要塞 All Kill）
  - 16961（盟军击杀任务）
  - 11074, 11075, 16979, 18832, 26979, 28832（纯单步过场对话任务）
  - 2231, 2724（标准多步对话链任务）
- **强依赖临时生成机制 QUEST_SPAWN（2 个）**：14112, 14123。杀怪后需在玩家身边临时刷出交付 NPC（`spawn-npc-at-player`），真端表无对应列，退役会导致 NPC 不出现。必须保留 XML。
- **进区域自动接取+串行 Boss 击杀（17 个）**：3122, 3123 等，后续由 DataDriven 顺序链专项接驳。
- **专设测试硬编码读取 XML（2 个）**：18805, 28805。`HousingRecycleRewardRowContractTest` 写死读取 XML，暂行保留。
- **NPC 别名组缺失/道具检查缺失（94 个）**：房屋管家、活动 NPC 别名或未解析，需后续补充数据层别名。

## 二、 核心改造内容
1. **补齐深层要塞守军怪群映射（`RetailNpcNameIndex.java`）**：
   - 为 `idabre_up3_crotan`、`idabre_up3_dkisas`、`idabre_up3_lamiren` 分别补齐 74 只普通守军怪物映射（将军作为独立 Slot 2 目标）。
2. **升级 `RetailSimpleHuntDefinitionCompiler.java`**：
   - 放开多槽大计数，支持 10 位掩码（0x3ff，完成值 0x450）多槽独立累加状态机。
   - 可重复任务接取清零：未接取节点变量解绑，并在接取动作挂载计数清零，彻底解决可重复要塞任务重接死锁。
3. **升级 `RetailSimpleTalkDefinitionCompiler.java`**：
   - 放开无道具单步 Cutscene 对话任务限制，同时支持 1007 与 20000（`QUEST_ACCEPT_SIMPLE`）触发点；
   - 移除 2231、2724 的硬编码拦截，启用真端规范多步对话链。
4. **资产彻底退役**：
   - 彻底删除 15 个手写 XML 文件；
   - `retail-xml-retention.tsv` 中 15 个任务状态转为 `RETAIL_TABLE`（`RETAIL_TABLE` 增至 5439，`XML_RETENTION` 降至 785）；
   - `quest_definition_catalog.xml` 精确移除对应条目；
   - `retail-simplehunt-compiler-rejects.tsv` 历史拒绝清空。

## 三、 门禁验证结果
执行测试：`mvn test -Dtest=RetailSimpleHuntFamilyGateTest,RetailSimpleTalkGateTest,RetailOwnershipGateTest,Quest1842RepeatLifecycleTest`
结果：**11 项核心门禁测试全绿通过（BUILD SUCCESS）**。
