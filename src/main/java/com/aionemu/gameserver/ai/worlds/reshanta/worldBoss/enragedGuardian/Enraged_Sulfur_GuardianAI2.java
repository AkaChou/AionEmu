package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.enragedGuardian;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Enraged Sulfur Guardian（@AIName "Ab1_BossNamed_60_01_Al"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Enraged Sulfur Guardian (@AIName "Ab1_BossNamed_60_01_Al"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Ab1_BossNamed_60_01_Al")
public class Enraged_Sulfur_GuardianAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		startLifeTask();
		announceAb1NamedAppears();
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			        @Override
			        public void visit(Player player) {
						AI2Actions.deleteOwner(Enraged_Sulfur_GuardianAI2.this);
						// 暴怒的硫磺守护者已消失。 / Enraged Sulfur Guardian has disappeared.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Despawn_01);
			        }
				});
			}
		}, 3600000); // 1 小时 / 1Hr.
	}
	
	private void announceAb1NamedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 希尔硫磺守护者已出现。 / Siel's Sulfur Guardian has appeared.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Named_Spawn_In_01, 0);
			}
		});
	}
	
	@Override
	protected void handleDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 暴怒的硫磺守护者已被击败。 / Enraged Sulfur Guardian has been defeated.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Die_01);
			}
		});
		super.handleDied();
	}
}
