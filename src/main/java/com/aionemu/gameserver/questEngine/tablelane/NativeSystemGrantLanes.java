package com.aionemu.gameserver.questEngine.tablelane;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;

/**
 * 已切换家族的 native 系统发放路由（计划 §7 P4 收口）。
 * <p>
 * 上层（{@code NpcFactions} 的阵营日常分配）只认这一条入口：它把每个已切换家族的
 * {@link NativeSystemGrantLane} 汇总起来，按"行归属唯一"分派到对应家族。owner 由
 * {@link NativeQuestOwnerResolver} 保证互斥，{@link #ownershipConflicts()} 是对交叠行集的
 * fail-fast 断言，避免同一行被两个家族受理。
 * <p>
 * Single system-grant entry point over every switched family: the caller ({@code NpcFactions}
 * faction-daily assignment) asks this aggregate instead of naming one family's handler, so switching a
 * family never orphans its sentinel rows. Ownership is exclusive by the owner resolver;
 * {@link #ownershipConflicts()} is the fail-fast assertion for overlapping row sets.
 */
public final class NativeSystemGrantLanes {

	private NativeSystemGrantLanes() {
	}

	/** 惰性持有：避免类初始化期与两个家族表的装载顺序耦合。 / Lazily held to avoid load-order coupling. */
	private static final class Holder {
		private static final List<NativeSystemGrantLane> LANES =
			List.of(SimpleTalkHandler.instance(), SimpleCollectItemHandler.instance());
	}

	/** 全部已切换家族的发放车道（只读、顺序稳定）。 / Every switched lane (read-only, stable order). */
	public static List<NativeSystemGrantLane> lanes() {
		return Holder.LANES;
	}

	/**
	 * 行归属交叠（应恒为空；非空即 owner 分解失败）。
	 * Overlapping ownership (must stay empty; a non-empty result means the owner split failed).
	 */
	public static Set<Integer> ownershipConflicts() {
		Set<Integer> seen = new TreeSet<>();
		Set<Integer> conflicts = new TreeSet<>();
		for (NativeSystemGrantLane lane : lanes()) {
			for (int questId : lane.ownedQuestIds()) {
				if (!seen.add(questId)) {
					conflicts.add(questId);
				}
			}
		}
		return conflicts;
	}

	/** 该行的归属车道（无归属返回 null）。 / The owning lane, or null. */
	public static NativeSystemGrantLane laneOf(int questId) {
		for (NativeSystemGrantLane lane : lanes()) {
			if (lane.owns(questId)) {
				return lane;
			}
		}
		return null;
	}

	/** 接取名类别（无归属按普通 NPC 行）。 / The acquire-name category (unknown rows read as plain NPCs). */
	public static RetailGrantKind grantKind(int questId) {
		NativeSystemGrantLane lane = laneOf(questId);
		return lane == null ? RetailGrantKind.NPC : lane.grantKind(questId);
	}

	/** 真端势力 id（无归属或未声明返回 0）。 / The retail faction id, or 0. */
	public static int factionId(int questId) {
		NativeSystemGrantLane lane = laneOf(questId);
		return lane == null ? 0 : lane.factionId(questId);
	}

	/** 是否系统发放（任一车道认领即为真）。 / Whether any lane system-grants the row. */
	public static boolean isSystemGranted(int questId) {
		for (NativeSystemGrantLane lane : lanes()) {
			if (lane.isSystemGranted(questId)) {
				return true;
			}
		}
		return false;
	}

	/** 指定势力的可轮换任务（各族并集）。 / Faction-rotation candidates over every lane. */
	public static Set<Integer> factionRotationCandidates(int factionId) {
		Set<Integer> candidates = new TreeSet<>();
		for (NativeSystemGrantLane lane : lanes()) {
			candidates.addAll(lane.factionRotationCandidates(factionId));
		}
		return candidates;
	}

	/** 阵营日常轮换资格（归属性由 owner 唯一确定）。 / Rotation eligibility, adjudicated by the owning lane. */
	public static boolean factionRotationEligible(Player player, int questId, int factionId) {
		NativeSystemGrantLane lane = laneOf(questId);
		return lane != null && lane.factionRotationEligible(player, questId, factionId);
	}

	/** 系统发放入口（归属性由 owner 唯一确定）。 / System-grant entry, adjudicated by the owning lane. */
	public static boolean grantSystemStart(Player player, int questId) {
		NativeSystemGrantLane lane = laneOf(questId);
		return lane != null && lane.grantSystemStart(player, questId);
	}
}
