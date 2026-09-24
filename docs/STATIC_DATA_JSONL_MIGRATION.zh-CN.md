# 静态数据 XML → JSONL 迁移方案

> 状态：**方案，未实施**。本文只描述要改什么，不含生产代码改动。全部性能数字来自 2026-09-23 的独立探针实测（Zulu JDK 26.0.2.1，aarch64 10 核，无服务端进程参与）。
>
> 范围：**第一阶段 item**（`items/item/item_template_*.xml` × 11，91.4 MB）。npc / skill / npc_drops 的评估见 §2.2。
>
> 核心目标：把物品模板的运行时格式换成人类可读的 JSONL，同时不牺牲启动速度——实测**并行冷启动 1540.4 ms → 858.0 ms（1.80×）**，分配量降 35%，体积降 6.3%。

---

## 1. 结论

1. **物品模板换标准 JSONL（键名原样、嵌套对象、一行一条）在每个测量维度上都优于现状**，且格式本身可读、可手工编辑。
2. **收益与可读性不互斥**——「替掉 JAXB 框架」那一半收益（冷启动类加载 1578 → 781）来自任何手写绑定器，与格式可读性无关。
3. **必须保留的四项既有设计**：分片并行 + 增量合并、自定义覆盖文件、按起始 ID 排序、目录/分片缺失即 fail-fast。改造只替换解析层。
4. **JSONL 应作为唯一源**（删除 XML）。它是可读可编辑的文本，直接改它就生效，"改一次数据就要重新生成"的问题不存在。
5. **JSON Schema 提供的数据契约强于现状**：item 加载路径**目前并未接入 XSD 校验**（§8.3），因此 JSONL 配 JSON Schema 是净增强，不是等价替代。校验放在迁移期与 CI，**不进加载期**（§4.5、§9.1）。
6. **收益不应外推到未测路径**。npc / skill / npc_drops 的数据形状不同，需各自验证。

---

## 2. 背景与范围

### 2.1 现状成本

启动静态数据窗口实测约 **8.05 s**，其中：

| 项 | 数值 | 说明 |
|---|---:|---|
| JIT 编译 | **16.2 核·秒** | 静态数据阶段头号 CPU 单项 |
| GC 并行阶段 | 3.0 核·秒 | |
| 平均核利用 | 约 6 个线程 | 10 核机器已吃满 |

解析负载归属（按采样含该帧的比例）：`unmarshalShard`（items/NPCs JAXB）**37.3%**、
`SkillDefinitionLoader.loadPart`（SAX→JAXB）10.2%、`NpcSkillDefinitionLoader` / `NpcDropData.loadEager` 各 3.7%。

### 2.2 本轮做 / 不做

| 项 | 决定 | 理由 |
|---|---|---|
| **item 11 分片 + custom 覆盖** | **做** | 最大单项（91.4 MB），JAXB 扁平路径，已逐字段验证 |
| npc 分片（72 MB） | 不做（另开一轮） | 同一 JAXB 路径、同一收益结构，但数据形状未验证 |
| skill parts（29 MB） | 不做（另开一轮） | 走 `SAX filter → JAXB`，含**组展开语义**（`SkillExpansionFilter`），是另一类改造 |
| npc_drops（35 MB） | 不做 | 纯 JAXB、结构扁平，风险低但未测 |
| `NpcSkillDefinitionLoader`、`RetailAiDefinitionLoader` | **不碰** | 纯 StAX 手写，无 JAXB 层可省 |
| `WindstreamDefinitionLoader` | **不碰** | 纯 DOM 手写 |

> **注意**：只换 item 时 JAXB 框架仍在热路径上（npc / skill / npc_drops 仍在使用）。本轮的类加载收益仍可拿到（因为它们与 item 同窗口并行，框架成本原本由 item 首次触发时支付），但**要彻底退场必须把全部 JAXB 路径换掉**。

---

## 3. 实测结论

### 3.1 性能（item 11 分片）

