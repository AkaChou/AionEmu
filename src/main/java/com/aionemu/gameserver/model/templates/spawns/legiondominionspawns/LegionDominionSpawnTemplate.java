package com.aionemu.gameserver.model.templates.spawns.legiondominionspawns;

import com.aionemu.gameserver.model.legiondominion.LegionDominionModType;
import com.aionemu.gameserver.model.legiondominion.LegionDominionRace;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 军团领地刷新点模板（静态数据/XML）。
 * Legion dominion spawn template (static data/XML).
 */

public class LegionDominionSpawnTemplate extends SpawnTemplate {
	/** 返回军团领地 ID / Returns the legion dominion id */
	@Getter
	@Setter
	private int legionDominionId;
	/** 获取军团领地种族。 / Returns the legion dominion race. */
	@Getter
	@Setter
	private LegionDominionRace legionDominionRace;
	/** 返回 legion dominion mod type / Returns the legion dominion mod type */
	@Getter
	@Setter
	private LegionDominionModType legionDominionModType;

	public LegionDominionSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public LegionDominionSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/**
	 * @return 是否处于和平状态。 / Whether peace
	  */
	public final boolean isPeace() {
		return legionDominionModType.equals(LegionDominionModType.PEACE);
	}

	/** 是否为领地。 / Whether dominion. */
	public final boolean isDominion() {
		return legionDominionModType.equals(LegionDominionModType.DOMINION);
	}
}
