# 生成物清理台账（`chore(agents)`，第二至七轮） / Generated-artifact cleanup ledger

> 本台账记录本轮从 `.agents/` 删除的中间产物、删除理由与再生成方式。
> 被删文件的实际内容仍可从 git 历史取得：`git show <删除提交>^:<path>`。
> 本文件是唯一保留的“清单类”文档；其余目录内的原始转储、日志、快照不再入库。

## 1. 删除范围与体积

| 组 | 文件数 | 体积 | 说明 |
|---|---:|---:|---|
| `scriptdll-quest-driver` | 249 | 24.47 MB | 真端驱动迁移阶段的一次性 census / dump / diff / gate 输出；保留 decision 登记表与生成脚本 |
| `quest-native-dispatch` | 131 | 4.56 MB | 退役 TSV 快照、指纹快照、census 输出；保留决策与台账 markdown |
| `other-topic-evidence` | 318 | 9.78 MB | 各任务主题的审计输出、备份 XML、测试日志、探针文本；保留报告 markdown 与决策表 |
| **合计** | **698** | **38.82 MB** | |

## 2. 保留边界

- 保留：`.md` 报告/台账/验收记录、`.py`/`.sh`/`.java` 工具与门禁、以及 `*-decisions*.tsv`、`*-registry*`、
  `retail-xml-retention.tsv` 等“人工裁定登记表”（不可从生产数据重算）。
- 删除：census / dump / diff / fingerprint / audit-output / gate 日志 / 备份 XML / 退役快照（`.retired-*`、
  `.before`/`.after`）等纯派生产物；同一信息的生产侧真源仍在 `src/main/resources/aion/data/**`。
- 仍然被 memory-bank `evidence:` 引用的产物（82 个）保留，避免破坏 `verify_memory_bank.py` 门禁。

## 3. 再生成入口（节选）

| 产物 | 再生成方式 |
|---|---|
| `item_name_index.tsv` | `.agents/summary/scriptdll-quest-driver/p0c11_build_item_name_index.py` |
| `npc_name_index.tsv` | `.agents/summary/scriptdll-quest-driver/p0c2_simple_talk_sentinel_census.py` 内的 `npc_name_index()` |
| `quest_registry.tsv` | `.agents/summary/scriptdll-quest-driver/extract_quest_registry.py` |
| `retail-xml-retention.tsv` | 生产真源 `src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv` |
| `m5b2b-quest-event-census.tsv` | `.agents/summary/scriptdll-quest-driver/m5b2b_handler_slot_scan.py --all --tsv` |
| 其他 `p0c*` / `m5*` 中间表 | 同目录同名生成脚本（`*.py`）重新运行 |

## 4. 被删文件清单

### 4.1 `scriptdll-quest-driver`（249 个）

