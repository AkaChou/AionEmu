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

/**
 * 创造力面板属性服务，应用精华属性加成。
 * Creativity panel stats service applying essence stat bonuses.
 */
public class CreativityStatsService {
	private static volatile ObjectProvider<CreativityStatsService> instanceProvider;

	/**
	 * 应用精华属性。
	 * Applies essence stats.
	 *
	 * 玩家 / player
	 * type
	 * size
	 * @param id ID / id
	 * point
	 */
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

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static CreativityStatsService getInstance() {
		ObjectProvider<CreativityStatsService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<CreativityStatsService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final CreativityStatsService INSTANCE = new CreativityStatsService();
	}
}
