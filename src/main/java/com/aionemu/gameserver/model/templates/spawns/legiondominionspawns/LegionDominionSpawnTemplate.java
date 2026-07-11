package com.aionemu.gameserver.model.templates.spawns.legiondominionspawns;

import com.aionemu.gameserver.model.legiondominion.LegionDominionModType;
import com.aionemu.gameserver.model.legiondominion.LegionDominionRace;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 军团领地刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

public class LegionDominionSpawnTemplate extends SpawnTemplate {
	private int legionDominionId;
	private LegionDominionRace legionDominionRace;
	private LegionDominionModType legionDominionModType;

	public LegionDominionSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public LegionDominionSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回军团领地 ID / Returns the legion dominion id */
	public int getLegionDominionId() {
		return legionDominionId;
	}

	/** 获取军团领地种族。 / Returns the legion dominion race. */
	public LegionDominionRace getLegionDominionRace() {
		return legionDominionRace;
	}

	/** 返回 legion dominion mod type / Returns the legion dominion mod type */
	public LegionDominionModType getLegionDominionModType() {
		return legionDominionModType;
	}

	/** 设置 legion dominion id / Sets the legion dominion id */
	public void setLegionDominionId(int legionDominionId) {
		this.legionDominionId = legionDominionId;
	}

	/** 设置军团领地种族。 / Sets the legion dominion race. */
	public void setLegionDominionRace(LegionDominionRace legionDominionRace) {
		this.legionDominionRace = legionDominionRace;
	}

	/** 设置 legion dominion mod type / Sets the legion dominion mod type */
	public void setLegionDominionModType(LegionDominionModType legionDominionModType) {
		this.legionDominionModType = legionDominionModType;
	}

	/**
	 * @return 是否 peace / 是否 peace。 / Whether peace / Whether peace
	 */
	public final boolean isPeace() {
		return legionDominionModType.equals(LegionDominionModType.PEACE);
	}

	/** 是否为领地。 / Whether dominion. */
	public final boolean isDominion() {
		return legionDominionModType.equals(LegionDominionModType.DOMINION);
	}
}
