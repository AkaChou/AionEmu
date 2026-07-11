package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Empyrean Crucible 副本 NPC AI：Arminos Draky（@AIName "arminos_draky"），继承 GeneralNpcAI2。
 * Empyrean Crucible instance NPC AI: Arminos Draky (@AIName "arminos_draky"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("arminos_draky")
public class ArminosDrakyAI2 extends GeneralNpcAI2
{
    private String walkerId = "300300001";
    private boolean isStart = true;
	
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
	
	@Override
	protected void handleMoveArrived() {
		int point = getOwner().getMoveController().getCurrentPoint();
		super.handleMoveArrived();
		if (point == 15) {
			if (!isStart) {
				getSpawnTemplate().setWalkerId(null);
				WalkManager.stopWalking(this);
				AI2Actions.deleteOwner(this);
			} else {
				isStart = false;
			}
		}
	}
}
