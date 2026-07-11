package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.krotan;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Krotan Auxillary General（@AIName "unsealed_krotan"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Krotan Auxillary General (@AIName "unsealed_krotan"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("unsealed_krotan")
public class Krotan_Auxillary_GeneralAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}

	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		//bossShield();
		announceUnsealedKrotan();
    }

	private void bossShield() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 18296, 60, getOwner()).useNoAnimationSkill(); //Boss Shield.
	}

	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			// 克罗坦避难所 5.3 / Krotan Refuge 5.3
			case 279149:
			case 279443:
			case 267811:
				treasureChest();
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			        @Override
			        public void run() {
						spawnTreasureChest(701481);
			        }
		        }, 10000);
			break;
		}
		super.handleDied();
	}

	private void treasureChest() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 出现了一个宝箱。 / A treasure chest has appeared.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdC_BoxSpawn);
			}
		});
	}

	private void announceUnsealedKrotan() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 解封的克罗坦。 / Unsealed Krotan.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Crotan_Named_Spawn);
				// 克罗坦龙族王子已出现！ / The Krotan Balaur Prince has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_Boss_hide_Dispel, 10000);
			}
		});
	}

	private void spawnTreasureChest(int npcId) {
		rndSpawnInRange(npcId, Rnd.get(1, 4));
		rndSpawnInRange(npcId, Rnd.get(1, 4));
		rndSpawnInRange(npcId, Rnd.get(1, 4));
		rndSpawnInRange(npcId, Rnd.get(1, 4));
		rndSpawnInRange(npcId, Rnd.get(1, 4));
		rndSpawnInRange(npcId, Rnd.get(1, 4));
	}

	private Npc rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		return (Npc) spawn(npcId, p.getX() + x1, p.getY() + y1, p.getZ(), (byte) 0);
	}
}
