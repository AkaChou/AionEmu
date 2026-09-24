# 物品模板 XML → 标准 JSONL 改造技术方案

> status: **方案（未实施）**。本文件只描述要改什么，不含生产代码改动。
> 依据: [2026-09-23-plain-jsonl-probe.zh-CN.md](2026-09-23-plain-jsonl-probe.zh-CN.md)（含 §8 优化后实测）
> 数据: [2026-09-23-item-format-probe.zh-CN.md](2026-09-23-item-format-probe.zh-CN.md)（紧凑格式对照）
> 约束来源: `AGENTS.md`、`.agents/memory-bank/patterns/static-data-jaxb.md`、
> `2026-09-19-static-data-critical-path-attribution.zh-CN.md` 第 0 节

---

## 1. 摘要

把 `src/main/resources/aion/data/static_data/items/item/item_template_*.xml`（11 分片 / 91.4 MB）
与其自定义覆盖文件换成**标准 JSONL**（键名原样、嵌套对象、一行一条）。

**实测收益**（item 11 分片，未含 npc/skill）：

| 口径 | XML/JAXB | **JSONL（优化后）** | 提升 |
|---|---:|---:|---:|
| **并行冷启动（≈生产首次启动）** | **1540.4 ms** | **858.0 ms** | **1.80×**，省 **682.4 ms** |
| 串行全量热态 | 997.3 ms | 441.3 ms | 2.26× |
| 冷启动分配量 | 596.7 MB | **385.9 MB** | **−35.3%** |
| 冷启动类加载 | 1578 | 781 | **−50.5%** |
| 磁盘体积 | 91.4 MB | **85.6 MB** | **−6.3%** |
| 可读性 | 属性 + 嵌套标签 | **键名 + 嵌套对象，可手工编辑** | — |

**折算**：682.4 ms 约占静态数据窗口（8.05 s）的 **8.5%**、总启动（12–14 s）的 **4.9–5.7%**。

**正确性**：11 分片逐字段比对，**11,925,674 个字段 / 3 处不一致**；3 处均为重复 id
（`110901029`、`169800175`、`169800476`）导致的一对多假阳性，与绑定器无关。

**核心代价**：需要把 7 类 JAXB 语义逐条复刻到自写绑定器（已全部识别并有实现），
且 JSONL 侧**没有 XSD 校验的等价物**（§7.3）。

---

## 2. 范围与边界

### 2.1 本轮做

| 项 | 内容 |
|---|---|
| 数据 | `items/item/item_template_<start>_<end>.xml` × 11 |
| 数据 | `items/item_template_custom.xml`（自定义覆盖，可不存在） |
| 代码 | JSONL 绑定器进生产 + 现有加载链的分派改造 |
| 验证 | 逐字段等价 + 生产测试 + 启动 A/B（均需授权，见 §10） |

### 2.2 本轮不做（及理由）

| 项 | 理由 |
|---|---|
| **npc 分片（72 MB）** | 同一 JAXB 路径、同一收益结构，**应做但未测**；数据形状未经逐字段验证，需单独一轮（§8 阶段 5） |
| **skill parts（29 MB）** | 走 `SAX filter → JAXB`，含**组展开语义**（`SkillExpansionFilter`），是另一类改造 |
| **npc_drops（35 MB）** | 纯 JAXB，结构扁平，风险低但未测 |
| `NpcSkillDefinitionLoader` / `RetailAiDefinitionLoader` | **纯 StAX 手写**，无 JAXB 层可省，不该碰 |
| `WindstreamDefinitionLoader` | 纯 DOM 手写，同上 |

**注意**：只换 item 时，JAXB 框架仍在热路径上（npc/skill/npc_drops 仍在用）。
冷启动类加载从 1578 降到 781 的收益**在本轮就能拿到**（因为它们与 item 同窗口并行，
框架成本原本就由 item 首触支付），但**要彻底退场必须把 §2.2 的 JAXB 路径全部换掉**。

---

## 3. 格式规范

### 3.1 形态规则（已固化为生成器逻辑）