| 口径 | XML/JAXB | 双字典 JSONL | **标准 JSONL** | 标准/JAXB |
|---|---:|---:|---:|---:|
| 单分片热态 | 90.0 ms | 27.8 ms | 48.5 ms | 1.86× |
| 串行全量热态 | 997.3 ms | 297.3 ms | 441.3 ms | 2.26× |
| 并行稳态 | 186.1 ms | 84.6 ms | **81.1 ms** | 2.29× |
| **并行冷启动（≈生产首次启动）** | **1540.4 ms** | 649.7 ms | **858.0 ms** | **1.80×** |

| 其他维度 | XML/JAXB | 双字典 JSONL | **标准 JSONL** |
|---|---:|---:|---:|
| 冷启动分配量 | 596.7 MB | 531.3 MB | **385.9 MB** |
| 冷启动类加载 | 1578 | 793 | **781** |
| 冷启动 JIT | 1842 ms | **1126 ms** | 1523 ms |
| 磁盘体积 | 91.4 MB (100%) | 36.6 MB (40.0%) | **85.6 MB (93.7%)** |

**关键读数**：

- **生产最相关口径省 682.4 ms**，约占静态数据窗口的 8.5%、总启动（12–14 s）的 4.9–5.7%。
- **分配量是三者中最低的**——标准 JSONL 比 JAXB 低 35%、比双字典低 30%（全量 364.2 MB vs 522.5 MB）。可读格式的分配量低于压缩格式，原因是后者每条记录仍需构建索引行。
- **体积比 XML 还小 6.3%**：XML 里重复的标签名被折叠成只出现一次的键，抵消了 JSON 的括号引号开销。
- **标准 JSONL 与双字典的差距集中在体积与 JIT**：体积 2.34× 导致 I/O 与扫描字符数更多；JIT 1523 vs 1126 ms 是因为递归下降 + 按类型查表的代码路径更多。**后一项与格式无关，AOT/AppCDS 可覆盖**。

### 3.2 正确性

11 分片逐字段与 JAXB 对照：**11,925,674 个字段，3 处不一致**。3 处均为数据中的**重复 id**（`110901029`、`169800175`、`169800476` 各出现 2 次）在一对多比较时产生的假阳性，与绑定器无关。

**记录数**：四条路径（JAXB / 单字典 / 双字典 / 标准 JSONL）均为 128,632；JAXB 侧为 128,629，因 `ItemData` 用 `Map<Integer, ItemTemplate>` 去重。

### 3.3 两项关键优化

下列优化把冷启动从 1099.1 ms 压到 858.0 ms、分配量从 696.7 MB 压到 385.9 MB。**移植时必须一并带入，否则性能回落。**

| # | 优化 | 消除的开销 |
|---|---|---|
| 1 | **键名按字符区间匹配，不建 String**：扫描键名时顺带算出与 `String.hashCode()` 同算法（`h = 31h + c`）的哈希，只留 `(start, end, hash)`；元数据侧用开放寻址表按整数哈希查，冲突时逐字符比对 | 每行约 20 个键 × 128,632 条 ≈ **257 万个键名 String** |
| 2 | **数字直接解析 + 无装箱写入**：`readInt`/`readLong`/`readBoolean` 在字符上累加；基础类型用 `Field.setInt/Long/Boolean/Short/Byte` | 约 390 万个数字 String，以及超出 `Integer` 缓存范围的值逐条产生的 Integer 对象 |

---

## 4. 格式规范

### 4.1 形态规则

由数据自动判定（对全量扫描，不能按单分片判定——同一路径在不同分片里可能只出现部分子标签）：

| 元素的子元素特征 | 渲染为 | item 数据中的实例 |
|---|---|---|
| 子标签 ≥ 2 种 | 有序数组，元素带标签 `[{"read":{}}]` | `/actions`（43 种）、`/modifiers`（2 种） |
| 子标签 1 种且可重复 | 数组 `[{...},{...}]` | `/tradein_list` 的 `tradein_item` |
| 子标签 1 种且唯一 | 对象 `{...}` | `weapon_stats`、`conditions` |
| 根元素 | 对象（特判） | `item_template` |

