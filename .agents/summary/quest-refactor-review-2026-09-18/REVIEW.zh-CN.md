# 2026-09-18 任务改造反向回归与门禁审计

状态：AUDITED / 修复及客户端验收未完成。此次为审计，不修改生产代码、测试断言或原验收台账，不提交。

## 固定范围与结果

- 时间范围：2026-09-17 00:00（Asia/Shanghai）起。
- 基线：e483c7ef3b95adafea745bba055ce89a4b19b813（2026-09-16 23:44）。
- 审计提交：50055337b7bec97e1a3347cfbc398cd3665f422d。工作区存在其他会话持续修改，以下不宣称覆盖该提交之后的未提交改动。
- 主工作树首次 Maven 在 testCompile 因缺失类文件失败；源码存在，不能归因为业务回归。已用临时隔离检出消除共享 target 干扰，未改动用户服务进程。

| 范围 | 测试数 | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| 审计提交全量 |3413|77|22|2|
| 审计提交 questEngine |1421|76|20|1|
| 改造前 questEngine |1379|60|16|1|

按测试类+方法/动态用例名比较：20 项新增失败，76 项原失败仍存在，未发现原失败消失。新增失败不等同20个玩法bug，其中有陈旧形状断言，需要分别归因。

同一轮生产目录输出：PRODUCTION_COMPILE_OK=6189；PRODUCTION_COMPILE_FAILURES=0；PRODUCTION_INTERACTION_OBJECT_FAILURES=0；PRODUCTION_WHITELIST_VIOLATIONS=0。编译/白名单通过不能证明可完成。

## 实际玩法与合同问题

以下代码定位固定到审计提交；明确区分静态合同确证与实际执行测试。未执行真实客户端验收。

|优先级|任务/位置|触发和结果|归因/证据|
|---|---|---|---|
|P1|13945.xml:78、18994.xml:35、28994.xml:32|started 固定 var1=0，首杀递增为1后所有后续 started 事件失配；源投影匹配见 QuestMutationPlanner.java:325|c2e1d7d2a 新增回归。客户端和旧handler要求连续2/3/3杀；静态确证|
|P1|15324.xml:102、132|第二段正确击杀233929、第三段234277不增长，当前配置了别组怪|05f4e18a8 新增回归。旧handler 911440146 的 _15324Unyielding_Spirit.java及客户端quest_monster.csv:4110印证；静态确证|
|P1|1917.xml:88|纳姆斯203075的1352→1353→1354→10000链被删1354路由，正常按钮链不能到talked_namus|3f4e507fc新增回归；QuestClientContractGateTest实测新增 BUTTON_WITHOUT_ROUTE 1917/203075/1353→1354|
|P1|1535.xml:46、1917.xml:38、1687.xml:50；2303/2332同型|旧版本已存 REWARD,var0=0，新领奖节点只接1/2/3等，没有迁移恢复边，旧待领奖角色无法命中|P5新增存档兼容回归；父版本投影与新source精确匹配静态确证，需持久化快照重登测试|
|P1|2677.xml:142、269|新增三组元数据，执行仍单窗口十二选项，没有手套/肩甲/鞋SETPRO10/20/30路线，客户端各档四选一合同不匹配|46a1598d1未完成执行重构；死档门禁实测tiers1/2未被授予|
|P1|21467.xml:156|第一次/第二次超时后杀女王仍转REWARD:0并给6币，应该分别3/2币|新增分组但未修既有路由错误；旧handler与客户端合同确证；死档门禁实测|
|P2|11467.xml:398；audit_tier_axis.py:82|reward3展示page8、内联实际给1币，但完成档记0且缺第四组；生成器只读[123]漏第四档|不能把门禁报tier0解读成实际给6币；窗口门禁实测失败，静态审计口径遗漏|
|P2|14026.xml:265、24026.xml:277|六个物品选项将位置2..7写成完成奖励档，元数据只有0/1；显式GrantReward仍发装备和称号|多组改造暴露既有索引混用，title/dead-group实测失败；不可为过测试删称号，也不可声称装备未发|
|P2|24155.xml:98、110|SETPRO2将SECTION_0置1，使三杀只剩两杀；旧handler对话清SECTION_5并将计数SECTION_0置0|f6d1678e8新增回归；客户端quest_monster.csv:6043、旧handler静态确证|
|P2|80798.xml:81、84|客户端收5个182215809即可交付，新增边沿用错误8个并扣8个|257ca01f9扩散旧数量错误；客户端quest.xml:253280合同确证|
|P2|26930.xml:90、98|START持有20个186000257交付要求10个，但count=ALL清空20个|257ca01f9扩散旧扣量错误；旧handler、客户端quest.xml:179274、镜像16930精确扣10印证|

