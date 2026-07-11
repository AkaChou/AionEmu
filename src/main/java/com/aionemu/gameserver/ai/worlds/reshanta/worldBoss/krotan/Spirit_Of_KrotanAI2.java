package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.krotan;

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
 * Reshanta 区域 NPC AI：Spirit Of Krotan（@AIName "spirit_of_krotan"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Spirit Of Krotan (@AIName "spirit_of_krotan"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("spirit_of_krotan")
public class Spirit_Of_KrotanAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		startLifeTask();
		announceSpiritOfKrotan();
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			        @Override
			        public void visit(Player player) {
						AI2Actions.deleteOwner(Spirit_Of_KrotanAI2.this);
						// 克罗坦狂战士之魂已消失。 / The Krotan Berserker Soul has disappeared.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Despawn_Crotan);
			        }
				});
			}
		}, 1800000); //30 Minutes.
	}
	
	private void announceSpiritOfKrotan() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦守护者出现。 / Krotan Guardian Appears.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Crotan_Named_Spawn_In);
			}
		});
	}
	
	@Override
	protected void handleDied() {
		updateKrotanLanding();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦狂战士之魂已被击杀。 / The Krotan Berserker Soul has been slain.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Die_Crotan);
			}
		});
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateKrotanLanding() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 13, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 1, 0);
					}
				}
			}
		});
	}
}
