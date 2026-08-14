package com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;

/**
 * 佐希夫无畏舰刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class ZorshivDredgionSpawnTemplate extends SpawnTemplate {
	private int id;
	private ZorshivDredgionStateType zorshivDredgionType;

	public ZorshivDredgionSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public ZorshivDredgionSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 z state type / Returns the z state type */
	public ZorshivDredgionStateType getZStateType() {
		return zorshivDredgionType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 z state type / Sets the z state type */
	public void setZStateType(ZorshivDredgionStateType zorshivDredgionType) {
		this.zorshivDredgionType = zorshivDredgionType;
	}

	/** 是否为登陆。 / Whether landing. */
	public final boolean isLanding() {
		return zorshivDredgionType.equals(ZorshivDredgionStateType.LANDING);
	}

	/**
	 * @return 是否处于和平状态 / Whether peace
	 */
	public final boolean isPeace() {
		return zorshivDredgionType.equals(ZorshivDredgionStateType.PEACE);
	}
}
