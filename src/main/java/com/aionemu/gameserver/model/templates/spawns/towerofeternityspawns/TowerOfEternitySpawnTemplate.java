package com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;

/**
 * 高塔 Of 永恒刷新点模板（静态数据/XML）。
 * XML template.
 */

public class TowerOfEternitySpawnTemplate extends SpawnTemplate {
	private int id;
	private TowerOfEternityStateType towerOfEternityType;

	public TowerOfEternitySpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public TowerOfEternitySpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 t state type / Returns the t state type */
	public TowerOfEternityStateType getTStateType() {
		return towerOfEternityType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 t state type / Sets the t state type */
	public void setTStateType(TowerOfEternityStateType towerOfEternityType) {
		this.towerOfEternityType = towerOfEternityType;
	}

	/**
	 * @return 永恒之塔是否开启。 / Whether tower of eternity open
	  */
	public final boolean isTowerOfEternityOpen() {
		return towerOfEternityType.equals(TowerOfEternityStateType.OPEN);
	}

	/**
	 * @return 永恒之塔是否关闭。 / Whether tower of eternity closed
	  */
	public final boolean isTowerOfEternityClosed() {
		return towerOfEternityType.equals(TowerOfEternityStateType.CLOSED);
	}
}
