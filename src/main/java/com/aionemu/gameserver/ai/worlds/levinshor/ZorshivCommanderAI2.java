package com.aionemu.gameserver.ai.worlds.levinshor;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Levinshor 区域 NPC AI：Zorshiv Commander（@AIName "zorshiv_commander"），继承 AggressiveNpcAI2。
 * Levinshor zone NPC AI: Zorshiv Commander (@AIName "zorshiv_commander"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("zorshiv_commander")
public class ZorshivCommanderAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 267814:
				case 267815:
				    announcePublicQuest();
				break;
			}
		}
	}
	
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			sendZorshivGuide();
		}
        announceZorshivDie();
		announceKilledZorshiv();
		GameLocationBootstrapServices.zorshivDredgionService().stopZorshivDredgion(1);
		GameLocationBootstrapServices.zorshivDredgionService().stopZorshivDredgion(2);
		super.handleDied();
	}
	
	private void announcePublicQuest() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 你加入了对抗入侵龙族的战斗。 / You joined the battle against the Invading Balaur.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Public_Quest_Accept);
			}
		});
	}
	private void announceZorshivDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 你赢得了对抗入侵龙族的战斗。 / You won the battle against the Invading Balaur.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Public_Quest_Reward);
			}
		});
	}
	private void announceKilledZorshiv() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				AionObject winner = getAggroList().getMostDamage();
				if (winner instanceof Creature) {
					final Creature kill = (Creature) winner;
					// “种族”的“玩家名”摧毁了龙族战舰。 / "Player Name" of the "Race" has destroyed the Balaur Battleship Dredgion.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1390196, kill.getRace().getRaceDescriptionId(), kill.getName()));
				}
			}
		});
	}
	private void sendZorshivGuide() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					HTMLService.sendGuideHtml(player, "Dredgion_Guide");
				}
			}
		});
	}
}
