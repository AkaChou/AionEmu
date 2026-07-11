package com.aionemu.gameserver.ai.agentFight;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 代理人战争相关 NPC AI：Empowered Mastarius（@AIName "empowered_mastarius"），继承 AggressiveNpcAI2。
 * Agent-fight related NPC AI: Empowered Mastarius (@AIName "empowered_mastarius"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("empowered_mastarius")
public class Empowered_MastariusAI2 extends AggressiveNpcAI2
{
	private int mastariusPhase = 0;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 235065: //Empowered Mastarius.
				    announceAgentUnderAttack();
				break;
			}
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage == 50 && mastariusPhase < 1) {
			mastariusPhase = 1;
			announceJusinOdSpawn();
			announceEmpyreanLordAgentHP50();
		} if (hpPercentage == 10 && mastariusPhase < 2) {
			mastariusPhase = 2;
			announceEmpyreanLordAgentHP10();
		}
	}
	
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			addGpPlayer();
			sendMastariusGuide();
		}
		despawnNpc(296913); //Mastarius's Aether Concentrator I.
		despawnNpc(296914); //Mastarius's Aether Concentrator II.
        applyVeilleEnergy();
		announceKilledMarchutan();
        announceEmpoweredMastariusDie();
		GameLocationBootstrapServices.agentService().stopAgentFight(1);
        GameFeatureServices.baseService().capture(90, Race.ELYOS);
		super.handleDied();
	}
	
	private void sendMastariusGuide() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					HTMLService.sendGuideHtml(player, "Agent_Fight");
				}
			}
		});
	}
	private void addGpPlayer() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					AbyssPointsService.addGp(player, 500);
				}
			}
		});
	}
	
	private void announceKilledMarchutan() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				AionObject winner = getAggroList().getMostDamage();
				if (winner instanceof Creature) {
					final Creature kill = (Creature) winner;
					// “种族”的“玩家名”击杀了玛尔库坦代理人玛斯塔里乌斯。 / "Player Name" of the "Race" has killed Marchutan's Agent Mastarius.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400323, kill.getRace().getRaceDescriptionId(), kill.getName()));
				}
			}
		});
	}
	private void announceAgentUnderAttack() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 玛尔库坦代理人玛斯塔里乌斯遭受攻击！ / Marchutan's Agent Mastarius is under attack!
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_DARKBOSS_ATTACKED);
			}
		});
	}
	private void announceJusinOdSpawn() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 主神代理人召唤了以太集中器。 / The Empyrean Lord Agent summoned the Aether Concentrator.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Jusin_OdSpawn, 0);
				// 主神代理人已启用以太集中器。 / The Empyrean Lord Agent has enabled the Aether Concentrator.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Jusin_OdStart, 20000);
			}
		});
	}
	private void announceEmpyreanLordAgentHP50() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 主神代理人生命值已降至 50% 以下。 / The Empyrean Lord Agent's HP has dropped below 50%
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Jusin_Hp50);
			}
		});
	}
	private void announceEmpyreanLordAgentHP10() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 主神代理人生命值已降至 10% 以下。 / The Empyrean Lord Agent's HP has dropped below 10%
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Jusin_Hp10);
			}
		});
	}
	private void announceEmpoweredMastariusDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 代理人之战已结束。 / The Agent battle has ended.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_GodElite_time_03);
			}
		});
	}
	
	public void applyVeilleEnergy() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
				    GameEngineServices.skillEngine().applyEffectDirectly(12119, player, player, 0); //Veille's Energy.
					GameEngineServices.skillEngine().applyEffectDirectly(20410, player, player, 0); //Victory Salute.
				}
			}
		});
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
