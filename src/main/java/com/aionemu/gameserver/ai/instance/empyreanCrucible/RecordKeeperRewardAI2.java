package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Empyrean Crucible 副本 NPC AI：Record Keeper Reward（@AIName "recordkeeperreward"），继承 NpcAI2。
 * Empyrean Crucible instance NPC AI: Record Keeper Reward (@AIName "recordkeeperreward"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("recordkeeperreward")
public class RecordKeeperRewardAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
        InstanceHandler instanceHandler = getPosition().getWorldMapInstance().getInstanceHandler();
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		if (dialogId == 10000) {
            switch (getNpcId()) {
                case 205344: //Record Keeper Reward.
                    getPosition().getWorldMapInstance().getInstanceHandler().doReward(player);
                break;
            }
		}
        return true;
    }
	
	@Override
    protected void handleSpawned() {
		switch (getNpcId()) {
			case 205344:
				// 太棒了，守护者！你完成了整个试炼场！令人印象深刻，尼尔克！ / Wonderful, Daeva! You completed entire Crucible! Very impressive, nyerk!
				sendMsg(1111469, getObjectId(), false, 2000);
			break;
		}
		super.handleSpawned();
    }
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
