package com.aionemu.gameserver.model.templates.spawns.outpostspawns;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 前哨刷新点模板（静态数据/XML）。
 * XML template.
 */

public class OutpostSpawnTemplate extends SpawnTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	private int id;
	/** 获取前哨种族。 / Returns the outpost race. */
	@Getter
	@Setter
	private Race outpostRace;

	public OutpostSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public OutpostSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}
}
