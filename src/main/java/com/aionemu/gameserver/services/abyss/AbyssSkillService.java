package com.aionemu.gameserver.services.abyss;

import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 欧比斯军阶技能服务：按种族与军阶刷新玩家欧比斯技能。
 * Abyss-rank skill service: refreshes a player's abyss skills by race and rank.
 *
 * @author ATracer
 */
public class AbyssSkillService {

	/**
	 * 移除同种族全部欧比斯技能后，在 5 星军官及以上重新授予对应技能。
	 * Strip all same-race abyss skills, then re-grant skills for STAR5_OFFICER and above.
	 *
	 * @param player 目标玩家 / target player
	 */
	public static final void updateSkills(Player player) {
		AbyssRank abyssRank = player.getAbyssRank();
		if (abyssRank == null) {
			return;
		}
		AbyssRankEnum rankEnum = abyssRank.getRank();
		// 先移除全部欧比斯技能 / remove all abyss skills first
		for (AbyssSkills abyssSkill : AbyssSkills.values()) {
			if (abyssSkill.getRace() == player.getRace()) {
				for (int skillId : abyssSkill.getSkills()) {
					player.getSkillList().removeSkill(skillId);
				}
			}
		}
		// 添加新技能。 / add new skills
		if (abyssRank.getRank().getId() >= AbyssRankEnum.STAR5_OFFICER.getId()) {
			for (int skillId : AbyssSkills.getSkills(player.getRace(), rankEnum)) {
				player.getSkillList().addAbyssSkill(player, skillId, 1);
			}
		}
	}

	/**
	 * 玩家进世界时同步欧比斯技能。
	 * Sync abyss skills when the player enters the world.
	 *
	 * @param player 目标玩家 / target player
	 */
	public static final void onEnterWorld(Player player) {
		updateSkills(player);
	}
}
