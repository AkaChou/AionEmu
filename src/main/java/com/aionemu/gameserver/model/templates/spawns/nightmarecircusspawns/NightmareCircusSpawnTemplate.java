package com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns;

import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 梦魇马戏团刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class NightmareCircusSpawnTemplate extends SpawnTemplate {
	private int id;
	private NightmareCircusStateType nightmareCircusType;

	public NightmareCircusSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public NightmareCircusSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 n state type / Returns the n state type */
	public NightmareCircusStateType getNStateType() {
		return nightmareCircusType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 n state type / Sets the n state type */
	public void setNStateType(NightmareCircusStateType nightmareCircusType) {
		this.nightmareCircusType = nightmareCircusType;
	}

	/**
	 * @return 马戏团是否开启 / whether circus is open
	 */
	public final boolean isCircusOpen() {
		return nightmareCircusType.equals(NightmareCircusStateType.OPEN);
	}

	/**
	 * @return 马戏团是否关闭 / whether circus is closed
	 */
	public final boolean isCircusClosed() {
		return nightmareCircusType.equals(NightmareCircusStateType.CLOSED);
	}
}
