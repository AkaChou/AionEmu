package com.aionemu.gameserver.questEngine.retail;

import java.util.Map;
import java.util.Set;

/**
 * 挑战任务哨兵接取采纳表（缺口批 1 · 批准裁定，冻结集）。
 * <p>
 * 原版模板表的 {@code acquired_npc_name=_challengetask_} 表示"由挑战任务子系统发放"，本服没有
 * 挑战任务的受理入口（只有完成回调）⇒ 这类行原本按 {@code RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH}
 * 拒绝并保留 XML。逐行取证（客户端 lifecycle 登记）证明其中一部分任务的**挑战 NPC 本人就是交付
 * NPC**，且客户端在该 NPC 上声明了接取边（{@code NPC_START}，NONE→START）：对这些行，"找挑战 NPC
 * 接话"就是客户端可见的接取路径，把接取名归一化到交付 NPC 后按普通 NPC 接取合成，与客户端合同一致。
 * <p>
 * 每个条目四方证据（缺一不可，由 {@code RetailSimpleHuntFamilyGateTest} 的采纳门逐条复核）：
 * ① 原版表行 {@code grantKind()==CHALLENGE_TASK}；② 交付名解析唯一且解析结果 == 本表值；
 * ③ 客户端 lifecycle 登记在**同一 NPC** 上有 {@code NPC_START} 接取边；④ 裁定登记表
 * （{@code retail-simple-hunt-adjudicated-decisions.tsv}）有 {@code ADOPT_RETAIL /
 * CHALLENGE_TASK_NPC_DELIVERY} 行。四方不齐的行继续拒绝（"本表有值但证据缺失"与"证据在册但本表
 * 缺值"都要变红）。
 * <p>
 * Frozen adoption set for the challenge-task acquire sentinel (gap batch 1). Rows listed here are
 * challenge quests whose challenge NPC is also the delivery NPC and which carry a client-authored
 * {@code NPC_START} accept edge on that same NPC, so the acquire name normalizes to the delivery
 * NPC and the row compiles as a plain NPC-acquire hunt quest.
 */
public final class RetailChallengeAcquireAdoptions {

	/** 冻结的采纳集（quest_id → 挑战/交付 NPC id）。 / The frozen adoption set (quest id → challenge NPC). */
	private static final Map<Integer, Integer> ADOPTED = Map.ofEntries(
			Map.entry(17000, 800447),
			Map.entry(17001, 800447),
			Map.entry(17002, 800447),
			Map.entry(17003, 800447),
			Map.entry(17004, 800447),
			Map.entry(17005, 800447),
			Map.entry(17006, 800447),
			Map.entry(17007, 800447),
			Map.entry(17008, 800447),
			Map.entry(17009, 800447),
			Map.entry(17010, 800447),
			Map.entry(17011, 800447),
			Map.entry(17015, 800446),
			Map.entry(17016, 800446),
			Map.entry(17017, 800446),
			Map.entry(17018, 800445),
			Map.entry(17019, 800445),
			Map.entry(17106, 831209),
			Map.entry(17108, 831209),
			Map.entry(17110, 831209),
			Map.entry(17112, 831209),
			Map.entry(17114, 831209),
			Map.entry(17116, 831209),
			Map.entry(17118, 831209),
			Map.entry(17120, 831209),
			Map.entry(17122, 831209),
			Map.entry(17124, 831209),
			Map.entry(17126, 831209),
			Map.entry(17128, 831209),
			Map.entry(17130, 831209),
			Map.entry(17132, 831209),
			Map.entry(17134, 831209),
			Map.entry(17136, 831209),
			Map.entry(17138, 831209),
			Map.entry(17140, 831209),
			Map.entry(17142, 831209),
			Map.entry(17144, 831209),
			Map.entry(17146, 831209),
			Map.entry(17148, 831209),
			Map.entry(17150, 831209),
			Map.entry(17152, 831209),
			Map.entry(17154, 831209),
			Map.entry(27000, 800452),
			Map.entry(27001, 800452),
			Map.entry(27002, 800452),
			Map.entry(27003, 800452),
			Map.entry(27004, 800452),
			Map.entry(27005, 800452),
			Map.entry(27006, 800452),
			Map.entry(27007, 800452),
			Map.entry(27008, 800452),
			Map.entry(27009, 800452),
			Map.entry(27010, 800452),
			Map.entry(27011, 800452),
			Map.entry(27015, 800451),
			Map.entry(27016, 800451),
			Map.entry(27017, 800451),
			Map.entry(27018, 800450),
			Map.entry(27019, 800450),
			Map.entry(27106, 831234),
			Map.entry(27108, 831234),
			Map.entry(27110, 831234),
			Map.entry(27112, 831234),
			Map.entry(27114, 831234),
			Map.entry(27116, 831234),
			Map.entry(27118, 831234),
			Map.entry(27120, 831234),
			Map.entry(27122, 831234),
			Map.entry(27124, 831234),
			Map.entry(27126, 831234),
			Map.entry(27128, 831234),
			Map.entry(27130, 831234),
			Map.entry(27132, 831234),
			Map.entry(27134, 831234),
			Map.entry(27136, 831234),
			Map.entry(27138, 831234),
			Map.entry(27140, 831234),
			Map.entry(27142, 831234),
			Map.entry(27144, 831234),
			Map.entry(27146, 831234),
			Map.entry(27148, 831234),
			Map.entry(27150, 831234),
			Map.entry(27152, 831234),
			Map.entry(27154, 831234));

	/** 该任务是否采纳"挑战 NPC 即接取人"。 / Whether the quest adopts its challenge NPC as acquirer. */
	public static boolean isAdopted(int questId) {
		return ADOPTED.containsKey(questId);
	}

	/** 采纳的接取 NPC id（未采纳返回空）。 / The adopted acquire NPC id, empty when not adopted. */
	public static int acquireNpcId(int questId) {
		return ADOPTED.getOrDefault(questId, -1);
	}

	/** 采纳集（门禁复核用）。 / The adoption set, for the family gate's audit. */
	public static Set<Integer> adoptedQuestIds() {
		return Set.copyOf(ADOPTED.keySet());
	}

	private RetailChallengeAcquireAdoptions() {
	}
}
