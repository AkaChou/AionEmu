package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.commons.utils.Rnd;
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
 * Empyrean Crucible 副本 NPC AI：Empyrean Record Keeper（@AIName "empyrean_record_keeper"），继承 NpcAI2。
 * Empyrean Crucible instance NPC AI: Empyrean Record Keeper (@AIName "empyrean_record_keeper"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("empyrean_record_keeper")
public class Empyrean_Record_KeeperAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		InstanceHandler instanceHandler = getPosition().getWorldMapInstance().getInstanceHandler();
		if (dialogId == 10000) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
			switch (getNpcId()) {
				case 799568:
					instanceHandler.onChangeStage(StageType.START_STAGE_2_ELEVATOR);
				break;
				case 799569:
					instanceHandler.onChangeStage(StageType.START_STAGE_3_ELEVATOR);
				break;
				case 205331:
					instanceHandler.onChangeStage(StageType.START_STAGE_4_ELEVATOR);
				break;
				case 205338:
					instanceHandler.onChangeStage(StageType.START_STAGE_5);
				break;
				case 205332:
					switch (Rnd.get(1, 2)) {
					    case 1:
						    instanceHandler.onChangeStage(StageType.START_AZOTURAN_STAGE_5_ROUND_1);
					    break;
					    case 2:
						    instanceHandler.onChangeStage(StageType.START_STEEL_RAKE_STAGE_5_ROUND_1);
					    break;
					}
				break;
				case 205339:
					instanceHandler.onChangeStage(StageType.START_STAGE_6);
				break;
				case 205333:
					instanceHandler.onChangeStage(StageType.START_STAGE_6_ROUND_1);
				break;
				case 205340:
					instanceHandler.onChangeStage(StageType.START_STAGE_7);
				break;
				case 205334:
					instanceHandler.onChangeStage(StageType.START_STAGE_7_ROUND_1);
					switch (player.getRace()) {
					    case ELYOS:
						    spawn(217582, 1783.0873f, 796.8426f, 469.35013f, (byte) 0);
					    break;
					    case ASMODIANS:
						    spawn(217578, 1783.0873f, 796.8426f, 469.35013f, (byte) 0);
					    break;
					}
				break;
				case 205341:
					instanceHandler.onChangeStage(StageType.START_STAGE_8);
				break;
				case 205335:
					instanceHandler.onChangeStage(StageType.START_STAGE_8_ROUND_1);
				break;
				case 205342:
					instanceHandler.onChangeStage(StageType.START_STAGE_9);
				break;
				case 205336:
					instanceHandler.onChangeStage(StageType.START_STAGE_9_ROUND_1);
				break;
				case 205343:
					instanceHandler.onChangeStage(StageType.START_STAGE_10);
				break;
				case 205337:
					instanceHandler.onChangeStage(StageType.START_STAGE_10_ROUND_1);
				break;
			}
		}
		AI2Actions.deleteOwner(this);
		return true;
	}
	
	@Override
    protected void handleSpawned() {
		switch (getNpcId()) {
			case 799568:
			    // 你已完成第 1 阶段，尼尔克。 / You have completed Stage 1, nyerk.
				sendMsg(1111460, getObjectId(), false, 2000);
				// 希望你拿到了资格票，尼尔克。 / I hope you got yourself a Worthiness Ticket, nyerk.
				sendMsg(1111451, getObjectId(), false, 6000);
			break;
			case 799569:
			    // 你已完成第 2 阶段，尼尔克。 / You have completed Stage 2, nyerk.
				sendMsg(1111461, getObjectId(), false, 2000);
				// 第 3 阶段开始，尼尔克！ / Stage 3 begins, nyerk!
				sendMsg(1111452, getObjectId(), false, 6000);
			break;
			case 205331:
			    // 你已完成第 3 阶段，尼尔克。 / You have completed Stage 3, nyerk.
				sendMsg(1111462, getObjectId(), false, 2000);
				// 希望你准备好了，第 4 阶段即将开始！ / Hope you are ready, because Stage 4 is about to begin!
				sendMsg(1111453, getObjectId(), false, 6000);
			break;
			case 205332:
				// 进展不错。想开始第 5 阶段时告诉我！ / Good progress. Let me know when you want to begin Stage 5!
				sendMsg(1111454, getObjectId(), false, 2000);
			break;
			case 205333:
				// 五人倒下！第 6 阶段即将开始！ / Five down! Stage 6 about to begin!
				sendMsg(1111455, getObjectId(), false, 2000);
			break;
			case 205334:
				// 集中。第 7 阶段会更难！ / Focus. Stage 7 will be more difficult!
				sendMsg(1111456, getObjectId(), false, 2000);
			break;
			case 205335:
				// 第 8 阶段已就绪！你准备好了吗？ / Stage 8 ready for you! Are you ready for it?
				sendMsg(1111457, getObjectId(), false, 2000);
			break;
			case 205336:
				// 保持警惕。准备好第 9 阶段时告诉我，尼尔克。 / Stay sharp. Tell me when you're ready for Stage 9, nyerk.
				sendMsg(1111458, getObjectId(), false, 2000);
			break;
			case 205337:
				// 准备好最终第 10 阶段了吗？ / Are you ready for final Stage 10?
				sendMsg(1111459, getObjectId(), false, 2000);
			break;
			case 205338:
			    // 你已完成第 4 阶段，尼尔克。 / You have completed Stage 4, nyerk.
				sendMsg(1111463, getObjectId(), false, 2000);
			break;
			case 205339:
			    // 恭喜，你已通过第 5 阶段！ / Congratulations, you have passed Stage 5!
				sendMsg(1111464, getObjectId(), false, 2000);
			break;
			case 205340:
			    // 恭喜，你已通过第 6 阶段！ / Congratulations, you have passed Stage 6!
				sendMsg(1111465, getObjectId(), false, 2000);
			break;
			case 205341:
			    // 恭喜，你已通过第 7 阶段！ / Congratulations, you have passed Stage 7!
				sendMsg(1111466, getObjectId(), false, 2000);
			break;
			case 205342:
			    // 太棒了！你通过了第 8 阶段！ / Great! You passed Stage 8!
				sendMsg(1111467, getObjectId(), false, 2000);
			break;
			case 205343:
				// 太棒了！你通过了第 9 阶段！ / Excellent! You passed Stage 9!
				sendMsg(1111468, getObjectId(), false, 2000);
			break;
		}
		super.handleSpawned();
    }
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