全量 schema 仅 8 条路径：

```
""                          -> object        （根）
/actions                    -> polymorphic   （43 种元素）
/modifiers                  -> polymorphic   （2 种元素）
/modifiers/add              -> object
/modifiers/add/conditions   -> object
/modifiers/rate             -> object
/modifiers/rate/conditions  -> object
/tradein_list               -> array
```

**已知局限**：规则把「非根的多标签子元素」一律当作多态列表元素。当前数据只有 `/actions` 与 `/modifiers` 命中，且二者在模型中确实都是 `List<...>`（`ItemActions.itemActions`、`ModifiersTemplate.modifiers`）。
**若将来某个元素的子元素既含列表元素又含普通字段，本规则会判错**——迁移工具应加断言：`polymorphic` 路径数恒为 2，否则报错退出。

### 4.2 输出示例

```json
{"name_desc":"sword_n_l1_nq_60a","race":"PC_ALL","level":60,"restrict":"60,60,...","price":2199250,"name":"Reian Legionary's Sword","id":100001266,
 "modifiers":[{"add":{"bonus":"true","name":"PHYSICAL_ATTACK","value":28}},
              {"add":{"bonus":"true","name":"PHYSICAL_ACCURACY","value":111}}],
 "weapon_stats":{"parry":962,"attack_range":1500,"max_damage":216,"hit_count":2},
 "tradein_list":[{"price":1,"id":100001263},{"price":16,"id":186000143}],
 "acquisition":{"item":186000143,"count":159,"type":"REWARD"}}
```

对应原 XML：

```xml
<item_template name_desc="sword_n_l1_nq_60a" race="PC_ALL" level="60" ... id="100001266">
  <modifiers>
    <add bonus="true" name="PHYSICAL_ATTACK" value="28" />
    <add bonus="true" name="PHYSICAL_ACCURACY" value="111" />
  </modifiers>
  <weapon_stats parry="962" attack_range="1500" max_damage="216" hit_count="2" />
  <tradein_list>
    <tradein_item price="1" id="100001263" />
    <tradein_item price="16" id="186000143" />
  </tradein_list>
</item_template>
```

### 4.3 数值转换规则

```
匹配数字:  ^-?(0|[1-9]\d{0,14})$         （无前导零、≤15 位）
匹配   -> JSON 数字      "level":60
不匹配 -> 保留字符串      "restrict":"60,60,..."、"bonus":"true"
```

**前导零刻意不转**：`"04450"` 保留为字符串以避免丢零。全量 8 处。
**绑定器必须处理「字符串形式的数字」**——见 §8.1 第 7 条。

### 4.4 源文件决策：JSONL 作为唯一源

| 方案 | 体积 | 「改后立即生效」 | 结论 |
|---|---|---|---|
| **JSONL 唯一源**（删 XML） | 85.6 MB | 直接改 JSONL | **推荐** |
| XML 为源 + JSONL 派生物 | 177 MB | 改 XML 后必须重跑生成器 | **否决** |

否决第二方案，是因为项目已有明确约束：

> **XML 加载不使用二进制缓存**：既要支持临时修改 XML 后 reload 立即生效，也不接受"改一次 XML 就要重新生成二进制"。

JSONL 本身人类可读可编辑，**直接改 JSONL 即可**，生成步骤不复存在，该约束冲突自然消解。迁移工具因此**只用于一次性转换，不进入日常流程**。

### 4.5 JSON Schema 数据契约

**先定时机——它决定成本与价值。**

| 时机 | 额外开销 | 价值 | 采用 |
|---|---|---|---|
| **迁移期一次性** | 零（不进运行时） | 验证转换正确 | **✅** |
| **开发期 / CI** | 零（不跑服务端） | 改数据后快速查错 | **✅** |
| 加载期（每次启动） | **高**（见下） | 数据在仓库内，非外部输入 | **❌** |

