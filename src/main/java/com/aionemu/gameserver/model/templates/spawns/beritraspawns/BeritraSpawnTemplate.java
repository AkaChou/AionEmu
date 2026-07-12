package com.aionemu.gameserver.model.templates.spawns.beritraspawns;

import com.aionemu.gameserver.model.beritra.BeritraStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 贝里特拉刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class BeritraSpawnTemplate extends SpawnTemplate {
	private int id;
	private BeritraStateType beritraType;

	public BeritraSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public BeritraSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 b state type / Returns the b state type */
	public BeritraStateType getBStateType() {
		return beritraType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 b state type / Sets the b state type */
	public void setBStateType(BeritraStateType beritraType) {
		this.beritraType = beritraType;
	}

	/**
	 * @return Whether beritra invasion
	 */
	public final boolean isBeritraInvasion() {
		return beritraType.equals(BeritraStateType.INVASION);
	}

	/**
	 * @return Whether beritra peace
	 */
	public final boolean isBeritraPeace() {
		return beritraType.equals(BeritraStateType.PEACE);
	}
}
