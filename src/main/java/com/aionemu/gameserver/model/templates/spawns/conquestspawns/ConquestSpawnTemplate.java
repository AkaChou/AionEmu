package com.aionemu.gameserver.model.templates.spawns.conquestspawns;

import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 征服刷新点模板（静态数据/XML）。
 * Conquest spawn template (static data/XML).
 * @author Rinzler (Encom)
 */

public class ConquestSpawnTemplate extends SpawnTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	private int id;
	private ConquestStateType conquestType;

	public ConquestSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public ConquestSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回占领状态类型。 / Returns the occupation state type. */
	public ConquestStateType getOStateType() {
		return conquestType;
	}

	/** 设置占领状态类型。 / Sets the occupation state type. */
	public void setOStateType(ConquestStateType conquestType) {
		this.conquestType = conquestType;
	}

	/** 是否为征服状态。 / Whether this is a conquest state. */
	public final boolean isConquest() {
		return conquestType.equals(ConquestStateType.CONQUEST);
	}

	/**
	 * 是否为征服和平状态。
	 * Whether this is a conquest peace state.
	 * @return 征服和平状态则为 true / true if conquest peace
	 */
	public final boolean isConquestPeace() {
		return conquestType.equals(ConquestStateType.PEACE);
	}
}
