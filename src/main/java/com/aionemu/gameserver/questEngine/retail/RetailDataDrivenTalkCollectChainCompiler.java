package com.aionemu.gameserver.questEngine.retail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.ProgressScope;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * talk/collectitem 交错行的混合链合成器（混合长尾切片 2）：单 {@code var0} 6 位阶梯，每个步骤推进
 * 一行（talk 步 = 信件页梯推进、collectitem 步 = 39 检查整组过/扣），与旧 XML（15301/16942 形）和
 * {@link RetailDataDrivenTalkCompiler} buildChain 同构。collect 段的客户端证据（段首页、39 按钮、
 * check_user_item_ok/fail 结果页）来自 {@link RetailClientTalkCollectChainPages} 登记表，其段类别
 * 序列与真端表 stepCategories 逐位一致。
 * <p>
 * 末步推进进领奖：talk 末步 = SET_SUCCEED 授予任务凭证（questWorkItems，15301 形）；collect 末步 =
 * 39/1009 检查对（16942 形）。领奖投影 = 客户端任务书末行（QE-051）；恰一条进入世界自愈边。
 * <p>
 * Mixed-chain synthesis for talk/collectitem rows (mixed-tail slice 2): a single 6-bit {@code var0}
 * ladder advancing one row per step — a talk step advances via its letter-page ladder, a collectitem
 * step via the group has-item/remove-item check (action 39) — isomorphic to the legacy XMLs (15301,
 * 16942) and to {@link RetailDataDrivenTalkCompiler} buildChain. Each collect stage's client evidence
 * (head page, check button, result pages) comes from {@link RetailClientTalkCollectChainPages}, whose
 * stage kinds mirror the retail table's step categories position for position. The final advance
 * reaches reward: a final talk step grants the quest work items via SET_SUCCEED (15301 shape), a
 * final collect step carries the 39/1009 check pairs (16942 shape). Reward projection = the last
 * client journal row (QE-051); exactly one enter-world heal edge.
 */
public final class RetailDataDrivenTalkCollectChainCompiler {

	private RetailDataDrivenTalkCollectChainCompiler() {
	}

	/** 编译结果：定义 + 稳定拒绝码（与家族编译器同形）。 / Outcome: definition plus a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 展开后的一个步骤：npc 名 + 段类别；骑行者（enterarea/enterworld/itemplay）无 npc、占行不占对话段。
	 * One expanded step: the npc name plus the stage kind; riders (enterarea/enterworld/itemplay) carry
	 * no npc and occupy a row without a dialog stage.
	 */
	record Step(String npc, boolean collect, String enterArea, Integer enterWorld,
			RetailDataDrivenTable.ItemPlayTarget itemPlay) {

		static Step talk(String npc) {
			return new Step(npc, false, null, null, null);
		}

		static Step collect(String npc) {
			return new Step(npc, true, null, null, null);
		}

		static Step enterArea(String zoneName) {
			return new Step(null, false, zoneName, null, null);
		}

		static Step enterWorld(int worldId) {
			return new Step(null, false, null, worldId, null);
		}

		static Step itemPlay(RetailDataDrivenTable.ItemPlayTarget target) {
			return new Step(null, false, null, null, target);
		}

		boolean isItemPlay() {
			return itemPlay != null;
		}

		boolean rider() {
			return enterArea != null || enterWorld != null || itemPlay != null;
		}
	}

	/**
	 * 把真端行展开为步骤序（talk 名 + collect 名按 table 顺序交错，骑行者按位插入）；含其他类别返回空表。
	 * Expands the retail row into ordered steps (talk and collect names interleaved in table order,
	 * riders inserted in place). Empty for rows carrying other step categories.
	 */
	static List<Step> expandSteps(RetailDataDrivenTable.Entry entry) {
		for (String category : entry.stepCategories()) {
			if (!category.equals("talk") && !category.equals("collectitem")
					&& !category.equals("enterarea") && !category.equals("enterworld")
					&& !category.equals("itemplay")) {
				return List.of();
			}
		}
		List<Step> steps = new ArrayList<>();
		int talkIndex = 0;
		int collectIndex = 0;
		int enterAreaIndex = 0;
		int enterWorldIndex = 0;
		int itemPlayIndex = 0;
		for (String category : entry.stepCategories()) {
			if (category.equals("talk")) {
				if (talkIndex < entry.talkSteps().size()) {
					steps.add(Step.talk(entry.talkSteps().get(talkIndex++)));
				}
			} else if (category.equals("collectitem")) {
				if (collectIndex < entry.collectSteps().size()) {
					steps.add(Step.collect(entry.collectSteps().get(collectIndex++)));
				}
			} else if (category.equals("enterarea")) {
				if (enterAreaIndex < entry.enterAreaSteps().size()) {
					steps.add(Step.enterArea(entry.enterAreaSteps().get(enterAreaIndex++)));
				}
			} else if (category.equals("enterworld")) {
				if (enterWorldIndex < entry.enterWorldSteps().size()) {
					steps.add(Step.enterWorld(entry.enterWorldSteps().get(enterWorldIndex++)));
				}
			} else if (itemPlayIndex < entry.itemPlaySteps().size()) {
				steps.add(Step.itemPlay(entry.itemPlaySteps().get(itemPlayIndex++)));
			}
		}
		return steps;
	}

