# P0a §4.4.7：启动量级评估（native 车道注册表）

> P0a 只读审计产物 · 2026-10-01。数量全部来自本批实测（camera-params.tsv / table-source-inventory.tsv / owner-identity.tsv）。

## 注册表规模（一次性构建、运行期不可变）

| 注册表 | 行数 | 单对象估算 | 内存估算 |
|---|---:|---:|---:|
| `CameraRegistry`（SimpleHunt 相机步骤） | 2463 步骤 / 1812 任务 | 步骤 ~48B + 任务桶 ~64B | ~0.2 MB |
| 家族表行模型（8 张表，DD 2492 + Talk 3152 + Hunt 1863 + …） | 8562 行 | 行 ~400–900B（含名字数组/monster 列表） | ~5–8 MB |
| `HtmlPagesRegistry` | 5904 页 | ~80B | ~0.5 MB |
| quest.xml 元数据（name/desc/can_report/等级） | 10035 | ~200B | ~2 MB |
| `NativeNpcNameResolver` 名字→id 索引 | 57,832 名（npcTemplates） | 名 ~60B + id 集 | ~5 MB |
| DD handler 工厂 | 2492 行 × ~1.2 handler | handler 对象 ~200B（首用时创建则更低） | ~0.6–6 MB |
| owner resolver（表 id 集 ∪ XML id 集） | ~9,200 id | long 集合 | ~0.2 MB |
| **合计** | — | — | **≈ 15–25 MB** |

## 构建耗时预估

- 最大解析件：quest.xml 22 MB（UTF-16）、SimpleTalk 1.3 MB、SimpleHunt 1.2 MB、DD 2.9 MB、HtmlPages 1.6 MB（合计 <30 MB XML）。
- Jakarta XML-Bind/DOM 单遍解析 + 不可变行模型构建：**预计 2–6 s**（按 JVM DOM ~10–20 MB/s 吞吐），一次性启动成本；名字索引构建（57,832 名）<1 s。
- 与现状对比：现有 retail 编译层每次启动全量编译 IR，native 车道为「解析 + 注册」两步，无图合成，预计**持平或更快**。

## 结论

- 量级不构成启动风险；无需懒加载/分片。建议 P1 保持「启动期一次构建 + fail-fast 校验」（与计划 §6.2 一致）。
- `item_quest.xml`（17 MB）按 §4.4.6 决策不参与，不进入加载面。
