# 全量测试已知问题清单（2026-09-12，3086 tests）

环境性（JDK 25 反射/Objenesis mock 限制，与本轮改动无关，起点同样失败）：

合计 0 个环境性失败。

断言漂移/待甄别（起点未跑过全量的存量 + 本会话批次可能引入）：
- LoginTaskShutdownHandlerTest#restartTaskUsesShutdownProviderOutsideBootEmbeddedMode
- LoginTaskShutdownHandlerTest#shutdownTaskUsesShutdownProviderOutsideBootEmbeddedMode
- PlayerLimitServiceTest#updateSellLimitDebitsAccountLimitAtomically

合计 3 个。已修复：CollectTurnInClientActionAlignmentBatchTest（18745 确认页顺序与断言）。

甄别方法：对每个失败类，git diff ee0363c10..HEAD 检查其断言的任务 XML 是否被本会话改动；
未改动者为既有漂移（起点从未跑过全量测试），改动了者按客户端证据更新断言或修正 XML。

确认的既有漂移样本：Quest2841RetailAlignmentTest（sync 模式断言与起点 XML 不符）、
Quest3057RetailFlowAlignmentTest（QUEST_SELECT target 断言与 NPC_START 生成语义矛盾，断言起点已存在）、
QuestStartItemDefinitionRegressionTest#startItemNpcs（1582 XML 未被改动）、
QuestAdditionalCapabilityDefinitionTest#lowersSelectableReward（inline XML 缺 COMPLETION sync，旧提交引入）。