| 元素的子元素特征 | 渲染为 | item 数据中的实例 |
|---|---|---|
| 子标签 ≥ 2 种 | 有序数组，元素带标签 `[{"read":{}}]` | `/actions`（43 种）、`/modifiers`（2 种） |
| 子标签 1 种且可重复 | 数组 `[{...},{...}]` | `/tradein_list` 的 `tradein_item` |
| 子标签 1 种且唯一 | 对象 `{...}` | `weapon_stats`、`conditions` |
| 根元素 | 对象（特判） | `item_template` |

**schema 必须由全量数据扫描得出**（`gen_plain_jsonl.py --schema-out`），不能按单分片判定——
同一路径在不同分片里可能只出现部分子标签，会产生不一致的形态。

全量 schema 仅 8 条路径：

```json
{"": "object", "/actions": "polymorphic", "/modifiers": "polymorphic",
 "/modifiers/add": "object", "/modifiers/add/conditions": "object",
 "/modifiers/rate": "object", "/modifiers/rate/conditions": "object",
 "/tradein_list": "array"}
```

**已知局限**：规则把"非根的多标签子元素"一律当作多态列表元素。当前数据只有 `/actions` 与
`/modifiers` 命中，且二者在模型中确实都是 `List<...>`。**若将来某个元素的子元素既含列表元素
又含普通字段，本规则会判错**，需要显式白名单。实施时应在生成器里加一条断言：schema 里
`polymorphic` 的路径数恒为 2，否则报错退出。

### 3.2 数值转换规则

```python
NUMERIC = re.compile(r"^-?(0|[1-9]\d{0,14})$")   # 无前导零、≤15 位
```

- 匹配 → JSON 数字（`"level":60`）；否则保留字符串（`"restrict":"60,60,..."`）
- **前导零刻意不转**：`"04450"` 保留为字符串，避免丢零。全量 8 处。
- 绑定器**必须**处理"字符串形式的数字"（§7.1 已列为必测项）

### 3.3 源文件决策：JSONL 作为唯一源

| 方案 | 体积 | "改后立即生效" | 结论 |
|---|---|---|---|
| **JSONL 唯一源**（删 XML） | 85.6 MB | ✓ 直接改 JSONL | **推荐** |
| XML 为源 + JSONL 派生物 | 177 MB | ✗ 改 XML 后必须重跑生成器 | **否决** |

否决第二方案的理由是项目已记录的约束：

> **XML 加载不使用二进制缓存**：既要支持临时修改 XML 后 reload 立即生效，
> 也不接受"改一次 XML 就要重新生成二进制"。—— `2026-09-19-static-data-critical-path-attribution.zh-CN.md` 第 0 节

JSONL 本身人类可读可编辑，**直接改 JSONL 即可**，"生成步骤"不复存在，该约束冲突自然消解。
生成器（`gen_plain_jsonl.py`）因此只用于**一次性迁移**，不进入日常流程。

---

## 4. 加载器设计

### 4.1 现有加载链（改造前）

```
DataManager.java:502   timedDataLoad("ItemData", loader::loadItemData, executor, …)
 └─ XmlDataLoader.loadItemData()                                    [813]
     └─ loadItemData(Config.dataFile(ITEM_SHARD_DIR),               [823]
                     Config.dataFile(CUSTOM_ITEM_DEFINITIONS_FILE))
          ├─ listTemplateShards(dir, ITEM_SHARD_PATTERN, "item")     [885]
          │    └─ 按文件名起始 ID 排序 + 目录/分片缺失即 fail-fast
          ├─ unmarshalShard(customOverrideFile, ItemData.class)      [951]  ← JAXB
          ├─ 11 × supplyAsync(unmarshalShard(shard, ItemData.class), STATIC_DATA_POOL)
          ├─ 增量合并 merged.putAll(future.join())                   [835-838]
          └─ combined.assembleFromMerged(merged, custom)             [ItemData:123]
               ├─ custom 按 ID 覆盖或新增
               └─ setData(new ArrayList<>(merged.values()))  ← 一次性重建全部索引

ItemData.reload(Player)                                              [ItemData:209]
 └─ XmlDataLoader.getInstance().loadItemData() → DataManager.ITEM_DATA.setData(…)
```

**必须原样保留的四项设计**（改造只替换解析层）：

