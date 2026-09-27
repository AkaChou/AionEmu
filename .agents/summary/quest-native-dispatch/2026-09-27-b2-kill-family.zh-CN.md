# 缺口批 2 收口台账（击杀/狩猎族 108 行 · quest-native-dispatch）

> SEMANTIC_GAP：412 → **304**（−108，全部零行为裁定改名）；与批 1 同片验收（提速协议：
> 零行为项并入形状批），源码/数据面落在提交 `06f4da5c8`。

## 根因二分（108/108 逐行）

| 码 | 行数 | (a) 可迁移 | (b) 裁定保留 | 判据 |
|---|---|---|---|---|
| `RETAIL_MONSTER_UNRESOLVED`（SimpleHunt 11 / DataDriven 58） | 69 | 0 | 69 | 击杀目标名本服 NPC 数据不可解析（判例：`IDAbRe_Up_Asteria`、批 1 十行同族）；58 行 DD 行 drift 码一致无滞后 |
| `KILL_COVERAGE_LOSS`（SimpleHunt） | 35 | 0* | 35 | 家族门 `RETAIL_TABLE_GAPS` 类：行**能被族表合成**（门强制），但真端表目标集低于客户端 SECTION 契约 ⇒ XML 才是正确 owner（XML 带修复期完整目标集）。目标集补齐需外部客户端 quest_monster 解包数据（现产登记只覆盖 Iluma/Norsvold 两图）⇒ 本环境不可迁移 |
| `RETAIL_HUNT_MULTI_STAGE_DEFERRED`（DataDriven） | 4 | 0 | 4 | 多段狩猎轴未实现（3122/3123/4122/4123，drift 码一致）；XML 可玩 |

\* 35 行"可合成但契约弱于 XML"——迁移会丢客户端契约覆盖，属**不应迁移**而非**不能编译**；
`ADJUDICATED:KILL_COVERAGE_LOSS` 行继承家族门"必须仍合成"的 fail-closed 断言（前缀不变式中立）。

## 改动面

- retention（双副本）108 行：`SEMANTIC_GAP:<码>` → `ADJUDICATED:<码>` + 逐行证据列
  （目标缺失明细指向 `retail-simplehunt-compiler-rejects.tsv` / `retail-data-driven-drift.tsv`）。
- 零生产代码变化；零形状变化；门禁 = 批 1 快筛 + M1 T1（家族门对 35 行"仍须合成"断言绿）。

## 记录

- 10 行挑战采纳延期行（批 1）与本批 MONSTER_UNRESOLVED 同轴：击杀目标轴若将来补齐
  （外部 quest_monster 数据 + 采纳边）可再裁定。
- 未 push；未启停服务；未新增/退役 TSV；无实机复验项（零 flip）。
