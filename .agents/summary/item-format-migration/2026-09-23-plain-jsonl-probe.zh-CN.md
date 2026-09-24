# 标准 JSONL（可读格式）实测：可读性值多少加速 (2026-09-23)

> status: **已按授权运行**（用户 2026-09-23「加」明确授权补测）。未改生产代码、未引入依赖、未提交。
> 探针: `JsonlItemProbe.java`（新增 `jsonl-plain` / `jsonl-plain-parse` / `batch-jsonl-plain` /
> `parallel-jsonl-plain` / `cold-jsonl-plain` 五个场景 + `JsonReader` + `PlainBinder`）
> 生成器: `gen_plain_jsonl.py`（两遍扫描建 schema）
> 前置: [2026-09-23-item-format-probe.zh-CN.md](2026-09-23-item-format-probe.zh-CN.md)（紧凑/双字典格式）
> 环境: Zulu JDK 26.0.2.1 (aarch64, 10 核)，无服务端进程参与

## 0. 为什么补这一测

前一份报告推荐的「双字典 + 整数索引」把体积压到 XML 的 40%，代价是**完全不可读**。
用户随后明确优先级：**可读性 > 速度 > 体积（体积大一点也没事）**——这直接推翻了双字典的选型，
因此需要标准 JSONL（键名原样、嵌套对象）的实测数据。

**本报告同时修正前一份报告的一处错误**：那里写的「标准 JSONL 体积为 XML 的 128%」不成立，
实测为 **93.7%**（见 §1）。

## 1. 格式与体积

生成器从数据自动判定形态（两遍扫描：先全量建 schema，再逐分片生成）。**必须全量扫描**——
同一路径在不同分片里可能只出现部分子标签，按单文件判定会得出不一致的形态。

| 元素的子元素特征 | 渲染为 | item 数据里的实例 |
|---|---|---|
| 子标签 ≥ 2 种（多态列表） | 有序数组，元素带标签 `[{"read":{}}]` | `/actions`（43 种）、`/modifiers`（2 种） |
| 子标签 1 种且可重复 | 数组，元素不带标签 `[{...},{...}]` | `/tradein_list` 的 `tradein_item` |
| 子标签 1 种且唯一 | 对象 `{...}` | `weapon_stats`、`conditions` |
| 根元素 | 对象（特判） | `item_template` |

全量 schema 仅 8 条路径：

```
{"": "object", "/actions": "polymorphic", "/modifiers": "polymorphic",
 "/modifiers/add": "object", "/modifiers/add/conditions": "object",
 "/modifiers/rate": "object", "/modifiers/rate/conditions": "object",
 "/tradein_list": "array"}
```

**体积**：

| 格式 | 字节 | 相对 XML |
|---|---:|---:|
| XML（现状） | 91,365,309 | 100% |
| **标准 JSONL** | **85,616,590** | **93.7%** |
| 双字典 JSONL | 36,581,706 | 40.0% |

标准 JSONL **比 XML 小 6.3%**——XML 里重复的标签名被折叠成一次出现的键，
抵消了 JSON 的括号与引号开销。前一份报告写的 128% 是错的。

## 2. 正确性验证（先于性能）

11 个分片逐字段与 JAXB 对照，**11,925,674 个字段，3 处不一致**——与紧凑格式的验证结果**完全一致**
（同为重复 id `110901029` / `169800175` / `169800476` 导致的一对多假阳性，与绑定器无关）。

记录数：`plain=128632`，与三条既有路径一致（JAXB 侧为 128629，因 `ItemData` 用 Map 去重）。

实现中踩到并复用的一条既有 JAXB 边界：`Stigma.skill` 是 `@XmlAttribute` 的 `List<String>` 且**无 `@XmlList`**，
整个值作单元素——直接复用 `Binder.applyAttribute` 解决。

## 3. 性能对照

### 3.1 单分片热态（中位数，warmup=3 / iter=7）

| 场景 | 墙钟 | CPU | 分配量 | 体积 |
|---|---:|---:|---:|---:|
| `jaxb-item` | 90.0 ms | 89.5 | 46.3 MB | 8.31 MB |
| `sax-scan-item`（XML 词法下限） | 40.9 ms | 40.7 | 0.4 MB | — |
| `jsonl-dict-stream` | **27.8 ms** | 27.8 | **36.2 MB** | 3.18 MB |
| **`jsonl-plain`** | **58.2 ms** | 58.0 | **62.2 MB** | 7.87 MB |
| `jsonl-dict-parse`（解析层） | 8.0 ms | 8.0 | 12.3 MB | — |
| `jsonl-plain-parse`（解析层） | 16.9 ms | 16.8 | 8.2 MB | — |

