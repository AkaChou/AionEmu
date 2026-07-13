package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.bonus_service.ServiceBuff;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/** Applies independent China VIP benefits from service_bonusattr data. */
public final class VipService {

	private static final int[] BUFF_BASE_IDS = { 1000000, 1000006, 1000012 };

	private VipService() {
	}

	public static void applyBenefits(Player player) {
		for (int buffId : benefitBuffIds(player.getPlayerAccount().getVipLevel())) {
			new ServiceBuff(buffId).applyEffect(player, buffId);
		}
	}

	static int[] benefitBuffIds(int vipLevel) {
		if (vipLevel == 0) {
			return new int[0];
		}
		if (vipLevel < 1 || vipLevel > 6) {
			throw new IllegalArgumentException("vipLevel must be between 0 and 6");
		}
		int[] buffIds = new int[BUFF_BASE_IDS.length];
		for (int i = 0; i < BUFF_BASE_IDS.length; i++) {
			buffIds[i] = BUFF_BASE_IDS[i] + vipLevel;
		}
		return buffIds;
	}
}
