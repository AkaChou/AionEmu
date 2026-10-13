package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.services.NpcShoutsService;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Empyrean Crucible 副本 NPC AI：Record Keeper Reward（@AIName "recordkeeperreward"），继承 NpcAI2。
 * Empyrean Crucible instance NPC AI: Record Keeper Reward (@AIName "recordkeeperreward"), extends NpcAI2.
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
             if (getNpcId() == 205344) {
             	//记录者奖励 / Record Keeper Reward
             	getPosition().getWorldMapInstance().getInstanceHandler().doReward(player);
             }
		}
        return true;
    }
	
	@Override
    protected void handleSpawned() {
 		if (getNpcId() == 205344) {
 			sendMsg(1111469, getObjectId(), false, 2000);
 		}
		super.handleSpawned();
    }
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		NpcShoutsService.getInstance().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
