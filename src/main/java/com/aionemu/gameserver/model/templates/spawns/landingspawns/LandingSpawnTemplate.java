package com.aionemu.gameserver.model.templates.spawns.landingspawns;

import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 登陆区域刷新点运行时模板：绑定登陆状态类型。
 * Runtime spawn template for landing zones: binds a landing state type.
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

	/** 返回登陆状态类型 / Returns the landing state type */
	public LandingStateType getEStateType() {
		return landingType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置登陆状态类型 / Sets the landing state type */
	public void setEStateType(LandingStateType landingLevel) {
		this.landingType = landingLevel;
	}

	/** 登陆是否开放 / Whether landing is open */
	public final boolean isLandingOpen() {
		return !landingType.equals(LandingStateType.NONE);
	}

	/**
	 * 登陆是否关闭（无状态）。
	 * Whether landing is closed (no state).
	 *
	 * @return 关闭则为 true / Whether landing closed
	 */
	public final boolean isLandingClosed() {
		return landingType.equals(LandingStateType.NONE);
	}
}
