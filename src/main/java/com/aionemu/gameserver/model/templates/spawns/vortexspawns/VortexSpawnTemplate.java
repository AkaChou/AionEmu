package com.aionemu.gameserver.model.templates.spawns.vortexspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.vortex.VortexStateType;
import lombok.Getter;
import lombok.Setter;

/**
 * 漩涡刷新点模板（静态数据/XML）。
 * Vortex spawn template (static data / XML).
 */

@Getter
@Setter
public class VortexSpawnTemplate extends SpawnTemplate {
	/** 返回 ID / Returns the id */
	private int id;
	/** 获取状态类型。 / Returns the state type. */
	private VortexStateType stateType;

	public VortexSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public VortexSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/**
	 * @return 是否为入侵状态 / Whether invasion
	 */
	public final boolean isInvasion() {
		return stateType.equals(VortexStateType.INVASION);
	}

	/**
	 * @return 是否处于和平状态 / Whether peace
	 */
	public final boolean isPeace() {
		return stateType.equals(VortexStateType.PEACE);
	}
}
