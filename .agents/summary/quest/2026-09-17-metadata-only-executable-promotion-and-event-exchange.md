# 2026-09-17 METADATA_ONLY 任务可执行工作流补齐与事件兑换漏洞根治总结

## 1. 背景与目标
在解决大德巴全系与制作名人交付契约后，针对 `item-role-gaps.tsv` 剩余的深水区任务展开进一步根治：
1. **METADATA_ONLY 任务生成完整可执行链并提升为 EXECUTABLE**：
   - `4338`（项链战争）：4.8/5.8 取代旧 2033 使命的 7 步任务，此前仅有 metadata 导致被审计为无交付边。基于真端 `quest_q4338.html` 完整构建从百夫长乌拉康(204391)接取、甘达尔彭(790020)测试收集(182215325 x3)、获取项链(182215326)、见金喜(204393)、姆克尔向导(204394..204398)、熔岩区销毁项链(use-item)到最终汇报领奖的完整可执行链。
   - `25082`（沙漠中的不法分子）：此前仅有 metadata，基于 `quest_q25082.html` 构建从克拉西亚(804923)接取、搜集切尔路斯肉(182215728)放入诱饵篝火(731559)、引出并消灭首领粗鳞库拉图(230492)、向沃格利(804924)汇报领奖的完整可执行链。同时修正真端怪物掉落契约，覆盖 219758 与 219759 两只目标怪。
   - `25608`（[组队] 萨波拉蜂蜜）：此前仅有 metadata，基于 `quest_q25608.html` 构建从宾戴尔(805964)接取、蒙德西斯(806177)探听、养蜂人(806197)委托消灭 10 只库库勒工人(241235)、击杀首领酷乐比(241234)获取精炼蜜袋(182216007)、交回宾戴尔领奖的完整可执行工作流。
2. **事件兑换空条件免费白嫖漏洞封堵**：
   - `51023`（[活动] 秘密指令线索）：此前 `<conditions></conditions>` 与 `<actions></actions>` 全空，且存在无条件 `SET_SUCCEED` 旁路。对齐天族镜像 `50023`，建立档位 1（1 枚线索换小盒 188051781）与档位 2（3 枚线索换大盒 188051783）的严格校验与扣除。
   - `80833`（手气如何）：此前直接进奖励不扣道具；补齐对活动扑克加成道具（182007397）的校验与扣除。

---

## 2. 治理成果
1. **生产编目 EXECUTABLE 规模提升**：
   - `quest_definition_catalog.xml` 中将 `4338`, `25082`, `25608` 由 `METADATA_ONLY` 提升为 `EXECUTABLE`。
   - 生产可执行任务编译总数由 `6186` 稳步提升至 **`6189`**。
2. **审计差距进一步收敛**：
   - `item-role-gaps.tsv` 从 30 行减少至 **25 行**。
   - `OWN_COLLECT_NO_TURNIN_GATE_MULTI_EDGE` 从 5 行收敛至仅剩 2 个纯活动任务（80816, 80829）。
   - `item-handin-route-gaps.tsv` 保持为 **0 违规**。
3. **门禁与测试全绿**：
   - `mvn test -Dtest=QuestItemSourceContractGateTest,QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest` 全量通过（**34/34 passed, 0 failures, 0 errors**）。
