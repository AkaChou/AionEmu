# P0c-11：非 IR 轴系统化（统一登记表 + 跨轴门禁）

- 日期：2026-09-25
- 归属：真端任务驱动替换目标（P0c 线，v3 提示词 §4 P0c-11）
- 触发：P0c-8c/9 证明**采纳真端会连带改变"节点/转换之外"的轴**（服务器侧封顶、旧存档自愈边、
  前置条件两表达），这些轴不在家族等价普查里、散落三处登记，曾只能靠 T3 clean 对账事后暴露。
  本切片把三处合并成一张机器可读统一登记表，并加跨轴门禁使其成为长期驻守（进 T1 固定清单）。

## 1. 交付

| 文件 | 作用 |
|---|---|
| `src/test/resources/quest/retail-non-ir-axis-registry.tsv` | **统一登记表（46 行 = 30 CAP_LEVEL + 15 LEGACY_SAVE_HEAL + 1 轴级 PREREQ_DUAL_EXPRESSION）**，列：quest_id / axis / disposition / detail / source |
| `.agents/summary/scriptdll-quest-driver/p0c11_build_non_ir_registry.py` | 生成器（唯一写入口）；**采纳切片落地后必须重跑**刷新登记表 |
| `src/test/java/.../retail/RetailNonIrAxisGateTest.java` | **跨轴门禁（4 例）**：登记新鲜度、封顶行不得为已采纳、自愈边任务必须真端驱动且无 EnterWorld 路由、前置两表达轴在案 |
| `src/test/resources/quest/retail-legacy-save-normalization.tsv` | ③ 旧存档自愈边登记的**规范位置**（从 `.agents` 迁入，21 条边/15 任务，受门禁守）；旧文件留指针 |
| `affected_quest_tests.py` | T1 固定清单 += `RetailNonIrAxisGateTest` + `RetailSimpleHuntFamilyGateTest` + `RetailSimpleTalkGateTest`（三族门禁长期驻守，清单现 15 类） |
| `p0c9_retire_rows.py` | 落地器登记路径指向规范化后的新位置 |

## 2. 门禁不变量（RetailNonIrAxisGateTest，4 例）

1. **登记新鲜度**：统一登记表的 CAP_LEVEL / LEGACY_SAVE_HEAL 行与两个逐任务源
   （封顶清单 / 自愈边登记）**逐任务完全一致**——任何一源变动而未重跑生成器即失败
   （把"落地后刷新登记表"从纪律变成机器约束）。
2. **封顶行不得为已采纳**：`CAP_LEVEL` 行的任务必须 owner=XML_RETENTION——
   采纳后生产==真端 UNLIMITED，封顶随旧 XML 失效；登记行必须随采纳切片同步移除
   （P0c-8c 的 80604/80609、P0c-9 的 80602..80610 先例的机器化）。实测 30 行全部 XML_RETENTION。
3. **自愈边任务必须真端驱动且无 EnterWorld 路由**：`LEGACY_SAVE_HEAL` 行的任务必须 owner=RETAIL_TABLE
   （边是采纳时移除的），且生产定义逐个编译后 **EnterWorld 路由数 = 0**——
   把"自愈边不编入真端定义"从测试判例（80601/80606、28313）升级为全登记表不变量。实测 15 任务全过。
4. **前置两表达轴在案**：轴级 `PREREQ_DUAL_EXPRESSION` 行存在，且 M1 分歧表
   `startConditions`（1136 行）/`prerequisites`（991 行）两轴均有登记（该轴由 M1 门禁逐行守，
   统一表只登记轴级指针，不重复逐任务行）。

## 3. 对拍与验收

| 项 | 结果 |
|---|---|
| `RetailNonIrAxisGateTest` | **4 例 / 0F**（首跑即绿，含 15 个真端定义逐个编译） |
| 生成器 | `46 行 = 30 CAP + 15 HEAL + 1 轴级`；重跑逐字确定 |
| T1（固定清单扩至 15 类后） | **58 例 / 1F**（唯一失败 = `RetailDataDrivenGateTest`，并发 DD 批在飞门禁）；本切片新增 8 例（非 IR 轴门禁 4 + 两族门禁 4 类）全绿 |
| `verify_retirement.py` | `catalog=2314 directory=2314 retired=3910 sum=6224 — OK` |
| 迁移完整性 | `.agents` 旧文件留指针；`p0c9_retire_rows.py` 已指向规范位置 |

## 4. 未验证 / 阻塞 / 下一步

- **未验证**：统一登记表当前只覆盖三轴；若后续采纳再暴露新非 IR 轴（如过场/影片、物品轴），
  按同模式扩轴（生成器 + 门禁各加一段）。
- **阻塞**：无。
- **下一步**（v3 队列）：**P1 SimpleItemPlay**（15 行全族流程：真端表已入仓 43 行、`FAMILY_PENDING` 已登记、
  41 行主形状 `triplet & select1` + 2 行 `_faction_`）——最小族，验证流程自动化程度。
  RETAIL_TALK_CHAIN 322 行链式合成与 M3-d 10 行 EQUIVALENT 归零为候选独立切片。
