package com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns;

import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 动态裂隙刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

public class DynamicRiftSpawnTemplate extends SpawnTemplate {
	private int id;
	private DynamicRiftStateType dynamicRiftType;

	public DynamicRiftSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public DynamicRiftSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 d state type / Returns the d state type */
	public DynamicRiftStateType getDStateType() {
		return dynamicRiftType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 d state type / Sets the d state type */
	public void setDStateType(DynamicRiftStateType dynamicRiftType) {
		this.dynamicRiftType = dynamicRiftType;
	}

	/**
	 * @return Whether dynamic rift open / Whether dynamic rift open
	 */
	public final boolean isDynamicRiftOpen() {
		return dynamicRiftType.equals(DynamicRiftStateType.OPEN);
	}

	/**
	 * @return Whether dynamic rift closed / Whether dynamic rift closed
	 */
	public final boolean isDynamicRiftClosed() {
		return dynamicRiftType.equals(DynamicRiftStateType.CLOSED);
	}
}
