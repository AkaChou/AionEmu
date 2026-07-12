package com.aionemu.gameserver.model.siege;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLocationTemplate;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.SiegeZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;

/**
 * 要塞据点模型。
 * Siege Location model.
 */

@Slf4j

public class SiegeLocation implements ZoneHandler {
	public static final int STATE_INVULNERABLE = 0;
	public static final int STATE_VULNERABLE = 1;

	protected SiegeLocationTemplate template;
	protected int locationId;
	protected SiegeType type;
	protected int worldId;
	protected SiegeRace siegeRace = SiegeRace.BALAUR;
	protected int legionId;
	protected long lastArtifactActivation;
	private boolean vulnerable;
	private int nextState;
	protected List<SiegeZoneInstance> zone;
	private List<SiegeShield> shields;
	private boolean isUnderShield;
	private boolean canTeleport;
	protected int siegeDuration;
	protected int influenceValue;
	private final Map<Integer, Creature> creatures = Collections.synchronizedMap(new LinkedHashMap<Integer, Creature>());
	private final Map<Integer, Player> players = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());
	protected int buffId;
	protected int buffIdA;
	protected int buffIdE;
	protected int outpostId;

	public SiegeLocation() {
	}

	public SiegeLocation(SiegeLocationTemplate template) {
		this.template = template;
		this.locationId = template.getId();
		this.worldId = template.getWorldId();
		this.type = template.getType();
		this.siegeDuration = template.getSiegeDuration();
		this.zone = new ArrayList<SiegeZoneInstance>();
		this.influenceValue = template.getInfluenceValue();
	}

	/** 获取模板。 / Returns the template. */
	public SiegeLocationTemplate getTemplate() {
		return template;
	}

	/** 返回地点 ID / Returns the location id */
	public int getLocationId() {
		return this.locationId;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.worldId;
	}

	/** 获取类型。 / Returns the type. */
	public SiegeType getType() {
		return this.type;
	}

	/** 返回攻城时长 / Returns the siege duration*/
	public int getSiegeDuration() {
		return siegeDuration;
	}

	/** 获取种族。 / Returns the race. */
	public SiegeRace getRace() {
		return this.siegeRace;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(SiegeRace siegeRace) {
		this.siegeRace = siegeRace;
	}

	/** 返回军团 ID / Returns the legion id */
	public int getLegionId() {
		return this.legionId;
	}

	/** 设置军团 ID / Sets the legion id */
	public void setLegionId(int legionId) {
		this.legionId = legionId;
	}

	/** 返回 next state / Returns the next state */
	public int getNextState() {
		return nextState;
	}

	/** 设置 next state / Sets the next state */
	public void setNextState(int nextState) {
		this.nextState = nextState;
	}

	/**
	 * @return Whether vulnerable
	 */
	public boolean isVulnerable() {
		return this.vulnerable;
	}

	/**
	 * @return Whether under shield
	 */
	public boolean isUnderShield() {
		return this.isUnderShield;
	}

	/** 设置 under shield / Sets the under shield */
	public void setUnderShield(boolean value) {
		this.isUnderShield = value;
		if (shields != null) {
			for (SiegeShield shield : shields) {
				shield.setEnabled(value);
			}
		}
	}

	/** 设置 shields / Sets the shields */
	public void setShields(List<SiegeShield> shields) {
		this.shields = shields;
		log.debug("Attached shields for locId: " + locationId);
		for (SiegeShield shield : shields) {
			log.debug(shield.toString());
		}
	}

	/**
	 * @param player 是否可传送。 / Whether can teleport
	  */
	public boolean isCanTeleport(Player player) {
		return canTeleport;
	}

	/** 设置 can teleport / Sets the can teleport */
	public void setCanTeleport(boolean canTeleport) {
		this.canTeleport = canTeleport;
	}

	/** 设置 vulnerable / Sets the vulnerable */
	public void setVulnerable(boolean value) {
		this.vulnerable = value;
	}

	/** 返回影响力值 / Returns the influence value*/
	public int getInfluenceValue() {
		return influenceValue;
	}

	/** 获取区域。 / Returns the zone. */
	public List<SiegeZoneInstance> getZone() {
		return zone;
	}

	/** 添加区域。 / Adds zone. */
	public void addZone(SiegeZoneInstance zone) {
		this.zone.add(zone);
		zone.addHandler(this);
	}

	/**
	 * @param creature Whether inside location
	 */
	public boolean isInsideLocation(Creature creature) {
		if (zone.isEmpty()) {
			return false;
		}
		for (int i = 0; i < zone.size(); i++) {
			if (zone.get(i).isInsideCreature(creature)) {
				return true;
			}
		}
		return false;
	}

	/** 是否处于活动攻城区域 / Whether in active siege zone */
	public boolean isInActiveSiegeZone(Player player) {
		if (isVulnerable() && isInsideLocation(player)) {
			return true;
		}
		return false;
	}

	/** 清空位置。 / Clear location. */
	public void clearLocation() {
	}

	/** 在 EnterZone / On Enter Zone */
	@Override
	public synchronized void onEnterZone(Creature creature, ZoneInstance zone) {
		if (!creatures.containsKey(creature.getObjectId())) {
			creatures.put(creature.getObjectId(), creature);
			if (creature instanceof Player) {
				players.put(creature.getObjectId(), (Player) creature);
			}
		}
	}

	/** 在 LeaveZone / On Leave Zone */
	@Override
	public synchronized void onLeaveZone(Creature creature, ZoneInstance zone) {
		if (!this.isInsideLocation(creature)) {
			creatures.remove(creature.getObjectId());
			players.remove(creature.getObjectId());
		}
	}

	/** 对所有玩家执行 / do On All Players. */
	public void doOnAllPlayers(Visitor<Player> visitor) {
		try {
			for (Player player : playersSnapshot()) {
				if (player != null) {
					visitor.visit(player);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.cc03391ccf0f", ex));
		}
	}

	/** 返回 creatures / Returns the creatures */
	public Map<Integer, Creature> getCreatures() {
		return creatures;
	}

	/** 返回 creatures snapshot / Returns the creatures snapshot */
	public List<Creature> getCreaturesSnapshot() {
		synchronized (creatures) {
			return new ArrayList<Creature>(creatures.values());
		}
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}

	private List<Player> playersSnapshot() {
		synchronized (players) {
			return new ArrayList<Player>(players.values());
		}
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId = template.getBuffId();
	}

	/** 返回增益 ID / Returns the buff id a */
	public int getBuffIdA() {
		return buffIdA = template.getBuffIdA();
	}

	/** 返回增益 IDe / Returns the buff id e */
	public int getBuffIdE() {
		return buffIdE = template.getBuffIdE();
	}

	/** 返回 outpost id / Returns the outpost id */
	public int getOutpostId() {
		return outpostId = template.getOutpostId();
	}
}
