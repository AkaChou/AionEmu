package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Beshmundir Temple 副本 NPC AI：Lakhara（@AIName "lakhara"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Lakhara (@AIName "lakhara"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("lakhara")
public class LakharaAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (Rnd.get(1, 100) < 10) {
			certainDoom();
		}
	}
	
	private void certainDoom() {
		int hp = getOwner().getLifeStats().getHpPercentage();
		if (hp <= 25) {
			sendMessage();
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					getOwner().getController().useSkill(18891);
				}
			}, 5000);
		}
	}
	
	private void sendMessage() {
		getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400382));
				}
			}
		});
	}
}
