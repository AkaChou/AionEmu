/*

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

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.SellLimit;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import org.springframework.beans.factory.ObjectProvider;

import javolution.util.FastMap;

/**
 * @author Source
 */
public class PlayerLimitService {

	private static FastMap<Integer, Long> sellLimit = new FastMap<Integer, Long>().shared();
	private static volatile ObjectProvider<PlayerLimitService> instanceProvider;

	public static boolean updateSellLimit(Player player, long reward) {
		if (!CustomConfig.LIMITS_ENABLED) {
			return true;
		}
		int accoutnId = player.getPlayerAccount().getId();
		Long limit = sellLimit.get(accoutnId);
		if (limit == null) {
			limit = SellLimit.getSellLimit(player.getPlayerAccount().getMaxPlayerLevel()) * CustomConfig.LIMITS_RATE;
			sellLimit.put(accoutnId, limit);
		}

		if (limit < reward) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DAY_CANNOT_SELL_NPC(limit));
			return false;
		} else {
			limit -= reward;
			sellLimit.putEntry(accoutnId, limit);
			return true;
		}
	}

	public void scheduleUpdate() {
		CronService.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				sellLimit.clear();
			}

		}, CustomConfig.LIMITS_UPDATE, true);
	}

	public static PlayerLimitService getInstance() {
		ObjectProvider<PlayerLimitService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<PlayerLimitService> instanceProvider) {
		PlayerLimitService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {

		protected static final PlayerLimitService instance = new PlayerLimitService();
	}
}
