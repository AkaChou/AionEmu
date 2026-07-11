package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Empyrean Crucible 副本 NPC AI：Empyrean Record Keeper2（@AIName "record_keeper2"），继承 NpcAI2。
 * Empyrean Crucible instance NPC AI: Empyrean Record Keeper2 (@AIName "record_keeper2"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("record_keeper2")
public class Empyrean_Record_Keeper2AI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		InstanceHandler instanceHandler = getPosition().getWorldMapInstance().getInstanceHandler();
		if (dialogId == 10000) {
			instanceHandler.onChangeStage(StageType.START_STAGE_1_ELEVATOR);
		} else if (dialogId == 10001) {
			instanceHandler.onChangeStage(StageType.START_STAGE_7);
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		AI2Actions.deleteOwner(this);
		return true;
	}
	
	@Override
    protected void handleSpawned() {
		switch (getNpcId()) {
			case 799567:
				// 准备好开始试炼场了吗，尼尔克？ / Ready to start running Crucible, nyerk?
				sendMsg(1111450, getObjectId(), false, 2000);
			break;
		}
		super.handleSpawned();
    }
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
