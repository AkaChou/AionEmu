package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.AStationConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SERVER_IDS;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.services.transfers.AStation;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldType;

/**
 * A-Station（快速通道）跨服服务，处理授权、迁入迁出与账号占用校验。
 * A-Station (fast-track) cross-server service handling auth, move-in/out, and account occupancy checks.
 *
 * @author Ranastic
 */
@Slf4j
public class AStationService {
	private static volatile ObjectProvider<AStationService> instanceProvider;
	/** Accountscurrently 在 Stationmappedplayers / Accounts currently on A-Station mapped to players */
	private ConcurrentMap<Integer, Player> accountsOnAStation = new ConcurrentHashMap<Integer, Player>(1);

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static AStationService getInstance() {
		ObjectProvider<AStationService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<AStationService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		private static final AStationService instance = new AStationService();
	}

	/**
	 * 校验等级后向客户端下发 A-Station 服务器列表。
	 * After level check, sends the A-Station server list to the client.
	 *
	 * @param player 玩家 / player
	 */
	public void checkAuthorizationRequest(Player player) {
		int level = AStationConfig.A_STATION_MAX_LEVEL;
		if (player.getLevel() > level) {
			return;
		}
		PacketSendUtility.sendPacket(player,
				new SM_SERVER_IDS(new AStation(AStationConfig.A_STATION_SERVER_ID, true, 1, level)));
	}

	/**
	 * 将玩家传送至 A-Station 服务器。
	 * Teleports the player to the A-Station server.
	 *
	 * @param player 玩家 / player
	 */
	public void handleMoveThere(Player player) {
		TeleportService2.moveAStation(player, AStationConfig.A_STATION_SERVER_ID, false);
	}

	/**
	 * 将玩家从 A-Station 传回原服。
	 * Teleports the player back from A-Station to the home server.
	 *
	 * @param player 玩家 / player
	 */
	public void handleMoveBack(Player player) {
		TeleportService2.moveAStation(player, AStationConfig.A_STATION_SERVER_ID, true);
	}

	/**
	 * 处理 A-Station 迁入/迁出后的账号占用与加成状态。
	 * Handles account occupancy and bonus state after A-Station move-in/out.
	 *
	 * 玩家 / player
	 * 账号 ID / account id
	 * @param back 是否回原服 / whether returning home
	 */
	public void checkAStationMove(Player player, int accId, boolean back) {
		if (back) {
			accountsOnAStation.remove(accId);
			player.setOnAStation(false);
			PacketSendUtility.sendYellowMessage(player, "You joined the standard server!");
			aStationBonus(player, true);
		} else {
			Player previousPlayer = accountsOnAStation.putIfAbsent(accId, player);
			if (previousPlayer != null) {
				accountsOnAStation.remove(accId, previousPlayer);
				handleMoveBack(player);
				player.setOnAStation(false);
				if (previousPlayer == player) {
					PacketSendUtility.sendYellowMessage(player,
							"You got teleported back to the normal server because you tried to enter the fast track server twice!");
				} else {
					PacketSendUtility.sendYellowMessage(player,
							"You got teleported back to the normal server because something went wrong!");
				}
				return;
			}
			player.setOnAStation(true);
			PacketSendUtility.sendYellowMessage(player, "You joined the fast track server!");
			aStationBonus(player, false);
		}
	}

	/**
	 * A-Station 进出时的加成钩子（当前为空实现）。
	 * Bonus hook on A-Station enter/leave (currently a no-op).
	 *
	 * 玩家 / player
	 * @param off 是否关闭加成 / whether turning bonus off
	 */
	public void aStationBonus(Player player, boolean off) {
	}

	/**
	 * 判断是否为 PvP 类型地图。
	 * Returns whether the world type is a PvP zone.
	 *
	 * @param wt 世界类型 / world type
	 * whether PvP zone
	 */
	public boolean isPvPZone(WorldType wt) {
		return wt == WorldType.BALAUREA || wt == WorldType.PANESTERRA || wt == WorldType.ABYSS;
	}

	/**
	 * 判断是否为普通大陆地图。
	 * Returns whether the world type is a normal continent zone.
	 *
	 * @param wt 世界类型 / world type
	 * @return 是否普通区 / whether normal zone
	 */
	public boolean isNormalZone(WorldType wt) {
		return wt == WorldType.ASMODAE || wt == WorldType.ELYSEA || wt == WorldType.NONE;
	}
}
