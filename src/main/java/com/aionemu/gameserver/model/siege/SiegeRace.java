package com.aionemu.gameserver.model.siege;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;

/**
 * 要塞种族枚举。
 * Siege Race enumeration.
 *
 * @author Sarynth
 */
public enum SiegeRace {
	/** 天族。 / Elyos. */
	ELYOS(0, 1800481),
	/** 魔族。 / Asmodians. */
	ASMODIANS(1, 1800483),
	/** 龙族。 / Balaur. */
	BALAUR(2, 1800485);

	private int raceId;
	private DescriptionId descriptionId;

	private SiegeRace(int id, int descriptionId) {
		this.raceId = id;
		this.descriptionId = new DescriptionId(descriptionId);
	}

	/** 返回种族 ID / Returns the race id */
	public int getRaceId() {
		return this.raceId;
	}

	/** 按通用种族映射为要塞种族 / Returns the siege race for a race */
	public static SiegeRace getByRace(Race race) {
		switch (race) {
		case ASMODIANS:
			return SiegeRace.ASMODIANS;
		case ELYOS:
			return SiegeRace.ELYOS;
		default:
			return SiegeRace.BALAUR;
		}
	}

	/**
	 * @return 描述 ID / the descriptionId
	 */
	public DescriptionId getDescriptionId() {
		return descriptionId;
	}
}
