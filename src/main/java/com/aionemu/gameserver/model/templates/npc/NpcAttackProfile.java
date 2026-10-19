package com.aionemu.gameserver.model.templates.npc;

import com.aionemu.gameserver.model.templates.item.ItemAttackType;

/**
 * 真端 NPC 攻击档案：一个 NPC 可拥有多段攻击，按目标距离选用。
 * Retail NPC attack profile: one NPC may own several attack segments selected by target distance.
 *
 * @param range 攻击距离（米） / attack range in metres
 * @param rate 攻击倍率（真端 attack_rate，1000 = 1.0） / retail attack rate (1000 = 1.0)
 * @param type 攻击类型 / attack type
 * @param attackMod 伤害修正百分比（真端 attack_mod） / damage modifier percentage
 */
public record NpcAttackProfile(float range, int rate, ItemAttackType type, int attackMod) {

	/**
	 * 目标距离是否落在本段攻击范围内。
	 * Whether the target distance is covered by this segment.
	 *
	 * @param distance 目标距离（米） / target distance in metres
	 * @return 在范围内时为 {@code true} / {@code true} when covered
	 */
	public boolean covers(float distance) {
		return distance <= range;
	}
}
