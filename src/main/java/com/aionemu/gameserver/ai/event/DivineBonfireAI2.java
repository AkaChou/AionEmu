package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Divine Bonfire（@AIName "divine_bonfire"），继承 GeneralNpcAI2。
 * Event NPC AI: Divine Bonfire (@AIName "divine_bonfire"), extends GeneralNpcAI2.
 *
 * @author xTz
 * @modified bobobear
 */
@AIName("divine_bonfire")
public class DivineBonfireAI2 extends GeneralNpcAI2 {

    @Override
    protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            case 831795: { // 神圣篝火 / Divine Bonfire
                super.handleDialogStart(player);
                break;
            }
            default: {
                PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
                break;
            }
        }
    }

    @Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
        QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
        env.setExtendedRewardIndex(extendedRewardIndex);
        if (GameEngineServices.questEngine().onDialog(env)) {
            return true;
        }
        if (dialogId == 10000) {

            //
            GameEngineServices.skillEngine().getSkill(getOwner(), 21493, 1, player).useWithoutPropSkill();
        } else if (dialogId == QuestDialog.START_DIALOG.id() && questId != 0) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
        }
        return true;
    }
}
