package com.aionemu.gameserver.model.templates.spawns.landingspecialspawns;

import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 登陆 Special 刷新点模板（静态数据/XML）。
 * XML template. / XML template.
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

	/** 返回 f state type / Returns the f state type */
	public LandingSpecialStateType getFStateType() {
		return landingSpecialType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 f state type / Sets the f state type */
	public void setFStateType(LandingSpecialStateType landingSpecialType) {
		this.landingSpecialType = landingSpecialType;
	}

	/**
	 * @return Whether special landing active / Whether special landing active
	 */
	public final boolean isSpecialLandingActive() {
		return landingSpecialType.equals(LandingSpecialStateType.ACTIVE);
	}

	/**
	 * @return Whether special landing no active / Whether special landing no active
	 */
	public final boolean isSpecialLandingNoActive() {
		return landingSpecialType.equals(LandingSpecialStateType.NO_ACTIVE);
	}
}
