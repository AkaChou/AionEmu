package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.kysis;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Reshanta 区域 NPC AI：Spirit Of Kysis（@AIName "spirit_of_kysis"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Spirit Of Kysis (@AIName "spirit_of_kysis"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("spirit_of_kysis")
public class Spirit_Of_KysisAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}

	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		startLifeTask();
		announceSpiritOfKysis();
    }

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			AI2Actions.deleteOwner(Spirit_Of_KysisAI2.this);
			// 基西斯狂战士之魂已消失。 / The Kysis Berserker Soul has disappeared.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Despawn_dkisas);
		}), 1800000); //30 Minutes.
	}

	private void announceSpiritOfKysis() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 基西斯守护者出现。 / Kysis Guardian Appears.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Dkisas_Named_Spawn_In);
		});
	}

	@Override
	protected void handleDied() {
		updateKysisLanding();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 基西斯狂战士之魂已被击杀。 / The Kysis Berserker Soul has been slain.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Die_dkisas);
		});
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}

	private void updateKysisLanding() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
				if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
					GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 15, 0);
				} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
					GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 3, 0);
				}
			}
		});
	}
}
