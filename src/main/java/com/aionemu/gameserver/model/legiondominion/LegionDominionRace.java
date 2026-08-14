package com.aionemu.gameserver.model.legiondominion;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;

/**
 * 军团领地种族枚举。
 * Legion Dominion Race enumeration.
 */

public enum LegionDominionRace {
	/** 天族 / Elyos. */
	ELYOS(0, 1800481),
	/** 魔族 / Asmodians. */
	ASMODIANS(1, 1800483),
	/** 龙族 / Balaur. */
	BALAUR(2, 1800485);

	private int raceId;
	private DescriptionId descriptionId;

	private LegionDominionRace(int id, int descriptionId) {
		this.raceId = id;
		this.descriptionId = new DescriptionId(descriptionId);
	}

	/** 返回种族 ID / Returns the race id */
	public int getRaceId() {
		return this.raceId;
	}

	/** 返回对应种族 / Returns the matching race */
	public static LegionDominionRace getByRace(Race race) {
		switch (race) {
		case ASMODIANS:
			return LegionDominionRace.ASMODIANS;
		case ELYOS:
			return LegionDominionRace.ELYOS;
		default:
			return LegionDominionRace.BALAUR;
		}
	}

	/** 返回描述 ID / Returns the description id */
	public DescriptionId getDescriptionId() {
		return descriptionId;
	}
}