**为什么不进加载期**：绑定器是**流式单遍、不建中间树**——这正是它比 JAXB 快的核心原因之一。
JSON Schema 校验需要完整的文档结构（或等价的状态机），无论哪种都要**再加一遍遍历**。
按同类校验器 30%–100% 的相对开销估算，串行全量会从 441.3 ms 涨到约 570–880 ms，
**吃掉本改造的大部分收益**；而它校验的是仓库内的静态资产，正确性应由 CI 保证，
不是每次启动重复支付。

**推荐组合**：

| 环节 | 做什么 |
|---|---|
| 迁移期 | 用 schema 校验全部生成结果；schema 与数据不同步即判失败 |
| CI / 开发期 | 改完数据跑一次 schema 校验（Python `jsonschema`，**零 Java 依赖**） |
| 加载期 | 只做**顺带可完成的**廉价断言，不额外遍历 |

**加载期的廉价断言**（在绑定过程中顺带做，零额外遍历）：

- 记录数非 0（`listTemplateShards` 已 fail-fast）
- `id` 唯一性 —— 一次 `HashMap` 检查，O(n)
- 类型不匹配显式抛错（带**行号 + 键名**，比现状的行列号更可定位）

**Schema 覆盖范围**：

| 项 | 写法 |
|---|---|
| 必需字段 | `required` |
| 类型 | `integer` / `string` / `boolean` |
| 枚举 | `enum`（与 Java 枚举常量对齐；**未知值的容忍语义见 §8.1 第 3 条**——schema 侧应列为告警而非错误，否则会与运行时的 JAXB 容忍行为矛盾） |
| 多态列表 | `oneOf` + 单键包装，对应 §4.1 的 `polymorphic` 规则 |
| 值域 | `minimum` / `maximum` / `pattern` |
| 字符串形式数字 | `oneOf: [integer, {type: "string", pattern: "^0[0-9]+$"}]`（§4.3） |

**Schema 应当生成而非手写**：从 JAXB 模型导出一份 `item_templates.schema.json`，避免与模型漂移。
手写的 schema 会在下次模型变更时静默过期，反而制造"以为有校验"的假象。

**工具选择**：迁移与 CI 环节用 **Python `jsonschema`**（生成器已是 Python，不引入 Java 依赖、
不影响运行时 classpath 与启动开销）。若将来确需 Java 侧校验，`com.networknt:json-schema-validator`
是可选实现。

---

## 5. 加载器设计

### 5.1 现有加载链

```
DataManager:502   timedDataLoad("ItemData", loader::loadItemData, executor, …)
 └─ XmlDataLoader.loadItemData()                                     [813]
     └─ loadItemData(Config.dataFile(ITEM_SHARD_DIR),
                     Config.dataFile(CUSTOM_ITEM_DEFINITIONS_FILE))  [823]
          ├─ listTemplateShards(dir, ITEM_SHARD_PATTERN, "item")      [885]
          │    ├─ 按文件名起始 ID 排序（templateShardStartId，不扫文件内容）
          │    └─ 目录缺失 / 无分片 → 立刻抛错，启动期不生成任何文件
          ├─ unmarshalShard(customOverrideFile, ItemData.class)       [951]  ← JAXB
          ├─ 11 × supplyAsync(unmarshalShard(shard, ItemData.class), STATIC_DATA_POOL)
          ├─ 增量合并 merged.putAll(future.join())                    [835-838]
          └─ combined.assembleFromMerged(merged, custom)              [ItemData:123]
               ├─ custom 按 ID 覆盖或新增（可不存在）
               └─ setData(new ArrayList<>(merged.values()))  ← 一次性重建全部索引

ItemData.reload(Player)                                               [ItemData:209]
 └─ XmlDataLoader.getInstance().loadItemData()
     → DataManager.ITEM_DATA.setData(new ArrayList<>(fresh.getItemData().values()))
```

