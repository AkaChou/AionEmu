package com.aionemu.gameserver.model.templates.spawns.moltenusspawns;

import com.aionemu.gameserver.model.moltenus.MoltenusStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 熔岩魔刷新点模板（静态数据/XML）。
 * Moltenus spawn template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

public class MoltenusSpawnTemplate extends SpawnTemplate {
	private int id;
	private MoltenusStateType moltenusType;

	public MoltenusSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public MoltenusSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 m state type / Returns the m state type */
	public MoltenusStateType getMStateType() {
		return moltenusType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 m state type / Sets the m state type */
	public void setMStateType(MoltenusStateType moltenusType) {
		this.moltenusType = moltenusType;
	}

	/**
	 * @return 是否处于战斗状态。 / Whether it is in fight state.
	 */
	public final boolean isFight() {
		return moltenusType.equals(MoltenusStateType.FIGHT);
	}

	/**
	 * @return 是否处于和平状态。 / Whether it is in peace state.
	 */
	public final boolean isPeace() {
		return moltenusType.equals(MoltenusStateType.PEACE);
	}
}
