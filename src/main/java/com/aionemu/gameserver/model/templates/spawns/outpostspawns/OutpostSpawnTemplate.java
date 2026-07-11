package com.aionemu.gameserver.model.templates.spawns.outpostspawns;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 前哨刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

public class OutpostSpawnTemplate extends SpawnTemplate {
	private int id;
	private Race outpostRace;

	public OutpostSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public OutpostSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 获取前哨种族。 / Returns the outpost race. */
	public Race getOutpostRace() {
		return outpostRace;
	}

	/** 设置前哨种族。 / Sets the outpost race. */
	public void setOutpostRace(Race baseRace) {
		this.outpostRace = baseRace;
	}
}