1. **并行 + 增量合并**——分片完成即 `putAll`，不同时持有全部分片索引（峰值内存 / GC 压力）
2. **自定义覆盖**——`item_template_custom.xml` 可不存在；按 ID 覆盖或新增
3. **起始 ID 排序**——`templateShardStartId` 从文件名读，不扫文件
4. **fail-fast**——目录或分片缺失时立刻失败，启动期不生成任何文件

### 4.2 新增组件

**`JsonlTemplateBinder`**（从探针的 `PlainBinder` + `JsonReader` 移植，建议放
`com.aionemu.gameserver.dataholders.loadingutils`）：

| 成员 | 职责 |
|---|---|
| `JsonReader` | 行内流式扫描：键名区间 + 哈希（不建 String）、`readInt`/`readLong`/`readBoolean`（不建 String） |
| `ClassMeta` | JAXB 注解扫描结果 + **开放寻址键表**（attributes 与 elements 合并，按整数哈希查） |
| `PlainBinder` | 递归绑定；`applyAttribute` 对基础类型用 `Field.setInt` 等（不装箱） |
| `finish` | 复刻 `ItemTemplate.afterUnmarshal` 的收尾（itemId / restricts / restrictsMax / weaponStats 默认值） |

**设计成通用**（不绑定 `ItemTemplate`），以便 npc 等后续路径复用；本轮只接 item。

**移植时必须一并带入的两项优化**（否则性能回落到 1099.1 ms 而非 858.0 ms）：

- 键名按字符区间查表，不建 String
- 数字直接解析 + `Field.setInt/Long/Boolean/Short/Byte`，不建 String、不装箱

### 4.3 解析器分派

在 `unmarshalShard` 处按扩展名分派，使**过渡期可双轨**：

```java
private static <T> T unmarshalShard(File shardFile, Class<T> type) {
    if (shardFile.getName().endsWith(".jsonl")) {
        return type.cast(JsonlTemplateBinder.forType(type).bind(stream, t -> { }));   // 伪代码
    }
    … 现有 JAXB 路径不变 …
}
```

**收益**：自定义覆盖文件可独立选择格式；回滚只需把 pattern 改回 `.xml`（§9）。

---

## 5. 集成点与改动清单

| # | 文件 | 改动 | 风险 |
|---|---|---|---|
| 1 | **新增** `loadingutils/JsonlTemplateBinder.java` | 从探针移植（约 400 行） | 中 |
| 2 | `XmlDataLoader.java:114` | `CUSTOM_ITEM_DEFINITIONS_FILE` → `.jsonl` | 低 |
| 3 | `XmlDataLoader.java:115` | `ITEM_SHARD_PATTERN` → `item_template_(\d+)_(\d+)\.jsonl` | 低 |
| 4 | `XmlDataLoader.java:951` | `unmarshalShard` 按扩展名分派 | 中 |
| 5 | `XmlDataLoader.java:885` | `listTemplateShards` 无改动（pattern 已参数化） | — |
| 6 | `ItemData.java:123` | `assembleFromMerged` **无改动** | — |
| 7 | `ItemData.java:209` | `reload` **无改动** | — |
| 8 | `DataManager.java:502` | **无改动** | — |
| 9 | **新增** `scripts/` 之外的生成器 | `gen_plain_jsonl.py` 移入 `.agents/summary/item-format-migration/`（**按 `ai-artifacts.md` 不得放 `scripts/`**） | 低 |
| 10 | 数据 | 11 个 `item_template_*.xml` → `.jsonl` | — |
| 11 | 数据 | `item_template_custom.xml` → `.jsonl`（若存在） | — |
| 12 | 未知 | `item_templates.xsd` 的去留（§7.3） | 中 |

**受影响测试**（12 个文件直接引用 `item_template` / `ItemData` / `XmlDataLoader`）：

```
src/test/java/com/aionemu/gameserver/dataholders/
    DataManagerTest.java
    DataholderLookupIndexTest.java
    StaticDataLoadRepro.java
    loadingutils/XmlDataLoaderTest.java
    loadingutils/ConsoleStaticDataProgressReporterTest.java
    loadingutils/RetailAiDefinitionLoaderTest.java
src/test/java/com/aionemu/gameserver/lifecycle/
    GameStaticDataLifecycleTest.java
    GameCoreServicesRuntimeBridgeTest.java
    LegacySingletonFallbackAuditTest.java
    GameServiceProviderCompatibilityTest.java
src/test/java/com/aionemu/gameserver/GameServerTest.java
src/test/java/com/aionemu/gameserver/ai/RetailPatternAI2Test.java
```

