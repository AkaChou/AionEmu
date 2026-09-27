# P1 wave-2：SimpleItemPlay 驱动接线 + wave-1 六行退役（本线 retired 3903 → 3909）

- 日期：2026-09-25
- 切片：P1 SimpleItemPlay wave-2（接线 + 退役；族收口）
- 权威口径：真端表为形状权威；退役判据 = wave-1 的"合成定义与退役前生产 XML IR 逐字等价"实测
  （`2026-09-25-P1-simple-item-play-wave1.zh-CN.md`），退役 = 删除（内容在 git 历史，不冻存副本）。

## 1. 交付

1. **裁定与保留清单**
   - `p1_build_itemplay_decisions.py` → `p1-itemplay-decisions.tsv`（15 行：6 `ADOPT_RETAIL/
     ITEM_PLAY_CANONICAL`（附 wave-1 IR 指纹）+ 9 `KEEP_XML/<稳定码>`）；
   - `build_retention_list.py` 接入 SimpleItemPlay 族（`IMPLEMENTED_FAMILIES` +1、
     `ITEM_PLAY_DECISIONS` 输入、显式族分支）：6 行 → `RETAIL_TABLE/OK`、9 行 →
     `XML_RETENTION/SEMANTIC_GAP:RETAIL_*`；全量 **6224 = RETAIL_TABLE/OK 3909 +
     SEMANTIC_GAP 1645 + SCRIPTED 494 + NO_TABLE 176**；两副本（test-resources + main-resources）
     与生成器输出逐字节一致。
2. **驱动接线**（`RetailQuestDriver`）：`retailOwnedSimpleItemPlay` 分区 + `RetailSimpleItemPlayTable`
   装载 + `compile()` ItemPlay 分派 + `compileSimpleItemPlay`（与 UseItem 同构：行缺失/元数据失败/
   稳定码拒绝/RUNTIME_FAILURE 四态）。
3. **退役落地**（`p1_retire_itemplay_rows.py`，前置断言 → `--dry-run` → 落地）：
   `Path.unlink()` 删除 13704/13708/19048/23704/23708/29048 六个生产 XML；
   `quest_definition_catalog.xml` 同步移除 6 条（余 **2315**）；
   **`verify_retirement.py` = `catalog=2315 directory=2315 retired=3909 sum=6224 — OK`，零悬空引用**。
4. **族门禁转退役后口径**（`RetailSimpleItemPlayGateTest`）：
   - wave-1 六行改**真端侧冻结指纹护栏**（P0c-3 裁定行先例）：合成 IR 指纹必须与
     `src/test/resources/quest/retail-simple-item-play-ir-fingerprints.tsv` 冻结值逐一相等
     （`-Dretail.itemPlay.fingerprintOut` 显式重冻结）；重算结果与 wave-1 证明**逐字节一致**；
   - retention 分区精确（RETAIL_TABLE 恰为 wave-one 集 / XML_RETENTION 恰为 9 KEEP 集，
     reason 与登记码一致）；退役态否定式（6 行生产 XML 必须已删，回插即失败）；
     KEEP 行 XML 必须在盘；9 行编译稳定码白名单不变。
5. **直读退役 XML 的对齐测试改生产视图**（3 文件 6 方法）：
   `Quest13704ClientDialogAlignmentTest` / `Quest13708ClientDialogAlignmentTest` /
   `Quest19048And23704And23708And29048ClientDialogAlignmentTest` 的 `definition()` 助手改
   `ProductionQuestDefinitions.definition(id)`（P0c-9 `ClientQuestSectionAlignmentTest` 先例）——
   断言零改动即绿：真端合成定义直接满足 XML 时代的客户端旅程合同（USE_OBJECT → 报告页 → 原生奖励结算）。

## 2. 对拍与验收

| 门禁 | 结果 |
|---|---|
| 族门禁（冻结模式 → 正常模式） | 冻结表生成并与 wave-1 指纹逐字节一致；正常模式 1 例绿 |
| T1（16 类） | `gates/T1-023202.log` **59 例 / 0F / 0E 绿**（325s） |
| T2（6 个退役 id 命中类） | 初跑 1F+8E：6E 自因（3 个直读 XML 对齐测试）→ 改生产视图后复跑**全绿**；其余 1F+2E 归并发会话在飞（见 §3） |
| verify_retirement | `catalog=2315 directory=2315 retired=3909 sum=6224 — OK` |
| T3 clean 副本（`/private/tmp/aion-p1w2`，forkCount=2，用后已删） | **1974 例 / 39F / 56E / 1 skipped**；方法级归账 vs P0c-9 clean 基线（107 个失败方法）：基线 21 个已解决、**新增 9 个全部归并发会话在飞面**，**本切片自因新增 0**（6 个 ItemPlay id 在失败集零命中） |

## 3. 归因细节（诚实记账）

- **T2 初跑的自因面（已修复）**：3 个对齐测试类直读已删 XML → 改生产视图后全绿（§1.5）。
- **并发会话在飞证据**：T1 02:32 全绿 → T2 02:38 出现 `NoClassDefFoundError:
  QuestDialogOrderAudit`（对方新增中的类）与 `RetailDataDrivenGateTest` 冻结指纹漂移
  （13xxx 系列 20+ 行）——对方在 02:32 后推送了新改动；
  T3 新增 9 个失败全部为其新退役 id（80990/15590/1514/19672/28932/19637/15041/26828/26829/
  16828/16829/1309 等）的直读 XML 测试与对话序在飞面，与 ItemPlay 无涉。
- 本会话遵守并发纪律：未触碰 `RetailDataDriven*` / `RetailSimpleUseItem*` / memory-bank /
  `.agents/summary/index.jsonl`。

## 4. 已知坑（再次踩中，复认记录）

- **`target/classes` 残留已删 XML（第 3 次）**：删除 6 个源 XML 后未清 classpath 残留，
  `verifyProductionCoverage` 报 `wrongOwner=[6 ids]`（T1 15E 假红）；外科手术删除
  `target/classes` 下 6 个残留后 T1 全绿。退役类切片跑门禁前先对齐
  `target/classes/.../quests` 与源树（2315=2315）。
- **rsync 排除项未锚定根**（P0f 同坑重踩）：`--exclude aion` 把
  `src/main/resources/aion/` 一并排除，副本缺全部静态数据；改 `/aion` 根锚定后重拷。

## 5. 未验证 / 下一步

- **未验证**：6 行运行时行为（接取发物 → 使用演出 → 交付领奖全链路）、客户端抽检
  （`quest_summary` 行 / `SECTION_n` 计数）——留给 P3 终局抽检统一做。
- **下一步候选**：RETAIL_TALK_CHAIN 322 行链式合成（SimpleTalk 族最大归零面）、
  M3-d 10 行 EQUIVALENT 归零、SimpleItemPlay 9 行 KEEP 的波次推进
  （对话链 2 / 名字证据 3 + 哨兵 2 / 推进轴 2）。
- SimpleItemPlay 族口径收口：15 行生产族每行有 owner、有证据、有可重跑门禁——**族收口完成**。
