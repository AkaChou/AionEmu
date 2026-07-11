package com.aionemu.gameserver.model.craft;

import com.aionemu.gameserver.model.Race;

/**
 * MasterQuests 列表。
 * Master Quests List enumeration.
 */

public enum MasterQuestsList {
	/** 天族烹饪 / Cooking Elyos*/
	COOKING_ELYOS(new int[] { 19039, 19038 }, Race.ELYOS, 40001),
	/** 魔族烹饪 / Cooking Asmodians*/
	COOKING_ASMODIANS(new int[] { 29039, 29038 }, Race.ASMODIANS, 40001),
	/** 天族武器锻造 / Weaponsmithing Elyos*/
	WEAPONSMITHING_ELYOS(new int[] { 19009, 19008 }, Race.ELYOS, 40002),
	/** 魔族武器锻造 / Weaponsmithing Asmodians*/
	WEAPONSMITHING_ASMODIANS(new int[] { 29009, 29008 }, Race.ASMODIANS, 40002),
	/** 天族防具锻造 / Armorsmithing Elyos*/
	ARMORSMITHING_ELYOS(new int[] { 19015, 19014 }, Race.ELYOS, 40003),
	/** 魔族防具锻造 / Armorsmithing Asmodians*/
	ARMORSMITHING_ASMODIANS(new int[] { 29015, 29014 }, Race.ASMODIANS, 40003),
	/** 天族裁缝 / Tailoring Elyos*/
	TAILORING_ELYOS(new int[] { 19021, 19020 }, Race.ELYOS, 40004),
	/** 魔族裁缝 / Tailoring Asmodians*/
	TAILORING_ASMODIANS(new int[] { 29021, 29020 }, Race.ASMODIANS, 40004),
	/** 天族炼金 / Alchemy Elyos*/
	ALCHEMY_ELYOS(new int[] { 19033, 19032 }, Race.ELYOS, 40007),
	/** 魔族炼金 / Alchemy Asmodians*/
	ALCHEMY_ASMODIANS(new int[] { 29033, 29032 }, Race.ASMODIANS, 40007),
	/** 天族工艺 / Handicrafting Elyos*/
	HANDICRAFTING_ELYOS(new int[] { 19027, 19026 }, Race.ELYOS, 40008),
	/** 魔族工艺 / Handicrafting Asmodians*/
	HANDICRAFTING_ASMODIANS(new int[] { 29027, 29026 }, Race.ASMODIANS, 40008),
	/** 天族木工 / Menuisier Elyos*/
	MENUISIER_ELYOS(new int[] { 19058, 19057 }, Race.ELYOS, 40010),
	/** 魔族木工 / Menuisier Asmodians*/
	MENUISIER_ASMODIANS(new int[] { 29058, 29057 }, Race.ASMODIANS, 40010);

	private int[] skillsIds;
	private Race race;
	private int craftSkillId;

	private MasterQuestsList(int[] skillsIds, Race race, int craftSkillId) {
		this.skillsIds = skillsIds;
		this.race = race;
		this.craftSkillId = craftSkillId;
	}

	private Race getRace() {
		return race;
	}

	private int getCraftSkillId() {
		return craftSkillId;
	}

	/** 返回技能 ID / Returns the skills ids */
	public static int[] getSkillsIds(int craftSkillId, Race race) {
		for (MasterQuestsList mql : values()) {
			if ((race.equals(mql.getRace())) && (craftSkillId == mql.getCraftSkillId()))
				return mql.skillsIds;
		}
		throw new IllegalArgumentException("Invalid craftSkillId: " + craftSkillId + " or race: " + race);
	}
}
