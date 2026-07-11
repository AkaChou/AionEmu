package com.aionemu.gameserver.world.zone;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 攻城区域实例：额外维护区内玩家集合，支持访问者遍历。
 * Siege zone instance: additionally tracks players inside and supports visitor iteration.
 *
 * @author MrPoke
 */
@Slf4j
public class SiegeZoneInstance extends ZoneInstance {


	/** 区内玩家集合 / players inside the zone */
	private final Map<Integer, Player> players = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());

	/**
	 * 创建攻城区域实例。
	 * Create a siege zone instance.
	 *
	 * map id
	 * @param template 区域模板信息 / zone template info
	 */
	public SiegeZoneInstance(int mapId, ZoneInfo template) {
		super(mapId, template);
	}

	/**
	 * 进入区域；若为玩家则加入玩家集合。
	 * Enter the zone; if the creature is a player, add to the player map.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功进入 / whether enter succeeded
	 */
	@Override
	public boolean onEnter(Creature creature) {
		if (super.onEnter(creature)) {
			if (creature instanceof Player) {
				players.put(creature.getObjectId(), (Player) creature);
			}
			return true;
		}
		return false;
	}

	/**
	 * 离开区域；若为玩家则从玩家集合移除。
	 * Leave the zone; if the creature is a player, remove from the player map.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功离开 / whether leave succeeded
	 */
	@Override
	public synchronized boolean onLeave(Creature creature) {
		if (super.onLeave(creature)) {
			if (creature instanceof Player) {
				players.remove(creature.getObjectId());
			}
			return true;
		}
		return false;
	}

	/**
	 * 对区内所有玩家执行访问者回调。
	 * Run the visitor callback for every player inside the zone.
	 *
	 * @param visitor 玩家访问者 / player visitor
	 */
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

	/**
	 * 快照区内玩家列表（线程安全）。
	 * Snapshot the list of players inside the zone (thread-safe).
	 *
	 * player snapshot
	 */
	private List<Player> playersSnapshot() {
		synchronized (players) {
			return new ArrayList<Player>(players.values());
		}
	}
}
