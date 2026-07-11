package com.aionemu.gameserver.model.templates.spawns.landingspawns;

import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 登陆刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

public class LandingSpawnTemplate extends SpawnTemplate {
	private int id;
	private LandingStateType landingType;

	public LandingSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public LandingSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 e state type / Returns the e state type */
	public LandingStateType getEStateType() {
		return landingType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 e state type / Sets the e state type */
	public void setEStateType(LandingStateType landingLevel) {
		this.landingType = landingLevel;
	}

	/** 是否登陆打开 / Whether landing open */
	public final boolean isLandingOpen() {
		return !landingType.equals(LandingStateType.NONE);
	}

	/**
	 * @return Whether landing closed / Whether landing closed
	 */
	public final boolean isLandingClosed() {
		return landingType.equals(LandingStateType.NONE);
	}
}
