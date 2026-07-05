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

import com.aionemu.gameserver.lifecycle.GameCronServices;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.SellLimit;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Source
 */
public class PlayerLimitService {

	private static ConcurrentMap<Integer, Long> sellLimit = new ConcurrentHashMap<Integer, Long>();
	private static volatile ObjectProvider<PlayerLimitService> instanceProvider;

	public static boolean updateSellLimit(Player player, long reward) {
		if (!CustomConfig.LIMITS_ENABLED) {
			return true;
		}
		int accountId = player.getPlayerAccount().getId();
		AtomicBoolean allowed = new AtomicBoolean();
		AtomicLong remaining = new AtomicLong();
		sellLimit.compute(accountId, (id, currentLimit) -> {
			long limit = currentLimit == null ? SellLimit.getSellLimit(player.getPlayerAccount().getMaxPlayerLevel()) * CustomConfig.LIMITS_RATE : currentLimit;
			if (limit < reward) {
				allowed.set(false);
				remaining.set(limit);
				return limit;
			}
			long updatedLimit = limit - reward;
			allowed.set(true);
			remaining.set(updatedLimit);
			return updatedLimit;
		});

		if (!allowed.get()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DAY_CANNOT_SELL_NPC(remaining.get()));
			return false;
		}
		return true;
	}

	public void scheduleUpdate() {
		GameCronServices.cronService().schedule(new Runnable() {

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
