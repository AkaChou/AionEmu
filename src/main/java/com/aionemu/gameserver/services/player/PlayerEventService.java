/**
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

public class PlayerEventService {
	private static volatile ObjectProvider<PlayerEventService> instanceProvider;
	private static final Logger log = LoggerFactory.getLogger(PlayerEventService.class);

	public PlayerEventService() {
		/**
		 * Event Awake [Event JAP] http://event2.ncsoft.jp/1.0/aion/1503awake/
		 */
		final EventAwake awake = new EventAwake();
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				World.getInstance().doOnAllPlayers(awake);
			}
		}, EventsConfig.SEED_TRANSFORMATION_PERIOD * 60000, EventsConfig.SEED_TRANSFORMATION_PERIOD * 60000);
		/**
		 * VIP Tickets.
		 */
		final AnnounceVIPTickets vipTickets = new AnnounceVIPTickets();
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				World.getInstance().doOnAllPlayers(vipTickets);
			}
		}, EventsConfig.VIP_TICKETS_PERIOD * 60000, EventsConfig.VIP_TICKETS_PERIOD * 60000);
	}

	private static final class AnnounceVIPTickets implements Visitor<Player> {
		@Override
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
					// You can become stronger with the VIP benefits.\n See the VIP Tickets in the
					// in-game shop.
				    PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_VIP_LOBBY_NOTICE_CASE12_POPUP_01, 0, 0));
				}
			}
		}
	};

	private static final class EventAwake implements Visitor<Player> {
		@Override
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

	public static PlayerEventService getInstance() {
		ObjectProvider<PlayerEventService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public static void setInstanceProvider(ObjectProvider<PlayerEventService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final PlayerEventService instance = new PlayerEventService();
	}
}
