package com.aionemu.gameserver.model.templates.spawns.vortexspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.vortex.VortexStateType;

/**
 * 漩涡刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

public class VortexSpawnTemplate extends SpawnTemplate {
	private int id;
	private VortexStateType stateType;

	public VortexSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public VortexSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取状态类型。 / Returns the state type. */
	public VortexStateType getStateType() {
		return stateType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置状态类型。 / Sets the state type. */
	public void setStateType(VortexStateType stateType) {
		this.stateType = stateType;
	}

	/**
	 * @return Whether invasion / Whether invasion
	 */
	public final boolean isInvasion() {
		return stateType.equals(VortexStateType.INVASION);
	}

	/**
	 * @return 是否 peace / 是否 peace。 / Whether peace / Whether peace
	 */
	public final boolean isPeace() {
		return stateType.equals(VortexStateType.PEACE);
	}
}
