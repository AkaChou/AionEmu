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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final GMService getInstance() {
		ObjectProvider<GMService> provider = instanceProvider;
		GMService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("GMService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<GMService> instanceProvider) {
		GMService.instanceProvider = instanceProvider;
	}

	/**
	 * 在线 GM：objectId → 玩家。
	 * Online GMs: objectId → player.
	 */
	private final Map<Integer, Player> gms = new HashMap<>();
	/**
	 * 返回当前在线 GM 集合。
	 * Returns the collection of currently online GMs.
	 * @return 当前在线 GM 集合 / collection of online GMs
	 */
	public Collection<Player> getGMs() {
		return gms.values();
	}

	/**
	 * 玩家登录：若为 GM 则登记并可全服公告出现。
	 * Player login: registers GM and may broadcast an appear announce.
	 * @param player 登录玩家 / logging-in player
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
	 * @param player 登出玩家 / logging-out player
	 */
	public void onPlayerLogedOut(Player player) {
		gms.remove(player.getObjectId());
	}

	/**
	 * GM 恢复可支援状态并通知全服（含会员标签）。
	 * Marks a GM available for support and notifies all players (with membership tags).
	 * @param player GM 玩家 / GM player
	 */
	public void onPlayerAvailable(Player player) {
		if (player.isGM()) {
			gms.put(player.getObjectId(), player);
			String adminTag = "%s";
			StringBuilder sb = new StringBuilder(adminTag);

			// * = 高级与 VIP 会员 / * = Premium & VIP Membership
			if (MembershipConfig.PREMIUM_TAG_DISPLAY_ENABLE) {
				adminTag = switch (player.getClientConnection().getAccount().getMembership()) {
					case 1 -> sb.replace(0, sb.length(), MembershipConfig.TAG_PREMIUM).toString();
					case 2 -> sb.replace(0, sb.length(), MembershipConfig.TAG_VIP).toString();
					default -> adminTag;
				};
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
	 * @param player GM 玩家 / GM player
	 */
	public void onPlayerUnavailable(Player player) {
		gms.remove(player.getObjectId());
		String adminTag = "%s";
		StringBuilder sb = new StringBuilder(adminTag);


		// * = 服务器职员访问等级 / * = Server Staff Access Level
		if (AdminConfig.ADMIN_TAG_ENABLE && player.isGmMode()) {
            adminTag = switch (player.getClientConnection().getAccount().getAccessLevel()) {
                case 1 -> AdminConfig.ADMIN_TAG_1.replace("%s", sb.toString());
                case 2 -> AdminConfig.ADMIN_TAG_2.replace("%s", sb.toString());
                case 3 -> AdminConfig.ADMIN_TAG_3.replace("%s", sb.toString());
                case 4 -> AdminConfig.ADMIN_TAG_4.replace("%s", sb.toString());
                case 5 -> AdminConfig.ADMIN_TAG_5.replace("%s", sb.toString());
                default -> adminTag;
            };
		}

		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), "Information : " + String.format(adminTag, player.getName()) + " is now unavailable for support!");
		}
	}

	/**
	 * 向所有在线 GM 广播消息。
	 * Broadcasts a message to all online GMs.
	 * @param message 广播内容 / broadcast content
	 */
	public void broadcastMesage(String message) {
		SM_MESSAGE packet = new SM_MESSAGE(0, null, message, ChatType.BRIGHT_YELLOW_CENTER);
		for (Player player : gms.values()) {
			PacketSendUtility.sendPacket(player, packet);
		}
	}
}
