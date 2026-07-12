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
 * 代理人战争相关 NPC AI：Mastarius Aether Concentrator（@AIName "mastarius_aether_concentrator"），继承 ActionItemNpcAI2。
 * Agent-fight related NPC AI: Mastarius Aether Concentrator (@AIName "mastarius_aether_concentrator"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("mastarius_aether_concentrator")
public class Mastarius_Aether_ConcentratorAI2 extends ActionItemNpcAI2
{
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 20125, 1, getOwner()).useNoAnimationSkill(); //Aether Concentrator Standby.
				GameEngineServices.skillEngine().getSkill(getOwner(), 22776, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
			}
		}, 1000);
    }
	
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    // 玛斯塔里乌斯奥德集中器 I / Mastarius's Aether Concentrator I
			case 296913:
				if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceMastariusI();
				    AI2Actions.targetCreature(Mastarius_Aether_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235065)); //Empowered Mastarius.
				    AI2Actions.useSkill(Mastarius_Aether_ConcentratorAI2.this, 20107); //Defense Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
		    break;
			// 玛斯塔里乌斯奥德集中器 II / Mastarius's Aether Concentrator II
			case 296914:
			    if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceMastariusII();
				    AI2Actions.targetCreature(Mastarius_Aether_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235065)); //Empowered Mastarius.
				    AI2Actions.useSkill(Mastarius_Aether_ConcentratorAI2.this, 20108); //Elemental Resistance Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
			break;
			// 玛斯塔里乌斯奥德集中器 III / Mastarius's Aether Concentrator III
			case 296915:
			    if (player.getInventory().decreaseByItemId(164000103, 1)) { //Blessing Of Concentration.
					announceMastariusII();
				    AI2Actions.targetCreature(Mastarius_Aether_ConcentratorAI2.this, getPosition().getWorldMapInstance().getNpc(235065)); //Empowered Mastarius.
				    AI2Actions.useSkill(Mastarius_Aether_ConcentratorAI2.this, 20109); //Power Aether.
				} else {
					// 使用天族/魔族化身失败。需重新汇聚力量并召唤。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again.
				    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_DEATHBLOW_FAIL);
				}
			break;
		}
		announceMastariusIII();
	}
	
	private void announceMastariusI() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 第一命运球已激活。 / The first Sphere of Destiny has been activated.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_FIRST_OBJECT_ON_DF);
			}
		});
	}
	private void announceMastariusII() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 第二命运球已激活。玛尔库坦代理人玛斯塔里乌斯准备施放主神祝福。 / The second Sphere of Destiny has been activated. Marchutan's Agent Mastarius prepares to cast the Empyrean Lord's blessing.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_SECOND_OBJECT_ON_DF);
			}
		});
	}
	private void announceMastariusIII() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 你可再次使用命运球。 / You may use the Sphere of Destiny again.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_BUFF_CAN_USE_OBJECT_DF, 120000);
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
