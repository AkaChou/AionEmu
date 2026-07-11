package com.aionemu.gameserver.ai.instance.stoneSpearReach;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Stone Spear Reach 副本 NPC AI：Macadamic Jester（@AIName "Macadamic_Jester"），继承 GeneralNpcAI2。
 * Stone Spear Reach instance NPC AI: Macadamic Jester (@AIName "Macadamic_Jester"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Macadamic_Jester")
public class Macadamic_JesterAI2 extends GeneralNpcAI2
{
    private String walkerId = "301500000";
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		getSpawnTemplate().setWalkerId(walkerId);
		WalkManager.startWalking(this);
		getOwner().setState(1);
		PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
	}
}