**实施时必须先跑一遍这些测试建立基线，改造后再跑一次对比**（需授权，§10）。

---

## 6. 验证方案

### 6.1 等价性（第一道关卡，已完成）

迁移前后用同一套逐字段比对：**11,925,674 字段 / 3 处**（重复 id 假阳性）。
**判据**：改造后重跑必须得到同样的 3 处；任何新增不一致一律阻断。

### 6.2 生产测试

`mvn -B test -Dtest='DataManagerTest,XmlDataLoaderTest,DataholderLookupIndexTest,…'`
——**需用户授权**（`AGENTS.md` 全局规则 1）。

### 6.3 启动 A/B

真实启动对照，度量：
- 静态数据窗口（现状 8.05 s）
- item 加载阶段耗时（现状 1–1.5 s）

**需用户授权**（`AGENTS.md` 全局规则 2：不得启动服务端进程）。

### 6.4 建议补充的专项验证

| 项 | 为什么 |
|---|---|
| **空集合 vs null**（§7.2） | `SDJ-004` 记录过同类 NPE |
| **`"04450"` 类字符串数字** | 已在探针阶段踩过，8 处静默错误 |
| **reload 后台路径** | `ItemData.reload` 在运行中替换数据，需确认索引重建无竞态 |
| **自定义覆盖文件缺失** | `customOverrideFile` 可为 null，覆盖与新增两条分支都要走 |

---

## 7. 风险与对策

### 7.1 JAXB 语义边界（7 类，全部已识别并实现）

| # | 边界 | 不复刻的后果 | 状态 |
|---|---|---|---|
| 1 | `List<T>` 字段的泛型实参才是元素类型 | `TradeinItem.id/price` 静默丢失 | 已实现 |
| 2 | `@XmlAttribute` 上的集合（`Stigma.skill`，无 `@XmlList`） | 抛异常中断加载 | 已实现 |
| 3 | 未知枚举值容忍（`armor_type="ARROW"`） | 应留 null，不能抛 | 已实现 |
| 4 | 无属性元素的存在性（`<read/>`，8135 个） | actions 列表少项（静默） | 已实现 |
| 5 | 数据比模型新（`@activate_target` 等） | 丢元素而非忽略属性 | 已实现 |
| 6 | 列表元素序号必须进路径 | modifiers 错位（实测 84825 处） | 已实现 |
| 7 | **字符串形式的数字**（`"04450"`） | **8 处 questid 静默读成 0** | 已实现（优化阶段踩到） |

**第 4、5、6 条是"路径→值编码"特有的问题，标准 JSON 天然有嵌套与数组，因此不存在**。
第 7 条是标准 JSONL 新增的边界，已固化在 `applyAttribute` 的字符串字面量判断里。

### 7.2 空集合 vs null（**最高优先级的未测项**）

`SDJ-004` 记录过：某静态数据表被改成"零子元素"后启动期抛 NPE，getter 返回 null 而非空集合。

JSONL 侧两种形态含义不同：

| JSON | 绑定结果 |
|---|---|
| `"actions":[{"read":{}}]` | 非空 List |
| `"actions":[]` | **空 List（非 null）** |
| **键完全不出现** | **字段保持 Java 默认（通常是 null）** |

**需要验证**：XML 侧"元素缺失"与 XML 侧"空元素"分别产生什么，JSONL 侧是否一一对应。
**对策**：在 §6.1 的逐字段比对里，把"null vs 空集合"作为独立判据——当前的 `diff` 递归遇到
null 会跳过，**可能掩盖这类差异**，需要专门补一条断言。

### 7.3 XSD 校验的损失

现状：`item_templates.xsd` + `XmlValidationHandler` 在反序列化时做结构与类型校验。
JSONL 侧**没有等价物**。

**对策（择一或并用）**：
- 生成器在迁移时跑一次 XSD 校验（离线、一次性）
- 绑定器对类型不匹配**抛异常而非静默**（探针的实现是"未知枚举留 null"这一条必须保留，
  其余类型错误应显式失败）