拆层：plain 绑定层 = 58.2 − 16.9 = **41.3 ms**；dict 绑定层 = 27.8 − 8.0 = **19.8 ms**。
**解析层 2.1×、绑定层 2.1×**——两部分各慢一倍。

### 3.2 全量 11 分片

| 口径 | `jaxb` | `dict` | **`plain`** | **plain/jaxb** | plain/dict |
|---|---:|---:|---:|---:|---:|
| 串行热态 墙钟 | 997.3 ms | 297.3 ms | **526.7 ms** | **1.89×** | 0.56× |
| 串行热态 分配量 | 575.6 MB | **522.5 MB** | **676.0 MB** | 0.85× | 0.77× |
| 并行稳态 墙钟 | 186.1 ms | 84.6 ms | **110.2 ms** | **1.69×** | 0.77× |
| 并行稳态 进程 CPU | 1331.8 ms | **526.2 ms** | 741.2 ms | 1.80× | 0.71× |
| **并行冷启动 墙钟** | **1540.4 ms** | **649.7 ms** | **1099.1 ms** | **1.40×** | 0.59× |
| 冷启动 分配量 | 596.7 MB | **531.3 MB** | **696.7 MB** | **0.86×** | 0.76× |
| 冷启动 类加载 | 1578 | 793 | **779** | **2.03×** | 1.02× |
| 冷启动 JIT | 1842 ms | **1126 ms** | 1622 ms | 1.14× | 0.69× |

冷启动每 arm 独立 JVM，各 3 轮取第 1 轮。

## 4. 三个结论

**① 标准 JSONL 在所有口径上都胜出 JAXB，但幅度从 3.35× 缩到 1.40–1.89×。**

生产最相关的并行冷启动：**1540.4 → 1099.1 ms，省 441.3 ms**。
相对静态数据窗口（8.05 s）约 **5.5%**，总启动（12–14 s）约 **3.2–3.7%**。

**② 标准 JSONL 完整拿到了「JAXB 框架退场」的收益。**

冷启动类加载 **779**，与双字典的 793 几乎相同，远低于 JAXB 的 1578（−51%）。
说明收益的两大来源里，「替掉 JAXB 框架」这一半**与格式可读性无关**，任何手写绑定都能拿到。

**③ 代价集中在分配量与 JIT。**

| 维度 | 结果 | 原因 |
|---|---|---|
| **分配量比 JAXB 高 16.8%**（696.7 vs 596.7 MB） | **唯一输给 JAXB 的维度** | 每个键名都 `substring` 建 String：每行约 20 个键 × 128,632 条 ≈ **257 万个键名 String**；双字典的键是整数索引，不建 String |
| JIT 比双字典高 44%（1622 vs 1126 ms） | 冷启动被拖慢 | 递归下降 + 按类型查表的代码路径比预编译 accessor 链多，需要编译更多方法 |
| 体积是双字典的 2.34× | I/O 与扫描字符数是 2.5× | 可读性的直接代价 |

分配量 +16.8% 对应 GC 成本：静态数据阶段 GC 实测 3.0 核·秒，按比例约 **+0.5 核·秒**，
会吃掉部分墙钟收益（并行下墙钟 ≈ 进程 CPU ÷ 并行度）。

## 5. 与「可读性优先」决策的对照

| | `dict`（双字典） | **`plain`（标准 JSONL）** | XML（现状） |
|---|---|---|---|
| 可读性 | ✗ 整数索引对 | **★★★ 键名 + 嵌套** | ★★ |
| 可手工编辑 | ✗ | **★★★** | ★★ |
| 可作唯一源格式 | ✗（须由 XML 生成） | **✓ 无生成步骤** | ✓ |
| 并行冷启动 | **649.7 ms** | 1099.1 ms | 1540.4 ms |
| 体积 | **40.0%** | 93.7% | 100% |
| 分配量 | **522.5 MB** | 676.0 MB | 575.6 MB |