### 5.2 必须原样保留的四项

1. **分片并行 + 增量合并**——分片完成即 `putAll`，不同时持有全部分片的索引（峰值内存 / GC 压力）。
2. **自定义覆盖**——`item_template_custom.xml` 可不存在；按 ID 覆盖或新增两条分支都要保留。
3. **按起始 ID 排序**——`templateShardStartId` 从文件名读，不扫描文件内容。
4. **fail-fast**——目录或分片缺失时立刻失败。

### 5.3 新增组件 `JsonlTemplateBinder`

建议置于 `com.aionemu.gameserver.dataholders.loadingutils`：

| 成员 | 职责 |
|---|---|
| `JsonReader` | 行内流式扫描：键名区间 + 哈希（不建 String）、`readInt`/`readLong`/`readBoolean`（不建 String）、字符串字面量的转义回退 |
| `ClassMeta` | JAXB 注解扫描结果 + **开放寻址键表**（attributes 与 elements 合并，按整数哈希查，冲突逐字符比对） |
| `PlainBinder` | 递归绑定；`applyAttribute` 对基础类型用 `Field.setInt` 等；容器塌缩（`"modifiers":[{"add":{…}}]`）按「字段类型的唯一列表字段」判定 |
| 收尾 | 复刻 `ItemTemplate.afterUnmarshal`：`itemId` / `restricts` / `restrictsMax` / `weaponStats` 默认值 |

**设计成通用（不绑定 `ItemTemplate`）**，以便 npc 等后续路径复用；本阶段只接 item。

### 5.4 解析器分派

在 `unmarshalShard` 处按扩展名分派，使过渡期可双轨：

```java
private static <T> T unmarshalShard(File shardFile, Class<T> type) {
    if (shardFile.getName().endsWith(".jsonl")) {
        return type.cast(JsonlTemplateBinder.forType(type).bind(stream));   // 伪代码
    }
    ... 现有 JAXB 路径不变 ...
}
```

好处：自定义覆盖文件可独立选择格式；回滚只需把 pattern 改回 `.xml`（§10）。

---

## 6. 集成点与改动清单

| # | 位置 | 改动 | 风险 |
|---|---|---|---|
| 1 | **新增** `loadingutils/JsonlTemplateBinder.java` | 从探针移植（约 400 行，含 §3.3 两项优化） | 中 |
| 2 | `XmlDataLoader.java:114` | `CUSTOM_ITEM_DEFINITIONS_FILE` → `.jsonl` | 低 |
| 3 | `XmlDataLoader.java:115` | `ITEM_SHARD_PATTERN` → `item_template_(\d+)_(\d+)\.jsonl` | 低 |
| 4 | `XmlDataLoader.java:951` | `unmarshalShard` 按扩展名分派 | 中 |
| 5 | `XmlDataLoader.java:885` | `listTemplateShards` **无改动**（pattern 已参数化） | — |
| 6 | `ItemData.java:123` | `assembleFromMerged` **无改动** | — |
| 7 | `ItemData.java:209` | `reload` **无改动** | — |
| 8 | `DataManager.java:502` | **无改动** | — |
| 9 | 数据 | 11 个 `item_template_*.xml` → `.jsonl` | — |
| 10 | 数据 | `item_template_custom.xml` → `.jsonl`（若存在） | — |
| 11 | 数据 | `item_templates.xsd` 的去留（§8.3、§11） | 低 |
| 12 | **新增** `item_templates.schema.json` | 从 JAXB 模型生成的数据契约（§4.5） | 低 |

### 受影响测试（12 个文件直接引用 `item_template` / `ItemData` / `XmlDataLoader`）

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

**改造前先跑一遍建立基线，改造后再跑一次对比。**

---

## 7. 迁移工具

一次性转换脚本，**不进入日常流程**（§4.4）。核心逻辑：

