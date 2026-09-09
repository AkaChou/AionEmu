package com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;
import lombok.Getter;
import lombok.Setter;

/**
 * 永恒之塔刷新点模板（静态数据/XML）。
 * Tower of Eternity spawn template (static data/XML).
 */

public class TowerOfEternitySpawnTemplate extends SpawnTemplate {
	/** 返回 ID。 / Returns the id. */
	@Getter
	@Setter
	private int id;
	private TowerOfEternityStateType towerOfEternityType;

	public TowerOfEternitySpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public TowerOfEternitySpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回状态类型。 / Returns the state type. */
	public TowerOfEternityStateType getTStateType() {
		return towerOfEternityType;
	}

	/** 设置状态类型。 / Sets the state type. */
	public void setTStateType(TowerOfEternityStateType towerOfEternityType) {
		this.towerOfEternityType = towerOfEternityType;
	}

	/**
	 * @return 永恒之塔是否开启 / Whether the tower of eternity is open
	 */
	public final boolean isTowerOfEternityOpen() {
		return towerOfEternityType.equals(TowerOfEternityStateType.OPEN);
	}

	/**
	 * @return 永恒之塔是否关闭 / Whether the tower of eternity is closed
	 */
	public final boolean isTowerOfEternityClosed() {
		return towerOfEternityType.equals(TowerOfEternityStateType.CLOSED);
	}
}
