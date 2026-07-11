package com.aionemu.gameserver.ai.instance.abyssalSplinter;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Abyssal Splinter 副本 NPC AI：Dayshade（@AIName "Dayshade"），继承 NpcAI2。
 * Abyssal Splinter instance NPC AI: Dayshade (@AIName "Dayshade"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Dayshade")
public class DayshadeAI2 extends NpcAI2
{
	@Override
    protected void handleCreatureSee(Creature creature) {
        checkDistance(this, creature);
    }
	
    @Override
    protected void handleCreatureMoved(Creature creature) {
        checkDistance(this, creature);
    }
	
	private void checkDistance(NpcAI2 ai, Creature creature) {
        if (creature instanceof Player && !creature.getLifeStats().isAlreadyDead()) {
        	if (MathUtil.isIn3dRange(getOwner(), creature, 30)) {
        		rukrilEbonsoulSpawn();
        	}
        }
    }
	
	private void rukrilEbonsoulSpawn() {
		announceIDAbReCoreNmdC();
		AI2Actions.deleteOwner(DayshadeAI2.this);
		spawn(216948, 457.50043f, 686.10956f, 432.39290f, (byte) 114);
		spawn(216949, 460.42260f, 695.25037f, 432.44205f, (byte) 114);
    }
	
	private void announceIDAbReCoreNmdC() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 1 分钟内击败埃本索尔将出现宝箱。 / A treasure chest will appear if you defeat Ebonsoul within one minute.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdC_Light_Die, 0);
					// 1 分钟内击败鲁克里尔将出现宝箱。 / A treasure chest will appear if you defeat Rukril within one minute.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdC_Dark_Die, 5000);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
