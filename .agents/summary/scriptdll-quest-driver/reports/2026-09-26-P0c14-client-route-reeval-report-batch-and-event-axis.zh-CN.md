# P0c-14：CLIENT_ROUTE 复験第二批——ReportToMany 16 + 过渡相等 20 采纳；事件轴 9 行真端缺口定性

> 日期：2026-09-26 ｜ 切片：P0c-14 ｜ lane：SimpleTalk ｜ 前序：P0c-13（Kaliga 20 复験反转）

## 交付

1. **余 53 行三分离**（探针差逐行分类，判据全为在先裁定类）：
   - **ADOPT 36**：ReportToMany 系 16（80268/80270/80310/80311/80324-80327/80353-80355/
     80362-80364/80623/80624，`p0c14-report-decisions.tsv`）+ 过渡相等/小差异 20
     （2150/3103/4913/18802/23902/28800/28802/1526/2110/18505/18509/28509/30312/30314/
     30315/80016/80018/1351/80290/80294，`p0c14-clean-decisions.tsv`）。
   - **KEEP（真端缺口，新定性）9**：80028/80031/80032/80034-80039——探针直证真端编译
     **丢失事件任务轴**（`EventActive` 条件、`LevelUp`/`EventQuestRefresh` 边、
     `UseItem` charm 卡激活）：80034 系 XML 的
     `COMPLETE→EventQuestRefresh→START`（live inventory 子任务重启）与
     `NONE→LevelUp→START` 三边全缺，而 CharmedEvent 契约测试明确断言该重启语义。
     SimpleTalk 形状无事件轴 → 保留 XML 降级（码 CLIENT_ROUTE 暂留），归属未来
     EventQuest/DataDriven 通道；M3-d 降级对本 9 行经精化证据**维持**。
   - **缓议 8**：26930（cnt179：XML 类别门选择性奖励 vs 真端简单交付，且
     `QuestDefinitionCatalogManifestTest` 注释与现状矛盾——26930 实际仍 XML_RETENTION）、
     80356/80365（cnt39）、2654（cnt20）、11003（cnt21）、1141（cnt19）、1117、1353
     （多梯队测试缠入在先红）——留 XML 下轮细裁。
2. **ReportToMany 16 直证**：临时探针 `RetailTalkReportRouteProbeTest`（源存 .java.txt）
   对 lenient overlay 编译逐行验证契约接取路由（START 节点 `TalkToNpc(npc,31)` →
   `ShowQuestDialog(page)`，页 2375/1003 与回归测试 EXPECTED_ROUTES 一致）——**16/16
   ROUTE_OK**。共享回归测试本体被在先失败（3914，早于本批迭代序）阻塞，增量判定待门复。
3. **过渡相等 20 直证**：探针过渡多重集**全等**（10 行纯节点投影 QE-051、6 行 cnt0、
   4 行小 legacy 差）；var0 敏感测试逐一核对——2150/3103/4913/18802/23902/28800/28802
   四节点断言 `var0=0`，`quest_client_summary_rows.tsv` 全部 =1 → retail var0=0 ✓；
   80290/80294/18505/80016/80018 同核 ✓；1526（journal 末行轴）/1351（阶段轴 s1=1/s2=2）
   有专门通道，静态一致性成立、待门终证。

## 结论（实测）

- SimpleTalk 族：**2223 = 2063 RETAIL_TABLE + 160 XML_RETENTION**（CLIENT_ROUTE 53→17）
- 全库 retention：**6224 = 4743 RETAIL_TABLE + 1481 名义 XML**（manifest 行数口径）
- catalog **1473**；36 XML 删除 + target/classes 陈旧副本 36 同步清理（P0c-13 判例流程化）
- 本 lane delta 恒等：**1473 + 4743 = 6216，差 8 = 并行 lane 在飞悬挂**（其删 XML 未落裁定，
  p5 层 mtime 00:59 未更新；重建两次均复现为 family-pending 悬挂，非本片所致）
- `RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 绿；
  `RetailOwnershipGateTest`/verify 的 6216 缺口同上（并行在飞）

## 门禁待复清单（并行 lane 收口后立即执行）

生产门当前 missing=40（并行 lane 删 40 行 XML 未翻转清单），全部 gated 契约测试 ERROR 于
装载而非断言。清单：①11 个专属契约测试（2150/3103/4913/18802/23902/28800/28802/2110/
30312/30314/30315）；②Haramel/RetailSingleStepRewardRow/DurableDaevanion/CollectTurnIn/
Movie×2/DialogOrder/CoreCapability；③`ReportToManyDialogRouteRegressionTest`（预期仅剩
在先 3914 失败 = 增量干净）；④`QuestCharmedEventDefinitionTest` 全方法（本片 9 行未翻，
应保持原状）。**回退路径**：若 1526/1351 断言翻红 → `git checkout` 恢复对应 XML +
`p0c14-clean-decisions.tsv` 该行改 KEEP_XML + 重建清单。

## 排障记

- 并行 lane 状态演变：01:00/01:21 落地（26 行翻转 + 15548/25548 修复）→ 01:21-01:45 间
  再删 40 行 XML（含 1100 段新手任务）未落裁定 → 门禁 missing=40 + 悬挂 8。按不触碰纪律
  未修补其行；两次重建吸收尝试均确认其裁定数据未落盘（p5 层 00:59 不动）。
- 探针修正一坑：`String[][]` 初始化缺内层花括号编译错——改 `List<String>` + split。
- 运行时行为与客户端目检：需启动服务授权，未执行。

## 下一步

- 门复清单执行（上节）→ 1526/1351 终证或回退
- 缓议 8 行细裁（26930 选择性奖励轴优先——ManifestTest 的 selectable-reward 契约族）
- CLIENT_ROUTE 余 17 行 + TALK_CHAIN 21 通道缺口 + CRAFT 28 续档；SimpleHunt gap 47