	/**
	 * 编译 talk/collectitem 交错链（Talk 接取、客户端混合链页登记覆盖全部步骤）。
	 * Compiles the talk/collectitem interleave chain (Talk acquire, the client mixed-chain registry
	 * covering every step).
	 */
	public static Outcome compile(RetailDataDrivenTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailQuestMetadataCompiler.Outcome metadata, int acquiredNpc, int rewardNpc,
			RetailClientTalkCollectChainPages chainPages, RetailClientSummaryRows summaryRows,
			RetailEnterAreaZoneResolution enterAreaZones,
			RetailItemNameIndex itemIndex, boolean selectNoneLadder) {
		Objects.requireNonNull(entry, "entry");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		// talkfobj 骑行者在窄链（talk/collect）里尚无动作实现（宽链编译器的 FOBJ 动作未抽公共件）——
		// 如实拒绝并给出具体骑行者名，而不是笼统的 "no steps"。
		// The talkfobj rider has no action implementation in the narrow chain yet (the broad compiler's
		// FOBJ action is not factored out) — reject honestly with the rider name instead of a blanket
		// "no steps".
		for (String category : entry.stepCategories()) {
			if (category.equals("talkfobj")) {
				return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED",
					"rider unsupported: " + category);
			}
		}
		List<Step> steps = expandSteps(entry);
		if (steps.isEmpty()) {
			return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED", "no steps");
		}
		// enterarea 别名先解析成 zones XML 登记名（未登记 → 稳定拒绝码，与宽链编译器同判据）。
		// Enterarea aliases resolve to registered zone names first (unregistered rejects with the same
		// stable code as the broad chain compiler).
		List<Step> resolved = new ArrayList<>(steps.size());
		for (Step step : steps) {
			if (step.enterArea() == null) {
				resolved.add(step);
				continue;
			}
			String zone = enterAreaZones.zoneName(entry.questId(), step.enterArea());
			if (zone == null) {
				return new Outcome(null, "RETAIL_ENTERAREA_ZONE_UNRESOLVED", step.enterArea());
			}
			resolved.add(Step.enterArea(zone));
		}
		steps = List.copyOf(resolved);
		// itemplay 骑行者的道具符号先解析成 item id（与宽链编译器同判据）：符号未登记、work item
		// 数量错配、输出物未登记都如实拒绝；多播形需要独立的计数段，单阶梯布局里没有落点，按稳定码
		// 留在 XML。
		// The itemplay rider's item symbol resolves to an item id first (the same rules as the broad
		// chain compiler): an unregistered symbol, a work-item count mismatch and an unregistered
		// output item are rejected honestly; the multi-play shape needs its own counter section and has
		// no home in the single-ladder layout, so it stays on XML with a stable code.
		if (steps.stream().anyMatch(Step::isItemPlay)) {
			Map<Integer, Integer> workItemCounts = new java.util.LinkedHashMap<>();
			for (QuestItemRequirement workItem : metadata.metadata().questWorkItems()) {
				workItemCounts.merge(workItem.itemId(), workItem.count(), Integer::sum);
			}
			List<Step> resolvedPlays = new ArrayList<>(steps.size());
			for (Step step : steps) {
				if (!step.isItemPlay()) {
					resolvedPlays.add(step);
					continue;
				}
				String symbol = step.itemPlay().symbol();
				Integer itemId = itemIndex.resolve(symbol);
				if (itemId == null) {
					return new Outcome(null, "RETAIL_ITEMPLAY_ITEM_UNRESOLVED", symbol);
				}
				int playCount = step.itemPlay().count();
				if (playCount > 1) {
					return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED",
						"rider unsupported: itemplay multiplay " + symbol + " x" + playCount);
				}
				if (workItemCounts.containsKey(itemId) && playCount < workItemCounts.get(itemId)) {
					return new Outcome(null, "RETAIL_ITEMPLAY_ITEM_COUNT_MISMATCH",
						symbol + " step=" + playCount + " work=" + workItemCounts.get(itemId));
				}
				int outputItemId = 0;
				if (step.itemPlay().outputSymbol() != null) {
					Integer resolvedOutput = itemIndex.resolve(step.itemPlay().outputSymbol());
					if (resolvedOutput == null || step.itemPlay().outputCount() <= 0
							|| !workItemCounts.containsKey(resolvedOutput)
							|| workItemCounts.get(resolvedOutput) != step.itemPlay().outputCount()) {
						return new Outcome(null, "RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED",
							step.itemPlay().outputSymbol() + " " + step.itemPlay().outputCount());
					}
					outputItemId = resolvedOutput;
				}
				resolvedPlays.add(Step.itemPlay(new RetailDataDrivenTable.ItemPlayTarget(symbol, playCount,
					itemId, step.itemPlay().outputSymbol(), step.itemPlay().outputCount(), outputItemId)));
			}
			steps = List.copyOf(resolvedPlays);
		}
		// 客户端混合链登记必须逐段覆盖**可见步**（骑行者占行不占段，生成器同口径排除）。
		// The client mixed-chain registry must cover every visible step (riders own a row but no stage;
		// the generator excludes them with the same rule).
		int visibleSteps = 0;
		for (Step step : steps) {
			if (!step.rider()) {
				visibleSteps++;
			}
		}
		List<RetailClientTalkCollectChainPages.Stage> stages = chainPages.find(entry.questId())
			.map(RetailClientTalkCollectChainPages.Pages::stageLadders).orElse(List.of());
		if (stages.size() != visibleSteps) {
			return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED",
				"stages=" + stages.size() + " steps=" + visibleSteps);
		}
		// 段梯上的导航页必须是登记过的对话动作：梯首页 id 允许登记表外（该页自身没有按钮发出它，
		// 15680 的 select7=3057 形，无同名回显边），梯上更深处的页 id 则是客户端导航按钮的落点，
		// 路由不了就如实拒绝，不静默丢边。
		// Every nav page on a stage ladder must be a registered dialog action: the head page id may sit
		// outside the registry (no client button sends it — 15680's select7 = 3057, which gets no echo
		// edge), but a deeper page id is the landing page of a client nav button, and one we cannot
		// route refuses honestly instead of dropping the edge.
		for (RetailClientTalkCollectChainPages.Stage stage : stages) {
			List<Integer> ladder = stage.ladder();
			for (int depth = 1; depth < ladder.size(); depth++) {
				if (QuestDialogAction.findId(ladder.get(depth)) == null) {
					return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED",
						"client nav page not a dialog action: " + ladder.get(depth));
				}
			}
		}
		// 采集物来自真端元数据交付物；交付物为空则没有可检查的整组（采集族同判据）。
		// The collect goods come from the retail metadata item requirements; empty means nothing
		// to check (same rule as the collect family).
		if (metadata.metadata().itemRequirements().isEmpty()) {
			return new Outcome(null, "RETAIL_COLLECT_ITEM_SHAPE", "真端交付物为空");
		}
		int lastRow = summaryRows.lastRowIndex(entry.questId());
		// 领奖行 0 会与 started 投影撞包（DUPLICATE_NODE_PROJECTION），如实拒绝。
		// A reward row of 0 would collide with the started projection; reject honestly.
		if (lastRow <= 0) {
			return new Outcome(null, "RETAIL_TALK_COLLECT_CHAIN_DEFERRED", "lastRow=0");
		}
		try {
			QuestDefinition definition = build(entry.questId(), steps, stages, npcIndex,
				metadata.metadata(), acquiredNpc, rewardNpc, lastRow,
				selectNoneLadder);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RetailNpcResolutionException e) {
			// 未登记模板（副本后缀名等）不属于编译缺陷，按稳定拒绝码留在 XML。
			// Unregistered templates (instance-suffixed names) are not compiler defects; reject
			// with the stable code and keep the row on XML.
			return new Outcome(null, e.code(), e.npc());
		} catch (RuntimeException e) {
			// -Dretail.talkCollect.debug=true 重掷原始栈（诊断用）。
			// -Dretail.talkCollect.debug=true rethrows the raw stack for diagnosis.
			if (Boolean.getBoolean("retail.talkCollect.debug")) {
				throw e;
			}
			return new Outcome(null, "COMPILATION_FAILED", e.toString());
		}
	}

	private static QuestDefinition build(int questId, List<Step> steps,
			List<RetailClientTalkCollectChainPages.Stage> stages, RetailNpcNameIndex npcIndex,
			QuestMetadata metadata, int acquiredNpc, int rewardNpc, int lastRow,
			boolean selectNoneLadder) {
		// 每步 npc 精确解析到 1 个模板；多解/零解按稳定码拒绝。骑行者无 npc（占位 0）。
		// Each step npc must resolve to exactly one template; ambiguous or empty rejects. Riders carry
		// no npc (placeholder 0).
		List<Integer> npcIds = new ArrayList<>(steps.size());
		for (Step step : steps) {
			if (step.rider()) {
				npcIds.add(0);
				continue;
			}
			Set<Integer> ids = npcIndex.resolveAll(List.of(step.npc())).npcIds();
			if (ids.size() != 1) {
				throw new RetailNpcResolutionException("RETAIL_TALK_NPC_UNRESOLVED", step.npc());
			}
			npcIds.add(ids.iterator().next());
		}
		// 步 index → 段 index：骑行者占行不占对话段（与登记表逐段对齐）。
		// Step index to stage index: riders own a row but no dialog stage.
		int[] stageIndexOf = new int[steps.size()];
		int visibleCursor = 0;
		for (int index = 0; index < steps.size(); index++) {
			stageIndexOf[index] = visibleCursor;
			if (!steps.get(index).rider()) {
				visibleCursor++;
			}
		}
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> rewardRow = Map.of("var0", lastRow);
		int stepCount = steps.size();
		List<QuestNode> nodes = new ArrayList<>(stepCount + 4);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zero)));
		for (int stage = 1; stage < stepCount; stage++) {
			nodes.add(new QuestNode("s" + stage, new NodeProjection(QuestStatus.START, Map.of("var0", stage))));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));
		List<QuestTransition> transitions = new ArrayList<>();
		// itemplay 骑行者的接取授予（与宽链编译器同判据）：被演的 work item 按真端数量在接取时发放
		// （遗留形在链中 give-item；真端模型接取即发、演出消耗、完成/放弃由 planner 回收），普通物品
		// 按演出次数发放。带 itemplay 的行末步不再重复授予任务凭证——否则被演物品与输出物都会被
		// 二次发放（放宽链 15680 形：被演 work item 已在接取发出、输出物在演出边产出）。
		// The itemplay rider's accept grant (the broad compiler's rule): a played work item is granted on
		// acceptance with its retail count (the legacy shape gives it mid-chain; the retail model grants on
		// accept, the play consumes it and the planner reclaims it on completion or abandon), and a plain
		// item is granted by its play count. A row carrying an itemplay rider no longer re-grants the work
		// items on its final step — that would hand out both the played item and its output twice (the
		// broad-chain 15680 shape: the played work item already went out on accept, the output on the play
		// edge).
		Map<Integer, Integer> acceptGrants = new java.util.LinkedHashMap<>();
		boolean hasItemPlay = false;
		for (Step step : steps) {
			if (!step.isItemPlay()) {
				continue;
			}
			hasItemPlay = true;
			int itemId = step.itemPlay().itemId();
			int workCount = metadata.questWorkItems().stream()
				.filter(item -> item.itemId() == itemId)
				.mapToInt(QuestItemRequirement::count).sum();
			if (workCount > 0) {
				acceptGrants.putIfAbsent(itemId, workCount);
			} else {
				acceptGrants.merge(itemId, step.itemPlay().count(), Integer::sum);
			}
		}
		if (acquiredNpc < 0) {
			// 链式/区域接取（none / enterarea，遗留 10501/10011 形）：升级 + 区域任务结束事件自动
			// 接取，StartEligible 承载等级门、QuestsFinished 承载真端前置。
			// Chain/area acquire (none / enterarea, the legacy 10501/10011 shapes): level-up and
			// zone-mission-end events auto-grant; StartEligible carries the level gate and
			// QuestsFinished the retail prerequisites.
			List<QuestCondition> grantGate = new ArrayList<>(2);
			grantGate.add(new QuestCondition.StartEligible());
			if (!metadata.prerequisites().isEmpty()) {
				grantGate.add(new QuestCondition.QuestsFinished(metadata.prerequisites()));
			}
			List<QuestAction> grantActions = new ArrayList<>(acceptGrants.size());
			acceptGrants.forEach((itemId, count) -> grantActions.add(new QuestAction.GiveItem(itemId, count)));
			for (QuestEvent grantEvent : new QuestEvent[] {new QuestEvent.LevelUp(),
					new QuestEvent.ZoneMissionEnd()}) {
				transitions.add(new QuestTransition(grantEvent, List.copyOf(grantGate),
					List.copyOf(grantActions), "started",
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
					null, "unaccepted"));
			}
		} else {
			// 接取规范形（P0-2 DD 尾片）：页 4 直发，select1/select_none 阶梯与 1007 中转随页链删除；
			// 接取授予挂接取提交边（canonical 三参重载，与原"只挂落地路由"同口径）。
			// Canonical accept (the DD tail slice): page 4 directly, the letter ladders and the 1007 hop
			// are gone; the grants ride the accept commit edges (the canonical three-arg overload).
			List<QuestAction> acceptActions = new ArrayList<>(acceptGrants.size());
			acceptGrants.forEach((itemId, count) -> acceptActions.add(new QuestAction.GiveItem(itemId, count)));
			transitions.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquiredNpc, "started",
				List.copyOf(acceptActions)));
		}
		for (int index = 0; index < stepCount; index++) {
			Step step = steps.get(index);
			String source = index == 0 ? "started" : "s" + index;
			boolean finalStep = index == stepCount - 1;
			String target = finalStep ? "reward" : "s" + (index + 1);
			if (step.rider()) {
				// 骑行者占行不占段：进入区域/世界推进一行（var0 = 步序，末步行落领奖行），无对话段、
				// 无页梯（与宽链编译器同形；遗留 <enter-zone>/<enter-world>+<world-is> 同构）。
				// A rider owns a row but no stage: entering the area/world advances one row (var0 = the
				// step ordinal, the reward row on the final step), with no dialog stage or page ladder
				// (isomorphic to the legacy enter-zone and enter-world + world-is elements).
				int landingRow = finalStep ? lastRow : index + 1;
				if (step.enterArea() != null) {
					transitions.add(new QuestTransition(new QuestEvent.EnterZone(step.enterArea()),
						List.of(), List.of(new QuestAction.SetVariable("var0", landingRow)), target,
						List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						null, source));
				} else if (step.isItemPlay()) {
					// itemplay 骑行者：使用道具演出推进一行（遗留 <use-item> 同形），无对话段；时长取
					// 遗留证据标准 3000ms，路由声明本身武装引擎的定时演出派发（QuestEventIndex
					// .itemPlayAnimationMillis 由路由派生）；声明了输出物时演出消耗被演道具并产出输出物
					// （15680 形：give 输出 + remove 被演）。
					// An itemplay rider advances one row by playing the item (the legacy use-item shape)
					// with no dialog stage; the duration is the legacy-evidence standard 3000ms and the
					// route declaration itself arms the engine's timed play dispatch (the QuestEventIndex
					// itemPlayAnimationMillis comes off the route). A declared output item makes the play
					// consume the played item and produce the output (the 15680 shape).
					List<QuestAction> actions = new ArrayList<>(3);
					if (step.itemPlay().outputItemId() > 0) {
						actions.add(new QuestAction.GiveItem(step.itemPlay().outputItemId(),
							step.itemPlay().outputCount()));
						actions.add(new QuestAction.RemoveItem(step.itemPlay().itemId(), 1));
					}
					actions.add(new QuestAction.SetVariable("var0", landingRow));
					transitions.add(new QuestTransition(
						new QuestEvent.ItemPlay(step.itemPlay().itemId(), ITEM_PLAY_MILLIS),
						List.of(), List.copyOf(actions), target,
						List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						null, source));
				} else if (step.enterWorld() != null && step.enterWorld() > 0) {
					transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
						List.of(new QuestCondition.WorldIs(step.enterWorld(), true)),
						List.of(new QuestAction.SetVariable("var0", landingRow)), target,
						List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						null, source));
				} else {
					throw new IllegalStateException("enterworld rider has no world id");
				}
				continue;
			}
			int npc = npcIds.get(index);
			RetailClientTalkCollectChainPages.Stage stage = stages.get(stageIndexOf[index]);
			// 段首页：QUEST_SELECT 显示段梯首页（talk 段 = select{i} 梯首；collect 段 = 段首页）；
			// 段首页自身的同名动作（如 SELECT3=1693）同样回显首页（客户端按钮图：ok 页 1693 → select3）。
			// 页 id 与按钮动作共用编号空间：登记表外的页 id 没有任何客户端按钮发出（15680 的 select7
			// =3057 页上只有推进按钮 SETPRO7），此时不发同名回显边——发了也没有事件源对应。
			// Head page: QUEST_SELECT shows the stage ladder's head page; the head's own action id
			// (e.g. SELECT3=1693) re-shows it, matching the client button graph. Page ids and button
			// actions share a numbering space: a page id outside the registry is sent by no client
			// button (15680's select7 = 3057 carries only the advance button SETPRO7), so no echo edge
			// is emitted for it — nothing would ever raise that event.
			transitions.add(RetailSimpleHuntDefinitionCompiler.talk(npc, QuestDialogAction.QUEST_SELECT,
				source, source, null, List.of(new AfterCommitAction.ShowQuestDialog(stage.ladder().get(0)))));
			QuestDialogAction headAction = QuestDialogAction.findId(stage.ladder().get(0));
			if (headAction != null) {
				transitions.add(RetailSimpleHuntDefinitionCompiler.talk(npc, headAction, source, source, null,
					List.of(new AfterCommitAction.ShowQuestDialog(stage.ladder().get(0)))));
			}
			// 段内导航/信息页（talk 段线性梯、collect 段 select2_1..3 扇出）：动作 id = 下一页 id；
			// 梯上出现登记表外的页 id 说明客户端有我们路由不了的导航按钮——如实拒绝（不静默丢边）。
			// FINISH_DIALOG 收尾（1008 按钮遍布客户端页；接取落点已由 acceptFlow 覆盖，不重复发）。
			// Stage navs (talk linear ladders, collect info fan-outs): the action id is the next
			// page's id; a ladder page id outside the registry means a client nav button we cannot
			// route, which refuses honestly rather than dropping the edge silently. FINISH_DIALOG
			// closes (acceptFlow already covers the accept landing state).
			List<Integer> ladder = stage.ladder();
			for (int depth = 1; depth < ladder.size(); depth++) {
				QuestDialogAction navAction = QuestDialogAction.findId(ladder.get(depth));
				if (navAction == null) {
					// compile() 已按稳定码拒绝（不变量，运行到此处即为缺陷）。
					// compile() already rejected this with a stable code (invariant: reaching here is a defect).
					throw new IllegalStateException("nav page is not a dialog action: " + ladder.get(depth));
				}
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, navAction.id()),
					List.of(), List.of(), source,
					List.of(new AfterCommitAction.ShowQuestDialog(ladder.get(depth))), null, source));
			}
			if (index > 0) {
				transitions.add(talk(npc, QuestDialogAction.FINISH_DIALOG, source, source,
					List.of(new AfterCommitAction.CloseDialog())));
			}
			if (stage.isCollect()) {
				// 落点段 NPC：39 边落在下一段后，ok 页（10000）的 FINISH_DIALOG 按钮仍由检查 NPC
				// 收发（遗留 s2→s2 FINISH_DIALOG npc=检查 NPC）；落点段换人时必须补该出口，否则
				// 客户端 ok 页按钮无路由。
				// 首段另需失败页出口：失败边停在源节点，若检查 NPC 与接取落地路由的 NPC 不同
				// （25084 形：接取 DF5_Zake_E、检查 DF5_Coli_E），失败页（10001）的同一 1008 按钮在
				// 源节点没有同键路由；接取 NPC 相同时 acceptFlow 已发出该出口，不重复发。
				// Landing stage npc: after the 39 edge lands on the next stage the ok page's
				// FINISH_DIALOG button is still served by the check npc (the legacy s2 -> s2
				// FINISH_DIALOG with the check npc), so the close route must be added whenever the
				// landing stage uses a different npc.
				// The leading stage also needs a fail-page exit: its fail edge stays on the source
				// node, so when the check npc differs from the npc owning the accept-landing routes
				// (the 25084 shape: accept DF5_Zake_E, check DF5_Coli_E) the same 1008 button on the
				// fail page (10001) has no same-key route there; an identical accept npc already owns
				// that exit through acceptFlow.
				int landedNpc = finalStep ? 0 : npcIds.get(index + 1);
				transitions.addAll(collectCheck(metadata, npc, stage, source, target, finalStep, landedNpc,
					index == 0 && npc != acquiredNpc));
				continue;
			}
			if (finalStep) {
				// 末步 SET_SUCCEED 授予任务凭证（15301 形：收尾发放凭证 + 关窗）；带 itemplay 骑行者
				// 的行例外——凭证已在接取与演出边发出（见 acceptGrants 注释）。
				// The final SET_SUCCEED grants the quest work items (15301 shape) and closes; a row with
				// an itemplay rider is the exception — its credentials went out on accept and on the play
				// edge (see the acceptGrants comment).
				List<QuestAction> actions = new ArrayList<>();
				if (!hasItemPlay) {
					for (QuestItemRequirement workItem : metadata.questWorkItems()) {
						actions.add(new QuestAction.GiveItem(workItem.itemId(), workItem.count()));
					}
				}
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, stage.advanceActionId()), List.of(), List.copyOf(actions),
					target, talkAdvanceAfterCommit(stage, true), null, source));
			} else {
				// 中间步推进（SETPRO{i}）：阶梯值 +1，PACKET_ONLY + 全局任务簿页（buildChain 同形）。
				// An intermediate advance bumps the ladder; PACKET_ONLY plus the journal page
				// (buildChain shape).
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, stage.advanceActionId()),
					List.of(), List.of(new QuestAction.SetVariable("var0", index + 1)), target,
					talkAdvanceAfterCommit(stage, false), null, source));
			}
		}
		transitions.add(talk(rewardNpc, QuestDialogAction.QUEST_SELECT, "reward", "reward",
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id()))));
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.completeFlow(questId, rewardNpc, metadata));
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.journalRowRepair(lastRow));
		// 掉落生效行的交互物自环：掉落在任务书该行计数的物体需要同 var0 的 TalkToNpc +
		// CAN_ACT(ACTION_ITEM_USE) 路由（QuestInteractionObjectValidator 启动合同）。
		// Interaction objects counting on a journal row get TalkToNpc + CanAct(ACTION_ITEM_USE)
		// self edges on that very row (the QuestInteractionObjectValidator startup contract).
		for (Integer objectId : new java.util.LinkedHashSet<>(metadata.drops().stream()
				.filter(drop -> drop.collectingStep() >= 0 && drop.collectingStep() < stepCount)
				.map(QuestDrop::npcId).toList())) {
			String collectSource = "started";
			for (QuestDrop drop : metadata.drops()) {
				if (drop.npcId() == objectId) {
					collectSource = drop.collectingStep() == 0 ? "started" : "s" + drop.collectingStep();
					break;
				}
			}
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
				collectSource, List.of(), null, collectSource));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"), List.of(),
				List.of(), collectSource, List.of(), null, collectSource));
		}
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** ItemPlay 演出时长：遗留证据标准值（80978/17505/20035/14153 全为 3000ms）。 /
	 * The item-play duration: the legacy-evidence standard (80978/17505/20035/14153 all use 3000ms). */
	private static final int ITEM_PLAY_MILLIS = 3000;

	/**
	 * talk 段推进的 afterCommit：末段 = LEVEL_AND_VISIBILITY + 关窗，中段 = PACKET_ONLY + 任务簿页；
	 * 梯尾页声明过场时推进附带 PlayMovie（客户端 HTML 的 CutScene，如 16942 的 899）。
	 * A talk stage's advance after-commit: the final stage syncs LEVEL_AND_VISIBILITY and closes,
	 * a mid stage syncs PACKET_ONLY into the journal page; a tail-page CutScene appends PlayMovie
	 * (the client HTML's CutScene, e.g. 16942's 899).
	 */
	private static List<AfterCommitAction> talkAdvanceAfterCommit(
			RetailClientTalkCollectChainPages.Stage stage, boolean finalStep) {
		List<AfterCommitAction> afterCommit = new ArrayList<>(4);
		afterCommit.add(new AfterCommitAction.SyncQuestState(finalStep
			? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY));
		if (stage.movieId() != null) {
			afterCommit.add(new AfterCommitAction.PlayMovie(stage.movieId(), QuestMovieType.CUTSCENE));
		}
		afterCommit.add(finalStep
			? new AfterCommitAction.CloseDialog()
			: new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
		return List.copyOf(afterCommit);
	}

	/**
	 * collect 段检查对：39（可选 1009）success = 整组 has-item + remove-item 进下一段/领奖，
	 * fail = 结果页自环；中间段 PACKET_ONLY + ok 页（15301 形），末段 LEVEL_AND_VISIBILITY +
	 * 领奖窗（16942 形，末段另带 1009 对）。
	 * The collect check pairs: 39 (plus 1009 on the final stage) success checks and removes the
	 * whole group into the next state or reward; fail shows the fail page. Mid stages sync
	 * PACKET_ONLY plus the ok page (15301 shape); the final stage syncs LEVEL_AND_VISIBILITY with
	 * the reward window and carries the 1009 pair (16942 shape).
	 */
	private static List<QuestTransition> collectCheck(QuestMetadata metadata, int npc,
			RetailClientTalkCollectChainPages.Stage stage, String source, String target, boolean finalStep,
			int landedNpc, boolean sourceNeedsClose) {
		List<QuestCondition> hasItems = metadata.itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = metadata.itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		int okPage = stage.resultPages()[0];
		int failPage = stage.resultPages()[1];
		List<AfterCommitAction> success = finalStep
			? List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))
			: List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(okPage));
		List<QuestTransition> transitions = new ArrayList<>(4);
		int checkAction = stage.advanceActionId();
		transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(npc, checkAction),
			List.copyOf(hasItems), List.copyOf(removeItems), target, success, 0, source));
		transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(npc, checkAction),
			List.of(), List.of(), source, List.of(new AfterCommitAction.ShowQuestDialog(failPage)), 1, source));
		if (sourceNeedsClose) {
			// 首段失败页的关窗出口（见调用点注释：失败边停在源节点，检查 NPC 与接取 NPC 不同时
			// 该节点上没有同键 1008 路由）。
			// The leading stage's fail-page close exit (see the call site: the fail edge stays on the
			// source node, which carries no same-key 1008 route when the check npc differs from the
			// accept npc).
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.FINISH_DIALOG.id()),
				List.of(), List.of(), source, List.of(new AfterCommitAction.CloseDialog()), null, source));
		}
		if (finalStep) {
			// 末段 1009 对（16942 形）：ok 页的 1009 按钮同组检查直达领奖窗。
			// The final stage's 1009 pair (16942 shape): the ok page's 1009 button repeats the
			// group check straight into the reward window.
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.copyOf(hasItems), List.copyOf(removeItems), target, success, 0, source));
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.of(), List.of(), source, List.of(new AfterCommitAction.ShowQuestDialog(failPage)),
				1, source));
		} else if (landedNpc != npc) {
			// 中间段落点关窗出口：ok 页（10000）唯一的 FINISH_DIALOG 按钮由检查 NPC 收发，落点段
			// 换人（或落点是 hunt 段）时同键路由不存在，须在落点节点补（遗留 s2→s2 FINISH_DIALOG
			// npc=检查 NPC 逐字同形）；落点段 NPC 相同时该出口已由段循环发出，不重复发。
			// Mid-stage close exit: the ok page's only FINISH_DIALOG button is served by the check
			// npc, so a different landing npc needs the route on the landed node (the legacy
			// s2 -> s2 FINISH_DIALOG with the check npc); an identical landing npc is already
			// covered by the stage loop.
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.FINISH_DIALOG.id()),
				List.of(), List.of(), target, List.of(new AfterCommitAction.CloseDialog()), null, target));
		}
		return transitions;
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, null, source);
	}

	/**
	 * NPC 名解析失败的稳定载体：携带拒绝码与未解析模板名（与混合 hunt 链同判据）。
	 * Stable carrier for npc-name resolution failures: the rejection code plus the unresolved
	 * template (same rule as the mixed hunt chain).
	 */
	private static final class RetailNpcResolutionException extends RuntimeException {
		private final String code;
		private final String npc;

		private RetailNpcResolutionException(String code, String npc) {
			super(code + ": " + npc);
			this.code = code;
			this.npc = npc;
		}

		private String code() {
			return code;
		}

		private String npc() {
			return npc;
		}
	}
}