**按用户声明的优先级（可读性 > 速度 > 体积），标准 JSONL 是正确选择**——
它比现状快 1.40×、比现状小 6.3%、可读可编辑、且可作为唯一源格式（从而消除
「改一次 XML 就要重新生成」的约束冲突）。

**但必须知道代价**：相对双字典**损失约一半的加速**（441 ms vs 891 ms），
且分配量比现状还高 17%。

**如果速度权重回升，可选的折中是「键名缩写」或「键名驻留」**（见 §6），
二者都保留可读性、缩小体积、降低分配量。

## 6. 未验证项与可优化点

**未验证**：
- **只测了 item**。npc（72 MB）、npc_drops（35 MB）、skill parts（29 MB）都未测；
  它们的数据形状不同（skill 有组展开语义），结论不可直接外推。
- 未接进真实加载路径（`XmlDataLoader.loadItemData` 端到端启动对照）。
- 未测「item + NPC + 定义」同窗口并行（生产抢核更凶，绝对值会更摊薄）。

**已知可优化点（未做）**：
1. **键名驻留**：键名集合只有约 30 个，可缓存归一，但 `substring` 已经发生，收益有限。
2. **键名按字符区间匹配**：`readKey` 返回 `(start,end)` 而不建 String，用区间查表——
   可直接消掉 257 万个键名 String，预计把分配量拉到 JAXB 以下。需要自建小型查表结构。
3. **缩写键名**（如 `weapon_stats`→`ws`）：体积与扫描成本同步下降，可读性略降。

**波动说明**：并行稳态 `parallel-jsonl` 的 wall 数组为 `[172.3, 73.1, 84.6, 91.6, 72.3]`，
首轮明显偏高（JIT/调度），中位数 84.6 ms 与前一份报告的 67.5 ms 有差距；
同机其它进程会污染并行测量，比值可信、绝对值偏乐观。

## 7. 复现命令

```bash
JAVA=/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/java
CP="target/classes:$(cat /tmp/aion-runtime-cp.txt):/tmp/probe-classes"
XMLDIR=src/main/resources/aion/data/static_data/items/item

# 1) 全量建 schema（必须全量，否则各分片形态不一致）
python3 .agents/summary/item-format-migration/gen_plain_jsonl.py --schema-out /tmp/item-schema.json \
  $XMLDIR/item_template_*.xml

# 2) 逐分片生成
for f in $XMLDIR/item_template_*.xml; do
  python3 .agents/summary/item-format-migration/gen_plain_jsonl.py --schema /tmp/item-schema.json \
    "$f" "/tmp/item-shards-plain/$(basename "$f" .xml).jsonl"
done

# 3) 编译 + 校验 + 计时
javac -nowarn -cp "target/classes:$(cat /tmp/aion-runtime-cp.txt)" -d /tmp/probe-classes \
  .agents/summary/item-format-migration/JsonlItemProbe.java
"$JAVA" -cp "$CP" JsonlItemProbe count /tmp                    # 四条路径都应为 128632
"$JAVA" -cp "$CP" JsonlItemProbe verify <shard.xml> <shard.jsonl>
"$JAVA" -Xms512m -Xmx3g -cp "$CP" JsonlItemProbe cold-jsonl-plain /tmp/item-shards-plain 3
```

## 8. 优化实现后的结果（同日追加）

§6 列的优化点 ①② 已实现，结果**好于预期**。

### 8.1 两项优化

**① 键名按字符区间匹配，不建 String。**
`JsonReader.readKeyRegion()` 扫描键名时**顺带算出 `String.hashCode()` 同算法（`h = 31h + c`）的哈希**，
只留下 `(keyStart, keyEnd, keyHash)`；`ClassMeta` 把 attributes 与 elements 合并成一张开放寻址表，
`byKey(reader)` 用整数哈希定位、冲突时沿 `keyCharAt` 逐字符比对。
**每行约 20 个键 × 128,632 条 ≈ 257 万个键名 String 全部消除**，且哈希是一次遍历的副产品，
不像 `String.hashCode()` 要再走一遍字符。

**② 数字直接解析，不建 String 且不装箱。**
`readInt`/`readLong`/`readBoolean` 直接在字符上累加；`PlainBinder.applyAttribute` 对基础类型改用
`Field.setInt/Long/Boolean/Short/Byte` 直接写入——`id=100001266` 这类超出 `Integer` 缓存（-128..127）
的值，原先每条记录都要新建一个 Integer。