1. **第 1 遍**：扫描全部 11 个分片，对每个结构路径统计「子标签集合」与「单条记录内的最大重复次数」。
2. **判定**：子标签 ≥ 2 种 → `polymorphic`；1 种且可重复 → `array`；否则 → `object`；根强制 `object`。
3. **第 2 遍**：按 schema 渲染每行。

```
子标签 ≥2 种     -> [{子标签: 内容}, …]      保持文档顺序
子标签 1 种可重复 -> [内容, …]
子标签 1 种唯一   -> 内容
```

**必须做全量扫描建 schema**（第 1 遍覆盖全部文件），按单文件判定会产生不一致的形态。

**断言**：schema 里 `polymorphic` 的路径数应恒为 2（`/actions`、`/modifiers`）；不符即退出，避免 §4.1 的规则局限被静默触发。

---

## 8. 风险与对策

### 8.1 JAXB 语义边界（7 类，实现时必须逐条复刻）

| # | 边界 | 不复刻的后果 |
|---|---|---|
| 1 | `List<T>` 字段的泛型实参才是元素类型 | `TradeinItem.id/price` 静默丢失 |
| 2 | `@XmlAttribute` 上的集合（`Stigma.skill`，无 `@XmlList`） | 抛异常，加载中断 |
| 3 | 未知枚举值容忍（数据含 `armor_type="ARROW"`，模型无此常量） | 应留 null，不能抛 |
| 4 | **无属性元素的存在性**（`<read/>`、`<disassemble/>`，全量 8135 个） | actions 列表少项（静默） |
| 5 | **数据比模型新**（`@activate_target` 31883 次、`instancetimeclear@sync_ids` 等） | 丢元素而非只忽略属性 |
| 6 | **列表元素序号必须进路径** | modifiers 元素错位（实测 84825 处不一致） |
| 7 | **字符串形式的数字**（`"questid":"04450"`，全量 8 处） | **静默读成 0** |

第 4、5、6 条是"路径→值编码"特有的问题，**标准 JSON 天然有嵌套与数组，因此不存在**。
第 7 条是标准 JSONL 新增的边界：数值快路径必须先用 `peek() == '"'` 分辨字符串字面量，
否则 `readInt` 见到 `"` 立即返回 0。**该错误实测发生过（8 处 questid），只有逐字段比对才暴露**。

### 8.2 空集合 vs null（最高优先级的未测项）

项目已记录同类事故：某静态数据表被改成"零子元素"后启动期抛 NPE，getter 返回 null 而非空集合。

JSONL 侧三种形态含义不同：

| JSON | 绑定结果 |
|---|---|
| `"actions":[{"read":{}}]` | 非空 List |
| `"actions":[]` | **空 List（非 null）** |
| 键完全不出现 | **字段保持 Java 默认（通常是 null）** |

**必须验证** XML 侧「元素缺失」与「空元素」分别产生什么，JSONL 侧是否一一对应。
**注意**：逐字段比对若在遇到 null 时跳过，**会掩盖这类差异**，需要专门补一条断言。

### 8.3 数据契约：现状并未接入 XSD 校验

**这是一个常见误解，需要先澄清。** 物品模板的加载路径**没有做 XSD 校验**：

```java
// XmlDataLoader 954 行（unmarshalShard）——只有事件处理器，没有 setSchema
Unmarshaller un = sharedJaxbContext(type).createUnmarshaller();
un.setEventHandler(new XmlValidationHandler());
return type.cast(un.unmarshal(input));
```

JAXB 只有在 `setSchema()` 之后才会产生 XSD 校验事件。`XmlDataLoader` 里唯一的 `setSchema` 调用在
`loadRetailAiWaypointData`（779 行），服务的是 AI 路径点数据。item 分片的根元素也没有
`xsi:noNamespaceSchemaLocation`（`item_template_custom.xml` 有该声明，但它同样经 `unmarshalShard`
加载，声明不生效）。

因此现状的实际能力是**解组错误检测**（类型转换失败、`@XmlID` 解析失败等，由
`XmlValidationHandler` 捕获并抛错），**不含**元素顺序、必需字段、值域与枚举约束。

