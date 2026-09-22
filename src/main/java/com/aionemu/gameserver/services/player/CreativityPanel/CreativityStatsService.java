package com.aionemu.gameserver.services.player.CreativityPanel;

import com.aionemu.gameserver.lifecycle.GameCreativityServices;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CREATIVITY_POINTS_APPLY;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 创造力面板属性服务，应用精华属性加成。
 * Creativity panel stats service applying essence stat bonuses.
 */
public class CreativityStatsService {
    /**
     * -- SETTER --
     *  setInstanceProvider 方法。
     *  setInstanceProvider method.
     * 提供者 / provider
     */
    @Setter
    private static volatile ObjectProvider<CreativityStatsService> instanceProvider;

	/**
	 * 应用精华属性。
	 * Applies essence stats.
	 * 玩家 / player
	 * @param type 类型 / type
	 * @param size 槽位大小 / size
	 * @param id ID / id
	 * @param point 点数 / point
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static CreativityStatsService getInstance() {
		ObjectProvider<CreativityStatsService> provider = instanceProvider;
		CreativityStatsService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("CreativityStatsService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
