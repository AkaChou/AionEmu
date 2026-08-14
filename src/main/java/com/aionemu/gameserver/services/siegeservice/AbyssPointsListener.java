package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;

/**
 * 欧比斯点数监听器，将 AP 变动计入攻城计数。
 * Abyss points listener attributing AP changes into siege counters.
 */
public class AbyssPointsListener extends AbyssPointsService.AddAPGlobalCallback {

	private final Siege<?> siege;

	public AbyssPointsListener(Siege<?> siege) {
		this.siege = siege;
	}

	/**
	 * 欧比斯点数增加回调。
	 * Callback when abyss points are added.
	 *
	 * @param player 玩家 / player
	 * @param abyssPoints 欧比斯点数 / abyss points
	 */
	public void onAbyssPointsAdded(Player player, int abyssPoints) {
		SiegeLocation fortress = siege.getSiegeLocation();

		if (fortress.isInsideLocation(player)) {
			siege.addAbyssPoints(player, abyssPoints);
		}
	}
}