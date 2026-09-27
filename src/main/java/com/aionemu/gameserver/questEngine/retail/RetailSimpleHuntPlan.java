package com.aionemu.gameserver.questEngine.retail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * 由真端模板表 + NPC 名索引绑定出的 SimpleHunt 执行计划（不依赖 quest-definition XML）。
 * <p>
 * 这是"表驱动"路线的第一层：表的 {@code countN/monsterN} 经名索引解析成 npc_id 后，
 * 每个槽位都能直接回答"这只怪属于哪个 SECTION、还需几只、打满后的完成值是多少"。
 * <p>
 * A SimpleHunt execution plan bound from the retail table and the NPC name index.
 */
	public record RetailSimpleHuntPlan(int questId, List<BoundCounter> counters, Set<Integer> acquiredNpcIds,
			Set<Integer> rewardNpcIds, int goalValue, List<String> unresolvedNames, String acquiredNpcName,
			String rewardNpcName, RetailGrantKind grantKind, Set<Integer> briefingNpcIds, String briefingNpcName,
			boolean pvpProgress, int pvpMinRank, int pvpLevelGap, int worldAcquireId,
			boolean acquiredNpcIsQuestAiNameGroup, boolean rewardNpcIsQuestAiNameGroup) {

		/** 兼容只关心计数与 id 集合的老调用方（不携带原始名）。 / Legacy shape without the raw names. */
		public RetailSimpleHuntPlan(int questId, List<BoundCounter> counters, Set<Integer> acquiredNpcIds,
				Set<Integer> rewardNpcIds, int goalValue, List<String> unresolvedNames) {
			this(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue, unresolvedNames, "", "",
				RetailGrantKind.NPC, Set.of(), "", false, 0, 0, 0, false, false);
		}

		/** 兼容只带原始名的调用方：接取类别由名字形态推导。 / Name-only callers derive the acquire kind. */
		public RetailSimpleHuntPlan(int questId, List<BoundCounter> counters, Set<Integer> acquiredNpcIds,
				Set<Integer> rewardNpcIds, int goalValue, List<String> unresolvedNames, String acquiredNpcName,
				String rewardNpcName) {
			this(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue, unresolvedNames, acquiredNpcName,
				rewardNpcName, RetailGrantKind.of(acquiredNpcName), Set.of(), "", false, 0, 0, 0, false, false);
		}

		/** 兼容未传 worldAcquireId 的全参调用。 / Full-parameter caller without worldAcquireId. */
		public RetailSimpleHuntPlan(int questId, List<BoundCounter> counters, Set<Integer> acquiredNpcIds,
				Set<Integer> rewardNpcIds, int goalValue, List<String> unresolvedNames, String acquiredNpcName,
				String rewardNpcName, RetailGrantKind grantKind, Set<Integer> briefingNpcIds, String briefingNpcName,
				boolean pvpProgress, int pvpMinRank, int pvpLevelGap) {
			this(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue, unresolvedNames, acquiredNpcName,
				rewardNpcName, grantKind, briefingNpcIds, briefingNpcName, pvpProgress, pvpMinRank, pvpLevelGap, 0,
				false, false);
		}

	/** 绑定后的一个击杀槽位。 / A bound kill counter slot. */
	public record BoundCounter(int slot, int required, Set<Integer> npcIds, List<String> monsterNames) {

		public boolean accepts(int npcId) {
			return npcIds.contains(npcId);
		}
	}

	/** 该 npc 属于哪个计数器槽位（1 起）；不属于本任务时为空。 / Which counter slot owns this npc. */
	public OptionalInt slotForNpc(int npcId) {
		for (BoundCounter counter : counters) {
			if (counter.accepts(npcId)) {
				return OptionalInt.of(counter.slot());
			}
		}
		return OptionalInt.empty();
	}

	public Optional<BoundCounter> counter(int slot) {
		return counters.stream().filter(counter -> counter.slot() == slot).findFirst();
	}

	/** 真端语义：这只怪当前还能不能计数。 / Retail semantics: can this kill still be counted. */
	public boolean canCount(int npcId, int packed) {
		OptionalInt slot = slotForNpc(npcId);
		if (slot.isEmpty()) {
			return false;
		}
		BoundCounter counter = counter(slot.getAsInt()).orElseThrow();
		return RetailHuntCounterLayout.canIncrement(packed, counter.slot(), counter.required());
	}

	/** 真端语义：计数后的 packed 值。 / Packed progress after counting this kill. */
	public int count(int npcId, int packed) {
		OptionalInt slot = slotForNpc(npcId);
		if (slot.isEmpty()) {
			return packed;
		}
		return RetailHuntCounterLayout.increment(packed, slot.getAsInt());
	}

	public boolean isComplete(int packed) {
		return packed == goalValue;
	}

	/**
	 * 用真端模板表行 + 名索引绑定执行计划。
	 * Binds an execution plan from a retail table row and the NPC name index.
	 */
	public static RetailSimpleHuntPlan bind(RetailSimpleHuntTable.Entry entry, RetailNpcNameIndex index) {
		List<BoundCounter> bound = new ArrayList<>(entry.counters().size());
		List<String> unresolved = new ArrayList<>();
		for (RetailSimpleHuntTable.Counter counter : entry.counters()) {
			RetailNpcNameIndex.Resolution resolution = index.resolveAll(counter.monsters());
			unresolved.addAll(resolution.unresolvedNames());
			// 击杀目标按同名族展开：真端表给的是模板 id，本服实刷常常是同一显示名的另一个 id。
			// Kill targets expand to the display-name family: the world may spawn a sibling id.
			Set<Integer> targets = index.withDisplayNameVariants(resolution.npcIds());
			bound.add(new BoundCounter(counter.slot(), counter.required(),
				Set.copyOf(new LinkedHashSet<>(targets)), counter.monsters()));
		}
		// 接取/交付字段走统一名字通道：精确 → 名前变体族 → 真端对话名组（客户端声明的组员全展开，
		// 与遗留 XML 的多 NPC 接取/报告同构；守备队、登陆点基地守卫、活动商人、副本台阶四族同口径）。
		// The acquire/hand-in fields ride the unified name channel: exact, then name variant families,
		// then the declared dialog-name group expanded to every member (isomorphic to the legacy XML's
		// multi-npc accept/report for the guard, landing-base, event-vendor and instance-stage families).
		// 缺口批 1：挑战任务哨兵采纳（冻结集 RetailChallengeAcquireAdoptions）——挑战 NPC 本人是交付
		// NPC 且客户端在该 NPC 上声明了 NPC_START 接取边 ⇒ 接取名归一化到交付名，下游按普通 NPC
		// 接取合成（归一化名必须贯到整条管道：ids、名字、grantKind、对话名组判定）。
		// Gap batch 1: challenge-sentinel adoption — the challenge NPC is the delivery NPC with a
		// client-authored NPC_START edge, so the acquire name normalizes to the delivery name and the
		// whole pipeline (ids, name, grantKind, group flag) sees a plain NPC acquire.
		String acquiredName = names(entry.acquiredNpc());
		RetailGrantKind kind = entry.grantKind();
		if (kind == RetailGrantKind.CHALLENGE_TASK
				&& RetailChallengeAcquireAdoptions.isAdopted(entry.questId())) {
			acquiredName = names(entry.rewardNpc());
			kind = RetailGrantKind.NPC;
		}
		Set<Integer> acquired = kind == RetailGrantKind.NPC
			? index.resolvePartyName(acquiredName)
			: Set.of();
		Set<Integer> reward = index.resolvePartyName(names(entry.rewardNpc()));
		// talk_npc1 = 真端简报 NPC（网格族据此合成"接取 → 见简报 → 清标志位 → 开计数"）。
		// talk_npc1 is the retail briefing NPC consumed by the grid synthesizer.
		String talkName = names(entry.talkNpc());
		Set<Integer> briefing = talkName.isBlank() ? Set.of()
			: index.resolveAll(List.of(talkName)).npcIds();
		return new RetailSimpleHuntPlan(entry.questId(), List.copyOf(bound), Set.copyOf(acquired),
			Set.copyOf(reward), RetailHuntCounterLayout.goal(entry.counters()), List.copyOf(unresolved),
			acquiredName, names(entry.rewardNpc()), kind, Set.copyOf(briefing),
			talkName, entry.pvpProgress(), 0, 0, 0,
			kind == RetailGrantKind.NPC && index.isQuestAiNameGroup(acquiredName),
			index.isQuestAiNameGroup(names(entry.rewardNpc())));
	}

	/**
	 * P5-4：PVP 计数形——网格步进发 KillInWorld(0)（世界通配）而不是 KillNpc。
	 * P5-4: the PVP counter shape — grid steps emit the world-wildcard KillInWorld(0) event.
	 */
	public RetailSimpleHuntPlan withPvpProgress(boolean pvpProgress) {
		return new RetailSimpleHuntPlan(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue,
			unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds, briefingNpcName,
			pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
				acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * 变体轴裁定（仅单段非 PVP 行）：目标集 = 真端表解析 ∪ 客户端任务书名单。真端怪物列只给
	 * 基础模板名，客户端 SECTION 名单还含同族 T_ 实刷变体（name_id 不同，显示名族闭包不可达）；
	 * 多段行需逐段登记、不在此并入。
	 * Variant-axis adjudication (single-stage non-PVP rows only): targets = retail resolution
	 * ∪ the client journal list. Retail monster columns name only base templates; the client
	 * SECTION lists also carry same-family spawned T_ variants unreachable by display-name
	 * closure. Multi-stage rows need a per-stage registry and are not merged here.
	 */
	public RetailSimpleHuntPlan withClientKillTargets(Set<Integer> clientTargets) {
		if (clientTargets == null || clientTargets.isEmpty() || pvpProgress || counters.size() != 1) {
			return this;
		}
		RetailSimpleHuntPlan.BoundCounter counter = counters.get(0);
		Set<Integer> merged = new LinkedHashSet<>(counter.npcIds());
		merged.addAll(clientTargets);
		List<RetailSimpleHuntPlan.BoundCounter> mergedCounters = new ArrayList<>(counters);
		mergedCounters.set(0, new RetailSimpleHuntPlan.BoundCounter(counter.slot(), counter.required(),
			Set.copyOf(merged), counter.monsterNames()));
		return new RetailSimpleHuntPlan(questId, List.copyOf(mergedCounters), acquiredNpcIds, rewardNpcIds,
			goalValue, unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds,
			briefingNpcName, pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
			acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * 逐段变体裁定（仅多段非 PVP 行）：按计数槽并入客户端逐段名单（{@code RetailClientKillTargets}
	 * 的 stages 文件，段号 = 槽位 1..N）。真端表段内只给基础模板名；客户端每 SECTION 行含同族
	 * T_ 实刷变体。只并入已登记的段，未登记段保持真端表解析——段间互斥不受影响。
	 * Per-stage variant adjudication (multi-stage non-PVP rows only): merge the client's per-stage
	 * lists by counter slot. The retail stage rows name only base templates while each client
	 * SECTION row carries the same-family T_ variants. Only registered stages merge; unregistered
	 * stages keep the retail resolution — stage isolation is untouched.
	 */
	public RetailSimpleHuntPlan withClientStageKillTargets(Map<Integer, Set<Integer>> stageTargets) {
		if (stageTargets == null || stageTargets.isEmpty() || pvpProgress || counters.size() < 2) {
			return this;
		}
		List<RetailSimpleHuntPlan.BoundCounter> merged = new ArrayList<>(counters);
		for (int index = 0; index < merged.size(); index++) {
			RetailSimpleHuntPlan.BoundCounter counter = merged.get(index);
			Set<Integer> extra = stageTargets.get(counter.slot());
			if (extra == null || extra.isEmpty()) {
				continue;
			}
			Set<Integer> ids = new LinkedHashSet<>(counter.npcIds());
			ids.addAll(extra);
			merged.set(index, new RetailSimpleHuntPlan.BoundCounter(counter.slot(), counter.required(),
				Set.copyOf(ids), counter.monsterNames()));
		}
		return new RetailSimpleHuntPlan(questId, List.copyOf(merged), acquiredNpcIds, rewardNpcIds,
			goalValue, unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds,
			briefingNpcName, pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
			acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * P5-4：军衔阈值 PVP 网格——网格步进发 KillRanked(minRank)（victim 军衔 ≥ 阈值计数）。
	 * P5-4: ranked PVP grid — grid steps emit KillRanked(minRank) (victim rank >= threshold counts).
	 */
	public RetailSimpleHuntPlan withPvpMinRank(int pvpMinRank) {
		return new RetailSimpleHuntPlan(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue,
			unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds, briefingNpcName,
			pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
				acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * P5-4：军衔阈值行附等级窗——victim 等级 ≥ killer - gap 才计数（ScriptDLL PvP Target Level Gap）。
	 * P5-4: rank-threshold rows also carry the level gap (a victim counts only at
	 * victim level >= killer level - gap; ScriptDLL "PvP Target Level Gap").
	 */
	public RetailSimpleHuntPlan withPvpLevelGap(int pvpLevelGap) {
		return new RetailSimpleHuntPlan(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue,
			unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds, briefingNpcName,
			pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
			acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * 客户端计数对齐（单段网格行）：真端 DD 表的 {@code value0_progress_} 尾数是**服务端表修订**的读数，
	 * 与客户端任务书/SECTION 门控的计数在部分行不一致（如 13758 族：服务端 15/12/8/6/20 vs 客户端 5；
	 * 15571 族：服务端 30 vs 客户端 10；13841 族：服务端 60/60/55 vs 客户端 40）。单段行只有一个计数器，
	 * 客户端进度行也只登记这一行时，以客户端计数为准——玩家看到并驱动的是客户端计数器；遗留 XML 的
	 * 计数门槛（13841 族 var1 上限 40）与客户端门控同侧，服务端读数才是异己的一侧。
	 * <p>
	 * 判据刻意不含怪名比对：真端表的名单可能是改名/幽灵模板（13841 族 {@code IDAbRe_Low_Wciel}
	 * 在本端解析不出任何 npc），按名对不上会漏掉这一族。客户端未登记、多行登记（并行段/多段形）、
	 * 多计数器或 PVP 行都保持真端读数，由常设门登记该分歧集。
	 * Client-count alignment for single-stage grid rows: the retail table's {@code value0_progress_}
	 * tail is a server-table revision readout that disagrees with the client journal/SECTION gate on
	 * some rows (13758 family: server 15/12/8/6/20 vs client 5; 15571 family: 30 vs 10; 13841 family:
	 * 60/60/55 vs 40). A single-stage row owns exactly one counter, so when the client registers exactly
	 * one progress row for the quest the client count wins — the client counter is what the player sees
	 * and drives, and the legacy XML gates (the 13841 family's var1 ceiling of 40) sit on the same side.
	 * <p>
	 * The predicate deliberately avoids name matching: the retail list may carry renamed or phantom
	 * templates (the 13841 family's {@code IDAbRe_Low_Wciel} resolves to no npc here), which would make
	 * a name test miss that family. Unregistered rows, multi-row registrations (parallel or multi-stage
	 * shapes), multi-counter or PVP rows all keep the retail readout; the permanent gate tracks the set.
	 */
	public RetailSimpleHuntPlan withClientStageCounts(List<RetailClientHuntProgressRows.Row> clientRows) {
		if (pvpProgress || counters.size() != 1 || clientRows == null || clientRows.size() != 1) {
			return this;
		}
		BoundCounter counter = counters.get(0);
		RetailClientHuntProgressRows.Row row = clientRows.getFirst();
		if (row.count() <= 0 || row.count() == counter.required()) {
			return this;
		}
		BoundCounter aligned = new BoundCounter(counter.slot(), row.count(), counter.npcIds(),
			counter.monsterNames());
		return new RetailSimpleHuntPlan(questId, List.of(aligned), acquiredNpcIds, rewardNpcIds,
			RetailHuntCounterLayout.goal(List.of(new RetailSimpleHuntTable.Counter(aligned.slot(),
				aligned.required(), aligned.monsterNames()))),
			unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds, briefingNpcName,
			pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
			acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	/**
	 * P5-4：进世界接取的世界 ID（EnterWorld 发放）。
	 * Sets the world ID for world-entry grants (EnterWorld acquire).
	 */
	public RetailSimpleHuntPlan withWorldAcquireId(int worldAcquireId) {
		return new RetailSimpleHuntPlan(questId, counters, acquiredNpcIds, rewardNpcIds, goalValue,
			unresolvedNames, acquiredNpcName, rewardNpcName, grantKind, briefingNpcIds, briefingNpcName,
			pvpProgress, pvpMinRank, pvpLevelGap, worldAcquireId,
				acquiredNpcIsQuestAiNameGroup, rewardNpcIsQuestAiNameGroup);
	}

	private static String names(String value) {
		return value == null ? "" : value;
	}

	/** 槽位 → npc_id 集合视图（便于与 quest-definition 对账）。 / Slot to npc ids view used for reconciliation. */
	public Map<Integer, Set<Integer>> npcIdsBySlot() {
		Map<Integer, Set<Integer>> result = new java.util.LinkedHashMap<>();
		for (BoundCounter counter : counters) {
			result.put(counter.slot(), counter.npcIds());
		}
		return Map.copyOf(result);
	}
}