| 能力 | 现状（XML） | **JSON Schema** |
|---|---|---|
| 解组/转换错误 | ✅ 抛错（带行列号） | ✅ 绑定器抛错（带**行号 + 键名**） |
| 结构 / 类型 / 必需字段 | ❌ 未接入 | ✅ |
| 值域 / 枚举 / pattern | ❌ 未接入 | ✅ |
| 多态列表（43 种 action） | ❌ 未接入 | ✅ `oneOf` |
| 跨文件引用完整性 | ❌ | ❌ |
| 记录 id 唯一性 | ❌ | ⚠️ 需单独检查（见下） |

**结论：JSON Schema 严格强于现状**，不是"能力倒退"。其设计与时机见 §4.5。

### 8.3.1 项目的既有校验模式（本方案遵循它）

校验被**刻意分成两层**，且第一层是显式关闭的：

| 层 | 行为 | 证据 |
|---|---|---|
| **运行时** | **明确不装 schema** | `XmlDataLoaderTest#staticDataUnmarshallerDoesNotInstallSynchronousSchemaValidation` 断言 `assertNull(unmarshaller.getSchema())`——**有测试锁死这个决策** |
| **测试期** | 按数据选择性做离线校验 | `static_data.xml`、`quest_data.xml`、AI `bombs/spawn_helpers`、skill parts、`global_rules.xml`、QuestRandomRewards 均有 validate 调用 |

**item 分片恰好落在两层之外**：
`static_data.xsd` 虽 include 了 `item_templates.xsd`，但 `staticDataSchemaCompiles` 只验证 schema 能编译，
`validate(static_data.xml)` 校验的是主入口文件（只有 section 声明列表，不含 item 模板数据）。
11 个分片、128,632 条记录目前**没有任何 schema 校验**，仅有解组错误检测。

**两个直接推论**：

1. **JSON Schema 应放在测试期**，与 `GlobalDropDataTest` 那批同一位置——这也正是 §4.5 的推荐。
   "不进加载期"不是本方案的性能取舍，而是**项目既有架构决策**。
2. **补这个缺口不必等迁移**：用 `item_templates.xsd` 为 11 个 item 分片写一个测试期校验，
   **现在就能补且与 JSONL 解耦**；迁移后再把校验对象从 XSD 换成 JSON Schema，位置与时机不变。

**已实施（2026-09-23）**：`ItemTemplateShardSchemaTest`（`dataholders/loadingutils/`），
2 个测试、耗时约 1.5 s。11 个分片全部通过 schema 校验。

**实施中暴露的一处不一致**：`item_template_custom.xml` 是**刻意的空容器**（文件头注释写明它
"不会被自动改写或删除"，空表示"当前没有自定义物品"），但它声明了
`xsi:noNamespaceSchemaLocation="item/item_templates.xsd"`，而该 schema 对 `item_template`
要求 `minOccurs="1"`——**它在空状态下不符合自己声明的 schema**。该声明从不生效（JAXB 不读它），
所以这个矛盾此前无人发现。

根因是同一份 schema 服务两种"合法空值不同"的文件：`minOccurs="1"` 是为**分片**定的
（空分片意味着切分出了问题），而覆盖文件为空是正常状态。测试因此对空覆盖文件 skip 而非静默通过。
若要消除该不一致，可选：放宽 schema（代价是空分片不再被拦）、或为覆盖文件单独声明 schema。

### 8.4 未测路径的外推风险

本方案全部数据来自 item。npc 与 item 共用 `unmarshalShard` 与 JAXB 路径，收益结构应类似，
但**数据形状未经逐字段验证**——§4.1 的 schema 规则需要在 npc 上重新扫描并人工确认。

### 8.5 反射绑定的性能上限

绑定层用 `Field.set`，开销低于 JAXB 的反射路径（实测分配量低 35%），但**反射占比未单独量化**。
若需再压，方向是 `MethodHandle` 或代码生成，成本显著更高。

