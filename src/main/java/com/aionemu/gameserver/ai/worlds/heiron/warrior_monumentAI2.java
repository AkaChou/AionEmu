package com.aionemu.gameserver.ai.worlds.heiron;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * Warrior Monument（帕休曼迪尔寺院 216739）AI：不可还手的靶子，受击每点伤害计数为 1。
 * Warrior monument (Beshmundir Temple 216739): a punching-bag target whose incoming damage is
 * clamped to 1.
 * @author cheatkiller
 */
@AIName("warrior_monument")
public class warrior_monumentAI2 extends GeneralNpcAI2 {

	@Override
	public boolean canThink() {
		return false;
	}

	@Override
	public int modifyDamage(int damage) {
		return 1;
	}

	/**
	 * 死亡处理：驱动原版执行器（幂等适配器）。
	 * Death handler: drives the retail executor through an idempotent adapter.
	 * <p>原版 216739 的 on_die 由模式 {@code IDCT_Quest_Reric_Normal} 承担（写计数器
	 * {@code IDCT_SpecterN_Spawn} + 发消息 1400465），但该模式因
	 * {@code AI2Engine.selectNpcAi} 对 {@code QUEST_SIDE_EFFECT_AI} 名单里模板 AI 名
	 * {@code warrior_monument} 的短路不会执行；这里按 IR-010 的幂等适配器口径直接驱动原版执行器：
	 * 计数器（驱动 condition-spawns#5021 的 Ahbana 阈值 10）与原版消息 1400465。
	 * 若将来放开该短路、让模式接管，必须同批移除本适配器，避免双计数。
	 * Retail 216739's on_die lives in the {@code IDCT_Quest_Reric_Normal} pattern (counter plus
	 * message 1400465), which the {@code QUEST_SIDE_EFFECT_AI} short-circuit keeps from running;
	 * this adapter drives the retail executor instead. Removing the short-circuit later requires
	 * removing this adapter in the same change to avoid double counting.</p>
	 */
	@Override
	protected void handleDied() {
		WorldMapInstance instance = getOwner().getPosition().getWorldMapInstance();
		super.handleDied();
		if (instance != null) {
			RetailConditionSpawnEngine.setVariable(instance, "IDCT_SpecterN_Spawn", 0, 1);
			SM_SYSTEM_MESSAGE message = new SM_SYSTEM_MESSAGE(1400465);
			instance.doOnAllPlayers(player -> {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player, message);
				}
			});
		}
		AI2Actions.deleteOwner(this);
	}
}
