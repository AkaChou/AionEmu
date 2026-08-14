package com.aionemu.gameserver.model.templates.spawns.rvrspawns;

import com.aionemu.gameserver.model.rvr.RvrStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 阵营战刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class RvrSpawnTemplate extends SpawnTemplate {
	private int id;
	private RvrStateType rvrType;

	public RvrSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public RvrSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 r state type / Returns the r state type */
	public RvrStateType getRStateType() {
		return rvrType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 r state type / Sets the r state type */
	public void setRStateType(RvrStateType rvrType) {
		this.rvrType = rvrType;
	}

	/** 是否为阵营战。 / Whether rvr. */
	public final boolean isRvr() {
		return rvrType.equals(RvrStateType.RVR);
	}

	/**
	 * 是否为和平状态。
	 * Whether the rvr state is peace.
	 *
	 * @return 是否和平 / whether rvr peace
	 */
	public final boolean isRvrPeace() {
		return rvrType.equals(RvrStateType.PEACE);
	}
}
