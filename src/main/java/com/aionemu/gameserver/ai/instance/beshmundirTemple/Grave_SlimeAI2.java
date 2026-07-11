package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Beshmundir Temple 副本 NPC AI：Grave Slime（@AIName "grave_slime"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Grave Slime (@AIName "grave_slime"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("grave_slime")
public class Grave_SlimeAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		announceGraveSlime();
		spawn(281671, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		spawn(281671, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void announceGraveSlime() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 墓地史莱姆一分为二！ / Grave Slime is splitting in two!
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDCatacombs_Normal_Slime_Isolation);
				}
			}
		});
	}
}
