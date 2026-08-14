package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.miren;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Spirit Of Miren（@AIName "spirit_of_miren"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Spirit Of Miren (@AIName "spirit_of_miren"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("spirit_of_miren")
public class Spirit_Of_MirenAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		startLifeTask();
		announceSpiritOfMiren();
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			        @Override
			        public void visit(Player player) {
						AI2Actions.deleteOwner(Spirit_Of_MirenAI2.this);
						// 米伦狂战士之魂已消失。 / The Miren Berserker Soul has disappeared..
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Despawn_lamiren);
			        }
				});
			}
		}, 1800000); //30 Minutes.
	}
	
	private void announceSpiritOfMiren() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 米伦守护者出现。 / Miren Guardian Appears.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Lamiren_Named_Spawn_In);
			}
		});
	}
	
	@Override
	protected void handleDied() {
		updateMirenLanding();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 米伦狂战士之魂已被击杀。 / The Miren Berserker Soul has been slain.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Die_lamiren);
			}
		});
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateMirenLanding() {
		final com.aionemu.gameserver.model.gameobjects.Creature mostHated = getOwner().getAggroList().getMostHated();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (mostHated != null && MathUtil.isIn3dRange(mostHated, getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 14, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 2, 0);
					}
				}
			}
		});
	}
}