客户端证据由只读审计读取 /Users/mc/PycharmProjects/unpak/Quest_unpacked/，不将历史handler直接当生产owner。14016/24016第二档无入口已确定，但旧handler也只用第一档，不能无依据创造领取第二档的剧情条件。

## 错误门禁、漏检与执行纪律

1. **明确误报**：QuestPrematureRewardRouteExclusionTest.java:35仅按started→reward和按钮名禁止路线，不检查计数条件。19631.xml:94有var1>=10合法恢复边仍报“提前领奖”。本类10项失败在改造前已经存在；应该测试未满不通、满数可交，不应删合法路线。
2. **交付边假绿**：QuestItemSourceContractGateTest.java:75按source@npc求所有路由HasItem条件并集；不同成功/失败分支可相互遮掩，且没有锁数量与实际扣除。应按每条成功交付边检查。
3. **掉落身份丢失**：QuestDropContractGateTest.java:56只检查物品种类数、概率桶行数，不检查npc/item关系；多个物品同怪也重复计“怪物数”。无法保证正确来源仍存在。
4. **图门禁过弱**：QuestDefinitionDirectoryLoaderTest.java:63有出边不等于可达COMPLETE，A↔B封闭循环仍可绿；QuestMovieAndDialogLoopRegressionTest.java:233只要任何边引用某档就计覆盖，未要求该源实际可达。
5. **奖励基线漏行**：build_item_selectable_contract_tsv.py:121解析ext前跳过没有普通道具或不可映射的整行，80899扩展奖励未入TSV；不是extended全量。
6. **陈旧豁免**：QuestRewardItemGateTest.java:71、302仍保留18606/50029/51029 EXT_FLATTENED和1687/2677 MULTI_TIER_FLATTENED，台账却写已重构/已移除。完成改造后缺少退役豁免校验。
7. **门禁已红仍称销项**：GOAL_PROGRESS.zh-CN.md:220-237声称多档100%完成、未解释缺陷0；同一HEAD的架构记录:59-62已记3401例70F21E，多数任务域。此次固定提交的全量以及核心门禁直接红，不能沿用较早12门禁50例结果为最终提交背书。
8. pom.xml只有正常Surefire，未发现通过排除测试配置绕过。证据支持未执行/未闭环全部相关测试，不支持主观声称故意绕门禁。

## 修复顺序和可用性验收

1. 先修阻断：首杀计数、错误怪物、1917按钮、旧存档领奖兼容。每类用共享行为回归保护同族，不批量猜测替换。
2. 再修多档：预览页面、实际发奖、complete reward index、客户端选择编号必须分别对齐；包括计时任务全部边界。不要只增加metadata组。
3. 修门禁误报与漏检：正/反案例验证规则本身；与客户端/旧handler独立基线对照，不以当前XML反推正确答案。豁免必须逐条证据和退出条件。
4. 把全部96个quest失败/错误逐项标记生产回归、测试陈旧、环境/fixture、未决；不能笼统称历史失败放行。
5. 分支行为测试覆盖未满/刚满/超额物品、连续击杀、所有结局/计时档、旧存档重登、页面和副作用顺序，之后跑完整quest及全量。固定最终提交记录结果；原台账在实际修复通过前应保持PENDING。
6. 客户端逐任务验收。编译、白名单和静态对齐均不能替代实际任务可用性。

