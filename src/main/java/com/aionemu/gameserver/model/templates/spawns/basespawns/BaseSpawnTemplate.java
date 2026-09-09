package com.aionemu.gameserver.model.templates.spawns.basespawns;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler
 */

public class BaseSpawnTemplate extends SpawnTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	private int id;
	/** 获取基础种族。 / Returns the base race. */
	@Getter
	@Setter
	private Race baseRace;

	public BaseSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public BaseSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}
}