- 保留 `item_templates.xsd` 文件作为**文档**，即使不再被运行时使用

**这是本方案最大的能力倒退，需要用户确认是否接受。**

### 7.4 反射绑定的性能不确定性

绑定层用 `Field.set`，开销低于 JAXB 的反射路径（实测分配量低 35%），但**未单独量化反射占比**。
若将来要再压，方向是 `MethodHandle` 或代码生成，成本显著高于本轮。

### 7.5 未测路径的外推风险

本方案的全部数据来自 item。npc（72 MB）与 item **共用 `unmarshalShard` 与 JAXB 路径**，
收益结构应类似，但**数据形状未经验证**——npc 的列表/多态结构与 item 不同，
§3.1 的 schema 规则需要在 npc 上重新扫一遍并人工确认。

---

## 8. 实施步骤

| 阶段 | 内容 | 产出 | 授权 |
|---|---|---|---|
| **0** | 跑一遍受影响测试建基线 | 基线报告 | 需授权（构建/测试） |
| **1** | 移植 `JsonlTemplateBinder` 进生产；`unmarshalShard` 按扩展名分派 | 代码（XML 路径不变） | — |
| **2** | 生成 11 个 `.jsonl` + 转换 `custom` 覆盖文件；逐字段比对 | 数据 + 验证报告 | — |
| **3** | 切换 `ITEM_SHARD_PATTERN` / `CUSTOM_ITEM_DEFINITIONS_FILE` 为 `.jsonl` | 配置改动 | — |
| **4** | 重跑受影响测试 + 启动 A/B | 对照报告 | 需授权（测试/启动） |
| **5** | （另开一轮）npc → JSONL | 同 1–4 | 需授权 |
| **6** | （另开一轮）skill parts，需处理 `SkillExpansionFilter` | — | 需授权 |

**阶段 1 与 2 之间不切换默认源**，XML 与 JSONL 并存但只有 XML 被读取——这保证任一阶段
出问题都能立刻停在一个可运行状态。

**删除 XML 文件放在阶段 4 通过之后**，且建议保留一个版本周期再删。

---

## 9. 回滚方案

**三档，成本递增**：

| 档 | 动作 | 生效时间 |
|---|---|---|
| 1 | 把 `ITEM_SHARD_PATTERN` / `CUSTOM_ITEM_DEFINITIONS_FILE` 改回 `.xml` | 重启 |
| 2 | 移除 `.xml` 分派的早期返回，恢复纯 JAXB `unmarshalShard` | 重启 |
| 3 | 删掉 `JsonlTemplateBinder`，回退提交 | 重启 |

**前提**：整个改造期间**不删除 XML 文件**，直到阶段 4 通过并稳定一个版本周期。
由此带来的体积代价（85.6 + 91.4 = 177 MB）是过渡期的临时成本。

---

## 10. 未决问题与需授权动作

### 10.1 需要用户决策

| # | 问题 | 影响 |
|---|---|---|
| 1 | **是否接受 XSD 校验能力的损失**（§7.3） | 决定方案是否成立 |
| 2 | JSONL 为唯一源、删除 XML 的时点 | 体积 / 回滚能力 |
| 3 | 是否连带做 npc（§2.2 指出：要 JAXB 彻底退场必须全部换掉） | 收益规模 |
| 4 | `item_templates.xsd` 与 `item_custom_set.xsd` 的去留 | 文档完整性 |

### 10.2 需要授权的执行动作

按 `AGENTS.md` 全局规则，以下**不得自行执行**：

| 动作 | 命令（示例） | 依据 |
|---|---|---|
| 跑测试 | `mvn -B test -Dtest='DataManagerTest,XmlDataLoaderTest,…'` | 规则 1 |
| 启动服务端做 A/B | `scripts/start-silent.sh` | 规则 2 |
| 提交 | `git commit` | 需显式授权 |

### 10.3 本方案未覆盖

- **并发/一致性**：`reload` 在运行中替换 `DataManager.ITEM_DATA`，本次未做竞态分析
- **内存驻留**：两条路径的最终对象图相同，仅解析期临时对象不同；未做全量内存对照
- **多线程 JIT 交互**：冷启动 JIT 1523 ms（JSONL）vs 1126 ms（双字典），差值来源未逐项归因
