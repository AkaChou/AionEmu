package com.aionemu.gameserver.services.abyss;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 欧比斯军阶技能表：按种族与军阶映射技能 ID 列表。
 * Abyss-rank skill table: maps race and rank to skill-id arrays.
 *
 * <p>天族（Elyos）与魔族（Asmodians）各有一套从 5 星军官到最高指挥官的技能配置。
 * Elyos and Asmodians each have a skill set from STAR5_OFFICER up to SUPREME_COMMANDER.</p>
 */
@Slf4j
enum AbyssSkills {
	/** 天族最高指挥官 / Elyos supreme commander */
	SUPREME_COMMANDER(Race.ELYOS, AbyssRankEnum.SUPREME_COMMANDER,
			new int[] { 11889, 11898, 11900, 11903, 11904, 11905, 11906 }),
	/** 天族指挥官 / Elyos commander */
	COMMANDER(Race.ELYOS, AbyssRankEnum.COMMANDER, new int[] { 11888, 11898, 11900, 11903, 11904 }),
	/** 天族大将军 / Elyos great general */
	GREAT_GENERAL(Race.ELYOS, AbyssRankEnum.GREAT_GENERAL, new int[] { 11887, 11897, 11899, 11903 }),
	/** 天族将军 / Elyos general */
	GENERAL(Race.ELYOS, AbyssRankEnum.GENERAL, new int[] { 11886, 11896, 11899 }),
	/** 天族 5 星军官 / Elyos 5-star officer */
	STAR5_OFFICER(Race.ELYOS, AbyssRankEnum.STAR5_OFFICER, new int[] { 11885, 11895 }),
	/** 魔族最高指挥官 / Asmodian supreme commander */
	SUPREME_COMMANDER_A(Race.ASMODIANS, AbyssRankEnum.SUPREME_COMMANDER,
			new int[] { 11894, 11898, 11902, 11903, 11904, 11905, 11906 }),
	/** 魔族指挥官 / Asmodian commander */
	COMMANDER_A(Race.ASMODIANS, AbyssRankEnum.COMMANDER, new int[] { 11893, 11898, 11902, 11903, 11904 }),
	/** 魔族大将军 / Asmodian great general */
	GREAT_GENERAL_A(Race.ASMODIANS, AbyssRankEnum.GREAT_GENERAL, new int[] { 11892, 11897, 11901, 11903 }),
	/** 魔族将军 / Asmodian general */
	GENERAL_A(Race.ASMODIANS, AbyssRankEnum.GENERAL, new int[] { 11891, 11896, 11901 }),
	/** 魔族 5 星军官 / Asmodian 5-star officer */
	STAR5_OFFICER_A(Race.ASMODIANS, AbyssRankEnum.STAR5_OFFICER, new int[] { 11890, 11895 });

	private int[] skills;
	private AbyssRankEnum rankenum;
	private Race race;

	private AbyssSkills(Race race, AbyssRankEnum rankEnum, int[] skills) {
		this.race = race;
		rankenum = rankEnum;
		this.skills = skills;
	}

	/**
	 * 返回该配置所属种族。
	 * Returns the race this skill set belongs to.
	 *
	 * @return 阵营 / Race
	 */
	public Race getRace() {
		return race;
	}

	/**
	 * 返回该军阶的技能 ID 数组。
	 * Returns the skill-id array for this rank.
	 *
	 * @return 技能 ID 数组 / skill ids
	 */
	public int[] getSkills() {
		return skills;
	}

	/**
	 * 按种族与军阶查找技能；未匹配时记录警告并返回空数组。
	 * Look up skills by race and rank; logs a warning and returns empty array when unmatched.
	 *
	 * @param race 阵营 / Race
	 * @param rank 军阶 / Rank
	 * @return 技能 ID 数组 / skill-id array
	 */
	public static int[] getSkills(Race race, AbyssRankEnum rank) {
		for (AbyssSkills aSkills : values()) {
			if ((aSkills.race == race) && (aSkills.rankenum == rank)) {
				return aSkills.skills;
			}
		}
		log.warn(I18n.get("log.f58257741924", race, rank));
		return new int[0];
	}
}
