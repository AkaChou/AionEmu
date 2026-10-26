# 配置编辑模型 (Configuration Edit Model)

本文说明控制台如何在一份带双语注释的 `.properties` 文件上做**安全**的写入。

This document explains how the console performs a **safe** write on a `.properties` file that carries bilingual
comment blocks.

---

## 一、为什么必须行级保留 (Why Line Preservation Is Mandatory)

生产配置文件长这样：

```properties
# GM 等级 / GM Level
# 默认值: 3 / Default: 3
gameserver.administration.gmlevel = 3
```

注释块承载运维语义：默认值是什么、这个键是干什么的、中英文各说一遍。用「键值表」重写整个文件会把它们
全部抹掉，而运维改一次配置就丢一次上下文。

生产文件因此**绝不能**整表重写。控制台以「行」为单位建模，只替换值真正发生变化的那一行。

The comment blocks carry operational meaning: the default value, what the key does, stated once per language.
Rewriting the file as a key-value map would wipe them out, and every operator edit would cost a little context.

Production files therefore must **never** be rewritten as a whole. The console models them in lines and replaces
only the line whose value actually changed.

---

## 二、文档模型 (Document Model)

`PropertiesDocument` 把文件拆成 `Line` 序列，每行携带：

| 字段 | 含义 |
|---|---|
| `original` | 该行的原始文本，逐字节保留 |
| `key` | 键；注释行与无 `=` 的行为 `null` |
| `value` | 值；键值两端空白已去除，值内部空白保留 |
| `valueStart` / `valueEnd` | 值在原始行中的偏移，用于就地替换 |

**行的判定**：去除首尾空白后为空、以 `#` 开头或以 `!` 开头的行是注释；含 `=` 的行是键值行，键为 `=` 之前的
部分（去除空白），值为 `=` 之后的部分（去除首尾空白）。

**就地替换**：`withValue(key, newValue)` 只把该行的 `[valueStart, valueEnd)` 区间换掉，键、分隔符与两侧空白
原样保留：

```properties
k =   hello world   
```

改写后仍是：

```properties
k =   hello world   
```

（值变成新值，其余逐字节不变。）

**重复键**：`withAllValues` 替换该键的**全部**出现位置。`occurrencesOf` 可查询出现次数。

---

## 三、物品 ID 列表 (Item ID List)

`item.restriction.txt` 不是键值文件，而是每行一个物品 ID。解析时忽略注释行与空行，键与值都是 ID 文本本身。
写入时只替换列表中**已存在**的 ID；提交一个不存在的 ID 不会向文件插入新行——避免控制台成为任意内容的注入口。

`item.restriction.txt` is not a key-value file but a list holding one item id per line. Parsing skips comment
lines and blank lines; the key and the value are both the id text. A write only replaces ids **already present**
in the list; submitting an absent id does not insert a new line, so the console never becomes a channel for
arbitrary content.

---

## 四、差异生成 (Diff Generation)

差异按行号对齐生成：逐行比较改写前后的文本，只输出**不同**的行。

```json
{
  "lineNumber": 16,
  "before": "gameserver.administration.gmlevel = 3",
  "after": "gameserver.administration.gmlevel = 5"
}
```

前端以红/绿两行成对呈现。`preview` 端点**不落盘**，因此运维可以先看差异再决定是否写入。

The diff is produced by comparing the texts line by line and reporting only the lines that differ. The `preview`
endpoint **does not touch the disk**, so an operator can review the change before committing to it.

---

## 五、写入与回滚 (Write and Rollback)

```text
备份原文件 → 写盘 → 重建配置源 → 定向热应用
                                  ↓ 失败
              用备份覆盖回原文件 → 重建配置源 → 再热应用一次（恢复旧值）
```

**为什么需要重建配置源**：改造前 `Environment` 持有启动时的快照，重读磁盘的值会被它遮蔽。改造后裸键由
`AionConfigFilePropertySource` 提供，该源指向磁盘文件本身，但 `Environment` 里的实例仍是启动时构造的，
因此改盘后必须重建该源，重建动作由 `AionConfigSourceRefresher` 完成。

**为什么回滚必须再热应用一次**：文件恢复只解决了磁盘状态，内存里的静态字段仍是被改坏的值。必须再跑一遍
热应用，把旧值写回内存。

The rebuild is needed because the `Environment` still holds the instance constructed at boot, so a rewritten file
would otherwise stay invisible. The rollback applies the values again because restoring the file alone fixes only
the disk state; the static fields still hold the broken values until the old ones are written back.

---

## 六、定向热应用 (Targeted Hot Apply)

| 文件 | 热应用动作 |
|---|---|
| `admin.properties` | `ConfigurableProcessor.process(AdminConfig.class, ...)` |
| `developer.properties` | `ConfigurableProcessor.process(DeveloperConfig.class, ...)` |
| `panel.properties` | `ConfigurableProcessor.process(PanelConfig.class, ...)` |
| `commands.properties` | `ChatProcessor.getInstance().reload()` |
| `pff.properties` | `PacketFloodFilter.getInstance().reload()` |
| `item.restriction.txt` | `AdminService.getInstance().reload()` |

**不调用完整 `Config.reload()`**：那会连带刷新副本、攻城与日常调度等二十余个调度器。改一个权限数字不该
重启攻城调度。