---

## 9. 实施步骤

| 阶段 | 内容 | 产出 | 前置 |
|---|---|---|---|
| **0** | 跑受影响测试建基线 | 基线报告 | 需授权（构建/测试） |
| **1** | 移植 `JsonlTemplateBinder`；`unmarshalShard` 按扩展名分派 | 代码（XML 路径不变） | — |
| **2** | 生成 11 个 `.jsonl` + 转换 custom 覆盖文件；逐字段比对 | 数据 + 验证报告 | — |
| **3** | 切换 `ITEM_SHARD_PATTERN` / `CUSTOM_ITEM_DEFINITIONS_FILE` 为 `.jsonl` | 配置改动 | — |
| **4** | 重跑受影响测试 + 启动 A/B 对照 | 对照报告 | 需授权 |
| **5** | （另开一轮）npc → JSONL | 同 1–4 | 需授权 |
| **6** | （另开一轮）skill parts，需处理 `SkillExpansionFilter` 组展开语义 | — | 需授权 |

**阶段 1 与 2 之间不切换默认源**：XML 与 JSONL 并存但只有 XML 被读取，保证任一阶段出问题都能停在一个可运行状态。

**删除 XML 文件放在阶段 4 通过之后**，并建议保留一个版本周期再删。

### 验证判据

| 关卡 | 判据 |
|---|---|
| **等价性** | 逐字段比对 **11,925,674 字段 / 3 处**（均为重复 id 假阳性）。改造后重跑必须得到同样的 3 处；**任何新增不一致一律阻断** |
| **空集合 vs null** | 专项断言（§8.2） |
| **字符串形式数字** | 专项断言（§8.1 第 7 条） |
| **reload 路径** | 运行中触发 `ItemData.reload`，确认索引重建无竞态 |
| **custom 覆盖** | 覆盖与新增两条分支 + 文件缺失分支都要走 |
| **JSON Schema** | 全部 11 个分片通过 schema 校验（迁移期与 CI，§4.5） |
| **id 唯一性** | 加载期顺带断言：重复 id 立即报错（现状的 3 处重复 id 应显式处理而非留作假阳性） |

---

## 10. 回滚方案

三档，成本递增：

| 档 | 动作 | 生效 |
|---|---|---|
| 1 | 把 `ITEM_SHARD_PATTERN` / `CUSTOM_ITEM_DEFINITIONS_FILE` 改回 `.xml` | 重启 |
| 2 | 移除 `.jsonl` 分派分支，恢复纯 JAXB `unmarshalShard` | 重启 |
| 3 | 删除 `JsonlTemplateBinder`，回退提交 | 重启 |

**前提**：整个改造期间**不删除 XML 文件**，直到阶段 4 通过并稳定一个版本周期。过渡期的体积代价（85.6 + 91.4 = 177 MB）是临时成本。

---

## 11. 未决问题

| # | 问题 | 影响 |
|---|---|---|
| 1 | JSON Schema 放迁移期 + CI，还是也要进加载期（§4.5 建议前者） | 决定是否牺牲启动收益 |
| 2 | JSONL 唯一源、删除 XML 的时点 | 体积 / 回滚能力 |
| 3 | 是否连带做 npc（§2.2：要 JAXB 彻底退场必须全部换掉） | 收益规模 |
| 4 | `item_templates.xsd`、`item_custom_set.xsd` 的去留（现状未被 item 路径接入，可作参考保留） | 文档完整性 |
| 5 | schema 的生成方式（从 JAXB 模型导出 vs 模板手写） | 漂移风险 |

### 本方案未覆盖

- **并发/一致性**：`reload` 在运行中替换 `DataManager.ITEM_DATA`，未做竞态分析
- **内存驻留**：两条路径的最终对象图相同，仅解析期临时对象不同；未做全量内存对照
- **多线程 JIT 交互**：冷启动 JIT 1523 ms（JSONL）vs 1126 ms（双字典），差值来源未逐项归因
