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
package com.aionemu.gameserver.services.player.CreativityPanel;

import com.aionemu.gameserver.lifecycle.GameCreativityServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CREATIVITY_POINTS_APPLY;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Agility;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Health;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Knowledge;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Power;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Precision;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Will;
import com.aionemu.gameserver.utils.PacketSendUtility;

public class CreativityStatsService {
	private static volatile ObjectProvider<CreativityStatsService> instanceProvider;

	public void onEssenceApply(Player player, int type, int size, int id, int point) {
		if (player.isArchDaeva()) {
			player.getCP().addPoint(player, id, point);
			switch (id) {
			case 1:
				player.setCPSlot1(point);
				GameCreativityServices.power().onChange(player, point);
				break;
			case 2:
				player.setCPSlot2(point);
				GameCreativityServices.health().onChange(player, point);
				break;
			case 3:
				player.setCPSlot3(point);
				GameCreativityServices.agility().onChange(player, point);
				break;
			case 4:
				player.setCPSlot4(point);
				GameCreativityServices.precision().onChange(player, point);
				break;
			case 5:
				player.setCPSlot5(point);
				GameCreativityServices.knowledge().onChange(player, point);
				break;
			case 6:
				player.setCPSlot6(point);
				GameCreativityServices.will().onChange(player, point);
				break;
			}
			PacketSendUtility.sendPacket(player, new SM_CREATIVITY_POINTS_APPLY(type, size, id, point));
		}
	}

	public static CreativityStatsService getInstance() {
		ObjectProvider<CreativityStatsService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	public static void setInstanceProvider(ObjectProvider<CreativityStatsService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final CreativityStatsService INSTANCE = new CreativityStatsService();
	}
}