### 8.2 踩坑（已修，值得记录）

**优化后一度回归 8 处**：`/actions/itemActions[0]/questid` 全被读成 0。原因：数据里存在
**以字符串书写的数字**——`"questid":"04450"`（带前导零，全量 8 处）。生成器的安全转换规则
（`^-?(0|[1-9]\d{0,14})$`，不容前导零）**故意保留它们是字符串**，而快路径假设"int 字段的值一定是
裸数字"，`readInt()` 见到 `"` 立即返回 0。

修法是快路径先分辨字符串字面量。**这条与 §2 的 6 类 JAXB 边界同类：都是静默错误**，
只有逐字段比对才能发现。

### 8.3 结果

| 指标 | `jaxb` | `dict` | plain 优化前 | **plain 优化后** | 优化后/JAXB |
|---|---:|---:|---:|---:|---:|
| 单分片热态 | 90.0 ms | 27.8 ms | 58.2 ms | **48.5 ms** | **1.86×** |
| 单分片分配量 | 46.3 MB | 36.2 MB | 62.2 MB | **30.5 MB** | **1.52×** |
| 串行全量 | 997.3 ms | 297.3 ms | 526.7 ms | **441.3 ms** | **2.26×** |
| 串行全量分配量 | 575.6 MB | 522.5 MB | 676.0 MB | **364.2 MB** | **1.58×** |
| 并行稳态 | 186.1 ms | 84.6 ms | 110.2 ms | **81.1 ms** | **2.29×** |
| **并行冷启动** | **1540.4 ms** | 649.7 ms | 1099.1 ms | **858.0 ms** | **1.80×** |
| 冷启动分配量 | 596.7 MB | 531.3 MB | 696.7 MB | **385.9 MB** | **1.55×** |
| 冷启动类加载 | 1578 | 793 | 779 | 781 | 2.02× |
| 冷启动 JIT | 1842 ms | 1126 ms | 1622 ms | 1523 ms | 1.21× |

### 8.4 三个结论

**① 分配量反超，成了三者中最低的。**

| 口径 | JAXB | dict | **plain 优化后** |
|---|---:|---:|---:|
| 单分片 | 46.3 MB | 36.2 MB | **30.5 MB** |
| 串行全量 | 575.6 MB | **522.5 MB** | **364.2 MB** |
| 冷启动 | 596.7 MB | 531.3 MB | **385.9 MB** |

原来比 JAXB 高 16.8%，现在**比 JAXB 低 35%、比 dict 低 30%**。
**可读格式的分配量低于压缩格式**——这是本次最意外的结果。原因：dict 虽共享值与键的实例，
但每条记录仍需构建索引行；优化后的 plain 只在真正遇到字符串值时才 `substring`。

**② 与 JAXB 的比值从 1.40× 提升到 1.80×。**

生产最相关的并行冷启动：1540.4 → **858.0 ms，省 682.4 ms**（优化前是 441.3 ms）。
相对静态数据窗口（8.05 s）约 **8.5%**，总启动（12–14 s）约 **4.9–5.7%**。

**③ 与 dict 的差距收窄到一个量级之内，并行稳态已反超。**

| 口径 | dict | plain 优化后 | plain/dict |
|---|---:|---:|---:|
| 并行稳态 | 84.6 ms | **81.1 ms** | **0.96×（反超）** |
| 串行全量 | 297.3 ms | 441.3 ms | 1.48× |
| 并行冷启动 | 649.7 ms | 858.0 ms | 1.32× |

**剩余差距的两个来源**：
- **体积 2.34×**（85.6 vs 36.6 MB）→ I/O 与扫描字符数更多；
- **JIT 1523 vs 1126 ms（+35%）**——递归下降 + 类型查表的代码路径比预编译 accessor 链多。
  这一项与格式无关，**AOT/AppCDS 可以覆盖**。

**折算到"可读性值多少"**：相对 dict，采用 plain 每次冷启动多花 208.3 ms
（858.0 vs 649.7），换来可读、可手工编辑、可作唯一源格式、且分配量更低。

### 8.5 正确性

优化后重跑全部 11 分片逐字段校验：**11,925,674 个字段，3 处不一致**——与优化前完全相同，
仍是那两个重复 id 的假阳性。
