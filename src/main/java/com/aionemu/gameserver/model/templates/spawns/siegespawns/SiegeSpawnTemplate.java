package com.aionemu.gameserver.model.templates.spawns.siegespawns;

import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.siege.SiegeSpawnType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 要塞刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
public class SiegeSpawnTemplate extends SpawnTemplate {

	private int siegeId;
	private SiegeRace siegeRace;
	private SiegeSpawnType siegeSpawnType;
	private SiegeModType siegeModType;

	public SiegeSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public SiegeSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回攻城 ID / Returns the siege id */
	public int getSiegeId() {
		return siegeId;
	}

	/** 获取要塞种族。 / Returns the siege race. */
	public SiegeRace getSiegeRace() {
		return siegeRace;
	}

	/** 获取要塞刷新点类型。 / Returns the siege spawn type. */
	public SiegeSpawnType getSiegeSpawnType() {
		return siegeSpawnType;
	}

	/** 获取要塞模式类型。 / Returns the siege mod type. */
	public SiegeModType getSiegeModType() {
		return siegeModType;
	}

	/** 设置攻城 ID / Sets the siege id */
	public void setSiegeId(int siegeId) {
		this.siegeId = siegeId;
	}

	/** 设置要塞种族。 / Sets the siege race. */
	public void setSiegeRace(SiegeRace siegeRace) {
		this.siegeRace = siegeRace;
	}

	/** 设置要塞刷新点类型。 / Sets the siege spawn type. */
	public void setSiegeSpawnType(SiegeSpawnType siegeSpawnType) {
		this.siegeSpawnType = siegeSpawnType;
	}

	/** 设置要塞模式类型。 / Sets the siege mod type. */
	public void setSiegeModType(SiegeModType siegeModType) {
		this.siegeModType = siegeModType;
	}

	/**
	 * @return 是否处于和平状态。 / Whether peace
	  */
	public final boolean isPeace() {
		return siegeModType.equals(SiegeModType.PEACE);
	}

	/** 是否要塞。 / Whether Siege. */
	public final boolean isSiege() {
		return siegeModType.equals(SiegeModType.SIEGE);
	}

	/**
	 * @return 是否处于袭击状态 / whether assault
	 */
	public final boolean isAssault() {
		return siegeModType.equals(SiegeModType.ASSAULT);
	}
}
