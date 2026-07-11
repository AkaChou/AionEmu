package com.aionemu.gameserver.ai.rvr.asmodianWarshipInvasion;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * RvR 相关 NPC AI：Suminid（@AIName "suminid"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Suminid (@AIName "suminid"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("suminid")
public class SuminidAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 75) {
			announceLF6G1BossSpawn01();
			spawn(240765, 1409.9818f, 1369.7706f, 1336.7855f, (byte) 60); //Suminid <Commander>
			AI2Actions.deleteOwner(this);
		}
	}
	
	private void announceLF6G1BossSpawn01() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族护卫舰指挥官已抵达。 / The Asmodian Frigate Commander has arrived.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Boss_Spawn_01);
			}
		});
	}
}
