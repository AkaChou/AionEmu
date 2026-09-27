package com.aionemu.gameserver.questEngine.retail;

import java.util.Locale;

/**
 * 真端接取名类别（M5-b2c 起）：真端族表的 {@code acquired_npc_name} 允许写"类别哨兵"
 * （{@code _faction_} / {@code _challengetask_} / {@code _area_}）而不是 NPC 名，此时接取不是
 * 由 NPC 对话触发，而是由对应子系统发放。
 * <p>
 * 三个已知哨兵各自的发放系统：{@code _faction_} → NPC 阵营日常轮换（{@code NpcFactions} +
 * {@code npc_factions_quest.xml} 星期位）；{@code _area_} → 世界 {@code quest_area} 进区域
 * （{@code RetailAreaEngine}）；{@code _challengetask_} → 挑战任务系统（本服只有完成回调，
 * 没有受理入口，因此该类行仍拒绝）。未知 {@code _..._} 形状不是发放键，保持拒绝。
 * <p>
 * Shared taxonomy of retail acquire-name categories: the three known sentinels each key a grant
 * subsystem, and any other {@code _..._} shape stays an unknown sentinel.
 */
public enum RetailGrantKind {

	/** 普通 NPC 名。 / A plain NPC name. */
	NPC,
	/** {@code _faction_}：NPC 阵营日常轮换。 / Faction daily rotation. */
	FACTION,
	/** {@code _challengetask_}：挑战任务系统。 / Challenge-task system. */
	CHALLENGE_TASK,
	/** {@code _area_}：世界 quest_area 进区域发放。 / World quest-area grant. */
	AREA,
	/** {@code EnterWorld}：进世界发放（地图 ID 在 worldAcquireId 中）。 / World-entry grant. */
	WORLD,
	/** 未知的 {@code _..._} 哨兵（不对应任何真端发放系统）。 / Unknown sentinel shape. */
	UNKNOWN_SENTINEL;

	/** 是否为系统发放（无 NPC 接取路由）。 / Whether the row is system-granted (no accept route). */
	public boolean systemGrant() {
		return this != NPC;
	}

	/** 真端已知的发放哨兵（未知哨兵不在此列）。 / Known grant sentinels only. */
	public boolean knownGrant() {
		return this == FACTION || this == CHALLENGE_TASK || this == AREA || this == WORLD;
	}

	/** 该类别在本服是否已有发放入口（NPC 接取或系统发放均可）。 / Whether a grant entry exists here. */
	public boolean grantable() {
		return this == NPC || this == FACTION || this == AREA || this == WORLD;
	}

	/**
	 * 接取名 → 类别：三类真端发放哨兵大小写不敏感识别；其余 {@code _..._} 形状记为未知哨兵；
	 * 非哨兵形状（含 null）按普通 NPC 名。
	 * Classifies an acquire name: the three known sentinels case-insensitively, any other
	 * {@code _..._} shape as an unknown sentinel, everything else as a plain NPC name.
	 */
	public static RetailGrantKind of(String acquiredNpc) {
		if (acquiredNpc == null) {
			return NPC;
		}
		String raw = acquiredNpc.trim();
		if (raw.length() <= 2 || !raw.startsWith("_") || !raw.endsWith("_")) {
			return NPC;
		}
		String token = raw.substring(1, raw.length() - 1).toLowerCase(Locale.ROOT);
		return switch (token) {
			case "faction" -> FACTION;
			case "challengetask" -> CHALLENGE_TASK;
			case "area" -> AREA;
			default -> UNKNOWN_SENTINEL;
		};
	}
}
