package com.aionemu.gameserver.ai.agentFight;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 代理人战争相关 NPC AI：Veille Aetheric Concentrator（@AIName "veille_aetheric_concentrator"），继承 ActionItemNpcAI2。
 * Agent-fight related NPC AI: Veille Aetheric Concentrator (@AIName "veille_aetheric_concentrator"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("veille_aetheric_concentrator")
public class Veille_Aetheric_ConcentratorAI2 extends ActionItemNpcAI2
{
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 20124, 1, getOwner()).useNoAnimationSkill(); //Aether Concentrator Standby.
				GameEngineServices.skillEngine().getSkill(getOwner(), 22776, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
			}
		}, 1000);
    }
	
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    // 维勒以太集中器 / Veille's Aetheric Concentrator I
			case 296907:
				if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceVeilleI();
					AI2Actions.targetCreature(Veille_Aetheric_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235064)); //Empowered Veille.
				    AI2Actions.useSkill(Veille_Aetheric_ConcentratorAI2.this, 20107); //Defense Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
		    break;
			// 维勒以太集中器 II / Veille's Aetheric Concentrator II
			case 296908:
			    if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceVeilleII();
				    AI2Actions.targetCreature(Veille_Aetheric_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235064)); //Empowered Veille.
				    AI2Actions.useSkill(Veille_Aetheric_ConcentratorAI2.this, 20108); //Elemental Resistance Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
			break;
			// 维勒以太集中器 III / Veille's Aetheric Concentrator III
			case 296909:
			    if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceVeilleII();
				    AI2Actions.targetCreature(Veille_Aetheric_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235064)); //Empowered Veille.
				    AI2Actions.useSkill(Veille_Aetheric_ConcentratorAI2.this, 20109); //Power Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
			break;
		}
		announceVeilleIII();
	}
	
	private void announceVeilleI() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 第一幻影球已激活。 / The first Sphere of Mirage has been activated.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_FIRST_OBJECT_ON);
			}
		});
	}
	private void announceVeilleII() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 第二幻影球已激活。凯希内尔代理人维勒准备施放主神祝福。 / The second Sphere of Mirage has been activated. Kaisinel's Agent Veille prepares to cast the Empyrean Lord's blessing.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_SECOND_OBJECT_ON);
			}
		});
	}
	private void announceVeilleIII() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 你可再次使用幻影球。 / You may use the Sphere of Mirage again.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_CAN_USE_OBJECT, 120000);
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
