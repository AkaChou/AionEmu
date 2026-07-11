package com.aionemu.gameserver.ai.rvr.elyosWarshipInvasion;

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
 * RvR 相关 NPC AI：Luluran（@AIName "luluran"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Luluran (@AIName "luluran"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("luluran")
public class LuluranAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 75) {
			announceDF6G1BossSpawn01();
			spawn(240768, 1391.9735f, 1615.5792f, 1010.55457f, (byte) 25); //Luluran <Commander>
			AI2Actions.deleteOwner(this);
		}
	}
	
	private void announceDF6G1BossSpawn01() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 天族护卫舰指挥官已抵达。 / The Elyos Frigate Commander has arrived.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Boss_Spawn_01);
			}
		});
	}
}
