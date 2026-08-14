package com.aionemu.gameserver.model.templates.spawns.landingspecialspawns;

import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 登陆特别刷新点模板（静态数据/XML）。
 * Landing Special Spawn Template (static data/XML).
 */

public class LandingSpecialSpawnTemplate extends SpawnTemplate {
	private int id;
	private LandingSpecialStateType landingSpecialType;

	public LandingSpecialSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public LandingSpecialSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回势力状态类型 / Returns the f state type */
	public LandingSpecialStateType getFStateType() {
		return landingSpecialType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置势力状态类型 / Sets the f state type */
	public void setFStateType(LandingSpecialStateType landingSpecialType) {
		this.landingSpecialType = landingSpecialType;
	}

	/**
	 * @return 特殊登陆是否激活 / whether special landing is active
	 */
	public final boolean isSpecialLandingActive() {
		return landingSpecialType.equals(LandingSpecialStateType.ACTIVE);
	}

	/**
	 * @return 特殊登陆是否未激活 / whether special landing is inactive
	 */
	public final boolean isSpecialLandingNoActive() {
		return landingSpecialType.equals(LandingSpecialStateType.NO_ACTIVE);
	}
}
