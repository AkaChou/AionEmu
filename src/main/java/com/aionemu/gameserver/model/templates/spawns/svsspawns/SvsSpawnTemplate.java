package com.aionemu.gameserver.model.templates.spawns.svsspawns;

import com.aionemu.gameserver.model.svs.SvsStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 势力战刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class SvsSpawnTemplate extends SpawnTemplate {
	private int id;
	private SvsStateType svsType;

	public SvsSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public SvsSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 p state type / Returns the p state type */
	public SvsStateType getPStateType() {
		return svsType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 p state type / Sets the p state type */
	public void setPStateType(SvsStateType svsType) {
		this.svsType = svsType;
	}

	/** 是否为势力战。 / Whether svs. */
	public final boolean isSvs() {
		return svsType.equals(SvsStateType.SVS);
	}

	/**
	 * @return 是否势力战和平状态 / whether svs peace
	 */
	public final boolean isSvsPeace() {
		return svsType.equals(SvsStateType.PEACE);
	}
}
