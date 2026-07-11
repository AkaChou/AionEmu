package com.aionemu.gameserver.model.templates.spawns.iuspawns;

import com.aionemu.gameserver.model.iu.IuStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * IU 活动刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

public class IuSpawnTemplate extends SpawnTemplate {
	private int id;
	private IuStateType iuType;

	public IuSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public IuSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 iu state type / Returns the iu state type */
	public IuStateType getIUStateType() {
		return iuType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 iu state type / Sets the iu state type */
	public void setIUStateType(IuStateType iuType) {
		this.iuType = iuType;
	}

	/** 是否打开 / Whether open*/
	public final boolean isOpen() {
		return iuType.equals(IuStateType.OPEN);
	}

	/** 是否关闭 / Whether closed */
	public final boolean isClosed() {
		return iuType.equals(IuStateType.CLOSED);
	}
}
