package com.aionemu.gameserver.model.templates.spawns.instanceriftspawns;

import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 副本裂隙刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

public class InstanceRiftSpawnTemplate extends SpawnTemplate {
	private int id;
	private InstanceRiftStateType instanceRiftType;

	public InstanceRiftSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public InstanceRiftSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 e state type / Returns the e state type */
	public InstanceRiftStateType getEStateType() {
		return instanceRiftType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 e state type / Sets the e state type */
	public void setEStateType(InstanceRiftStateType instanceRiftType) {
		this.instanceRiftType = instanceRiftType;
	}

	/**
	 * @return Whether instance rift open / Whether instance rift open
	 */
	public final boolean isInstanceRiftOpen() {
		return instanceRiftType.equals(InstanceRiftStateType.OPEN);
	}

	/**
	 * @return Whether instance rift closed / Whether instance rift closed
	 */
	public final boolean isInstanceRiftClosed() {
		return instanceRiftType.equals(InstanceRiftStateType.CLOSED);
	}
}
