package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 玩家活动服务，按计划推送/结算玩家侧活动。
 * Player event service pushing/settling player-side events on schedule.
 */
public class PlayerEventService {
    /**
     * -- SETTER --
     * 设置实例提供者（Spring 注入）。
     *  Sets the instance provider (Spring injection).
     *
     *  @param instanceProvider 实例提供者 / instance provider
     */
    @Setter
    private static volatile ObjectProvider<PlayerEventService> instanceProvider;
	private Future<?> awakeTask;
	private Future<?> vipTask;

	public PlayerEventService() {
		reload();
	}

	/**
	 * 重载配置。
	 * Reloads configuration.
	 */
	public synchronized void reload() {
		if (awakeTask != null) {
			awakeTask.cancel(false);
			awakeTask = null;
		}
		if (vipTask != null) {
			vipTask.cancel(false);
			vipTask = null;
		}
		if (!EventsConfig.EVENT_ENABLED) {
			return;
		}
		if (EventsConfig.ENABLE_AWAKE_EVENT && EventsConfig.SEED_TRANSFORMATION_PERIOD > 0) {
			long period = TimeUnit.MINUTES.toMillis(EventsConfig.SEED_TRANSFORMATION_PERIOD);
			EventAwake awake = new EventAwake();
			awakeTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
					() -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(awake),
					period, period);
		}
		if (EventsConfig.ENABLE_VIP_TICKETS && EventsConfig.VIP_TICKETS_PERIOD > 0) {
			long period = TimeUnit.MINUTES.toMillis(EventsConfig.VIP_TICKETS_PERIOD);
			AnnounceVIPTickets vipTickets = new AnnounceVIPTickets();
			vipTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
					() -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(vipTickets),
					period, period);
		}
	}

	private static final class AnnounceVIPTickets implements Visitor<Player> {
		@Override
		/**
		 * visit 方法。
		 * visit method.
		 *
		 * @param player 玩家 / player
		 */
		public void visit(Player player) {
			if (EventsConfig.ENABLE_VIP_TICKETS) {
				if (player.getClientConnection().getAccount().getMembership() == 1) {
					HTMLService.sendGuideHtml(player, "Premium_Benefits");
				}
				if (player.getClientConnection().getAccount().getMembership() == 2) {
					HTMLService.sendGuideHtml(player, "Vip_Benefits");
				}
				if (player.getClientConnection().getAccount().getMembership() == 0) {
					HTMLService.sendGuideHtml(player, "Regular_Benefits");
					// 可用 VIP 福利变得更强。\n 参见商城 VIP 票。 / You can become stronger with the VIP benefits.\n See the VIP Tickets in the
					// 游戏内商店。 / in-game shop.
				    PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_VIP_LOBBY_NOTICE_CASE12_POPUP_01, 0, 0));
				}
			}
		}
	};

	private static final class EventAwake implements Visitor<Player> {
		@Override
		/**
		 * visit 方法。
		 * visit method.
		 *
		 * @param player 玩家 / player
		 */
		public void visit(Player player) {
			if (EventsConfig.ENABLE_AWAKE_EVENT) {
				if (player.getLevel() >= 10 && player.getLevel() <= 64) {
					HTMLService.sendGuideHtml(player, "Event_Awake_10");
				}
				if (player.getLevel() >= 65 && player.getLevel() <= 83) {
					HTMLService.sendGuideHtml(player, "Event_Awake_65");
				}
			}
		}
	};

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static PlayerEventService getInstance() {
		ObjectProvider<PlayerEventService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

    private static class SingletonHolder {
		protected static final PlayerEventService instance = new PlayerEventService();
	}
}
