package com.aionemu.gameserver.model.templates.spawns.riftspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 裂隙刷新点模板（静态数据/XML）。
 * Rift spawn template (static data / XML).
 *
 * @author Source
 */
public class RiftSpawnTemplate extends SpawnTemplate {

	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	private int id;

	public RiftSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public RiftSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}
}
