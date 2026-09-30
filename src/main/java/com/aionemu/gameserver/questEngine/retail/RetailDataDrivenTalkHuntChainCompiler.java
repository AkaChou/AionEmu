package com.aionemu.gameserver.questEngine.retail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * talk/hunt/collectitem 交错行的混合链合成器（客户端 SECTION 对齐形）。
 * <p>
 * 布局 = 客户端任务书硬合同：{@code var0}（偏移 0，6 位）= 行阶梯（SECTION_0，每个步骤完成 +1）、
 * {@code var1}（偏移 6，6 位）= 当前 hunt 段计数（SECTION_1，段完成清零；仅存在 hunt 步时声明）。
 * hunt 步不再逐杀建链态，改为旧 XML 同形的**条件边形**：自环 {@code IncrementVariable}（优先级 1，
 * {@code VariableBelow(count-1)}）+ 完成边（优先级 0，{@code VariableAtLeast(count-1)} + 行 +1 +
 * 计数清零）；末段为 hunt 时完成边直达领奖且保留满计数（QE-051：领奖投影携带末段击杀数）。
 * talk 步 = 信件页梯（梯尾 CutScene 附带 PlayMovie）；collectitem 步 = 39 整组检查对；pvp 步
 * （80846/15673 形）= 世界通配（或军衔阈值）击杀的 6 位计数对，等级窗取 ScriptDLL 缺省 10。
 * 领奖行 = 客户端任务书末行；恰一条进入世界自愈边。
 * <p>
 * Mixed-chain synthesis for talk/hunt/collectitem rows in the client SECTION-aligned shape:
 * {@code var0} (offset 0, 6 bits) is the journal-row ladder (SECTION_0, +1 per completed step)
 * and {@code var1} (offset 6, 6 bits) the current hunt stage's kill counter (SECTION_1, reset on
 * stage completion, declared only when hunts exist). Hunts use the legacy conditional edge pair —
 * a self-loop increment (priority 1, {@code VariableBelow(count-1)}) plus a completing edge
 * (priority 0, {@code VariableAtLeast(count-1)} advancing the row and resetting the counter); a
 * final hunt completes straight into reward keeping its full count (QE-051 carries the last
 * stage's kills in the reward projection). Talk steps carry letter ladders (tail-page CutScene
 * adds PlayMovie); collectitem steps carry the group check pair. The reward row is the client
 * journal's last row; exactly one enter-world heal edge.
 */
public final class RetailDataDrivenTalkHuntChainCompiler {

	/**
	 * 本家族接取段占用的页族数：接取走 select_none 询问窗（未接态首屏），声明的首个 select 页族即第 1 段。
	 * Leading page families owned by this family's acquire segment: the accept runs through the
	 * select_none ask window (the unaccepted-state head), so the first declared select family is stage 1.
	 */
	static final int ACQUIRE_PAGE_FAMILIES = 0;

	private RetailDataDrivenTalkHuntChainCompiler() {
	}

	/** 编译结果：定义 + 稳定拒绝码（与家族编译器同形）。 / Outcome: definition plus a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 展开后的一个步骤：talkNpc 非空 = 信件步；collectNpc 非空 = 采集检查步；huntBlock 非空 =
	 * 一个 hunt 块（块内分号段 = 并行目标，与客户端同行/同行组一一对应）。
	 * One expanded step: talkNpc = letter stage, collectNpc = hand-in check stage, huntBlock = one
	 * hunt block (its semicolon segments are parallel objectives matching the client's same-row
	 * group).
	 */
	public record Step(String talkNpc, List<RetailDataDrivenTable.HuntStage> huntBlock, String collectNpc,
			String enterArea, Integer enterWorld, RetailDataDrivenTable.ItemPlayTarget itemPlay,
			RetailDataDrivenTable.TalkFobjTarget talkFobj, PvpTarget pvp) {

		/**
		 * 链内 PVP 计数载荷：所需击杀数 + 军衔阈值（0 = {@code KillInWorld(0)} 世界通配）+ PvP
		 * 目标等级窗（0 = ScriptDLL 缺省 10；victim 等级 ≥ killer − gap 才计数）。
		 * The in-chain PVP counter payload: the required kill count, the rank threshold (0 = the
		 * {@code KillInWorld(0)} world wildcard) and the PvP target level gap (0 = the ScriptDLL
		 * default of 10; only a victim at level &gt;= killer level − gap counts).
		 */
		public record PvpTarget(int count, int minRank, int levelGap) {
		}

		public static Step talk(String npc) {
			return new Step(npc, null, null, null, null, null, null, null);
		}

		public static Step hunt(List<RetailDataDrivenTable.HuntStage> block) {
			return new Step(null, List.copyOf(block), null, null, null, null, null, null);
		}

		public static Step collect(String npc) {
			return new Step(null, null, npc, null, null, null, null, null);
		}

		public static Step enterArea(String areaAlias) {
			return new Step(null, null, null, areaAlias, null, null, null, null);
		}

		public static Step enterWorld(int worldId) {
			return new Step(null, null, null, null, worldId, null, null, null);
		}

		public static Step itemPlay(RetailDataDrivenTable.ItemPlayTarget target) {
			return new Step(null, null, null, null, null, target, null, null);
		}

		public static Step talkFobj(RetailDataDrivenTable.TalkFobjTarget target) {
			return new Step(null, null, null, null, null, null, target, null);
		}

		public static Step pvp(PvpTarget target) {
			return new Step(null, null, null, null, null, null, null, target);
		}

		public boolean isTalkFobj() {
			return talkFobj != null;
		}

		public boolean isTalk() {
			return talkNpc != null;
		}

		public boolean isCollect() {
			return collectNpc != null;
		}

		public boolean isHunt() {
			return huntBlock != null;
		}

		public boolean isEnterArea() {
			return enterArea != null;
		}

		public boolean isEnterWorld() {
			return enterWorld != null;
		}

		public boolean isItemPlay() {
			return itemPlay != null;
		}

		public boolean isPvp() {
			return pvp != null;
		}
	}

