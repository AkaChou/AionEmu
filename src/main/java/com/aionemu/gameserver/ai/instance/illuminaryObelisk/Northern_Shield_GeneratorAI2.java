package com.aionemu.gameserver.ai.instance.illuminaryObelisk;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Illuminary Obelisk 副本 NPC AI：Northern Shield Generator（@AIName "northern_shield_generator"），继承 ActionItemNpcAI2。
 * Illuminary Obelisk instance NPC AI: Northern Shield Generator (@AIName "northern_shield_generator"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("northern_shield_generator")
public class Northern_Shield_GeneratorAI2 extends ActionItemNpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 702013: //Northern Shield Generator.
				    announceNorthernShield();
				break;
			}
		}
	}
	
	private void announceNorthernShield() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 北部护盾能量发生器遭受攻击。 / The northern power shield generator is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_DEFENCE_04_ATTACKED);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
