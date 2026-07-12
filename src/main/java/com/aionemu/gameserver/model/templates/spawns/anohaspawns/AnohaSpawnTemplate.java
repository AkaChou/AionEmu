package com.aionemu.gameserver.model.templates.spawns.anohaspawns;

import com.aionemu.gameserver.model.anoha.AnohaStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 阿诺哈刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class AnohaSpawnTemplate extends SpawnTemplate {
	private int id;
	private AnohaStateType anohaType;

	public AnohaSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public AnohaSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 c state type / Returns the c state type */
	public AnohaStateType getCStateType() {
		return anohaType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 c state type / Sets the c state type */
	public void setCStateType(AnohaStateType anohaType) {
		this.anohaType = anohaType;
	}

	/**
	 * @return Whether anoha fight
	 */
	public final boolean isAnohaFight() {
		return anohaType.equals(AnohaStateType.FIGHT);
	}

	/**
	 * @return Whether anoha peace
	 */
	public final boolean isAnohaPeace() {
		return anohaType.equals(AnohaStateType.PEACE);
	}
}