	/**
	 * 把真端行展开为步骤序（块级交错 + hunt 块内的分号段依次展开）；含其他类别返回空表。
	 * Expands the retail row into ordered steps (block-level interleaving; a hunt block's
	 * semicolon segments expand consecutively). Empty for rows carrying other step categories.
	 */
	public static List<Step> expandSteps(RetailDataDrivenTable.Entry entry) {
		// 骑行类别（enterarea/enterworld/itemplay/talkfobj/pvp）占行不占对话段，与 talk/hunt/collectitem
		// 同为域内词汇——talkfobj 此前在下方展开循环已实现而白名单漏收，含它的一律返回空表（"no steps"）。
		// The rider categories (enterarea/enterworld/itemplay/talkfobj/pvp) occupy a row without a
		// dialog stage and belong to the same domain; talkfobj was already expanded below while the
		// guard still rejected it, so any row carrying it came back empty ("no steps").
		for (String category : entry.stepCategories()) {
			if (!category.equals("talk") && !category.equals("hunt") && !category.equals("collectitem")
					&& !category.equals("enterarea") && !category.equals("enterworld")
					&& !category.equals("itemplay") && !category.equals("talkfobj")
					&& !category.equals("pvp")) {
				return List.of();
			}
		}
		// PVP 载荷是整行级的扁平计数序（value0 的分号段合并进一个列表），与步数不一一对应时
		// （分号多段 = 并行槽位，链内词汇未覆盖）如实按无步骤处理。
		// The PVP payload is a row-level flat count list (a value0 semicolon list is merged), so a
		// row whose PVP count count differs from its PVP step count has an uncovered shape (parallel
		// slots) and is treated as having no steps, honestly.
		long pvpBlocks = entry.stepCategories().stream().filter("pvp"::equals).count();
		if (pvpBlocks != entry.pvpCounts().size()) {
			return List.of();
		}
		List<Step> steps = new ArrayList<>();
		int huntBlock = 0;
		int huntStage = 0;
		int talkIndex = 0;
		int collectIndex = 0;
		int enterAreaIndex = 0;
		int enterWorldIndex = 0;
		int itemPlayIndex = 0;
		int talkFobjIndex = 0;
		int pvpIndex = 0;
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
			} else if (category.equals("itemplay")) {
				if (itemPlayIndex < entry.itemPlaySteps().size()) {
					steps.add(Step.itemPlay(entry.itemPlaySteps().get(itemPlayIndex++)));
				}
			} else if (category.equals("talkfobj")) {
				if (talkFobjIndex < entry.talkFobjSteps().size()) {
					steps.add(Step.talkFobj(entry.talkFobjSteps().get(talkFobjIndex++)));
				}
			} else if (category.equals("pvp")) {
				// 军衔阈值与等级窗是行级字段（真端表每行一份），逐 PVP 步复用。
				// The rank threshold and the level gap are row-level fields (one per retail row) and
				// are reused by every PVP step of the row.
				steps.add(Step.pvp(new Step.PvpTarget(entry.pvpCounts().get(pvpIndex++),
					entry.pvpMinRank(), entry.pvpLevelGap())));
			} else {
				int segments = huntBlock < entry.huntBlocks().size()
					? entry.huntBlocks().get(huntBlock) : 0;
				List<RetailDataDrivenTable.HuntStage> block = new ArrayList<>();
				for (int i = 0; i < segments && huntStage < entry.huntStages().size(); i++) {
					block.add(entry.huntStages().get(huntStage++));
				}
				if (!block.isEmpty()) {
					steps.add(Step.hunt(block));
				}
				huntBlock++;
			}
		}
		return steps;
	}

	/**
	 * 编译 talk/hunt/collectitem 交错链（Talk 接取、行阶梯 + 段计数的客户端对齐形）。
	 * Compiles the talk/hunt/collectitem interleave chain (Talk acquire; the client-aligned
	 * row-ladder plus stage-counter shape).
	 */
	public static Outcome compile(RetailDataDrivenTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailQuestMetadataCompiler.Outcome metadata, java.util.Set<Integer> acquiredNpcs, int rewardNpc,
			RetailQuestUseItemNpcs interactionObjects, RetailClientHuntProgressRows huntProgressRows,
			RetailClientSummaryRows summaryRows, RetailEnterAreaZoneResolution enterAreaZones,
			RetailItemNameIndex itemIndex, boolean levelUpAcquire, boolean selectNoneLadder) {
		Objects.requireNonNull(entry, "entry");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		List<Step> steps = expandSteps(entry);
		if (steps.isEmpty()) {
			return new Outcome(null, "RETAIL_TALK_HUNT_CHAIN_DEFERRED", "no steps");
		}
		// EA 别名必须解析成登记区名：运行时 enter-zone 只按 zones XML 登记名派发
		// （PlayerController → QuestEngine.onEnterZone(ZoneName)），未登记别名 = 永不命中的死边。
		// EA aliases must resolve to registered zone names: runtime enter-zone dispatches by the
		// zones-XML registered name only, so an unregistered alias is a permanently dead edge.
		if (steps.stream().anyMatch(Step::isEnterArea)) {
			List<Step> resolved = new ArrayList<>(steps.size());
			for (Step step : steps) {
				if (!step.isEnterArea()) {
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
		}
		// ItemPlay 步：道具符号经物品名索引解析（大小写不敏感），并须落在真端 work_items 命名域
		// （接取授予、完成/放弃清理对称）；数量与 work item 计数不一致或未解析时如实拒绝。
		// ItemPlay steps: the symbol resolves through the item-name index (case-insensitive) and
		// must live in the retail work-items domain (granted on accept, cleared on completion or
		// abandon — symmetric); reject honestly on unresolved names or count mismatches.
		if (steps.stream().anyMatch(Step::isItemPlay)) {
			Set<Integer> workItemIds = new java.util.HashSet<>();
			Map<Integer, Integer> workItemCounts = new java.util.HashMap<>();
			for (QuestItemRequirement workItem : metadata.metadata().questWorkItems()) {
				workItemIds.add(workItem.itemId());
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
				if (workItemIds.contains(itemId)) {
					int workCount = workItemCounts.get(itemId);
					// 数量语义两形：count == work 数量 = 单播形（授一演一，80978 形）；count > work 且
					// work == 1 = 多播形（15042 客户端步文本 [%2]/3 计数背书：授一次、演 N 次）。
					// 其余错配如实拒绝。
					// Two count shapes: count == work quantity is the single-play shape (granted once,
					// played once, the 80978 shape); count > work with work == 1 is the multi-play shape
					// (15042's client step text backs a [%2]/3 counter: granted once, played N times).
					// Other mismatches are rejected honestly.
					if (playCount < workCount || (playCount > workCount && workCount != 1)) {
						return new Outcome(null, "RETAIL_ITEMPLAY_ITEM_COUNT_MISMATCH",
							symbol + " step=" + playCount + " work=" + workCount);
					}
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
				// 普通物品形（18738 炸弹）：符号不在 work_items 域——接取按 count 授予（acceptGrants
				// 既有机制，遗留六变体各授 ×10），使用消耗、完成/放弃不回收（planner 清理只覆盖
				// work items）；多播计数同形（客户端步文本 [%2]/10 背书）。
				// Normal-item shape (the 18738 bomb): the symbol lives outside the work-items domain —
				// granted on accept by count (the existing acceptGrants machinery; the legacy shape
				// grants ×10 on every variant), consumed by use, never reclaimed on completion or
				// abandon (planner cleanup covers work items only); the multi-play counter shape
				// applies unchanged (the client step text backs a [%2]/10 counter).
				resolvedPlays.add(new Step(step.talkNpc(), step.huntBlock(), step.collectNpc(), step.enterArea(),
					step.enterWorld(), new RetailDataDrivenTable.ItemPlayTarget(symbol, playCount, itemId,
						step.itemPlay().outputSymbol(), step.itemPlay().outputCount(), outputItemId),
					step.talkFobj(), step.pvp()));
			}
			steps = List.copyOf(resolvedPlays);
		}
		List<Step> talks = steps.stream().filter(Step::isTalk).toList();
		List<Step> collects = steps.stream().filter(Step::isCollect).toList();
		// hunt 步的客户端进度行组：组数必须等于 hunt 步数（门→段校准；无 hunt 步不查）。
		// The client progress-row groups for the hunt steps: the group count must equal the hunt
		// step count (gate-to-SECTION calibration; skipped when no hunts exist).
		List<Step> huntSteps = steps.stream().filter(Step::isHunt).toList();
		List<List<RetailClientHuntProgressRows.Row>> huntGroups = huntSteps.isEmpty()
			? List.of()
			: huntProgressRows.groups(entry.questId());
		if (!huntGroups.isEmpty() && huntGroups.size() != huntSteps.size()) {
			return new Outcome(null, "RETAIL_HUNT_PROGRESS_SHAPE",
				"groups=" + huntGroups.size() + " hunts=" + huntSteps.size());
		}
		// 门→段映射按计数行（hunt 步的绝对步号 = 其源行 = 客户端 S0 值）：交织 EnterArea 步也占
		// 行号，顺序取组会错位（14263 形 S0==1/2/4/5）。
		// The gate-to-block mapping is keyed by the counting row (a hunt step's absolute index is
		// its source row = the client S0 value): interleaved EnterArea steps also occupy rows, so
		// ordinal group consumption would misalign (the 14263 shape S0==1/2/4/5).
		Map<Integer, List<RetailClientHuntProgressRows.Row>> huntGroupsByRow = new java.util.TreeMap<>();
		for (List<RetailClientHuntProgressRows.Row> group : huntGroups) {
			huntGroupsByRow.put(group.getFirst().ladderRow(), group);
		}
		for (List<RetailClientHuntProgressRows.Row> group : huntGroups) {
			if (group.size() > 1 && group.stream().map(RetailClientHuntProgressRows.Row::section).distinct().count()
					!= group.size()) {
				return new Outcome(null, "RETAIL_HUNT_PROGRESS_SHAPE",
					"quest " + entry.questId() + " shares a counter section inside one group");
			}
			for (RetailClientHuntProgressRows.Row row : group) {
				if (row.section() < 1 || row.section() > 5) {
					return new Outcome(null, "RETAIL_HUNT_PROGRESS_SHAPE",
						"section=" + row.section() + " outside SECTION_1..5");
				}
			}
		}
		// 客户端契约声明的 select 页族必须覆盖全部**可见步**（talk + collectitem；hunt/EA/EW/
		// itemplay/talkfobj/pvp 占行不占段）：声明族不足即如实拒绝，不按逐任务页梯发明页；段内翻页
		// （selectN_m）由客户端本地完成，服务端不再消费逐页路由。
		// The select families the client contract declares must cover every visible step (talk plus
		// collectitem; hunt/EA/EW/itemplay/talkfobj/pvp own a row but no stage): too few declared
		// families reject honestly instead of inventing pages from a per-quest ladder, and in-stage
		// page turns stay client-local.
		int visibleSteps = talks.size() + collects.size();
		int declaredFamilies = RetailQuestDialogPages.familyCount(entry.questId());
		if (declaredFamilies < visibleSteps) {
			return new Outcome(null, "RETAIL_TALK_HUNT_CHAIN_DEFERRED",
				"declaredFamilies=" + declaredFamilies + " visible=" + visibleSteps);
		}
		// 采集检查步必须有真端交付物整组（采集族同判据）。
		// A collect check step requires the retail item-requirement set (collect-family rule).
		if (collects.size() > 0 && metadata.metadata().itemRequirements().isEmpty()) {
			return new Outcome(null, "RETAIL_COLLECT_ITEM_SHAPE", "真端交付物为空");
		}
		int lastRow = summaryRows.lastRowIndex(entry.questId());
		// 领奖行 0 会与 started 投影撞包（DUPLICATE_NODE_PROJECTION），如实拒绝。
		// A reward row of 0 would collide with the started projection; reject honestly.
		if (lastRow <= 0) {
			return new Outcome(null, "RETAIL_TALK_HUNT_CHAIN_DEFERRED", "lastRow=0");
		}
		try {
			// EnterWorld 接取行：acquireParam = 世界 id（数字）；其余行世界 id = 0（NPC 接取）。
			// EnterWorld-acquired rows: acquireParam is the numeric world id; otherwise 0 (npc
			// acquire).
			int worldAcquireId = 0;
			if ("enterworld".equalsIgnoreCase(entry.acquireCategory())
					&& entry.acquireParam() != null
					&& entry.acquireParam().trim().chars().allMatch(Character::isDigit)) {
				worldAcquireId = Integer.parseInt(entry.acquireParam().trim());
			}
				QuestDefinition definition = build(entry.questId(), steps, entry.stepCutscenes(), npcIndex,
					metadata.metadata(),
				acquiredNpcs, rewardNpc,
				interactionObjects, huntGroupsByRow,
				worldAcquireId, "enterarea".equalsIgnoreCase(entry.acquireCategory())
					|| "none".equalsIgnoreCase(entry.acquireCategory()), levelUpAcquire,
				lastRow, selectNoneLadder, isDropDrivenFobjCollect(entry, metadata));
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RetailNameResolutionException e) {
			// 未登记模板（交互物体/副本后缀名等）不属于编译缺陷，按稳定拒绝码留在 XML。
			// Unregistered templates (interactive objects / instance-suffixed names) are not
			// compiler defects; reject with the stable code and keep the row on XML.
			return new Outcome(null, e.code(), e.monster());
		} catch (RuntimeException e) {
			// -Dretail.talkHunt.debug=true 重掷原始栈（诊断用）。
			// -Dretail.talkHunt.debug=true rethrows the raw stack for diagnosis.
			if (Boolean.getBoolean("retail.talkHunt.debug")) {
				throw e;
			}
			return new Outcome(null, "COMPILATION_FAILED", e.toString());
		}
	}

	/** 一个 hunt 块内的一行：击杀族（显示名族闭包）+ 客户端计数段 + 计数。 */
	/** One row inside a hunt block: the kill family (display-name closure) plus the client
	 * counter section and count. */
	private record BlockRow(Set<Integer> family, int section, int count) {
	}

	/**
	 * 解析一个 hunt 块：真端名单可解析时按名连接客户端进度行（同名多行 = 并行段，18990 形）；
	 * 名单不解析（Q 后缀改名模板）且客户端行可解析时由客户端行名单取代（15306 形）；无登记行
	 * 退回真端名单单 SECTION_1 计数（15101 形）。
	 * Resolves one hunt block: with a resolvable retail monster list, join the client progress
	 * rows by name (same-name multi-row = parallel sections, the 18990 form); when the retail list
	 * fails to resolve (Q-suffixed renamed templates) and the client rows resolve, the client row
	 * names supersede (the 15306 form); with no registered rows fall back to the retail list on a
	 * single SECTION_1 counter (the 15101 form).
	 */
	private static List<BlockRow> resolveHuntBlock(RetailNpcNameIndex npcIndex,
			List<RetailDataDrivenTable.HuntStage> stages, List<RetailClientHuntProgressRows.Row> group) {
		List<String> stageMonsters = stages.stream()
			.flatMap(stage -> stage.monsters().stream())
			.toList();
		int stageCount = stages.stream().mapToInt(RetailDataDrivenTable.HuntStage::count).sum();
		if (group.isEmpty()) {
			if (stages.size() > 1) {
				// 多段块没有客户端进度行组：段间关系（并行/顺序）无法校准，如实拒绝。
				// A multi-segment block without client progress rows: the inter-segment relation
				// (parallel vs sequential) cannot be calibrated; reject honestly.
				throw new RetailNameResolutionException("RETAIL_HUNT_PROGRESS_SHAPE", stageMonsters.getFirst());
			}
			return List.of(new BlockRow(resolveFamily(npcIndex, stageMonsters), 1, stageCount));
		}
		Set<String> stageNames = new LinkedHashSet<>();
		stageMonsters.forEach(name -> stageNames.add(name.toLowerCase(java.util.Locale.ROOT)));
		Set<String> rowNames = new LinkedHashSet<>();
		group.forEach(row -> row.monsters().forEach(name -> rowNames.add(name.toLowerCase(java.util.Locale.ROOT))));
		if (rowNames.equals(stageNames)) {
			// 并行目标：每行一段（18990 形）。 / Parallel targets: one section per row (the 18990 form).
			List<BlockRow> block = new ArrayList<>(group.size());
			for (RetailClientHuntProgressRows.Row row : group) {
				block.add(new BlockRow(resolveFamily(npcIndex, row.monsters()), row.section(), row.count()));
			}
			return List.copyOf(block);
		}
		int section = group.getFirst().section();
		boolean shared = group.stream().allMatch(row -> row.section() == section);
		if (!shared) {
			// 家族代表形（18996 形）：真端块每段只列**一个**成员名（`IDLF1_T_Barricade_Dragon_01 1;
			// IDLF1_T_Barricade_Dragon_03 1;`），客户端同一门组的行列的是该段所属**家族全表**
			// （SECTION_1 = 01/02/04/05/07/09，SECTION_2 = 03/06/08/10）——段数 == 行数且每段名恰好
			// 落在唯一一行名单里时按家族行取段号与计数（客户端任务书计数权威；真端名是家族成员，
			// 击杀真端名同样被客户端行计入）。非此形状（段数不符/落点不唯一/无包含关系）保持拒。
			// The family-representative shape (the 18996 shape): the retail block lists a single member
			// per segment while the client's rows for that gate list the whole family of the segment
			// (SECTION_1 = 01/02/04/05/07/09, SECTION_2 = 03/06/08/10). When the segment count equals
			// the row count and every segment name sits in exactly one row's list, the row supplies the
			// section and count (the client journal is the counting authority, and the retail name is a
			// family member so its kills are counted by that very client row). Any other shape stays
			// rejected.
			if (stages.size() == group.size()) {
				List<BlockRow> block = new ArrayList<>(group.size());
				Set<Integer> usedRows = new java.util.HashSet<>();
				boolean matched = true;
				for (RetailDataDrivenTable.HuntStage stage : stages) {
					int hit = -1;
					for (int index = 0; index < group.size(); index++) {
						RetailClientHuntProgressRows.Row row = group.get(index);
						boolean contained = stage.monsters().stream().allMatch(name -> row.monsters().stream()
							.anyMatch(rowName -> rowName.equalsIgnoreCase(name)));
						if (contained && !usedRows.contains(index)) {
							if (hit >= 0) {
								hit = -1;
								break;
							}
							hit = index;
						}
					}
					if (hit < 0) {
						matched = false;
						break;
					}
					usedRows.add(hit);
					RetailClientHuntProgressRows.Row row = group.get(hit);
					block.add(new BlockRow(resolveFamily(npcIndex, row.monsters()), row.section(), row.count()));
				}
				if (matched) {
					return List.copyOf(block);
				}
			}
			throw new RetailNameResolutionException("RETAIL_HUNT_PROGRESS_SHAPE", stageMonsters.getFirst());
		}
		try {
			// 名单歧义但段唯一：保持真端名单解析，段号取客户端（15101 族判例）。
			// Ambiguous names but a unique section: keep the retail list, take the section from the
			// client (the 15101-family rule).
			return List.of(new BlockRow(resolveFamily(npcIndex, stageMonsters), section, stageCount));
		} catch (RetailNameResolutionException e) {
			// 真端名单不可解析（Q 后缀改名模板）：客户端行名单取代（15306 形）。
			// The retail list is unresolvable (Q-suffixed renamed templates): the client row names
			// supersede (the 15306 form).
			List<String> clientNames = group.stream()
				.flatMap(row -> row.monsters().stream())
				.toList();
			return List.of(new BlockRow(resolveFamily(npcIndex, clientNames), section, stageCount));
		}
	}

	/** 显示名族闭包解析；任何名字不可解析即抛稳定拒绝码。 */
	/** Display-name family closure resolution; any unresolvable name throws the stable code. */
	private static Set<Integer> resolveFamily(RetailNpcNameIndex npcIndex, List<String> monsters) {
		Set<Integer> family = new LinkedHashSet<>();
		for (String monster : monsters) {
			Set<Integer> ids = npcIndex.withDisplayNameVariants(npcIndex.resolveAll(List.of(monster)).npcIds());
			if (ids.isEmpty()) {
				throw new RetailNameResolutionException("RETAIL_MONSTER_UNRESOLVED", monster);
			}
			family.addAll(ids);
		}
		return family;
	}

	private static QuestDefinition build(int questId, List<Step> steps, Map<Integer, Integer> stepCutscenes,
			RetailNpcNameIndex npcIndex,
			QuestMetadata metadata, java.util.Set<Integer> acquiredNpcs, int rewardNpc,
			RetailQuestUseItemNpcs interactionObjects,
			Map<Integer, List<RetailClientHuntProgressRows.Row>> huntGroupsByRow, int worldAcquireId,
			boolean areaAcquire, boolean levelUpAcquire, int lastRow, boolean selectNoneLadder,
		boolean dropDrivenFobj) {
		// 解析 talk/collect NPC 与 hunt 块（块 = 单行共享 SECTION_1 计数或客户端进度行的并行段）。
		// Resolve talk/collect npcs and hunt blocks (a block is either one shared-SECTION_1 row or
		// the client progress rows' parallel sections).
		List<Integer> talkNpcIds = new ArrayList<>();
		List<Integer> collectNpcIds = new ArrayList<>();
		for (Step step : steps) {
			if (step.isTalk()) {
				Set<Integer> ids = npcIndex.resolveAll(List.of(step.talkNpc())).npcIds();
				if (ids.size() != 1) {
					throw new RetailNameResolutionException("RETAIL_TALK_NPC_UNRESOLVED", step.talkNpc());
				}
				talkNpcIds.add(ids.iterator().next());
			} else if (step.isCollect()) {
				Set<Integer> ids = npcIndex.resolveAll(List.of(step.collectNpc())).npcIds();
				if (ids.size() != 1) {
					throw new RetailNameResolutionException("RETAIL_TALK_NPC_UNRESOLVED", step.collectNpc());
				}
				collectNpcIds.add(ids.iterator().next());
			}
		}
		List<List<BlockRow>> huntBlocks = new ArrayList<>();
		for (int index = 0; index < steps.size(); index++) {
			Step step = steps.get(index);
			if (!step.isHunt()) {
				continue;
			}
			huntBlocks.add(resolveHuntBlock(npcIndex, step.huntBlock(),
				huntGroupsByRow.getOrDefault(index, List.of())));
		}

		// 客户端对齐布局：var0 = 行阶梯（SECTION_0）；每个用到的计数段一个 6 位字段（SECTION_m）。
		// Client-aligned layout: var0 = the row ladder (SECTION_0); one 6-bit field per used
		// counter section (SECTION_m).
		boolean hasHunt = !huntBlocks.isEmpty();
		ProgressLayout.Builder layoutBuilder = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
		Set<Integer> sections = new java.util.TreeSet<>();
		huntBlocks.forEach(block -> block.forEach(row -> sections.add(row.section())));
		// ItemPlay 多播计数段：取未被 hunt 段占用的最小段号（15042 形：单 itemplay 多播占 SECTION_1；
		// 客户端步文本 [%2]/3 计数背书——hunt 行表无该步时以客户端步计数为准）。
		// ItemPlay multi-play counter sections: the lowest section not claimed by hunt blocks (the
		// 15042 shape: one multi-play itemplay takes SECTION_1); the client step text backs the
		// [%2]/3 counter when the hunt-row table has no entry for the step.
		java.util.Map<Integer, Integer> itemPlayCounterSections = new java.util.HashMap<>();
		int counterSectionCandidate = 1;
		for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
			Step step = steps.get(stepIndex);
			if (!step.isItemPlay() || step.itemPlay().count() <= 1) {
				continue;
			}
			while (sections.contains(counterSectionCandidate)) {
				counterSectionCandidate++;
			}
			if (counterSectionCandidate > 5) {
				throw new IllegalStateException(
					"itemplay counter sections exhausted (SECTION_1..5): quest " + questId);
			}
			itemPlayCounterSections.put(stepIndex, counterSectionCandidate);
			sections.add(counterSectionCandidate);
		}
		// 链内 PVP 计数段（80846 pvp+collectitem / 15673 enterarea+pvp）：与多播 itemplay 同判据取
		// 未被占用的最小段号——客户端行索引必须留在 SECTION_0、计数只能放 SECTION_1+（QE-012），
		// 且客户端任务书无 PVP 需求行（quest_monster.csv 无 pvp 源类型），段号由链内计数惯例裁定。
		// In-chain PVP counter sections (80846 pvp+collectitem, 15673 enterarea+pvp): the same
		// lowest-free-section rule as the multi-play itemplay — the client row index must stay in
		// SECTION_0 and counters may only live in SECTION_1+ (QE-012), and the client journal carries
		// no PVP requirement row (no pvp source type in quest_monster.csv), so the section follows the
		// in-chain counter convention.
		java.util.Map<Integer, Integer> pvpCounterSections = new java.util.HashMap<>();
		for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
			Step step = steps.get(stepIndex);
			if (!step.isPvp()) {
				continue;
			}
			while (sections.contains(counterSectionCandidate)) {
				counterSectionCandidate++;
			}
			if (counterSectionCandidate > 5) {
				throw new IllegalStateException(
					"pvp counter sections exhausted (SECTION_1..5): quest " + questId);
			}
			pvpCounterSections.put(stepIndex, counterSectionCandidate);
			sections.add(counterSectionCandidate);
		}
		for (int section : sections) {
			layoutBuilder.add(new BitField("var" + section, RetailHuntCounterLayout.shiftFor(section + 1),
				RetailHuntCounterLayout.SECTION_BITS, 0, RetailHuntCounterLayout.SECTION_MASK,
				PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
		}
		ProgressLayout layout = layoutBuilder.build();

		// 行态：started = 行 0，s{k} = 行 k（第 k 步落点）；末步落点 = 领奖行（lastRow）。
		// Row states: started = row 0, s{k} = row k (step k's landing); the final step lands on
		// the reward row (lastRow).
		int stepCount = steps.size();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> zeroFrozen = Map.copyOf(zero);
		boolean finalIsHunt = !steps.get(stepCount - 1).isTalk() && !steps.get(stepCount - 1).isCollect();
		Map<String, Integer> rewardRow = Map.of("var0", lastRow);
		if (hasHunt && finalIsHunt) {
			// 末段为 hunt：领奖投影携带末段各段满计数（QE-051）。
			// Final hunt: the reward projection carries each final section's full count (QE-051).
			Map<String, Integer> reward = new LinkedHashMap<>(rewardRow);
			huntBlocks.getLast().forEach(row -> reward.merge("var" + row.section(), row.count(), Integer::sum));
			rewardRow = Map.copyOf(reward);
		}
		if (finalIsHunt && steps.get(stepCount - 1).isPvp()) {
			// 末步为 PVP 计数：领奖投影同样携带满计数（QE-051；目标投影对动作未触及的字段是权威的，
			// 落点即把计数写成满值——与末段 hunt 同判据）。
			// Final PVP counter: the reward projection carries the full count as well (QE-051; the
			// target projection is authoritative for fields the actions did not touch, so landing
			// writes the full count — the same rule as the final hunt block).
			Map<String, Integer> reward = new LinkedHashMap<>(rewardRow);
			reward.merge("var" + pvpCounterSections.get(stepCount - 1),
				steps.get(stepCount - 1).pvp().count(), Integer::sum);
			rewardRow = Map.copyOf(reward);
		}
		List<QuestNode> nodes = new ArrayList<>(stepCount + 3);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zeroFrozen)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zeroFrozen)));
		for (int row = 1; row < stepCount; row++) {
			nodes.add(new QuestNode("s" + row, new NodeProjection(QuestStatus.START, Map.of("var0", row))));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		Map<String, Integer> completeZero = new LinkedHashMap<>(zero);
		sections.forEach(section -> completeZero.put("var" + section, 0));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE,
			Map.copyOf(completeZero))));
		// ItemPlay 链的道具接取授予映射（与接取形无关：NPC 接取挂接取路由，LevelUpLogIn 挂
		// 升级发放边；真端 work item 接取即发，与 planner 的完成/放弃清理对称；共享物品只发一次）。
		// The itemplay accept-grant map (acquire-form independent: npc acquires hang it on the
		// accept routes, LevelUpLogIn on the level-up grant edge; the retail work item is granted
		// on acceptance, symmetric with the planner's completion and abandon cleanup; repeated
		// plays of the same item grant once).
		Map<Integer, Integer> acceptGrants = new java.util.LinkedHashMap<>();
		for (Step step : steps) {
			if (step.isItemPlay()) {
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
		}
		List<QuestTransition> transitions = new ArrayList<>();
		// 接取 NPC 变体家族（18738 形：前缀名展开 _A.._F 六变体）：每个变体各建一套接取路由，
		// 授予随 canonical 三参重载挂到每套接取提交边（遗留形：六变体各授炸弹 ×10）。
		// Acquire-variant family (the 18738 shape): each variant gets its own accept routes, with the
		// grants riding the canonical commit edges (the legacy shape grants the bombs ×10 per variant).
		if (!acquiredNpcs.isEmpty()) {
			// 接取规范形（P0-2 DD 尾片）：页 4 直发，select1/select_none 阶梯与 1007 中转随页链删除。
			// Canonical accept (the DD tail slice): page 4 directly; the letter ladders and the 1007 hop
			// are gone.
			List<QuestAction> acceptActions = new ArrayList<>(acceptGrants.size());
			acceptGrants.forEach((itemId, count) -> acceptActions.add(new QuestAction.GiveItem(itemId, count)));
			for (int acquireVariant : acquiredNpcs) {
				transitions.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquireVariant,
					"started", List.copyOf(acceptActions)));
			}
		} else if (levelUpAcquire) {
			// LevelUpLogIn 接取（P5-5，遗留 13830 形）：升级事件自动接取，StartEligible 承载
			// 等级/前置门（真端 minlevel_permitted/prerequisites 元数据）；ItemPlay 道具随接取
			// 同事务授予（遗留 give-item 与 level-up 同边）。
			// LevelUpLogIn acquire (P5-5, the legacy 13830 shape): the level-up event auto-grants
			// with StartEligible carrying the level and prerequisite gates (the retail minlevel and
			// prerequisites metadata); the itemplay item grants in the same transaction (the legacy
			// give-item riding the level-up edge).
			List<QuestAction> grantActions = new ArrayList<>(acceptGrants.size());
			acceptGrants.forEach((itemId, count) -> grantActions.add(new QuestAction.GiveItem(itemId, count)));
			// LevelUpLogIn 的 LogIn 半边：登录进世界同样自动接取（遗留 <enter-world/> 同边同事务）。
			// The LogIn half of LevelUpLogIn: world entry auto-grants too (the legacy enter-world
			// edge, same transaction).
			for (QuestEvent grantEvent : new QuestEvent[] {new QuestEvent.LevelUp(),
					new QuestEvent.EnterWorld()}) {
				transitions.add(new QuestTransition(grantEvent,
					List.of(new QuestCondition.StartEligible()), List.copyOf(grantActions), "started",
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
					null, "unaccepted"));
			}
		} else if (areaAcquire) {
			// EnterArea 接取（副本任务，遗留 16823 形）：升级 + 区域任务结束事件自动接取，
			// 前置任务（真端元数据 prerequisites）以 QuestsFinished 门禁；itemplay 骑行者（10503 形：
			// 区域收尾 + 演出步）的道具与 levelUpAcquire 同判据接取即发，否则演出步无物可用。
			// EnterArea acquire (instance quests, the legacy 16823 shape): level-up and zone-mission-
			// end events auto-grant, gated by the retail metadata prerequisites (QuestsFinished); the
			// itemplay rider's item (the 10503 area-tail shape) grants on acceptance exactly like the
			// levelUpAcquire branch, otherwise the play step has nothing to consume.
			List<QuestCondition> grantGate = new ArrayList<>(2);
			grantGate.add(new QuestCondition.StartEligible());
			if (!metadata.prerequisites().isEmpty()) {
				grantGate.add(new QuestCondition.QuestsFinished(metadata.prerequisites()));
			}
			List<QuestAction> grantActions = new ArrayList<>(acceptGrants.size());
			acceptGrants.forEach((itemId, count) -> grantActions.add(new QuestAction.GiveItem(itemId, count)));
			for (QuestEvent grantEvent : new QuestEvent[] {new QuestEvent.LevelUp(),
					new QuestEvent.ZoneMissionEnd()}) {
				transitions.add(new QuestTransition(grantEvent, List.copyOf(grantGate), List.copyOf(grantActions),
					"started",
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
					null, "unaccepted"));
			}
		} else {
			// EnterWorld 接取（副本任务）：无接取 NPC，进世界事件 + 世界 id 条件直接进 START
			// （遗留 10010 形；世界 id 来自 DD acquireParam）。
			// EnterWorld acquire (instance quests): no acquire npc — the world-enter event with the
			// world-id condition lands START directly (the legacy 10010 shape; world id from the DD
			// acquireParam).
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.WorldIs(worldAcquireId, true)), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
				null, "unaccepted"));
		}

		// 各步的源行态与落点行值：步 k 源 = 行 k、落点 = 行 k+1；末步落点 = 领奖（lastRow）。
		// Step k sources from row k and lands on row k+1; the final step lands on reward (lastRow).
		int huntOrdinalCursor = 0;
		int talkOrdinalCursor = 0;
		int collectOrdinalCursor = 0;
		// 步序 → 段序映射：hunt 步不占客户端对话段，非 hunt 步按序消费登记段（PVP 计数步同判据：
			// 客户端任务书 PVP 行不占对话段，只占任务书行）。
		// Step index to stage index: hunts own no client dialog stage, so non-hunt steps consume
		// the registry stages in order (PVP counters alike: the client's PVP journal row owns no
		// dialog stage, only a journal row).
		int[] stageIndexOf = new int[stepCount];
		int visibleCursor = 0;
		for (int index = 0; index < stepCount; index++) {
			Step step = steps.get(index);
			stageIndexOf[index] = visibleCursor;
			if (step.isHunt() || step.isEnterArea() || step.isEnterWorld() || step.isItemPlay()
					|| step.isTalkFobj() || step.isPvp()) {
				continue;
			}
			visibleCursor++;
		}
		// 末段 talk 的段下标（末段收下客户端页尾的三种收尾按钮；行末若为 collect 段，本值属于它之前
		// 的最后一个 talk 段，与 collect 段互不影响）。
		// The stage index of the last talk stage (it accepts the client tail page's three terminal
		// buttons; when the row ends with a collect stage the value belongs to the last talk stage
		// before it, so the two never overlap).
		int lastTalkVisibleIndex = -1;
		for (int index = 0; index < stepCount; index++) {
			if (steps.get(index).isTalk()) {
				lastTalkVisibleIndex = stageIndexOf[index];
			}
		}
		for (int index = 0; index < stepCount; index++) {
			Step step = steps.get(index);
			boolean finalStep = index == stepCount - 1;
			String source = index == 0 ? "started" : "s" + index;
			int landingRow = finalStep ? lastRow : index + 1;
			String target = finalStep ? "reward" : "s" + (index + 1);
			int npc = step.isTalk() ? talkNpcIds.get(talkOrdinalCursor)
				: step.isCollect() ? collectNpcIds.get(collectOrdinalCursor) : 0;
			// hunt/骑行者步不占对话段：对话段只挂在 talk/collect 步上。
			// Hunts and riders own no dialog stage: only talk/collect steps carry one.
			int stageIndex = stageIndexOf[index];
			RetailQuestDialogPages.StagePage stagePage = step.isCollect() || step.isTalk()
				? RetailQuestDialogPages.stage(questId, ACQUIRE_PAGE_FAMILIES, stageIndex)
					.orElseThrow(() -> new IllegalStateException("missing client stage page for retail "
						+ "talk/hunt chain quest " + questId + " stage " + stageIndex))
				: null;
			if (stagePage != null) {
				// 段首屏按客户端契约声明的第 stageIndex 个 select 页族取页（页族号可跳号）。
				// 段首页回声用页 id 原值（客户端翻页/回跳以页 id 作为 dialog 动作回传；页 id 不必是
				// 枚举动作）；段内翻页由客户端本地完成，服务端不再建逐页路由。
				// The stage head is the stageIndex-th select family the client contract declares (family
				// numbers may skip). The head echo uses the raw page id (the client sends page ids back as
				// dialog actions, and a page id need not be an enum action); in-stage page turns stay
				// client-local, so no per-page route is built.
				int headPage = stagePage.headPageId();
				transitions.add(RetailSimpleHuntDefinitionCompiler.talk(npc, QuestDialogAction.QUEST_SELECT,
					source, source, null, List.of(new AfterCommitAction.ShowQuestDialog(headPage))));
				transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(npc, headPage),
					List.of(), List.of(), source,
					List.of(new AfterCommitAction.ShowQuestDialog(headPage)), null, source));
				if (index > 0) {
					transitions.add(finishTalk(npc, source));
				}
			}
			if (step.isEnterArea()) {
				// EnterArea 步：进入区域推进（遗留 <enter-zone zone="别名"/> 同形），无计数无对话段；
				// 区域别名来自 DD value0，客户端 HTML 无 CutScene 声明时不附过场。
				// EnterArea step: entering the area advances the row (the legacy enter-zone shape) —
				// no counter, no dialog stage; the alias comes from the DD value0 and no movie is
				// attached without a client CutScene declaration.
				List<AfterCommitAction> afterCommit = new ArrayList<>(2);
				afterCommit.add(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
				if (stepCutscenes.containsKey(index)) {
					afterCommit.add(new AfterCommitAction.PlayMovie(stepCutscenes.get(index)));
				}
				transitions.add(new QuestTransition(new QuestEvent.EnterZone(step.enterArea()),
					List.of(), List.of(new QuestAction.SetVariable("var0", landingRow)), target,
					List.copyOf(afterCommit),
					null, source));
			} else if (step.isEnterWorld()) {
				// EnterWorld 步：进入指定世界推进（遗留 `<enter-world/>` + `<world-is world-id>` 同形，
				// 16835 形），无计数无对话段；世界 id 来自 DD value0（负数 = 值解析失败，稳定拒绝）。
				// EnterWorld step: entering the named world advances the row (the legacy enter-world
				// event with its world-is condition, the 16835 shape) — no counter, no dialog stage;
				// the world id comes from the DD value0 (a negative id means the value failed to
				// parse and the row is rejected with a stable code).
				if (step.enterWorld() <= 0) {
					throw new IllegalStateException("enterworld step has no world id: " + step.enterWorld());
				}
				transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
					List.of(new QuestCondition.WorldIs(step.enterWorld(), true)),
					List.of(new QuestAction.SetVariable("var0", landingRow)), target,
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					null, source));
			} else if (step.isItemPlay()) {
				// ItemPlay 步：使用道具演出推进（遗留 <item-play> 同形），无对话段；时长取
				// 遗留证据标准 3000ms（80978/17505/20035/14153 全为 3000）；路由声明本身武装引擎的
				// 定时演出派发（QuestEventIndex.itemPlayAnimationMillis）。
				// ItemPlay step: using the item plays and advances the row (the legacy item-play
				// shape) — no dialog stage; the duration is the legacy-evidence standard 3000ms
				// (80978/17505/20035/14153 all use 3000), and the route declaration itself arms the
				// engine's timed play dispatch (QuestEventIndex.itemPlayAnimationMillis).
				if (step.itemPlay().count() <= 1) {
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
					continue;
				}
				// 多播形（15042 客户端 [%2]/3）：hunt 计数对形复用——完成边（priority 0，段计数达标
				// 推进行阶梯；非末步清段计数）+ 自环（priority 1，未达标递增段计数）。
				// Multi-play shape (15042's client [%2]/3): reuse the hunt counting edge pair — the
				// completing edge (priority 0) advances the row ladder once the counter fills and
				// resets it on non-final steps, the self loop (priority 1) increments while short.
				int counterSection = itemPlayCounterSections.get(index);
				List<QuestCondition> completingGate = List.of(new QuestCondition.VariableAtLeast(
					"var" + counterSection, step.itemPlay().count() - 1));
				List<QuestAction> completing = new ArrayList<>(5);
				if (step.itemPlay().outputItemId() > 0) {
					completing.add(new QuestAction.GiveItem(step.itemPlay().outputItemId(),
						step.itemPlay().outputCount()));
					completing.add(new QuestAction.RemoveItem(step.itemPlay().itemId(), 1));
				}
				completing.add(new QuestAction.SetVariable("var0", landingRow));
				if (!finalStep || step.itemPlay().outputItemId() > 0) {
					completing.add(new QuestAction.SetVariable("var" + counterSection, 0));
				}
				transitions.add(new QuestTransition(
					new QuestEvent.ItemPlay(step.itemPlay().itemId(), ITEM_PLAY_MILLIS),
					completingGate, List.copyOf(completing), target,
					List.of(new AfterCommitAction.SyncQuestState(finalStep
						&& step.itemPlay().outputItemId() > 0
						? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY)),
					0, source));
				transitions.add(new QuestTransition(
					new QuestEvent.ItemPlay(step.itemPlay().itemId(), ITEM_PLAY_MILLIS),
					List.of(new QuestCondition.VariableBelow("var" + counterSection,
						step.itemPlay().count() - 1)),
					List.of(new QuestAction.IncrementVariable("var" + counterSection, 1)), source,
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					1, source));
			} else if (step.isTalkFobj()) {
				// TalkFOBJ 步（15601 形）：FOBJ 模板 USE_OBJECT 交互推进（遗留 TalkToNpc(703135,
				// USE_OBJECT) 同形），无页梯；value2 凭证声明 → 本步授予任务 work item
				// （遗留 give-item ×1，映射到本任务 work_items 首项）。
				// TalkFOBJ step (the 15601 shape): the fobj template's USE_OBJECT interaction
				// advances the row (the legacy TalkToNpc(703135, USE_OBJECT) shape) with no page
				// ladder; a declared value2 credential grants this quest's work item in-step (the
				// legacy give-item ×1, mapped to this quest's first work item).
				java.util.Set<Integer> fobjIds = npcIndex.resolve(step.talkFobj().npcName());
				if (fobjIds.size() != 1) {
					throw new IllegalStateException("talkfobj npc unresolved: "
						+ step.talkFobj().npcName() + " -> " + fobjIds);
				}
				int fobjNpc = fobjIds.iterator().next();
				if (dropDrivenFobj) {
					// 掉落驱动的 FOBJ 采集步（25052 形）：交互 = 从该对象**抽取采集物**（真端
					// drop_monster/drop_item 声明、掉落表按 collect_progress 发放），行态不推进；
					// 交付合同见 fobjCollectHandIn。本步**不另发边**：下方「掉落源自环」循环
					//（drop.collectingStep == 步号；collectingStep=0 即首行 started）已按
					// QuestInteractionObjectValidator 的 START 态合同发出 TalkToNpc 通配 + CAN_ACT
					// 自环对。
					// Drop-driven FOBJ collect step (the 25052 shape): the interaction extracts the
					// collected item (the retail drop_monster/drop_item declaration, granted by the
					// drop table per collect_progress) and never advances the row; the hand-in
					// contract lives in fobjCollectHandIn. No edge is emitted here — the drop-source
					// self-loop pass below (drop.collectingStep == step index; collectingStep 0 means
					// the first row, started) already emits the wildcard TalkToNpc plus CAN_ACT pair
					// that the START-state validator contract requires.
				} else {
					List<QuestAction> fobjActions = new ArrayList<>(2);
					fobjActions.add(new QuestAction.SetVariable("var0", landingRow));
					if (step.talkFobj().grantsWorkItem()) {
						for (QuestItemRequirement workItem : metadata.questWorkItems()) {
							fobjActions.add(new QuestAction.GiveItem(workItem.itemId(), workItem.count()));
						}
					}
					transitions.add(new QuestTransition(
						new QuestEvent.TalkToNpc(fobjNpc, QuestDialogAction.USE_OBJECT.id()),
						List.of(), List.copyOf(fobjActions), target,
						List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						null, source));
				}
			} else if (step.isCollect()) {
				// 落点步 NPC：39 边落在下一步后，ok 页（10000）的 FINISH_DIALOG 按钮仍由检查 NPC
				// 收发；落点步换人（或落点是 hunt 等无对话段）时必须补该出口，否则客户端 ok 页
				// 按钮无路由（遗留 s2→s2 FINISH_DIALOG npc=检查 NPC 逐字同形）。
				// Landing-step npc: after the 39 edge lands on the next step the ok page's
				// FINISH_DIALOG button is still served by the check npc, so the close route must be
				// added whenever the landing step uses another npc (or owns no dialog stage at all,
				// e.g. a hunt step) — the legacy s2 -> s2 FINISH_DIALOG with the check npc.
				int landedNpc = 0;
				if (!finalStep) {
					Step next = steps.get(index + 1);
					if (next.isTalk()) {
						landedNpc = talkNpcIds.get(talkOrdinalCursor);
					} else if (next.isCollect()) {
						landedNpc = collectNpcIds.get(collectOrdinalCursor + 1);
					}
				}
				transitions.addAll(collectCheck(metadata, npc, source, target, landingRow, finalStep,
					landedNpc));
				collectOrdinalCursor++;
			} else if (step.isTalk()) {
				// talk 推进：行 +1；末步授予任务凭证（15314 形）。推进按钮 = 该页族尾的按钮（中间段
				// SETPRO{K}；末 talk 段收下客户端末段页尾三种收尾按钮）；段尾过场来自客户端 HTML 的
				// CutScene 常量账（不再消费逐任务页梯的 movie 列）。
				// The talk advance bumps the row; the final step grants the work items. The advance
				// buttons are the family tail's buttons (SETPRO{K} mid-chain; the client's three terminal
				// tail buttons on the last talk stage); the tail cutscene comes from the client HTML
				// CutScene ledger (the ladder registry's movie column is retired).
				List<AfterCommitAction> advance = new ArrayList<>(4);
				advance.add(new AfterCommitAction.SyncQuestState(finalStep
					? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY));
				Integer stageMovie = RetailChainCutscenes.movieId(questId, stageIndex);
				if (stageMovie != null) {
					advance.add(new AfterCommitAction.PlayMovie(stageMovie, QuestMovieType.CUTSCENE));
				}
				advance.add(finalStep
					? new AfterCommitAction.CloseDialog()
					: new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
				List<QuestAction> actions = new ArrayList<>(2);
				actions.add(new QuestAction.SetVariable("var0", landingRow));
				if (finalStep) {
					for (QuestItemRequirement workItem : metadata.questWorkItems()) {
						actions.add(new QuestAction.GiveItem(workItem.itemId(), workItem.count()));
					}
				}
				for (int advanceAction : RetailQuestDialogPages.advanceActions(stagePage,
						stageIndex == lastTalkVisibleIndex)) {
					transitions.add(new QuestTransition(
						new QuestEvent.TalkToNpc(npc, advanceAction), List.of(), List.copyOf(actions),
						target, List.copyOf(advance), null, source));
				}
				talkOrdinalCursor++;
			} else if (step.isHunt()) {
				// hunt 步条件边形：单行块 = 旧 XML 同形（count-1 语义）；多行块（并行目标）每行
				// 一段计数，完成边检查**其余行**已满（kills-done 事后值；最后补齐的那一杀触发推进），
				// 自身行继续边在未满时递增。
				// Hunt conditional edge pair: a single-row block keeps the legacy (count-1) shape; a
				// multi-row (parallel-targets) block gives each row its own section, the completing
				// edge checks the OTHER rows' post values (the last filling kill advances the row),
				// and the own-row continuing edge increments while below its count.
				List<BlockRow> block = huntBlocks.get(huntOrdinalCursor);
				boolean parallel = block.size() > 1;
				Set<Integer> blockSections = new java.util.TreeSet<>();
				block.forEach(row -> blockSections.add(row.section()));
				List<AfterCommitAction> killAfterCommit = new ArrayList<>(2);
				killAfterCommit.add(new AfterCommitAction.SyncQuestState(finalStep
					&& stepCutscenes.containsKey(index)
					? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY));
				if (stepCutscenes.containsKey(index)) {
					killAfterCommit.add(new AfterCommitAction.PlayMovie(stepCutscenes.get(index)));
				}
				for (int rowIndex = 0; rowIndex < block.size(); rowIndex++) {
					BlockRow row = block.get(rowIndex);
					List<QuestCondition> completingGate;
					if (parallel) {
						completingGate = new ArrayList<>(block.size() - 1);
						for (int otherIndex = 0; otherIndex < block.size(); otherIndex++) {
							if (otherIndex == rowIndex) {
								continue;
							}
							BlockRow other = block.get(otherIndex);
							completingGate.add(new QuestCondition.VariableAtLeast(
								"var" + other.section(), other.count()));
						}
					} else {
						completingGate = List.of(new QuestCondition.VariableAtLeast("var" + row.section(),
							row.count() - 1));
					}
					List<QuestAction> completing = new ArrayList<>(1 + blockSections.size());
					completing.add(new QuestAction.SetVariable("var0", landingRow));
					if (!finalStep) {
						for (int section : blockSections) {
							completing.add(new QuestAction.SetVariable("var" + section, 0));
						}
					}
					for (int npcId : row.family()) {
						transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
							List.copyOf(completingGate), List.copyOf(completing), target,
							List.copyOf(killAfterCommit),
							0, source));
						transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
							List.of(new QuestCondition.VariableBelow("var" + row.section(),
								row.count() - (parallel ? 0 : 1))),
							List.of(new QuestAction.IncrementVariable("var" + row.section(), 1)), source,
							List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
							1, source));
					}
				}
				huntOrdinalCursor++;
			} else if (step.isPvp()) {
				// PVP 计数步（80846 pvp+collectitem / 15673 enterarea+pvp 形）与单行 hunt 块同形：
				// 完成边（priority 0，段计数达标推进；非末步清段计数）+ 自环（priority 1，未达标递增）；
				// 击杀事件 = 军衔阈值行 KillRanked(阈值)、其余 KillInWorld(0) 世界通配，两边形都带
				// PvpVictimLevelDelta 等级窗（victim 等级 ≥ killer − gap；窗外的击杀不计数也不完成），
				// 判据与 P5-4 网格族一致（RetailSimpleHuntDefinitionCompiler.gridEdges）。
				// A PVP counter step (the 80846 pvp+collectitem and 15673 enterarea+pvp shapes) mirrors
				// the single-row hunt block: the completing edge (priority 0, advances once the counter
				// fills and resets it on non-final steps) plus the self loop (priority 1, increments
				// while short). The kill event is KillRanked(threshold) on rank-threshold rows and the
				// KillInWorld(0) world wildcard otherwise, and both edges carry the PvpVictimLevelDelta
				// level window (only a victim at level >= killer - gap counts, in neither counting nor
				// completing otherwise) — the same rule as the P5-4 grid family.
				int count = step.pvp().count();
				if (count > RetailHuntCounterLayout.SECTION_MASK) {
					throw new IllegalStateException("pvp count exceeds the 6-bit counter: quest "
						+ questId + " count=" + count);
				}
				int counterSection = pvpCounterSections.get(index);
				int gap = step.pvp().levelGap() > 0 ? step.pvp().levelGap() : DEFAULT_PVP_LEVEL_GAP;
				QuestEvent kill = step.pvp().minRank() > 0
					? new QuestEvent.KillRanked(step.pvp().minRank())
					: new QuestEvent.KillInWorld(0);
				QuestCondition levelWindow =
					new QuestCondition.PvpVictimLevelDelta(Integer.MIN_VALUE, gap);
				List<AfterCommitAction> pvpAfterCommit = new ArrayList<>(2);
				pvpAfterCommit.add(new AfterCommitAction.SyncQuestState(finalStep
					&& stepCutscenes.containsKey(index)
					? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY));
				if (stepCutscenes.containsKey(index)) {
					pvpAfterCommit.add(new AfterCommitAction.PlayMovie(stepCutscenes.get(index)));
				}
				List<QuestAction> completing = new ArrayList<>(2);
				completing.add(new QuestAction.SetVariable("var0", landingRow));
				if (!finalStep) {
					completing.add(new QuestAction.SetVariable("var" + counterSection, 0));
				}
				transitions.add(new QuestTransition(kill,
					List.of(new QuestCondition.VariableAtLeast("var" + counterSection, count - 1),
						levelWindow),
					List.copyOf(completing), target, List.copyOf(pvpAfterCommit), 0, source));
				transitions.add(new QuestTransition(kill,
					List.of(new QuestCondition.VariableBelow("var" + counterSection, count - 1),
						levelWindow),
					List.of(new QuestAction.IncrementVariable("var" + counterSection, 1)), source,
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					1, source));
			}
		}

		// 掉落生效步的交互物自环：collectingStep = 客户端行号（var0 == collectingStep 的行态，
		// QuestInteractionObjectValidator 启动合同要求数值相等），在该行挂 TalkToNpc +
		// CAN_ACT(ACTION_ITEM_USE)；collectingStep=0 时验证器接受任意 START 行，首步即
		// collect 的行（15605 形）自环挂在 started。
		// Interaction-object self edges for live drops: collectingStep is the client row number
		// (the validator requires var0 == collectingStep), so the TalkToNpc + CanAct(ACTION_ITEM_USE)
		// pair hangs on that row; collectingStep=0 is accepted by the validator on any START row, and
		// a first-step collect (the 15605 shape) hangs its self edges on started.
		for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
			Set<Integer> objectIds = new LinkedHashSet<>();
			for (QuestDrop drop : metadata.drops()) {
				if (drop.collectingStep() == stepIndex
						&& interactionObjects.isInteractionObject(drop.npcId())) {
					objectIds.add(drop.npcId());
				}
			}
			String landing = stepIndex == 0 ? "started" : "s" + stepIndex;
			for (int objectId : objectIds) {
				transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
					landing, List.of(), null, landing));
				transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"),
					List.of(), List.of(), landing, List.of(), null, landing));
			}
		}

		transitions.add(talk(rewardNpc, QuestDialogAction.QUEST_SELECT, "reward", "reward",
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id()))));
		if (dropDrivenFobj) {
			transitions.addAll(fobjCollectHandIn(metadata, rewardNpc));
		}
		// 完成流统一走 hunt 家族 canonical 形：普通可选 = 选择梯；use_class_reward 的职业可选 =
		// AdvancedClassIs 职业梯（collect 家族 completeFlow 只有选择梯，会把职业奖励错成任选）。
		// The completion flow uses the hunt-family canonical form: ordinary selectables ride the
		// choice ladder; use_class_reward class selectables expand per AdvancedClassIs (the collect
		// family's completeFlow only has the choice ladder, which would flatten class rewards into
		// pick-any).
		transitions.addAll(RetailSimpleHuntDefinitionCompiler.completeFlow(metadata, rewardNpc, "reward",
			"complete"));
		transitions.addAll(finalIsHunt
			? RetailSimpleCollectItemDefinitionCompiler.journalRowRepair(lastRow)
			: journalRowRepair(lastRow, rewardNpc));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** ItemPlay 演出时长：遗留证据标准值（80978/17505/20035/14153 全为 3000ms）。 /
	 * The item-play duration: the legacy-evidence standard (80978/17505/20035/14153 all 3000ms). */
	private static final int ITEM_PLAY_MILLIS = 3000;

	/**
	 * 掉落驱动的 FOBJ 采集行（25052 形）：步类别恰为 {@code [talkfobj]} 且真端元数据声明采集交付物
	 * （{@code collect_item*} → {@code itemRequirements}）。FOBJ 步在此形里是**采集物来源**（交互抽取，
	 * 掉落表按 collect_progress 发放），不推进行态；交付合同落在领奖 NPC 的 1009 组检查对上。
	 * <p>
	 * Drop-driven FOBJ collect row (the 25052 shape): the step list is exactly {@code [talkfobj]} and
	 * the retail metadata declares a collected item ({@code collect_item*} → {@code itemRequirements}).
	 * The fobj step is the item source (interaction extraction; the drop table grants it per
	 * collect_progress) and never advances the row — the hand-in contract sits on the reward npc's
	 * 1009 check pair.
	 */
	static boolean isDropDrivenFobjCollect(RetailDataDrivenTable.Entry entry,
			RetailQuestMetadataCompiler.Outcome metadata) {
		// inventoryItems 是「物品要求」的另一通道：本形状只认 collect_item（itemRequirements），
		// 两通道并存的行不进本形状（路由侧守卫按拒绝处置，不做半采纳）。
		// inventoryItems is the other "item requirement" channel: this shape only honours
		// collect_item (itemRequirements), so rows carrying both channels stay rejected (the router
		// guard refuses them rather than adopting them halfway).
		return entry.stepCategories().equals(List.of("talkfobj"))
			&& !metadata.metadata().itemRequirements().isEmpty()
			&& metadata.metadata().inventoryItems().isEmpty();
	}

	/**
	 * 掉落驱动 FOBJ 采集行的交付段（25052 形）：报告页（客户端 select_success）自理 + 领奖 NPC 的
	 * {@code SELECT_QUEST_REWARD(1009)} 组检查对——有货则扣整组、进领奖态并开领奖窗；缺货时无失败页
	 * 声明（客户端任务书无 check_user_item_fail 页）故只关窗。
	 * The hand-in section of a drop-driven FOBJ collect row (the 25052 shape): the report page
	 * (the client's select_success) plus the reward npc's {@code SELECT_QUEST_REWARD(1009)} check
	 * pair — with the items it removes the whole group, enters the reward state and opens the reward
	 * window; without them no failure page is declared (the client letter has no
	 * check_user_item_fail page), so the dialog simply closes.
	 */
	private static List<QuestTransition> fobjCollectHandIn(QuestMetadata metadata, int rewardNpc) {
		List<QuestCondition> hasItems = metadata.itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = metadata.itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		List<AfterCommitAction> success = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		return List.of(
			talk(rewardNpc, QuestDialogAction.QUEST_SELECT, "started", "started",
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id()))),
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.copyOf(hasItems), List.copyOf(removeItems), "reward", success, 0, "started"),
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.of(), List.of(), "started", List.of(new AfterCommitAction.CloseDialog()), 1, "started"));
	}

	/**
	 * PvP 目标等级窗缺省值（ScriptDLL DataDrivenQuest 进度槽位的 "PvP Target Level Gap"，
	 * 真端表 value3 缺省时取 10；victim 等级 ≥ killer − gap 才计数）。
	 * The default PvP target level gap (the ScriptDLL DataDrivenQuest progress slot; the retail
	 * table's value3 falls back to 10; only a victim at level &gt;= killer level - gap counts).
	 */
	private static final int DEFAULT_PVP_LEVEL_GAP = 10;

	/** 已进入领奖态的早期行存档按客户端末行恢复，并允许立即重开领奖对话。 /
	 * Recover an earlier reward row to the client's final row on login or on reward NPC selection. */
	private static List<QuestTransition> journalRowRepair(int lastRow, int rewardNpc) {
		if (lastRow <= 0) {
			return List.of();
		}
		List<QuestCondition> conditions = List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.VariableBelow("var0", lastRow));
		List<QuestAction> actions = List.of(new QuestAction.SetVariable("var0", lastRow));
		return List.of(
			new QuestTransition(new QuestEvent.EnterWorld(), conditions, actions, "reward",
				List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), null, null),
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
				conditions, actions, "reward",
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				null, null));
	}

	/**
	 * collect 步检查对：39 success = 整组 has-item + remove-item + 行 +1（末段另授予任务凭证并挂
	 * 1009 对），fail = 结果页自环；中段 PACKET_ONLY + ok 页，末段 LEVEL_AND_VISIBILITY + 领奖窗。
	 * The collect check pair: 39 success checks and removes the whole group advancing the row (the
	 * final stage also grants the work items and carries the 1009 pair); fail shows the fail page.
	 */
	private static List<QuestTransition> collectCheck(QuestMetadata metadata, int npc,
			String source, String target, int landingRow,
			boolean finalStep, int landedNpc) {
		List<QuestCondition> hasItems = metadata.itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		// 移除量以真端 collect_item 计数为权威（遗留迁移对 15602/25602 的 ALL 没收无真端/客户端
		// 痕迹——四家组队任务元数据同为 drop_each_member=1，无机械判据，按真端优先裁剪）。
		// Removal follows the retail collect_item counts (the legacy migration's ALL confiscation
		// for 15602/25602 has no retail/client trace — all four party quests share
		// drop_each_member=1 with no mechanical discriminator — trimmed retail-first).
		List<QuestAction> removeItems = metadata.itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		int okPage = RetailQuestDialogPages.COLLECT_OK_PAGE_ID;
		int failPage = RetailQuestDialogPages.COLLECT_FAIL_PAGE_ID;
		List<AfterCommitAction> success = finalStep
			? List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))
			: List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(okPage));
		List<QuestAction> successActions = new ArrayList<>(removeItems);
		successActions.add(new QuestAction.SetVariable("var0", landingRow));
		if (finalStep) {
			for (QuestItemRequirement workItem : metadata.questWorkItems()) {
				successActions.add(new QuestAction.GiveItem(workItem.itemId(), workItem.count()));
			}
		}
		List<QuestTransition> transitions = new ArrayList<>(4);
		int checkAction = RetailQuestDialogPages.COLLECT_CHECK_ACTION_ID;
		transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(npc, checkAction),
			List.copyOf(hasItems), List.copyOf(successActions), target, success, 0, source));
		transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(npc, checkAction),
			List.of(), List.of(), source, List.of(new AfterCommitAction.ShowQuestDialog(failPage)), 1, source));
		if (finalStep) {
			// 末段 1009 对（16942 形）：ok 页的 1009 按钮同组检查直达领奖窗。
			// The final stage's 1009 pair (16942 shape): the ok page's 1009 button repeats the
			// group check into the reward window.
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.copyOf(hasItems), List.copyOf(successActions), target, success, 0, source));
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.SELECT_QUEST_REWARD.id()),
				List.of(), List.of(), source, List.of(new AfterCommitAction.ShowQuestDialog(failPage)),
				1, source));
		} else if (landedNpc != npc) {
			// 中间段落点关窗出口：ok 页（10000）唯一的 FINISH_DIALOG 按钮由检查 NPC 收发，落点步
			// 换人或无对话段（hunt 步）时该节点没有同键路由，须补发（遗留 s2→s2 FINISH_DIALOG
			// npc=检查 NPC 逐字同形）；落点步 NPC 相同时段循环已发出，不重复。
			// Mid-stage close exit: the ok page's only FINISH_DIALOG button is served by the check
			// npc, so a landing step with another npc (or none at all, e.g. a hunt step) needs the
			// route on the landed node (the legacy s2 -> s2 FINISH_DIALOG with the check npc).
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.FINISH_DIALOG.id()),
				List.of(), List.of(), target, List.of(new AfterCommitAction.CloseDialog()), null, target));
		}
		return transitions;
	}

	/** FINISH_DIALOG 收尾自环（客户端 1008 按钮；关闭对话，不改任务状态）。 */
	/** The FINISH_DIALOG close self-loop (the client's 1008 button); it closes the dialog only. */
	private static QuestTransition finishTalk(int npcId, String source) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, QuestDialogAction.FINISH_DIALOG.id()),
			List.of(), List.of(), source, List.of(new AfterCommitAction.CloseDialog()), null, source);
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, null, source);
	}

	/**
	 * 名字解析失败的稳定载体：携带拒绝码与未解析模板名，供上游转成如实拒绝而非编译失败。
	 * Stable carrier for name-resolution failures: carries the rejection code and the unresolved
	 * template so upstream emits an honest deferral instead of a compilation failure.
	 */
	private static final class RetailNameResolutionException extends RuntimeException {
		private final String code;
		private final String monster;

		private RetailNameResolutionException(String code, String monster) {
			super(code + ": " + monster);
			this.code = code;
			this.monster = monster;
		}

		private String code() {
			return code;
		}

		private String monster() {
			return monster;
		}
	}
}
