package com.aionemu.gameserver.services;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EVERGALE_CANYON;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 风谷（永恒峡谷）服务，处理玩家登录时相关数据包。
 * Windy gorge (Evergale Canyon) service that handles related packets on player login.
 * @author Wnkrz
 */
public class WindyGorgeService {
	/**
	 * -- SETTER --
	 *  设置 Spring 实例提供者。
	 *  Sets the Spring instance provider.
	 */
	@Setter
	private static volatile ObjectProvider<WindyGorgeService> instanceProvider;

	/**
	 * 玩家登录时发送风谷相关数据包。
	 * Sends windy gorge related packets on player login.
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_EVERGALE_CANYON(2));
		PacketSendUtility.sendPacket(player, new SM_EVERGALE_CANYON(4));
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
	public static final WindyGorgeService getInstance() {
		ObjectProvider<WindyGorgeService> provider = instanceProvider;
		WindyGorgeService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("WindyGorgeService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