## 可复核命令与产物

- 当前全量（隔离50055337b）：`mvn -B -Dstyle.color=never test`。
- 改造前任务域（隔离e483c7ef3）：`mvn -B -Dstyle.color=never '-Dtest=com.aionemu.gameserver.questEngine.**.*Test' test`。
- [主工作树编译失败日志](mvn-test.log)、[隔离全量原始日志](mvn-isolated-test.log)、[基线任务域日志](mvn-baseline-quest-test.log)。
- [当前逐测试结果](head-results.json)、[基线逐测试结果](baseline-results.json)、[用例级差分](comparison.json)。上述保留为本审计证据，不作为代码或永久Memory Bank模式。
- 两个临时worktree已remove --force并prune；没有服务生命周期操作、没有本地commit/push。

## 20项新增失败

- definition.AcceptAndConfirmationEntryContractTest#checkHandOverShowsTheClientConfirmationPageThenClosesOrClaims
- definition.JavaHandlerFamilyDefinitionTest#nymphsGownStartsByUsingTheDiaryAndBranchesIntoTwoRewardPaths
- definition.LegacyTemplateMirrorRouteRegressionTest#handlerProvenFollowUpDialogsUseTheirPageTargetState
- definition.LegacyTemplateMirrorRouteRegressionTest#legacyTemplateCloseControlsDoNotChangeQuestState
- definition.LegacyTemplateMirrorRouteRegressionTest#quest2920RewardChoicesUseTheHandlerProvenRewardWindows
- definition.Quest14112LogoutPersistenceTest#keepsPoisonousBubblegutKillProgressAcrossLogout
- definition.Quest1466ClientDialogAlignmentTest#preservesTheRetailItemPlayRouteAndSoleRewardOwner
- definition.Quest24026RetailAlignmentTest#preservesMetadataPrerequisitesAndRewards
- definition.QuestClientContractGateTest#productionQuestDialogsDoNotIntroduceFatalClientContractRegressions
- definition.QuestMovieAndDialogLoopRegressionTest#multiTierQuestsNeverDeclareDeadRewardGroups
- definition.QuestMovieAndDialogLoopRegressionTest#multiTierHandInWindowsMatchTheirGrantedTier
- definition.QuestTitleRewardCoverageTest#catalogMatchesEveryKnownServerTitleQuest
- definition.QuestTitleRewardCoverageTest#everyExecutableCompletionGrantsExactlyItsConfiguredRegularTitles
- definition.SilenteraSpawnedDialogFamilyTest#statueSpawnedNpcChainsMatchTheLegacyContracts
- runtime.PlayerQuestStartEligibilityPortTest#quest10521RejectsLevelTenAndRequiresQuest10520AtLevelSixtyFive
- runtime.QuestDispatchToVerteronFamilyProductionFlowTest#preservesRetailMetadataForTechnistDispatchBranches
- runtime.QuestLegacyMonsterHuntProductionFlowTest#independentGroupedCountedMonsterHuntsAcceptEitherRetailKillOrder()[1]
- runtime.QuestLegacyMonsterHuntProductionFlowTest#independentCountedMonsterHuntsAcceptEitherRetailKillOrder()[1]
- runtime.QuestLegacyMonsterHuntProductionFlowTest#independentCountedMonsterHuntsAcceptEitherRetailKillOrder()[2]
- runtime.QuestLegacyMonsterHuntProductionFlowTest#independentCountedMonsterHuntsAcceptEitherRetailKillOrder()[3]

## 既有 Journey NO_MATCH 的限制

13708 等首步NO_MATCH在基线即存在，不能归因本轮metadata。fixture真实Player和虚拟客户端固定65级（QuestE2eWorldFixture.java:406、QuestE2eRuntime.java:320），13708两版本均min65且无职业限制。Planner.java:512只从transition选择职业、缺省GLADIATOR，Runtime.java:684直接注入StartEligibility.allowed()，构成真实资格验证覆盖缺口，但尚未证明是NO_MATCH根因；不据此修改生产接取规则。
