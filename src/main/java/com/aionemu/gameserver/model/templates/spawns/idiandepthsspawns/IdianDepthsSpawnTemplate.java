package com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns;

import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 伊迪安深渊刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class IdianDepthsSpawnTemplate extends SpawnTemplate {
	private int id;
	private IdianDepthsStateType idianDepthsType;

	public IdianDepthsSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public IdianDepthsSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 i state type / Returns the i state type */
	public IdianDepthsStateType getIStateType() {
		return idianDepthsType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 i state type / Sets the i state type */
	public void setIStateType(IdianDepthsStateType idianDepthsType) {
		this.idianDepthsType = idianDepthsType;
	}

	/**
	 * @return Whether idian depths open
	 */
	public final boolean isIdianDepthsOpen() {
		return idianDepthsType.equals(IdianDepthsStateType.OPEN);
	}

	/**
	 * @return Whether idian depths closed
	 */
	public final boolean isIdianDepthsClosed() {
		return idianDepthsType.equals(IdianDepthsStateType.CLOSED);
	}
}
