# P1 第二横切报告：NPC 名解析器 + SimpleHunt 表装载与相机行推导（纯新增，零切换）

日期：2026-10-01　车道：quest-engine-native（计划：`../2026-10-01-真端引擎迁移计划.zh-CN.md`）
前置：P0a / P0b / P1 第一横切（RawQuestVarsCodec / ProgressCamera / CameraRegistry，17 例绿）

## 1. 本批范围（声明后执行）

| 产出 | 路径 | 性质 |
|---|---|---|
| NPC 名解析器 | `src/main/java/com/aionemu/gameserver/questEngine/tablelane/NativeNpcNameResolver.java` | 新增生产类 |
| SimpleHunt 表装载器 | `src/main/java/com/aionemu/gameserver/questEngine/tablelane/NativeQuestTableLoader.java` | 新增生产类 |
| 两个门测试类 | `src/test/java/com/aionemu/gameserver/questEngine/tablelane/NativeNpcNameResolverTest.java`（5 例）、`NativeQuestTableLoaderTest.java`（6 例） | 新增测试 |
| 表行 vs 脚本矩阵对拍 | `p1/tools/reconcile_hunt_table_vs_camera.py` + `p1/tools/hunt-dormant-table-only.tsv` | 证据工具 |

未触碰任何既有生产路径；未删除任何文件；无新第 3 类（反漂移 D.3 停批线内）。

## 2. 对拍结论（全量 PASS）

`reconcile_hunt_table_vs_camera.py`：入仓 `Quest_SimpleHunt.xml`（1863 活跃行）推导相机规约 vs P0a 脚本矩阵（camera-params.tsv，2463 调用点 / 1812 任务）：

- **宽度规则实证成立**：任一 count > 63 ⇒ 10 位，否则 6 位 —— 1812 任务零失败（数据自身即判据，无需额外宽度列）。
- **槽/required 全等**：脚本 (slot, required) ⊆ 表 (countN) 且 required == count，零 mismatch。
- **fullValue 全等**：表推导（各槽 count 按位移或）== 脚本调用字面 fullValue，1812 任务零 mismatch。
- **13912/23912 "分歧"消解**：脚本字面 fullValue=0x1001 本就等于表推导（count1=1 + count3=1）；脚本只是没给槽 3 事件源（无 monster3 ⇒ 相机永不满 ⇒ 真端自身休眠）。表推导如实含槽 3，不需要任何数据层特例登记。
- **休眠行 51**：表有行、脚本无相机包装 ⇒ 原生侧同样不接 handler。构成 = 4 零计数行（11013/11014/11208/11209）+ 23×37xxx + 24×47xxx（清单：`hunt-dormant-table-only.tsv`）。

## 3. 数据级新发现（全部已按事实建模）

1. **836025 同分片定义两次**：名字索引对两个定义都入索引、按名去重 id，防假多义（若丢弃第二定义会丢 1 个独立名字——首版实现就丢了：116224 vs 116225，已修）。
2. **元素文本跨行**（37116 的 12 个 monster 名单含换行）：装载器与对拍脚本都做内部空白归一。
3. **真端占位 `name=" "`（375 处）**：规范化为空后不入索引键。
4. **零计数行合法装载但拒绝派生相机**（NATIVE_CAMERA_ROW_MISSING）。
5. **记忆纠错（测试抓出）**：80817 是 **DataDriven** 任务（P0a DD 宽计数修正里的 count=100 例子），不在 SimpleHunt 表——此前会话记忆把它当 SimpleHunt 形状引用是错的，本批测试实证并纠正。

## 4. 组件语义（对齐计划 §6.2）

- `NativeNpcNameResolver`：索引 = npcTemplates `name` ∪ `name_desc`（P0a owner-identity 修正判例），trim+小写精确唯一解析；MISSING → `NATIVE_NAME_UNRESOLVED`、多义 → `NATIVE_NAME_AMBIGUOUS`，无别名表/模糊匹配（D.3-13）。分片发现 = classpath 目录 `aion/data/static_data/npcs` + 与生产 XmlDataLoader 相同的 `npc_template_{a}_{b}.xml` 约定；jar 内态 fail-closed。
- `NativeQuestTableLoader`：DOM 安全解析（内部实体子集允许、外部 DTD 全拒）；行 = 接取/交付 NPC 名 + 槽 1..5 的 count/monster；`monster` 无 `count` 真端无此形 fail-closed，`count` 无 `monster` 是 13912 形如实装载；`cameraSpec()` 纯函数产出 `CameraRegistry.RowSpec`；**哪些行进运行时 = 切换批按脚本接线集决定**（本类不做接线）。
- 行 id 在真端表是行元素自身的 `id` 属性（`<id id="N">`），非子元素。

## 5. 验证

`mvn -Dtest='RawQuestVarsCodecTest,ProgressCameraTest,CameraRegistryTest,NativeNpcNameResolverTest,NativeQuestTableLoaderTest,HtmlPagesRegistryTest,TableSourceProvenanceGateTest' test`
→ **35/35 绿**（本批新增 11 例；修红 4 轮：行 id 属性误读、计数元素误当行元素、836025 丢名、80817 家族错置/37116 计数 12）。
启动规模：解析器全量装载 0.6s / 表 0.06s（测试实测），符合 P0a 启动估算。

## 6. 仍被门住的部分（未变）

- **完整 P1 切换批**（SimpleHunt handler + NativeQuestStatePort + owner/路由接入 + 同批删旧编译器与 TSV 读取）仍在等 name-resolution 残差处置裁决（A 冻结行清单 / B 用户供真端 NPC 模板数据 / C 先 B 后 A）——sql.rar 已证不含该数据（见 `../p1-prereqs/name-resolution-decision.md` §5）。
- DD 337 客户端缺失行处置、80817（DD）KNOWN_BROKEN 复刻 vs 显禁用——用户裁决项。
- 后台考古两路在飞：SimpleSerialHunt 推进机制（P2 前置）、DD 事件分发器与其余 7 类 handler（P7 前置），结论另报。
