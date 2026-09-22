package com.aionemu.gameserver.ai.worlds.silenteraCanyon;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Silentera Canyon 区域 NPC AI：High Elder Roamim（@AIName "roamim"），继承 AggressiveNpcAI2。
 * Silentera Canyon zone NPC AI: High Elder Roamim (@AIName "roamim"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("roamim")
public class High_Elder_RoamimAI2 extends AggressiveNpcAI2
{
	private int elderPhase = 0;
	private final AtomicBoolean isAggred = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		isAggred.compareAndSet(false, true);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage == 95 && elderPhase < 1) {
			elderPhase = 1;
			announceHighElderRoamimFurious();
		} if (hpPercentage == 50 && elderPhase < 2) {
			elderPhase = 2;
			announceHighElderRoamimSummoned();
		} if (hpPercentage == 10 && elderPhase < 3) {
			elderPhase = 3;
			announceHighElderRoamimFurious();
		}
	}

	private void announceHighElderRoamimFurious() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 大长老罗阿米姆暴怒！ / High Elder Roamim is furious!
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Underpass_Nephilim_Raid_Rage);
		});
	}
	private void announceHighElderRoamimSummoned() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 大长老罗阿米姆已召唤玩家。 / High Elder Roamim has summoned players.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Underpass_Nephilim_Raid_Recall);
		});
	}
	private void announceHighElderRoamimReset() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 大长老罗阿米姆的威胁等级已重置！ / High Elder Roamim's threat level has reset!
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Underpass_Nephilim_Raid_ResetAggro);
		});
	}

	@Override
	protected void handleBackHome() {
		isAggred.set(false);
		announceHighElderRoamimReset();
		super.handleBackHome();
	}
}
