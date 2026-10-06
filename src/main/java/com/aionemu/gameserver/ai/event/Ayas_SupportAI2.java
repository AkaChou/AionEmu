package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Ayas Support（@AIName "ayas_support"），继承 GeneralNpcAI2。
 * Event NPC AI: Ayas Support (@AIName "ayas_support"), extends GeneralNpcAI2.
 * @author Encom
 */
@AIName("ayas_support")
public class Ayas_SupportAI2 extends GeneralNpcAI2
{
    @Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            // 天族。 / Elyos.
			case 833671: // 乐于助人的 Ayas / Helpful Ayas.
			case 833672: // 友善的 Ayas / Friendly Ayas.
            // 魔族。 / Asmodians.
            case 833673: // 乐于助人的 Ayas / Helpful Ayas.
			case 833674: { // 友善的 Ayas / Friendly Ayas.
				super.handleDialogStart(player);
				break;
			} default: {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
				break;
			}
		}
	}
	
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		// 应援按钮（HACTION_SETPRO1=10000）是 NPC 自身的服务动作（真端 AI 脚本
		// World_event_NPC_support_buffer_01 + 本 NPC 模板的应援技能），不属于任务对话面：
		// 先于任务引擎处理——任务面一旦认领该动作（DD 接取面的 ≥1000 回发/关窗），
		// 本处增益整链不触发（2026-10-06 实机 833671/833672：buff 缺失 + 页 10000 load fail）。
		// 收尾零发页、零关窗，与同族可施法 NPC（831031 / Npc_SupportAI2）严格一致：真端 cabb10 的
		// mgr+0x5d8 关窗属**任务侧对话分派面**（questId≠0 的 SETPRO），本按钮 questId=0、由 NPC 自身
		// AI 脚本（World_event_NPC_support_buffer_01）处理，不经过该面——把任务侧收尾语义套到 NPC
		// 自身动作上会多发一条收尾包。2026-10-06 实机：多发关窗包时客户端收不到 NPC 的施法动作
		//（同样的增益 + 同样的施法包，仅收尾不同），撤销后实机复测通过（buff + 施法动作）。
		if (dialogId == 10000) {
			PlayerEffectController effectController = player.getEffectController();
			int skillId = 0;
			switch (getNpcId()) {
			    case 833671: // 乐于助人的 Ayas（天族） / Helpful Ayas E.
				case 833673: // 乐于助人的 Ayas（魔族） / Helpful Ayas A.
					skillId = 11047; // 乐于助人的 Ayas 的助威 / Helpful Ayas' Cheer.
					effectController.removeEffect(11049);
				break;
			    case 833672: // 友善的 Ayas（天族） / Friendly Ayas E.
				case 833674: // 友善的 Ayas（魔族） / Friendly Ayas A.
					skillId = 11049; // 友善的 Ayas 的助威 / Friendly Ayas' Cheer.
					effectController.removeEffect(11047);
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useWithoutPropSkill();
			return true;
		}
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
