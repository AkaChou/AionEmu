package com.aionemu.gameserver.utils.audit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * GM 在线管理：维护在线 GM 列表，处理登录/下线公告与审计消息广播。
 * GM online registry: tracks online GMs, login/logout announcements and audit broadcasts.
 */
public class GMService {
	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<GMService> instanceProvider;

	/**
	 * 获取服务实例（优先 Spring provider）。
	 * Returns service instance (prefers Spring provider).
	 *
	 * GM service
	 */
	public static final GMService getInstance() {
		ObjectProvider<GMService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<GMService> instanceProvider) {
		GMService.instanceProvider = instanceProvider;
	}

	/**
	 * 在线 GM：objectId → 玩家。
	 * Online GMs: objectId → player.
	 */
	private Map<Integer, Player> gms = new HashMap<Integer, Player>();
	/**
	 * 返回当前在线 GM 集合。
	 * Returns the collection of currently online GMs.
	 *
	 * online GMs
	 */
	public Collection<Player> getGMs() {
		return gms.values();
	}

	/**
	 * 玩家登录：若为 GM 则登记并可全服公告出现。
	 * Player login: registers GM and may broadcast an appear announce.
	 *
	 * logging-in player
	 */
	public void onPlayerLogin(Player player) {
		if (player.isGM()) {
			gms.put(player.getObjectId(), player);
			if (shouldAnnounce(player.getAccessLevel())) {
				PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player, "Announce: " + player.getCustomTag(true) + player.getName() + " appear !!", ChatType.BRIGHT_YELLOW_CENTER), true);
			}
		}
	}

	private boolean shouldAnnounce(byte accessLevel) {
		if (AdminConfig.ANNOUNCE_LEVEL_LIST.equals("*")) {
			return true;
		}
		try {
			for (String level : AdminConfig.ANNOUNCE_LEVEL_LIST.split(",")) {
				if (Byte.parseByte(level) == accessLevel) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException e) {
			return true;
		}
	}

	/**
	 * 玩家下线：从在线 GM 列表移除。
	 * Player logout: removes the player from the online GM map.
	 *
	 * logging-out player
	 */
	public void onPlayerLogedOut(Player player) {
		gms.remove(player.getObjectId());
	}

	/**
	 * GM 恢复可支援状态并通知全服（含会员标签）。
	 * Marks a GM available for support and notifies all players (with membership tags).
	 *
	 * GM player
	 */
	public void onPlayerAvailable(Player player) {
		if (player.isGM()) {
			gms.put(player.getObjectId(), player);
			String adminTag = "%s";
			StringBuilder sb = new StringBuilder(adminTag);

			// * = 高级与 VIP 会员 / * = Premium & VIP Membership
			if (MembershipConfig.PREMIUM_TAG_DISPLAY_ENABLE) {
				switch (player.getClientConnection().getAccount().getMembership()) {
					case 1:
						adminTag = sb.replace(0, sb.length(), MembershipConfig.TAG_PREMIUM).toString();
						break;
					case 2:
						adminTag = sb.replace(0, sb.length(), MembershipConfig.TAG_VIP).toString();
						break;
					}
				}

			Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
			while (iter.hasNext()) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), "Information : " + String.format(adminTag, player.getName()) + " is now available for support!");
			}
		}
	}

	/**
	 * GM 进入不可支援状态并通知全服（含管理标签）。
	 * Marks a GM unavailable for support and notifies all players (with admin tags).
	 *
	 * GM player
	 */
	public void onPlayerUnavailable(Player player) {
		gms.remove(player.getObjectId());
		String adminTag = "%s";
		StringBuilder sb = new StringBuilder(adminTag);


		// * = 服务器职员访问等级 / * = Server Staff Access Level
		if (AdminConfig.ADMIN_TAG_ENABLE && player.isGmMode()) {
			switch (player.getClientConnection().getAccount().getAccessLevel()) {
			case 1:
				adminTag = AdminConfig.ADMIN_TAG_1.replace("%s", sb.toString());
				break;
			case 2:
				adminTag = AdminConfig.ADMIN_TAG_2.replace("%s", sb.toString());
				break;
			case 3:
				adminTag = AdminConfig.ADMIN_TAG_3.replace("%s", sb.toString());
				break;
			case 4:
				adminTag = AdminConfig.ADMIN_TAG_4.replace("%s", sb.toString());
				break;
			case 5:
				adminTag = AdminConfig.ADMIN_TAG_5.replace("%s", sb.toString());
				break;
			}
		}

		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), "Information : " + String.format(adminTag, player.getName()) + " is now unavailable for support!");
		}
	}

	/**
	 * 向所有在线 GM 广播消息。
	 * Broadcasts a message to all online GMs.
	 *
	 * broadcast content
	 */
	public void broadcastMesage(String message) {
		SM_MESSAGE packet = new SM_MESSAGE(0, null, message, ChatType.BRIGHT_YELLOW_CENTER);
		for (Player player : gms.values()) {
			PacketSendUtility.sendPacket(player, packet);
		}
	}

	/**
	 * 非 Spring 环境下的单例持有者。
	 * Singleton holder for non-Spring fallback.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final GMService instance = new GMService();
	}
}
