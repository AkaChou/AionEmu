package com.aionemu.gameserver.ai.event.rainbowSnakeFestival;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Rainbow Snake Festival 活动 NPC AI：Dapplie（@AIName "dapplie"），继承 GeneralNpcAI2。
 * Rainbow Snake Festival event NPC AI: Dapplie (@AIName "dapplie"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("dapplie")
public class DapplieAI2 extends GeneralNpcAI2
{
	/**
	 * 对话框开启入口：活动 NPC 直接开启对话框，其余 NPC 发送关闭窗口。
	 * Dialog start entry: opens the dialog for event NPCs, sends a close window otherwise.
	 */
  	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            case 832964: // 活动 NPC：Dapplie E. / event NPC: Dapplie E.
			case 832975: { // 活动 NPC：Dapplie A. / event NPC: Dapplie A.
				super.handleDialogStart(player);
				break;
			} default: {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
				break;
			}
		}
	}

	/**
	 * 处理对话框选择：先转发任务引擎，再按随机结果施加对应增益效果。
	 * Handles dialog selection: forwards to the quest engine, then applies the matching buff by random pick.
	 *
	 * @param player 对话玩家 / dialog player
	 * @param dialogId 对话框选项 ID / dialog option ID
	 * @param questId 任务 ID / quest ID
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @return 始终为 true / always true
	 */
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		PlayerEffectController effectController = player.getEffectController();
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
			int skillId = 0;
			switch (getNpcId()) {
				case 832964: //Dapplie E.
				case 832975: //Dapplie A.
					switch (Rnd.get(1, 2)) {
						case 1:
							skillId = 10978;
							effectController.removeEffect(10976);
							effectController.removeEffect(10977);
							effectController.removeEffect(10979);
						break;
						case 2:
							skillId = 10979;
							effectController.removeEffect(10976);
							effectController.removeEffect(10977);
							effectController.removeEffect(10978);
						break;
					}
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}
}
