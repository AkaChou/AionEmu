package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DISPUTE_LAND;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneAttributes;

/**
 * 纷争之地服务：按计划开启/关闭，并在指定地图广播与同步 PvP 状态。
 * Dispute Land service: schedules open/close and broadcasts/syncs PvP state for listed maps.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class DisputeLandService {
	private static volatile ObjectProvider<DisputeLandService> instanceProvider;
	private boolean active;
	private List<Integer> worlds = new ArrayList<>();

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public DisputeLandService() {
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static DisputeLandService getInstance() {
		ObjectProvider<DisputeLandService> provider = instanceProvider;
		if (provider == null) {
			return DisputeLandServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> DisputeLandServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<DisputeLandService> instanceProvider) {
		DisputeLandService.instanceProvider = instanceProvider;
	}

	/**
	 * 初始化纷争之地：注册世界 ID，并在启用时按 cron 调度启停。
	 * Initializes Dispute Land: registers world IDs and schedules open/close when enabled.
	 */
	public void initDisputeLand() {
		if (CustomConfig.DISPUTE_LAND_ENABLED) {
			log.info(I18n.get("log.87d65384cfca"));
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					if (isActive()) {
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								setActive(false);
							}
						}, CustomConfig.DISPUTE_LAND_DURATION * 3600 * 1000);
					}
				}
			}, () -> CustomConfig.DISPUTE_LAND_SCHEDULE);
		}
		worlds.add(210020000); // Eltnen.
		worlds.add(210040000); // Heiron.
		worlds.add(210050000); // Inggison.
		worlds.add(210130000); // Inggison [Master Server].
		worlds.add(210060000); // Theobomos.
		worlds.add(220020000); // Morheim.
		worlds.add(220040000); // Beluslan.
		worlds.add(220050000); // Brusthonin.
		worlds.add(220070000); // Gelkmaros.
		worlds.add(220140000); // Gelkmaros [Master Server].
		// 4.7
		worlds.add(600090000); // Kaldor.
		worlds.add(600100000); // Levinshor.
		// 4.8
		worlds.add(210070000); // Cygnea.
		worlds.add(220080000); // Enshar.
		// 5.0
		worlds.add(210100000); // Iluma.
		worlds.add(220110000); // Norsvold.
	}

	/**
	 * 是否处于激活状态。
	 * Whether Dispute Land is currently active.
	 *
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * 设置激活状态，同步世界 PvP 选项并向全体玩家广播。
	 * Sets active state, syncs world PvP options, and broadcasts to all players.
	 *
	 * @param value 是否激活 / whether active
	 */
	public void setActive(boolean value) {
		active = value;
		syncState();
		broadcast();
	}

	private void syncState() {
		for (int world : worlds) {
			if (world == 210020000 || // Eltnen.
					world == 210040000 || // Heiron.
					world == 210050000 || // Inggison.
					world == 210130000 || // Inggison [Master Server].
					world == 210060000 || // Theobomos.
					world == 210070000 || // Cygnea.
					world == 220020000 || // Morheim.
					world == 220040000 || // Beluslan.
					world == 220050000 || // Brusthonin.
					world == 220070000 || // Gelkmaros.
					world == 220140000 || // Gelkmaros [Master Server].
					world == 220080000 || // Enshar.
					world == 210100000 || // Iluma.
					world == 220110000 || // Norsvold.
					world == 600090000 || // Kaldor.
					world == 600100000) { // Levinshor.
				continue;
			}
			if (active) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(world).setWorldOption(ZoneAttributes.PVP_ENABLED);
			} else {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(world).removeWorldOption(ZoneAttributes.PVP_ENABLED);
			}
		}
	}

	private void broadcast(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DISPUTE_LAND(worlds, active));
	}

	private void broadcast() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				broadcast(player);
			}
		});
	}

	/**
	 * 玩家登录时下发当前纷争之地状态。
	 * Sends current Dispute Land state to the player on login.
	 *
	 * logging-in player
	 */
	public void onLogin(Player player) {
		broadcast(player);
	}

	private static class DisputeLandServiceHolder {
		private static final DisputeLandService INSTANCE = new DisputeLandService();
	}
}