| 文件 | 字节 |
|---|---:|
| `scriptdll-quest-driver/P0c35ContractScanProbeTest.java.txt` | 1997 |
| `scriptdll-quest-driver/P0c35RewardRecoveryEdgeProbeTest.java.txt` | 1924 |
| `scriptdll-quest-driver/P0c37GapPartitionProbeTest.java.txt` | 3901 |
| `scriptdll-quest-driver/P0c37LadderProbeTest.java.txt` | 3342 |
| `scriptdll-quest-driver/P0c43CanonicalResynthesisProbeTest.java.txt` | 4306 |
| `scriptdll-quest-driver/P0c44StageOwnerDriftProbeTest.java.txt` | 3760 |
| `scriptdll-quest-driver/P0c45ExtraOwnerPruneProbeTest.java.txt` | 3349 |
| `scriptdll-quest-driver/P0c48PvpChainProbeTest.java.txt` | 13993 |
| `scriptdll-quest-driver/P0c49PreflipContractProbeTest.java.txt` | 17528 |
| `scriptdll-quest-driver/P0c49TalkFobjHuntProbeTest.java.txt` | 14034 |
| `scriptdll-quest-driver/P0c50FobjOnlyProbeTest.java.txt` | 14031 |
| `scriptdll-quest-driver/P0c50PreflipContractProbeTest.java.txt` | 17507 |
| `scriptdll-quest-driver/P0c51FobjCollectProbeTest.java.txt` | 18174 |
| `scriptdll-quest-driver/P0c53DeliverUncoveredProbeTest.java.txt` | 3543 |
| `scriptdll-quest-driver/P0c53DialogAxisProbeTest.java.txt` | 2816 |
| `scriptdll-quest-driver/P0c53bShardPinProbeTest.java.txt` | 3479 |
| `scriptdll-quest-driver/P0c54AuditProbeTest.java.txt` | 3263 |
| `scriptdll-quest-driver/P0c54StagePageProbeTest.java.txt` | 6995 |
| `scriptdll-quest-driver/P0c55AuditProbeTest.java.txt` | 3359 |
| `scriptdll-quest-driver/P0c55StageLegProbeTest.java.txt` | 6983 |
| `scriptdll-quest-driver/P0c57AcceptProbeTest.java.txt` | 6964 |
| `scriptdll-quest-driver/P0c57AuditProbeTest.java.txt` | 3359 |
| `scriptdll-quest-driver/Retail1526And1351OverlayProbeTest.java.txt` | 2248 |
| `scriptdll-quest-driver/Retail28800ShapeProbeTest.java.txt` | 1968 |
| `scriptdll-quest-driver/Retail80294HealEdgeProbeTest.java.txt` | 1060 |
| `scriptdll-quest-driver/RetailAcceptEntranceReplayProbeTest.java.txt` | 2212 |
| `scriptdll-quest-driver/RetailContractDeltaProbeTest.java.txt` | 3564 |
| `scriptdll-quest-driver/RetailEarlyElyosBarrelReplayProbeTest.java.txt` | 2278 |
| `scriptdll-quest-driver/RetailItemCheckCoverageProbeTest.java.txt` | 3712 |
| `scriptdll-quest-driver/RetailItemCheckGateChannelProbeTest.java.txt` | 12023 |
| `scriptdll-quest-driver/RetailLegacyHealRegistryProbeTest.java.txt` | 1429 |
| `scriptdll-quest-driver/RetailLenientWindowReplayProbeTest.java.txt` | 9901 |
| `scriptdll-quest-driver/RetailStormPollProbeTest.java.txt` | 844 |
| `scriptdll-quest-driver/RetailStormSignatureProbeTest.java.txt` | 1209 |
| `scriptdll-quest-driver/RetailTalkChainGateProbeTest.java.txt` | 7118 |
| `scriptdll-quest-driver/RetailTalkChainProbeTest.java.txt` | 8294 |
| `scriptdll-quest-driver/RetailTalkCutsceneProbeTest.java.txt` | 8737 |
| `scriptdll-quest-driver/RetailTalkEntryAxisProbeTest.java.txt` | 5084 |
| `scriptdll-quest-driver/RetailTalkItemProbeTest.java.txt` | 7086 |
| `scriptdll-quest-driver/RetailTalkReportRouteProbeTest.java.txt` | 2841 |
| `scriptdll-quest-driver/RetailTalkSentinelProbeTest.java.txt` | 7102 |
| `scriptdll-quest-driver/RetailTalkWorkItemProbeTest.java.txt` | 7102 |
| `scriptdll-quest-driver/RetailWindowRowShapeProbeTest.java.txt` | 2505 |
| `scriptdll-quest-driver/client-contract-introduced-now.tsv` | 5127 |
| `scriptdll-quest-driver/coverage-report.txt` | 2686 |
| `scriptdll-quest-driver/datadriven-coverage-report.txt` | 1331 |
| `scriptdll-quest-driver/datadriven-loader-messages.txt` | 2569 |
| `scriptdll-quest-driver/datadriven-progress-schema.tsv` | 2522 |
| `scriptdll-quest-driver/datadriven-step-reconciliation.tsv` | 132244 |
| `scriptdll-quest-driver/dd-fp-fresh.tsv` | 73924 |
| `scriptdll-quest-driver/dd-fp-new.tsv` | 80665 |
| `scriptdll-quest-driver/enterarea-retired-xml-evidence.tsv` | 6347 |
| `scriptdll-quest-driver/enterworld-retired-xml-evidence.tsv` | 284 |
| `scriptdll-quest-driver/gates/head-baseline-failures.txt` | 4742 |
| `scriptdll-quest-driver/gates/item-contract-before-ext-fix.tsv` | 141760 |
| `scriptdll-quest-driver/gates/m5b2-baseline-failure-details.tsv` | 6307 |
| `scriptdll-quest-driver/gates/m5b2-questengine-failures.txt` | 4742 |
| `scriptdll-quest-driver/gates/p0c48-t3-diff-vs-1027.txt` | 15596 |
| `scriptdll-quest-driver/gates/p0c48-t3-diff-vs-1303.txt` | 18623 |
| `scriptdll-quest-driver/gates/p0c49-t3-diff-vs-1438.txt` | 14426 |
| `scriptdll-quest-driver/gates/p0c51-25052-preflip.xml` | 18619 |
| `scriptdll-quest-driver/gates/p0c51-fp-post.tsv` | 87999 |
| `scriptdll-quest-driver/helper_families.tsv` | 1182 |
| `scriptdll-quest-driver/hunt-counter-model-validation.txt` | 186 |
| `scriptdll-quest-driver/item_name_index.tsv` | 4393279 |
| `scriptdll-quest-driver/m2e-retired-xml-evidence.tsv` | 48509 |
| `scriptdll-quest-driver/m3b-client-summary-cross-tab.txt` | 1439 |
| `scriptdll-quest-driver/m3b-retired-simple-talk-evidence.tsv` | 74697 |
| `scriptdll-quest-driver/m3d-downgraded-quests.tsv` | 3700 |
| `scriptdll-quest-driver/m4-combine-task-shapes.txt` | 480 |
| `scriptdll-quest-driver/m4b-retired-combine-task-evidence.tsv` | 89622 |
| `scriptdll-quest-driver/m5b-collect-probe-fingerprints.tsv` | 941 |
| `scriptdll-quest-driver/m5b2-retired-collect-item-evidence.tsv` | 4605 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-CombineTask.tsv` | 29360 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleCollectItem.tsv` | 35926 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleHunt.tsv` | 261806 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleItemPlay.tsv` | 6125 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleSerialHunt.tsv` | 2406 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleTalk.tsv` | 419063 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-SimpleUseItem.tsv` | 24442 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-vs-sentinel-hunt.tsv` | 86 |
| `scriptdll-quest-driver/m5b2b-client-accept-page-vs-sentinel.tsv` | 24294 |
| `scriptdll-quest-driver/m5b2b-collect-client-action-codes.tsv` | 62663 |
| `scriptdll-quest-driver/m5b2b-event-family-crosstab.tsv` | 2369 |
| `scriptdll-quest-driver/m5b2b-faction-grant-coverage.tsv` | 321 |
| `scriptdll-quest-driver/m5b2b-family-accept-crosstab.tsv` | 1147 |
| `scriptdll-quest-driver/m5b2b-family-ids-CombineTask.tsv` | 2879 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleCollectItem.tsv` | 1484 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleHunt.tsv` | 10700 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleItemPlay.tsv` | 266 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleSerialHunt.tsv` | 104 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleTalk.tsv` | 17758 |
| `scriptdll-quest-driver/m5b2b-family-ids-SimpleUseItem.tsv` | 913 |
| `scriptdll-quest-driver/m5b2b-quest-event-census.tsv` | 3978146 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-CombineTask.tsv` | 40813 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-SimpleCollectItem.tsv` | 15576 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-SimpleItemPlay.tsv` | 2380 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-SimpleSerialHunt.tsv` | 827 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-SimpleTalk.tsv` | 174638 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start-SimpleUseItem.tsv` | 5827 |
| `scriptdll-quest-driver/m5b2b-triplet-vs-sentinel-start.tsv` | 10210 |
| `scriptdll-quest-driver/m5b3-retired-collect-item-evidence.tsv` | 13548 |
| `scriptdll-quest-driver/m5b3-route-axis-breakdown.tsv` | 42450 |
| `scriptdll-quest-driver/m5b3x-client-reward-npcs-evidence.tsv` | 8047 |
| `scriptdll-quest-driver/m5b3x-collect-sentinel-grants.tsv` | 4282 |
| `scriptdll-quest-driver/mixed-chain-client-section-census.tsv` | 2525 |
| `scriptdll-quest-driver/npc_name_index.tsv` | 2833352 |
| `scriptdll-quest-driver/p0c10-family-divergence-table.tsv` | 1705 |
| `scriptdll-quest-driver/p0c10-family-reconciliation.tsv` | 90167 |
| `scriptdll-quest-driver/p0c10-m3d-recheck.tsv` | 4179 |
| `scriptdll-quest-driver/p0c10e-talk-chain-census.tsv` | 36216 |
| `scriptdll-quest-driver/p0c10f-chain-no-routes.tsv` | 139 |
| `scriptdll-quest-driver/p0c10f-talk-chain-probe.java.txt` | 8294 |
| `scriptdll-quest-driver/p0c10h-chain-axis-mismatch.tsv` | 548 |
| `scriptdll-quest-driver/p0c10h-chain-deferred-compound.tsv` | 334 |
| `scriptdll-quest-driver/p0c10h-chain-keep-closure.tsv` | 3677 |
| `scriptdll-quest-driver/p0c10h-chain-probe-round2.tsv` | 42696 |
| `scriptdll-quest-driver/p0c10h-chain-unverified-pages.tsv` | 433 |
| `scriptdll-quest-driver/p0c10i-canonical-gaps.tsv` | 139 |
| `scriptdll-quest-driver/p0c10m-routediff-probe.tsv` | 4133 |
| `scriptdll-quest-driver/p0c10m-talkitem-ids.txt` | 844 |
| `scriptdll-quest-driver/p0c10m-talkitem-probe.tsv` | 11764 |
| `scriptdll-quest-driver/p0c10n-cutscene-probe.tsv` | 11102 |
| `scriptdll-quest-driver/p0c12-17100-canonical.txt` | 12023 |
| `scriptdll-quest-driver/p0c2-simple-talk-reward-npc-probe.tsv` | 2555 |
| `scriptdll-quest-driver/p0c2-simple-talk-sentinel-census.tsv` | 17057 |
| `scriptdll-quest-driver/p0c20-28800-adjudication.tsv` | 1931 |
| `scriptdll-quest-driver/p0c24-1141-readoption.tsv` | 1814 |
| `scriptdll-quest-driver/p0c27-1526-1351-journal-axis-flip.tsv` | 2075 |
| `scriptdll-quest-driver/p0c28-80290-heal-channel-flip.tsv` | 2080 |
| `scriptdll-quest-driver/p0c3-simple-hunt-reward-npc-probe.tsv` | 4763 |
| `scriptdll-quest-driver/p0c3-simple-hunt-sentinel-census.tsv` | 28146 |
| `scriptdll-quest-driver/p0c3-stale-xml-test-refs.tsv` | 1141 |
| `scriptdll-quest-driver/p0c34-chain-reward-row-divergence.tsv` | 375 |
| `scriptdll-quest-driver/p0c34-chain-reward-row-overrides.tsv` | 643 |
| `scriptdll-quest-driver/p0c38-probe.txt` | 37214 |
| `scriptdll-quest-driver/p0c39-expectation-replay.txt` | 16534 |
| `scriptdll-quest-driver/p0c39-probe-after.txt` | 12934 |
| `scriptdll-quest-driver/p0c39-refreeze-25084.tsv` | 2600 |
| `scriptdll-quest-driver/p0c39-start-dialog-routes.txt` | 14310 |
| `scriptdll-quest-driver/p0c4-quest-area-delta.tsv` | 19991 |
| `scriptdll-quest-driver/p0c4-quest-area-missing-quests.tsv` | 401 |
| `scriptdll-quest-driver/p0c4-quest-area-snippets.xml` | 4398 |
| `scriptdll-quest-driver/p0c4-world-questscript-area.tsv` | 55438 |
| `scriptdll-quest-driver/p0c40-t2-attribution.tsv` | 3857 |
| `scriptdll-quest-driver/p0c41-fingerprint-dump.tsv` | 20237 |
| `scriptdll-quest-driver/p0c41-pre-refreeze-fingerprints.tsv` | 20237 |
| `scriptdll-quest-driver/p0c41-sensory-area-scan.tsv` | 3145 |
| `scriptdll-quest-driver/p0c43-24123-registry-post.tsv` | 1604 |
| `scriptdll-quest-driver/p0c43-24123-registry-pre.tsv` | 2402 |
| `scriptdll-quest-driver/p0c43-blast-radius.txt` | 4274 |
| `scriptdll-quest-driver/p0c44-registry-pre.tsv` | 430735 |
| `scriptdll-quest-driver/p0c45-registry-pre.tsv` | 430910 |
| `scriptdll-quest-driver/p0c46-registry-pre.tsv` | 429549 |
| `scriptdll-quest-driver/p0c46-role-axis-census-post.tsv` | 9181 |
| `scriptdll-quest-driver/p0c47-registry-pre.tsv` | 422554 |
| `scriptdll-quest-driver/p0c47-role-axis-census-post.tsv` | 8089 |
| `scriptdll-quest-driver/p0c48-dry-run-talk-collect-chain-pages.tsv` | 8618 |
| `scriptdll-quest-driver/p0c48-pre-regen-talk-collect-chain-pages.tsv` | 8208 |
| `scriptdll-quest-driver/p0c52_dual_side.out.txt` | 76508 |
| `scriptdll-quest-driver/p0c52_kill_axis.out.txt` | 1770 |
| `scriptdll-quest-driver/p0c52_quest_ai_name_groups.out.txt` | 3269 |
| `scriptdll-quest-driver/p0c53-acquire-variant-census.tsv` | 2291 |
| `scriptdll-quest-driver/p0c53-classification-guardonly.tsv` | 44750 |
| `scriptdll-quest-driver/p0c53-deliver-uncovered-probe.txt` | 56004 |
| `scriptdll-quest-driver/p0c53-dialog-axis-orderfix.txt` | 54667 |
| `scriptdll-quest-driver/p0c53-fp-dump-orderfix.tsv` | 91616 |
| `scriptdll-quest-driver/p0c53-fp-dump-post.tsv` | 89838 |
| `scriptdll-quest-driver/p0c53-group-scope-census.tsv` | 77957 |
| `scriptdll-quest-driver/p0c53-groups-guardonly-tmp.tsv` | 3429 |
| `scriptdll-quest-driver/p0c54-audit-pre.txt` | 20236 |
| `scriptdll-quest-driver/p0c54-blast-radius.txt` | 13702 |
| `scriptdll-quest-driver/p0c54-classification-preflip.tsv` | 44348 |
| `scriptdll-quest-driver/p0c54-fp-dump.tsv` | 91768 |
| `scriptdll-quest-driver/p0c54-generator-stage-page-axis.out` | 675 |
| `scriptdll-quest-driver/p0c54-ir-post.txt` | 172546 |
| `scriptdll-quest-driver/p0c54-registry-pre.tsv` | 422519 |
| `scriptdll-quest-driver/p0c55-blast-radius.txt` | 12757 |
| `scriptdll-quest-driver/p0c55-fingerprints-dump.tsv` | 20237 |
| `scriptdll-quest-driver/p0c55-registry-pre.tsv` | 419727 |
| `scriptdll-quest-driver/p0c55-stage-advance-census.tsv` | 2975 |
| `scriptdll-quest-driver/p0c55-stage-leg-census.tsv` | 5227 |
| `scriptdll-quest-driver/p0c56-classification-now.tsv` | 44348 |
| `scriptdll-quest-driver/p0c56-classification-post.tsv` | 43664 |
| `scriptdll-quest-driver/p0c56-fp-post.tsv` | 92680 |
| `scriptdll-quest-driver/p0c56-item-acquire-census.tsv` | 6026 |
| `scriptdll-quest-driver/p0c56-t3-vs-lane-concurrent.out.txt` | 88 |
| `scriptdll-quest-driver/p0c56-t3-vs-lane-samestate.out.txt` | 88 |
| `scriptdll-quest-driver/p0c57-accept-axis-recon.tsv` | 146582 |
| `scriptdll-quest-driver/p0c57-accept-entrance-census.tsv` | 5036 |
| `scriptdll-quest-driver/p0c57-blast-radius.txt` | 1645 |
| `scriptdll-quest-driver/p0c57-fingerprints-dump.tsv` | 20237 |
| `scriptdll-quest-driver/p0c57-registry-pre.tsv` | 419803 |
| `scriptdll-quest-driver/p0c58-classification-post.tsv` | 43303 |
| `scriptdll-quest-driver/p0c6-briefing-audit-probe.txt` | 3567 |
| `scriptdll-quest-driver/p0c6-briefing-gap-closure.tsv` | 1605 |
| `scriptdll-quest-driver/p0c6-hunt-briefing-census.tsv` | 15344 |
| `scriptdll-quest-driver/p0c6-spawn-reachability-census.tsv` | 32578 |
| `scriptdll-quest-driver/p0c8-retention-diff-census.tsv` | 498997 |
| `scriptdll-quest-driver/p0c8_retention_diff_probe.java.txt` | 14065 |
| `scriptdll-quest-driver/p0c8b-dialog-route-buckets.tsv` | 701 |
| `scriptdll-quest-driver/p0c8c-retired-xml-evidence.tsv` | 12514 |
| `scriptdll-quest-driver/p0c8c_gap_shape_probe.java.txt` | 14397 |
| `scriptdll-quest-driver/p0c8c_production_shape_probe.java.txt` | 2933 |
| `scriptdll-quest-driver/p0c9-class-select-census.tsv` | 349378 |
| `scriptdll-quest-driver/p0c9-family-divergence-table.tsv` | 1307 |
| `scriptdll-quest-driver/p0c9-family-reconciliation.tsv` | 38476 |
| `scriptdll-quest-driver/p0c9-keep-recheck.tsv` | 8651 |
| `scriptdll-quest-driver/p0c9-phase53-shape-census.tsv` | 701416 |
| `scriptdll-quest-driver/p0c9_gap_shape_probe.java.txt` | 15014 |
| `scriptdll-quest-driver/p1-itemplay-family-gate-wave2.tsv` | 1460 |
| `scriptdll-quest-driver/p1-itemplay-family-gate.tsv` | 1484 |
| `scriptdll-quest-driver/p3-client-hunt-stages-evidence.tsv` | 4992 |
| `scriptdll-quest-driver/p3-client-sampling-check.tsv` | 1456 |
| `scriptdll-quest-driver/p3-retired-serial-hunt-evidence.tsv` | 1678 |
| `scriptdll-quest-driver/p3b-retired-use-item-evidence.tsv` | 16111 |
| `scriptdll-quest-driver/p3b-useitem-family.txt` | 574 |
| `scriptdll-quest-driver/p5-2-dd-collect-three-axis.tsv` | 188216 |
| `scriptdll-quest-driver/p5-datadriven-decisions.tsv` | 361491 |
| `scriptdll-quest-driver/p5-datadriven-family.txt` | 8982 |
| `scriptdll-quest-driver/p5-datadriven-shape-census.tsv` | 35662 |
| `scriptdll-quest-driver/p5-retired-datadriven-evidence.tsv` | 20667 |
| `scriptdll-quest-driver/p51-hunt-contract-probe.tsv` | 184287 |
| `scriptdll-quest-driver/p52-handin-deferred-quests.tsv` | 11820 |
| `scriptdll-quest-driver/p52b-ok-page-close-census.tsv` | 19969 |
| `scriptdll-quest-driver/p53-step-unsupported-census.tsv` | 41983 |
| `scriptdll-quest-driver/p53-talk-shape-census.tsv` | 11368 |
| `scriptdll-quest-driver/p54-enterarea-pvp-census.tsv` | 28388 |
| `scriptdll-quest-driver/phase5-3-rejections.txt` | 247 |
| `scriptdll-quest-driver/quest-data-vs-xml-metadata-audit.tsv` | 471301 |
| `scriptdll-quest-driver/quest_registry.tsv` | 1615081 |
| `scriptdll-quest-driver/realign-retired-xml-evidence.tsv` | 1077 |
| `scriptdll-quest-driver/registry_helpers.tsv` | 757 |
| `scriptdll-quest-driver/removed-spawned-audit.tsv` | 16088 |
| `scriptdll-quest-driver/retail-combine-task-shapes.tsv` | 32705 |
| `scriptdll-quest-driver/retail-simple-collect-item-diff-lines.tsv` | 927209 |
| `scriptdll-quest-driver/retail-simple-collect-item-name-resolution.tsv` | 15524 |
| `scriptdll-quest-driver/retail-simple-collect-item-shapes.tsv` | 14968 |
| `scriptdll-quest-driver/retail-simple-talk-client-variant.tsv` | 917 |
| `scriptdll-quest-driver/retail-simple-talk-drift.tsv` | 132535 |
| `scriptdll-quest-driver/retail-simple-talk-shapes.tsv` | 713952 |
| `scriptdll-quest-driver/retail-table-coverage.txt` | 835 |
| `scriptdll-quest-driver/retail-xml-retention.tsv` | 559345 |
| `scriptdll-quest-driver/sample_topology_vs_xml.txt` | 4781 |
| `scriptdll-quest-driver/scriptdll64-quest-strings.txt` | 15815 |
| `scriptdll-quest-driver/scriptdll64-quest-xrefs.txt` | 27518 |
| `scriptdll-quest-driver/simple-hunt-reconciliation.txt` | 1991 |
| `scriptdll-quest-driver/simple-talk-reconciliation.tsv` | 111747 |
| `scriptdll-quest-driver/simplehunt-dialog-route-gaps.txt` | 2652 |

### 4.2 `quest-native-dispatch`（131 个）

| 文件 | 字节 |
|---|---:|
| `quest-native-dispatch/cleanup-2026-09-27/DELETED-2026-09-27.tsv` | 18918 |
| `quest-native-dispatch/dd-fp-da.tsv` | 93028 |
| `quest-native-dispatch/dd-fp-da2.tsv` | 93104 |
| `quest-native-dispatch/dd-fp-db.tsv` | 93104 |
| `quest-native-dispatch/gates/T1-baseline-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-m1-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-m2-reds.txt` | 223 |
| `quest-native-dispatch/gates/T1-m2b-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-m3-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-m4-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-n1-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-n3-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-s3cobj-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w5g1-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w5g1b-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w5g2-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w5g3-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w5g4-reds.txt` | 97 |
| `quest-native-dispatch/gates/T1-w6a-reds.txt` | 97 |
| `quest-native-dispatch/gates/T2-chain-baseline-reds.txt` | 6526 |
| `quest-native-dispatch/gates/T2-chain-final-reds.txt` | 6526 |
| `quest-native-dispatch/gates/T2-m4-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-n1-baseline-reds.txt` | 6076 |
| `quest-native-dispatch/gates/T2-n1-reds.txt` | 6076 |
| `quest-native-dispatch/gates/T2-n3-reds.txt` | 6076 |
| `quest-native-dispatch/gates/T2-s3a-added.txt` | 254 |
| `quest-native-dispatch/gates/T2-s3a-final-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-s3a-reds.txt` | 6737 |
| `quest-native-dispatch/gates/T2-s3a-removed.txt` | 1 |
| `quest-native-dispatch/gates/T2-s3c-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-s3cd-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-s3cobj-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-w5g1-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-w5g1b-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-w5g2-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-w5g3-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T2-w6a-reds.txt` | 6483 |
| `quest-native-dispatch/gates/T3-final-reds.txt` | 13009 |
| `quest-native-dispatch/gates/T3-hardening-added.txt` | 1 |
| `quest-native-dispatch/gates/T3-hardening-reds.txt` | 17019 |
| `quest-native-dispatch/gates/T3-hardening-removed.txt` | 1 |
| `quest-native-dispatch/gates/T3-m1-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-m2-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-m3-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-m4-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-n1-baseline-reds.txt` | 12189 |
| `quest-native-dispatch/gates/T3-n1-reds.txt` | 12189 |
| `quest-native-dispatch/gates/T3-n3-reds.txt` | 12189 |
| `quest-native-dispatch/gates/T3-s3a-added.txt` | 1 |
| `quest-native-dispatch/gates/T3-s3a-reds.txt` | 17019 |
| `quest-native-dispatch/gates/T3-s3a-removed.txt` | 1 |
| `quest-native-dispatch/gates/T3-s3b-added.txt` | 1 |
| `quest-native-dispatch/gates/T3-s3b-reds.txt` | 17019 |
| `quest-native-dispatch/gates/T3-s3b-removed.txt` | 1 |
| `quest-native-dispatch/gates/T3-s3c-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-s3cd-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-s3cobj-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-w5g1-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-w5g1b-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-w5g2-reds.txt` | 12596 |
| `quest-native-dispatch/gates/T3-w5g34-reds.txt` | 12714 |
| `quest-native-dispatch/gates/T3-w6a-reds.txt` | 12596 |
| `quest-native-dispatch/gates/n1-T3-invocation-reds.txt` | 16612 |
| `quest-native-dispatch/gates/p1-T1-reds.txt` | 97 |
| `quest-native-dispatch/gates/p1-T3-invocation-reds.txt` | 16797 |
| `quest-native-dispatch/gates/p1-T3-reds-baseline-convention.txt` | 12189 |
| `quest-native-dispatch/gates/p1-T3-reds.txt` | 16797 |
| `quest-native-dispatch/gates/p1-T3-vs-n1-invocation-diff.txt` | 201 |
| `quest-native-dispatch/gates/p2b-T1-reds.txt` | 97 |
| `quest-native-dispatch/gates/p2b-T3-invocation-reds.txt` | 16612 |
| `quest-native-dispatch/gates/p2b-T3-reds-baseline-convention.txt` | 12189 |
| `quest-native-dispatch/gates/p4b-T1-reds.txt` | 97 |
| `quest-native-dispatch/gates/p4b-T3-invocation-reds.txt` | 16612 |
| `quest-native-dispatch/gates/p4b-T3-reds-baseline-convention.txt` | 12189 |
| `quest-native-dispatch/gates/p4b-static-equivalence.txt` | 356 |
| `quest-native-dispatch/gates/p4c-T1-reds.txt` | 97 |
| `quest-native-dispatch/gates/p4c-T3-invocation-reds.txt` | 16612 |
| `quest-native-dispatch/gates/p4c-T3-reds-baseline-convention.txt` | 12189 |
| `quest-native-dispatch/gates/p4d-T1-reds.txt` | 97 |
| `quest-native-dispatch/gates/p4d-T3-invocation-reds.txt` | 16612 |
| `quest-native-dispatch/gates/p4d-T3-reds-baseline-convention.txt` | 12189 |
| `quest-native-dispatch/gates/p4d-static-equivalence.txt` | 2413 |
| `quest-native-dispatch/gates/p4e-T1-reds.txt` | 121 |
| `quest-native-dispatch/gates/p4e-static-equivalence.txt` | 3155 |
| `quest-native-dispatch/phase3-provenance/authority-provenance.tsv` | 7001 |
| `quest-native-dispatch/phase3-provenance/census-dialog-exits-v2.tsv` | 218430 |
| `quest-native-dispatch/phase3-provenance/dialog-exits-removed-tokens-20260928.tsv` | 6279 |
| `quest-native-dispatch/phase3-provenance/dialog-exits.post-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/dialog-exits.post-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-provenance/dialog-exits.pre-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/dialog-exits.pre-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-provenance/p4b.post-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/p4b.post-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-provenance/p4c.post-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/p4c.post-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-provenance/p4d.post-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/p4d.post-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-provenance/p4e-repro-measurements.txt` | 2063 |
| `quest-native-dispatch/phase3-provenance/p4e.post-chain-fp.tsv` | 20237 |
| `quest-native-dispatch/phase3-provenance/p4e.post-dd-fp.tsv` | 93180 |
| `quest-native-dispatch/phase3-recon/census-readers.tsv` | 6719 |
| `quest-native-dispatch/r1-dialog-exits-shrink/census-report.tsv` | 251290 |
| `quest-native-dispatch/r1-dialog-exits-shrink/dialog_exits.pre-shrink.tsv` | 109324 |
| `quest-native-dispatch/r1-dialog-exits-shrink/dialog_exits.shrunk.tsv` | 32862 |
| `quest-native-dispatch/retired-tsv/quest_client_briefing_chains.tsv.retired-20260927` | 69431 |
| `quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.retired-20260928` | 9098 |
| `quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.rows-20260928.after` | 33052 |
| `quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.rows-20260928.before` | 34093 |
| `quest-native-dispatch/retired-tsv/quest_client_entry_pages.tsv.retired-20260927` | 27013 |
| `quest-native-dispatch/retired-tsv/quest_client_handin_exceptions.tsv.retired-20260928` | 427513 |
| `quest-native-dispatch/retired-tsv/quest_client_handin_pages.tsv.retired-20260928` | 12638 |
| `quest-native-dispatch/retired-tsv/quest_client_hunt_progress_rows.tsv.retired-20260928` | 629006 |
| `quest-native-dispatch/retired-tsv/quest_client_hunt_stages.tsv.retired-20260928` | 4992 |
| `quest-native-dispatch/retired-tsv/quest_client_kill_targets.tsv.retired-20260928` | 46209 |
| `quest-native-dispatch/retired-tsv/quest_client_kill_targets_stages.tsv.retired-20260928` | 607 |
| `quest-native-dispatch/retired-tsv/quest_client_report_pages.tsv.retired-20260927` | 67431 |
| `quest-native-dispatch/retired-tsv/quest_client_reward_npcs.tsv.retired-20260928` | 8047 |
| `quest-native-dispatch/retired-tsv/quest_client_summary_rows.tsv.retired-20260928` | 69440 |
| `quest-native-dispatch/retired-tsv/quest_client_talk_chain_pages.tsv.retired-20260928` | 4102 |
| `quest-native-dispatch/retired-tsv/quest_client_talk_chain_steps.tsv.retired-20260928` | 415369 |
| `quest-native-dispatch/retired-tsv/quest_client_talk_collect_chain_pages.tsv.retired-20260928` | 8426 |
| `quest-native-dispatch/retired-tsv/quest_client_talk_pages.tsv.retired-20260927` | 23270 |
| `quest-native-dispatch/retired-tsv/quest_client_use_item_report.tsv.retired-20260928` | 1581 |
| `quest-native-dispatch/retired-tsv/quest_use_item_npcs.tsv.retired-20260928` | 5986 |
| `quest-native-dispatch/retired-tsv/retail-quest-ai-name-groups.tsv.retired-20260928` | 3429 |
| `quest-native-dispatch/s2-movie-migration.tsv` | 994 |
| `quest-native-dispatch/s2-r-record-conflicts.tsv` | 420581 |
| `quest-native-dispatch/s2-t2-triage-data.json` | 71411 |
| `quest-native-dispatch/s3-gamma-rows.tsv` | 116628 |
| `quest-native-dispatch/s3-t2-triage-data.json` | 110795 |
| `quest-native-dispatch/w6tail-earlyelyos/probe-dump-1131-1561-1691.txt` | 9966 |

### 4.3 `other-topic-evidence`（318 个）

| 文件 | 字节 |
|---|---:|
| `ai-kb-index/holdout-cases-2026-09-19.tsv` | 1362 |
| `ai-kb-index/retrieval-eval-2026-09-19-holdout.json` | 18877 |
| `ai-kb-index/retrieval-eval-2026-09-19-keywords.json` | 46668 |
| `ai-kb-index/retrieval-eval-2026-09-19.json` | 46665 |
| `chinese-gm-commands/teleloc_zh.tsv` | 47424 |
| `comment_i18n/sm_progress.json` | 117 |
| `comment_i18n/tail_special_progress.json` | 392 |
| `inggison-somation-rock-z/audit_full_output.txt` | 60446 |
| `item-format-migration/results-20260923-213403.txt` | 2710 |
| `item-format-migration/results-20260923-213415.txt` | 1821 |
| `item-format-migration/results-batch.txt` | 629 |
| `item-format-migration/results-coldstart-full.txt` | 2415 |
| `item-format-migration/results-coldstart.txt` | 394 |
| `item-format-migration/results-dict.txt` | 697 |
| `item-format-migration/results-parallel.txt` | 1690 |
| `item-format-migration/results-restrict-cache.txt` | 1046 |
| `item-format-migration/results-schema-gate-20260924-maintree-conflict.txt` | 118892 |
| `item-format-migration/results-schema-gate-20260924-worktree.txt` | 4152 |
| `item-format-migration/results-verify.txt` | 1296 |
| `item-format-migration/results-xmldataloader-gate-20260924-worktree.txt` | 5027 |
| `non-quest-test-repair/run-head.txt` | 41 |
| `non-quest-test-repair/run-status.txt` | 2009 |
| `quest-10501-handover-continuation/client-finish-page-family.csv` | 59623 |
| `quest-10501-handover-continuation/family-ir-probe-before-fix.txt` | 12124 |
| `quest-10501-handover-continuation/handover-fix-plan.tsv` | 6180 |
| `quest-10501-handover-continuation/quest-10501-audit-rows-after-fix.txt` | 3743 |
| `quest-10522-reward-reentry/external-reward-advance-before-fix.tsv` | 1282 |
| `quest-10522-reward-reentry/external-reward-advance.tsv` | 1282 |
| `quest-10525-testimony-counter/increment-range-findings.csv` | 85034 |
| `quest-10527-reward-row/aligned-var0-samples.tsv` | 2604 |
| `quest-10527-reward-row/audit-output.tsv` | 1592203 |
| `quest-10527-reward-row/audit-stdout.txt` | 9288 |
| `quest-10527-reward-row/batch10-evidence.tsv` | 2020 |
| `quest-10527-reward-row/batch12-evidence.tsv` | 2526 |
| `quest-10527-reward-row/batch13-evidence.tsv` | 4598 |
| `quest-10527-reward-row/batch14-evidence.tsv` | 3509 |
| `quest-10527-reward-row/batch15-evidence.tsv` | 3206 |
| `quest-10527-reward-row/batch16-evidence.tsv` | 1797 |
| `quest-10527-reward-row/batch17-evidence.tsv` | 2310 |
| `quest-10527-reward-row/batch18-evidence.tsv` | 4401 |
| `quest-10527-reward-row/batch19-evidence.tsv` | 2452 |
| `quest-10527-reward-row/batch20-evidence.tsv` | 3705 |
| `quest-10527-reward-row/batch22-evidence.tsv` | 3411 |
| `quest-10527-reward-row/batch23-evidence.tsv` | 3717 |
| `quest-10527-reward-row/batch24-evidence.tsv` | 4353 |
| `quest-10527-reward-row/batch26-evidence.tsv` | 4334 |
| `quest-10527-reward-row/batch31-evidence.tsv` | 2147 |
| `quest-10527-reward-row/batch32-evidence.tsv` | 2047 |
| `quest-10527-reward-row/batch33-evidence.tsv` | 1512 |
| `quest-10527-reward-row/batch34-evidence.tsv` | 1897 |
| `quest-10527-reward-row/batch35-evidence.tsv` | 2131 |
| `quest-10527-reward-row/batch36-evidence.tsv` | 15346 |
| `quest-10527-reward-row/batch37-evidence.tsv` | 4792 |
| `quest-10527-reward-row/batch38-evidence.tsv` | 4123 |
| `quest-10527-reward-row/batch39-evidence.tsv` | 3031 |
| `quest-10527-reward-row/batch40-evidence.tsv` | 3101 |
| `quest-10527-reward-row/batch41-evidence.tsv` | 2929 |
| `quest-10527-reward-row/batch42-evidence.tsv` | 1237 |
| `quest-10527-reward-row/batch43-evidence.tsv` | 1239 |
| `quest-10527-reward-row/batch44-evidence.tsv` | 1558 |
| `quest-10527-reward-row/batch45-evidence.tsv` | 2011 |
| `quest-10527-reward-row/batch46-evidence.tsv` | 1142 |
| `quest-10527-reward-row/batch48-evidence.tsv` | 1175 |
| `quest-10527-reward-row/batch49-evidence.tsv` | 1278 |
| `quest-10527-reward-row/batch50-evidence.tsv` | 2429 |
| `quest-10527-reward-row/batch51-detail.tsv` | 8994 |
| `quest-10527-reward-row/batch51-evidence.tsv` | 547 |
| `quest-10527-reward-row/batch51-legacy.txt` | 13690 |
| `quest-10527-reward-row/batch51-rows.txt` | 13114 |
| `quest-10527-reward-row/batch52-evidence.tsv` | 1103 |
| `quest-10527-reward-row/batch7-evidence.tsv` | 2586 |
| `quest-10527-reward-row/blank-journal-slots.tsv` | 700 |
| `quest-10527-reward-row/legacy-reward-entry-scan.tsv` | 50442 |
| `quest-10527-reward-row/section0-evidence.txt` | 31051 |
| `quest-10527-reward-row/section0-requirements.tsv` | 10194 |
| `quest-1192-step-chain/audit-cross.tsv` | 5656 |
| `quest-1192-step-chain/audit-missing-last-row.tsv` | 16859 |
| `quest-1192-step-chain/audit-output.tsv` | 1593711 |
| `quest-1192-step-chain/audit-qe051-candidates.tsv` | 7831 |
| `quest-1192-step-chain/candidate-sweep.tsv` | 36909 |
| `quest-1192-step-chain/unrouted-progress-actions.tsv` | 9845 |
| `quest-1192/2026-09-14-work-item-migration-defect-inventory.txt` | 4238 |
| `quest-15001-multicounter-step/section0-report-row-closure-candidates.csv` | 166378 |
| `quest-15545-minion-accept-grant/legacy-accept-item-grant-audit.tsv` | 2850 |
| `quest-20528-reward-row/audit-full-run-2026-09-22.txt` | 12298 |
| `quest-20528-reward-row/duplicate-row0-family-run.txt` | 4019 |
| `quest-20528-reward-row/duplicate-row0-family.tsv` | 3601 |
| `quest-30721/audit-enter-zone-start-owner-latest.txt` | 1234 |
| `quest-30721/audit-none-state-entry-latest.txt` | 6465 |
| `quest-30721/audit-retail-talk-entry-latest.txt` | 29333 |
| `quest-30721/audit-unreachable-start-latest.txt` | 1628 |
| `quest-counter-residue/audit-output.tsv` | 32524 |
| `quest-counter-residue/audit-stats.txt` | 34 |
| `quest-e2e/page-not-in-task-html-candidates.csv` | 42652 |
| `quest-kill-contracts/backups-complex-16/10011.xml` | 11603 |
| `quest-kill-contracts/backups-complex-16/10112.xml` | 8898 |
| `quest-kill-contracts/backups-complex-16/13705.xml` | 2859 |
| `quest-kill-contracts/backups-complex-16/13945.xml` | 5042 |
| `quest-kill-contracts/backups-complex-16/15546.xml` | 4602 |
| `quest-kill-contracts/backups-complex-16/17510.xml` | 9212 |
| `quest-kill-contracts/backups-complex-16/18994.xml` | 3698 |
| `quest-kill-contracts/backups-complex-16/20011.xml` | 11584 |
| `quest-kill-contracts/backups-complex-16/20112.xml` | 9964 |
| `quest-kill-contracts/backups-complex-16/25406.xml` | 4960 |
| `quest-kill-contracts/backups-complex-16/25407.xml` | 4957 |
| `quest-kill-contracts/backups-complex-16/25408.xml` | 4971 |
| `quest-kill-contracts/backups-complex-16/25546.xml` | 4606 |
| `quest-kill-contracts/backups-complex-16/25580.xml` | 5238 |
| `quest-kill-contracts/backups-complex-16/27510.xml` | 10402 |
| `quest-kill-contracts/backups-complex-16/28994.xml` | 4459 |
| `quest-kill-contracts/backups/13758.xml` | 5489 |
| `quest-kill-contracts/backups/13759.xml` | 4035 |
| `quest-kill-contracts/backups/13760.xml` | 5939 |
| `quest-kill-contracts/backups/13761.xml` | 4946 |
| `quest-kill-contracts/backups/13762.xml` | 3732 |
| `quest-kill-contracts/backups/13763.xml` | 5930 |
| `quest-kill-contracts/backups/13764.xml` | 5489 |
| `quest-kill-contracts/backups/13765.xml` | 4024 |
| `quest-kill-contracts/backups/13766.xml` | 5931 |
| `quest-kill-contracts/backups/13767.xml` | 4945 |
| `quest-kill-contracts/backups/13768.xml` | 3721 |
| `quest-kill-contracts/backups/13769.xml` | 5930 |
| `quest-kill-contracts/backups/13770.xml` | 4550 |
| `quest-kill-contracts/backups/13840.xml` | 2185 |
| `quest-kill-contracts/backups/13841.xml` | 15493 |
| `quest-kill-contracts/backups/13844.xml` | 2185 |
| `quest-kill-contracts/backups/13845.xml` | 15493 |
| `quest-kill-contracts/backups/13848.xml` | 2172 |
| `quest-kill-contracts/backups/13849.xml` | 14394 |
| `quest-kill-contracts/backups/13860.xml` | 2011 |
| `quest-kill-contracts/backups/13864.xml` | 2010 |
| `quest-kill-contracts/backups/13868.xml` | 2010 |
| `quest-kill-contracts/backups/13880.xml` | 2013 |
| `quest-kill-contracts/backups/13920.xml` | 5733 |
| `quest-kill-contracts/backups/13921.xml` | 2983 |
| `quest-kill-contracts/backups/13922.xml` | 1970 |
| `quest-kill-contracts/backups/13923.xml` | 5732 |
| `quest-kill-contracts/backups/13924.xml` | 2967 |
| `quest-kill-contracts/backups/13925.xml` | 1968 |
| `quest-kill-contracts/backups/13926.xml` | 8507 |
| `quest-kill-contracts/backups/13927.xml` | 2528 |
| `quest-kill-contracts/backups/13928.xml` | 1987 |
| `quest-kill-contracts/backups/13930.xml` | 7127 |
| `quest-kill-contracts/backups/13931.xml` | 2534 |
| `quest-kill-contracts/backups/13932.xml` | 1878 |
| `quest-kill-contracts/backups/13933.xml` | 4356 |
| `quest-kill-contracts/backups/13934.xml` | 2527 |
| `quest-kill-contracts/backups/13935.xml` | 1886 |
| `quest-kill-contracts/backups/13936.xml` | 4347 |
| `quest-kill-contracts/backups/13937.xml` | 2540 |
| `quest-kill-contracts/backups/13938.xml` | 1871 |
| `quest-kill-contracts/backups/13940.xml` | 4636 |
| `quest-kill-contracts/backups/13941.xml` | 2055 |
| `quest-kill-contracts/backups/13942.xml` | 1869 |
| `quest-kill-contracts/backups/13943.xml` | 11299 |
| `quest-kill-contracts/backups/13944.xml` | 2092 |
| `quest-kill-contracts/backups/13946.xml` | 2106 |
| `quest-kill-contracts/backups/13963.xml` | 1944 |
| `quest-kill-contracts/backups/13964.xml` | 1909 |
| `quest-kill-contracts/backups/14203.xml` | 3052 |
| `quest-kill-contracts/backups/14204.xml` | 3052 |
| `quest-kill-contracts/backups/18314.xml` | 3411 |
| `quest-kill-contracts/backups/18951.xml` | 6900 |
| `quest-kill-contracts/backups/18972.xml` | 4404 |
| `quest-kill-contracts/backups/18973.xml` | 4405 |
| `quest-kill-contracts/backups/18974.xml` | 4385 |
| `quest-kill-contracts/backups/19631.xml` | 5037 |
| `quest-kill-contracts/backups/19632.xml` | 4183 |
| `quest-kill-contracts/backups/19633.xml` | 5107 |
| `quest-kill-contracts/backups/19634.xml` | 4156 |
| `quest-kill-contracts/backups/19635.xml` | 4181 |
| `quest-kill-contracts/backups/19636.xml` | 4319 |
| `quest-kill-contracts/backups/19691.xml` | 5633 |
| `quest-kill-contracts/backups/21326.xml` | 1859 |
| `quest-kill-contracts/backups/21327.xml` | 1848 |
| `quest-kill-contracts/backups/23920.xml` | 42223 |
| `quest-kill-contracts/backups/25533.xml` | 269576 |
| `quest-kill-contracts/backups/25640.xml` | 143186 |
| `quest-kill-contracts/backups/25698.xml` | 11208 |
| `quest-kill-contracts/backups/27160.xml` | 3018 |
| `quest-kill-contracts/backups/27161.xml` | 3019 |
| `quest-kill-contracts/backups/28314.xml` | 3587 |
| `quest-kill-contracts/backups/28743.xml` | 5861 |
| `quest-kill-contracts/backups/28932.xml` | 3594 |
| `quest-kill-contracts/backups/28951.xml` | 5822 |
| `quest-kill-contracts/backups/28972.xml` | 3780 |
| `quest-kill-contracts/backups/28973.xml` | 3778 |
| `quest-kill-contracts/backups/28974.xml` | 3781 |
| `quest-kill-contracts/backups/28991.xml` | 2860 |
| `quest-kill-contracts/backups/28993.xml` | 2861 |
| `quest-kill-contracts/backups/28995.xml` | 2857 |
| `quest-kill-contracts/backups/28997.xml` | 2898 |
| `quest-kill-contracts/backups/29631.xml` | 3284 |
| `quest-kill-contracts/backups/29632.xml` | 3372 |
| `quest-kill-contracts/backups/29633.xml` | 3375 |
| `quest-kill-contracts/backups/29634.xml` | 3384 |
| `quest-kill-contracts/backups/29635.xml` | 3376 |
| `quest-kill-contracts/backups/29636.xml` | 4427 |
| `quest-kill-contracts/backups/29637.xml` | 3404 |
| `quest-kill-contracts/backups/29638.xml` | 3495 |
| `quest-kill-contracts/backups/29639.xml` | 3484 |
| `quest-kill-contracts/backups/29640.xml` | 3488 |
| `quest-kill-contracts/backups/29641.xml` | 3497 |
| `quest-kill-contracts/backups/29642.xml` | 4432 |
| `quest-kill-contracts/backups/29691.xml` | 3900 |
| `quest-kill-contracts/backups/30516.xml` | 2437 |
| `quest-kill-contracts/backups/35052.xml` | 5595 |
| `quest-kill-contracts/backups/35058.xml` | 5648 |
| `quest-kill-contracts/backups/35059.xml` | 5388 |
| `quest-kill-contracts/backups/35060.xml` | 5614 |
| `quest-kill-contracts/backups/35061.xml` | 5346 |
| `quest-kill-contracts/backups/35062.xml` | 5690 |
| `quest-kill-contracts/backups/35063.xml` | 5291 |
| `quest-kill-contracts/backups/35064.xml` | 5700 |
| `quest-kill-contracts/backups/35065.xml` | 5009 |
| `quest-kill-contracts/backups/36532.xml` | 6122 |
| `quest-kill-contracts/backups/36533.xml` | 5458 |
| `quest-kill-contracts/backups/36534.xml` | 6109 |
| `quest-kill-contracts/backups/36535.xml` | 6098 |
| `quest-kill-contracts/backups/36536.xml` | 6099 |
| `quest-kill-contracts/backups/42000.xml` | 2717 |
| `quest-kill-contracts/backups/42001.xml` | 2184 |
| `quest-kill-contracts/backups/42002.xml` | 2192 |
| `quest-kill-contracts/backups/42003.xml` | 2318 |
| `quest-kill-contracts/backups/42004.xml` | 2313 |
| `quest-kill-contracts/backups/42005.xml` | 2316 |
| `quest-kill-contracts/backups/42006.xml` | 2310 |
| `quest-kill-contracts/backups/42010.xml` | 2171 |
| `quest-kill-contracts/backups/42011.xml` | 2172 |
| `quest-kill-contracts/backups/42012.xml` | 2179 |
| `quest-kill-contracts/backups/42013.xml` | 2180 |
| `quest-kill-contracts/backups/42100.xml` | 2720 |
| `quest-kill-contracts/backups/42101.xml` | 2189 |
| `quest-kill-contracts/backups/42102.xml` | 2190 |
| `quest-kill-contracts/backups/42103.xml` | 2312 |
| `quest-kill-contracts/backups/42104.xml` | 2350 |
| `quest-kill-contracts/backups/42105.xml` | 2318 |
| `quest-kill-contracts/backups/42106.xml` | 2303 |
| `quest-kill-contracts/backups/42110.xml` | 2175 |
| `quest-kill-contracts/backups/42111.xml` | 2177 |
| `quest-kill-contracts/backups/42112.xml` | 2174 |
| `quest-kill-contracts/backups/42113.xml` | 2184 |
| `quest-kill-contracts/backups/45052.xml` | 6092 |
| `quest-kill-contracts/backups/45058.xml` | 6958 |
| `quest-kill-contracts/backups/45059.xml` | 5503 |
| `quest-kill-contracts/backups/45060.xml` | 6955 |
| `quest-kill-contracts/backups/45061.xml` | 6950 |
| `quest-kill-contracts/backups/45062.xml` | 6130 |
| `quest-kill-contracts/backups/45063.xml` | 6101 |
| `quest-kill-contracts/backups/45064.xml` | 6949 |
| `quest-kill-contracts/backups/45065.xml` | 5511 |
| `quest-kill-contracts/backups/46531.xml` | 5717 |
| `quest-kill-contracts/backups/46533.xml` | 4285 |
| `quest-kill-contracts/backups/46534.xml` | 4283 |
| `quest-kill-contracts/backups/46535.xml` | 5300 |
| `quest-kill-contracts/backups/46536.xml` | 6184 |
| `quest-kill-contracts/backups/46539.xml` | 5092 |
| `quest-kill-contracts/backups/46540.xml` | 6094 |
| `quest-kill-contracts/backups/46541.xml` | 6184 |
| `quest-kill-contracts/backups/46542.xml` | 5524 |
| `quest-kill-contracts/backups/46543.xml` | 6109 |
| `quest-kill-contracts/backups/46544.xml` | 5557 |
| `quest-kill-contracts/backups/46545.xml` | 5721 |
| `quest-kill-contracts/backups/46546.xml` | 6038 |
| `quest-kill-contracts/backups/46547.xml` | 6032 |
| `quest-kill-contracts/backups/46548.xml` | 6020 |
| `quest-kill-contracts/backups/50068.xml` | 2675 |
| `quest-kill-contracts/backups/50069.xml` | 2689 |
| `quest-kill-contracts/backups/50072.xml` | 2635 |
| `quest-kill-contracts/backups/50073.xml` | 4294 |
| `quest-kill-contracts/backups/50074.xml` | 4312 |
| `quest-kill-contracts/backups/50097.xml` | 1948 |
| `quest-kill-contracts/backups/50098.xml` | 1957 |
| `quest-kill-contracts/backups/50104.xml` | 2677 |
| `quest-kill-contracts/backups/50105.xml` | 2691 |
| `quest-kill-contracts/backups/50129.xml` | 3736 |
| `quest-kill-contracts/backups/51076.xml` | 1996 |
| `quest-kill-contracts/backups/51077.xml` | 2063 |
| `quest-kill-contracts/backups/51078.xml` | 2069 |
| `quest-kill-contracts/backups/51097.xml` | 1967 |
| `quest-kill-contracts/backups/51098.xml` | 1979 |
| `quest-kill-contracts/backups/51129.xml` | 3738 |
| `quest-multistep-contract-batch/batch-plan.tsv` | 12598 |
| `quest-refactor-review-2026-09-18/baseline-results.json` | 122875 |
| `quest-refactor-review-2026-09-18/comparison.json` | 3064 |
| `quest-refactor-review-2026-09-18/head-results.json` | 245975 |
| `quest-report-npc-mismatch/report-npc-mismatch.csv` | 4148 |
| `quest-retail-review-fixes/collect-item-fingerprints.tsv` | 13346 |
| `quest-retail-review-fixes/combine-task-fingerprints.tsv` | 43175 |
| `quest-retail-review-fixes/data-driven-fingerprints.tsv` | 89847 |
| `quest-retail-review-fixes/talk-fingerprints.tsv` | 18823 |
| `quest-retail-review-fixes/use-item-fingerprints.tsv` | 7823 |
| `quest-reward-projection-audit/report.tsv` | 64608 |
| `quest-search-audit/quest_npc_name_collisions.csv` | 1378451 |
| `quest-systemic-goal/class-axis-audit.tsv` | 918079 |
| `quest-systemic-goal/prerequisite-axis-audit.tsv` | 38622 |
| `quest-systemic-goal/reward-axis-audit.tsv` | 57567 |
| `quest-systemic-goal/start-metadata-diff.tsv` | 1070235 |
| `quest-systemic-goal/tier-axis-audit.tsv` | 551 |
| `quest/item-producer-scan/item-role-gaps.tsv` | 2011 |
| `quest/item-producer-scan/quest-item-handin-baseline.tsv` | 1515 |
| `retail-template-reconciliation/owner-mismatch.tsv` | 8779 |
| `retail-template-reconciliation/prereq-batch-ledger.tsv` | 2499 |
| `retail-template-reconciliation/prereq-mismatch.tsv` | 2759 |
| `retail-template-reconciliation/prereq-plan.tsv` | 10861 |
| `retail-template-reconciliation/prereq-reachable-audit.tsv` | 1045 |
| `retail-template-reconciliation/prereq-review-v2.tsv` | 3098 |
| `retail-template-reconciliation/prereq-semantic-audit.tsv` | 36207 |
| `retail-template-reconciliation/reconciliation.tsv` | 298761 |
| `retail-template-reconciliation/steps-mismatch.tsv` | 38761 |
| `retail-template-reconciliation/targets-mismatch.tsv` | 10636 |
| `spawn-duplicate-spots/apply_list.json` | 58707 |
| `spawn-duplicate-spots/confirmed.json` | 51225 |
| `spawn-z-audit/path_projection.csv` | 1111 |
| `spawn-z-audit/worlds.csv` | 13063 |
| `startup-perf/xml-parser-probe/results-20260919-215023.txt` | 5191 |
| `startup-perf/xml-parser-probe/results-20260919-215104.txt` | 5084 |
| `startup-perf/xml-parser-probe/results-20260920-0004.txt` | 1508 |

---

## 5. 第三轮（2026-09-30）：docs 生成物与失效内容清理

| 组 | 文件数 | 体积 | 说明 |
|---|---:|---:|---|
| `docs/data/*.txt` | 3 | 20.4 MB | 客户端/服务端 ID→名称一次性转储，全库无消费方 |
| `docs/quest/client-dialog-mapping/quest-order-audit.csv` | 1 | 52.8 MB | 逐路径顺序审计快照，无测试/脚本消费；改为按需运行 `QuestDialogOrderAudit`（产物临时、不入库） |
| `docs/QUEST_CATALOG.zh-CN.md` | 1 | 5.2 MB | 生成式任务目录，1,284 条链接指向已重命名目录且生成链已失效 |
| catalog 生成/刷写脚本 | 4 | 13 KB | 目标文档删除 + 目录重命名双重失效（audit/refresh ×3、doc-links ×1） |
| `.DS_Store` | 5 | 60 KB | OS 垃圾文件（未被 git 跟踪） |
| **合计** | **14** | **~78.4 MB** | |

补充说明：

- 同一轮补齐 `0997233c2`（任务静态资源收拢）的路径对齐尾巴：`static_data/quest_definition/` →
  `static_data/quest/definitions/`，覆盖 `docs/**`、`.agents/rules/**`、`.agents/summary/**` 共 287 个文件、5,740 处引用。
  生产代码本就使用新路径，因此这是文档/脚本侧的收尾。
- memory-bank：`activeContext.md` 中已关闭的焦点（已沉淀 / 已验收 / 已收口 / 已完成改造项）移入
  `archive/2026-09-30-active-context-closed-focus.md`；同步清除失效引用（quest-order-audit 快照、
  已清理的 weather 补丁目录、已删除的 kill-target 契约快照、quest-load-fail 历史路径）。
- 保留边界不变：`.py`/`.sh`/`.java` 工具与门禁、`*-decisions*`/`*-registry*` 登记表、`.md` 报告与验收记录继续保留；
  memory-bank `evidence:` 引用到的产物一律保留，避免破坏 `verify_memory_bank.py` 门禁。

---

## 6. 第四轮（2026-09-30）：client-dialog-mapping 过期审计报告

| 文件 | 体积 | 判定依据 |
|---|---:|---|
| `docs/quest/client-dialog-mapping/client-lifecycle-alignment.csv` | 2.4 MB | **过期**：报告以 `quest_xml_sha256` 为键，3,134 个被审计 XML 中 2,843 个已不存在、164 个内容已变（仅 127 个哈希仍一致）；无测试消费，仅一次性脚本读取，可由 `align_client_quest_dialog_lifecycle.py` 按需重生成 |

同目录其余文件经实测确认**仍在使用，不清理**：

| 文件 | 体积 | 保留原因 |
|---|---:|---|
| `quest-dialog-pages.csv` / `quest-dialog-action-details.csv` | 14.2 / 11.7 MB | `ClientResourceOracle`（33 个测试）与 4 个门禁测试的输入；`QuestMovieContinuationGateTest` 以 SHA-256 钉住它们作为 `client_dialog_contract.tsv` 的来源 |
| `legacy-quest-dialog-contracts.csv` | 2.1 MB | `LegacyQuestEvidenceOracle`、`ReportToManyLegacyFlowRegressionTest` 输入 |
| `legacy-quest-dialog-template-index.csv` | 2.2 MB | 生产 TSV 生成器 `build_quest_client_talk_chain_steps.py` 的输入 |
| `client-monster-progress-contracts.csv` | 494 KB | 2 个门禁测试输入 |
| `client-hyperlinks.csv` / `client-html-pages.csv` | 277 / 178 KB | `ClientResourceOracle` 输入 |
| `page-action-map.csv`、`same-id-map.csv`、`same-symbol-map.csv`、`quest-action-summary.csv`、`parse-errors.csv`、`parse-recoveries.csv` | 1.2 MB | 仍新鲜的查阅表（源为客户端 HTML，不随仓库演进失效）；README 数据字典维护，`page-action-map.csv` 另被 5 份专题记录引用 |

---

## 8. 第六轮（2026-09-30）：客户端映射与无头客户端对拍层迁出

用户裁定"去掉 e2e、门禁、测试等"。执行方式是**先整体搬到仓库外的本地项目，再从本仓库移除**：

- 新项目：`AionEmu-headless`（仓库外，未纳入本仓库 git；31 MB，48 个 Java 文件，含 `README.zh-CN.md` 与 `pom.xml.template`）。
- 从本仓库移除：`docs/quest/client-dialog-mapping/`（15 文件 / 31 MB）+ 48 个 Java 文件
  （22 个 `definition/` 客户端对拍测试、10 个 `e2e/` 无头审计测试、3 个 `definition/` 对拍工具
  `QuestDialogOrderAudit`/`QuestDialogSequenceAudit`/`QuestPrematureRewardRouteAudit`、
  12 个支持类/CLI）。合并上一轮的 `client-lifecycle-alignment.csv`，本目录两轮共移出 64 个文件。
- 本仓库保留：`client_dialog_contract.tsv`、`src/main/resources/quest/quest_client_*.tsv`（运行时冻结表）、
  进程内运行时夹具（`QuestE2eRuntime`、`QuestE2eWorldFixture`、`QuestProtocolLoop`、`QuestE2ePacketValidator`、
  `QuestE2eTransitionMatch`、`QuestE2eStatus`、`VirtualClientState`、`QuestTrace`、`ServerPacketObservation`、
  `ClientActionRequest`、`VirtualClock`）及其不依赖客户端数据的 8 个 runtime/definition 测试。
- 为解耦新增两个中性类型：`ClientActionOutcome`、`ClientActionBridge`（从 `QuestHeadlessClient` 下沉，
  否则 `QuestProtocolLoop`/`QuestPacketOrderRegressionTest` 会依赖已迁出的类）。
- 文档同步：新增 `.agents/summary/headless-client-extraction/MIGRATION.zh-CN.md`（在库证据入口，含全部迁出文件
  SHA-256）；改写 `memory-bank/patterns/quest-engine.md`（12 处证据 + 散文）、`activeContext.md`、
  `docs/QUEST_REPAIR_PLAYBOOK`、`repair-playbook/PATTERNS`、`repair-playbook/CASES`、`docs/README`、
  `docs/quest/NPC_DIALOG_CONTEXT`、`.agents/rules/quest-repair.md`。
- 顺带修复 Playbook 自检的 5 处失效引用：`ORDERED_MULTI_NPC_REPORT_FLOW`、`EQUIPPED_START_CONDITION_RUNTIME`
  重指现存代表测试（`ReportToManySetSucceedAlignmentTest`、`QuestMutationPlannerTest`）；
  `COUNTER_SOURCE_PROJECTION_NO_LOCK`、`SATURATED_COUNTER_EXTRA_KILL_SILENT`、`AUTO_START_KEEPS_NONE_DIALOG_FREE`
  修正到现存方法名（`killEdgesAdvanceOnlyTheFirstUnfinishedStage`、`extraKillsOfASaturatedStage...`、`areaAutoStart...`）。
- 验证（已执行）：`verify_memory_bank.py` 绿（EVIDENCE_REFS=750）、`check_quest_repair_playbook.py` 绿
  （PLAYBOOK_PATTERNS=72）、`git diff --check` 干净；被删类在 `src/` 内零引用。
- 验证（已执行，2026-09-30 用户授权）：`mvn -q test` 编译通过；全量 3965 个测试为 187 failures / 90 errors，
  失败集中在并发车道的真端迁移（退役/缺失 XML、catalog 校验、retail 编译器改形）。本提交触及的测试类中只有 3 个
  既有红（`QuestCounterProjectionLockFollowUpTest#quest27510…`、`QuestCounterSourceProjectionProductionFlowTest
  #quests30603And30613…`、`QuestResidualCounterLocksTest#quest28504…`），根因是并发车道改形/删 XML 后未重钉；
  保留夹具的 8 个运行时测试类共 24 个用例，21 绿。残余说明性引用两处
  （`RetailDataDrivenGateTest` 断言消息 2 行、`Batch44FoamWispFiveRowContractTest` 注释 1 行，非编译依赖），
  其中前者所在文件正被并发车道修改，未改动。

---

## 9. 第七轮（2026-10-03）：门禁产物、零引用旧主题与缓存清理

> 保留口径（本轮起生效）：**只记录重要的过程内容，不记录门禁**——迁移计划、批次报告、裁定登记表与工具保留；
> 门禁运行产物（`*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）不再入库、不再保留。

| 组 | 内容 | 体积 | 说明 |
|---|---|---:|---|
| 门禁运行产物 | `quest-engine-native/gates/*.log`（87 个）+ `gates/*.tsv`（44 个）+ `p4/*-red-class-delta.tsv`（3 个） | ≈62 MB | `gates/` 仅保留聚焦基线与两份红线归因 md（重要过程内容）；33 份报告/README/迁移计划中的对应引用已就地加注「门禁产物清理提示（2026-10-03）」 |
| 零引用旧主题 | 43 个已完结任务主题与报告（2026-09-09 ~ 09-30） | ≈0.4 MB | 判据 = 全库零引用（`.agents/summary/…` 前缀 + `主题名/` 相对路径两种形式，覆盖 memory-bank / rules / ledger / docs / scripts / src / 全部 summary）；今日在办的 `quest-owner-derived`、`ai-registration-gate` 保留 |
| 缓存/垃圾 | `.DS_Store` ×3、`__pycache__` ×5 | — | 均已被 `.gitignore` 忽略 |

### 9.1 零引用旧主题清单（43 项）

ai-support-guard-npe、archives-of-eternity-door、charge-skill-damage、comment-quality-fix-2026-09-09.md、confuse-platform-edge、deadcode、heiron-undead-farm-day-night、idea-inspection-batch-fix、inggison-live-world-210050000、instance-reset、knownlist-move-notify、lombok-refactor-2026-09-09.md、maven4-migration、npc-immobile-retaliation、npc-soul-absorption-857783、npc-sp-chase-disengage、player-enter-world-event-quest-npe、quest-10100、quest-11323-reward-window、quest-130-audit-and-retirement、quest-15301-daevanion-flow、quest-15400-kill-counter、quest-16802-realtime-reward、quest-18504-wide-counter、quest-1916-accept-dispatch-verteron、quest-19637-kill-counter、quest-2947、quest-3934-reward-preview-ambiguity、quest-arena-reward-entrance、quest-catalog-audit、quest-conquest-offering-reachability、quest-contract-failure-convergence、quest-dialog-migration、quest-dialog-root-guard、quest-e2e-provider-cache、quest-runtime-resource-retirement、quest-snapshot-perf、quest-start-condition-retail-semantics、quest-trace-log、quest-wiki、resha-phantom-aggro、taloc-hollow-drop-index、tower-of-eternity-entrance-uptime

### 9.2 恢复路径

- tracked 文件（47 个门禁 tsv + 43 项旧主题）均可从 git 历史取回：`git show <清理提交>^:<path>`；
- 未跟踪的 gate 日志由重跑对应聚焦套件再生成（关键读数已内联于各批次报告与 README，无需回读日志）；
- 保留边界（经 §9.3 修订）：`.py`/`.sh`/`.java` 工具与门禁脚本、`*-decisions*`/`*-registry*` 登记表、验收记录、
  memory-bank `evidence:` 引用到的产物一律保留；零引用已完结报告按 §9.3 的逐文件判据处置。

### 9.3 第二刀（同日）：逐文件零引用清理（72 个 / ≈530KB）

判据（§9 同口径降到文件粒度）：**逐文件全库零引用**（同主题文件 + memory-bank / rules / ledger / docs /
scripts / src-test / pom 的引用扫描）且任务已完结。

| 组 | 文件数 | 说明 |
|---|---:|---|
| 派生物 / 门禁输出 | 5 | `talk-classification.tsv`、`itemplay-classification.tsv`（族门禁逐行结果）、`dd-classification-before/after.tsv`（漂移登记快照）、`batch52-residual.txt`（批次 52 残留清单） |
| 零引用已完结报告 / 记录 | 67 | 分布 24 个主题：`quest-native-dispatch`×16、`quest`×15、`quest-e2e`×6、`quest-kill-contracts`×4、`startup-perf`×3、`ai-kb-index`×3，其余 18 个主题共 20 个（spawn-*、retail-template-reconciliation、quest-refactor-review 等；明细见 git 历史） |

**保护边界（本轮明确）**：

- `quest-acceptance/*.md` 零引用验收记录 33 个**保留**——验收记录是 quest-repair 规则约定的权威存档，
  按约定独立存在，不以被引用为条件；
- `.py`/`.sh`/`.java`/`.jsh` 工具与探针（零引用 73 个）**保留**——按既有保留边界；
- 当日（2026-10-03）新产出文件**保留**（在办），如 `quest-native-dispatch/2026-10-03-*`。
