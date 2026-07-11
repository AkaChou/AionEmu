package com.aionemu.gameserver.services.territory;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionTerritory;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STONESPEAR_SIEGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TERRITORY_LIST;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 领地服务，管理军团领地占领、增益与传送。
 * Territory service managing legion territory conquest, buffs and teleports.
 */
public class TerritoryService {
	private static volatile ObjectProvider<TerritoryService> instanceProvider;
	private TerritoryBuff territoryBuff;
	private Map<Integer, TerritoryBuff> buffs = new HashMap<>();
	private TreeMap<Integer, LegionTerritory> territories = new TreeMap<Integer, LegionTerritory>();
	private TreeMap<Integer, TreeMap<Integer, WorldPosition>> teleporters = new TreeMap<Integer, TreeMap<Integer, WorldPosition>>();

	/**
	 * 初始化全部领地槽位并从数据库加载军团占领状态。
	 * Initializes all territory slots and loads legion ownership from DB.
	 */
	public void initTerritory() {
		LegionService ls = GameCoreGameplayServices.legionService();
		Collection<Legion> legions = new ArrayList<Legion>();
		int counter = 0;
		for (int i = 1; i <= 6; i++) {
			territories.put(i, new LegionTerritory(i));
		}
		for (Integer legionId : DAOManager.getDAO(LegionDAO.class).getLegionIdsWithTerritories()) {
			legions.add(ls.getLegion(legionId));
		}
		for (Legion legion : legions) {
			LegionTerritory territory = legion.getTerritory();
			territories.remove(territory.getId());
			territories.put(territory.getId(), territory);
			counter++;
		}
	}

	/**
	 * 通过领地 NPC 将玩家传送到对应领地坐标。
	 * Teleports a player via territory NPC to the mapped position.
	 *
	 * 玩家 / Player
	 * Teleporter NPC id
	 */
	public void onTeleport(Player player, int npcid) {
		if (player.getLegion() == null || player.getLegion().getTerritory().getId() == 0) {
			return;
		}
		int territoryId = player.getLegion().getTerritory().getId();
		TreeMap<Integer, WorldPosition> teleportMap = teleporters.get(territoryId);
		WorldPosition pos = null;
		if (teleportMap.containsKey(npcid)) {
			pos = teleportMap.get(npcid);
		}
		if (pos != null) {
			TeleportService2.teleportTo(player, pos.getMapId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
		}
	}

	/**
	 * 玩家进世界时下发领地列表。
	 * Sends territory list when a player enters the world.
	 *
	 * @param player 玩家 / Player
	 */
	public void onEnterWorld(Player player) {
		PacketSendUtility.sendPacket(player, new SM_TERRITORY_LIST(territories.values()));
	}

	/**
	 * 发送石矛攻城相关包（当前实现为空）。
	 * Sends Stonespear siege packet (currently no-op).
	 *
	 * @param player 玩家 / Player
	 */
	public void sendStoneSpearPacket(Player player) {
		// PacketSendUtility.sendPacket(player, new
		// SM_STONESPEAR_SIEGE(player.getLegion(), 0));
	}

	/**
	 * 玩家进入所属领地时施加领地增益。
	 * Applies territory buff when a player enters owned territory.
	 *
	 * @param player 玩家 / Player
	 */
	public void onEnterTerritory(Player player) {
		if (player.getLegion() == null || player.getLegion().getTerritory().getId() == 0) {
			return;
		}
		territoryBuff = new TerritoryBuff();
		territoryBuff.applyEffect(player);
		buffs.put(player.getObjectId(), territoryBuff);
	}

	/**
	 * 玩家离开领地时移除增益。
	 * Removes territory buff when a player leaves.
	 *
	 * @param player 玩家 / Player
	 */
	public void onLeaveTerritory(Player player) {
		if (player.getLegion() == null || player.getLegion().getTerritory().getId() == 0) {
			return;
		}
		if (buffs.containsKey(player.getObjectId())) {
			buffs.get(player.getObjectId()).endEffect(player);
			buffs.remove(player.getObjectId());
		}
	}

	/**
	 * 扫描同图敌对种族玩家（入侵者探测）。
	 * Scans same-map enemy-race players (intruder detection).
	 *
	 * @param player 发起扫描的玩家 / Scanning player
	 */
	public void scanForIntruders(Player player) {
		Collection<Player> players = new ArrayList<Player>();
		Iterator<Player> playerIt = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (playerIt.hasNext()) {
			Player enemy = playerIt.next();
			if (player.getWorldId() == enemy.getWorldId() && player.getRace() != enemy.getRace()) {
				players.add(enemy);
			}
		}
		// PacketSendUtility.sendPacket(player, new SM_SERIAL_KILLER(players, false));
	}

	/**
	 * 军团征服指定领地并广播结果。
	 * Assigns a territory to a legion and broadcasts the result.
	 *
	 * Legion
	 * @param id 领地 ID / Territory id
	 */
	public void onConquerTerritory(Legion legion, int id) {
		if (legion.ownsTerretory()) {
			onLooseTerritory(legion);
		}
		LegionTerritory territory = new LegionTerritory(id);
		territory.setLegionId(legion.getLegionId());
		territory.setLegionName(legion.getLegionName());
		legion.setTerritory(territory);
		territories.remove(id);
		territories.put(id, territory);
		broadcastTerritoryList(territories);
		broadcastToLegion(legion);
	}

	/**
	 * 向军团成员广播领地/石矛信息。
	 * Broadcasts territory/Stonespear info to legion members.
	 *
	 * Legion
	 */
	private void broadcastToLegion(Legion legion) {
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_INFO(legion));
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_STONESPEAR_SIEGE(legion, 0));
	}

	/**
	 * 军团失去领地并广播空置状态。
	 * Clears a legion's territory and broadcasts the vacant state.
	 *
	 * Legion
	 */
	public void onLooseTerritory(Legion legion) {
		int oldTerritoryId = legion.getTerritory().getId();
		legion.clearTerritory();
		if (oldTerritoryId == 0) {
		}
		LegionTerritory fakeTerritory = new LegionTerritory(oldTerritoryId);
		territories.remove(oldTerritoryId);
		territories.put(oldTerritoryId, fakeTerritory);
		TreeMap<Integer, LegionTerritory> lostTerr = new TreeMap<Integer, LegionTerritory>();
		lostTerr.put(oldTerritoryId, fakeTerritory);
		broadcastTerritoryList(lostTerr);
		broadcastToLegion(legion);
	}

	/**
	 * 向在线玩家广播领地列表。
	 * Broadcasts territory list to online players.
	 *
	 * @param terr 领地映射 / Territory map
	 */
	public void broadcastTerritoryList(TreeMap<Integer, LegionTerritory> terr) {
		Collection<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();
		for (Player player : players) {
			if (!player.isOnline()) {
				return;
			}
			PacketSendUtility.sendPacket(player, new SM_TERRITORY_LIST(terr.values()));
		}
	}

	/**
	 * 获取全部领地集合。
	 * Returns all territories.
	 *
	 * Territory collection
	 */
	public Collection<LegionTerritory> getTerritories() {
		return territories.values();
	}

	/**
	 * 获取服务单例（支持 Spring 注入回退）。
	 * Returns the service singleton (Spring provider with fallback).
	 *
	 * Service instance
	 */
	public static TerritoryService getInstance() {
		ObjectProvider<TerritoryService> provider = instanceProvider;
		if (provider == null) {
			return TerritoryService.SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> TerritoryService.SingletonHolder.instance);
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<TerritoryService> instanceProvider) {
		TerritoryService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final TerritoryService instance = new TerritoryService();
	}
}
