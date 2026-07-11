package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Rentus Base 副本 NPC AI：Infernal Captain Xasta Ride（@AIName "infernal_captain_xasta_ride"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Infernal Captain Xasta Ride (@AIName "infernal_captain_xasta_ride"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("infernal_captain_xasta_ride")
public class InfernalCaptainXastaRideAI2 extends AggressiveNpcAI2
{
	private String walkerId = "Captain_Xasta_Ride";
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
        getSpawnTemplate().setWalkerId(walkerId);
		WalkManager.startWalking(this);
		getOwner().setState(1);
		PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
    }
}
