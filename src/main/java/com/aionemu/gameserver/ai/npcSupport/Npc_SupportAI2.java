package com.aionemu.gameserver.ai.npcSupport;

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
 * NPC 支援/增益 AI：Npc Support（@AIName "npc_support"），继承 GeneralNpcAI2。
 * NPC support/buff AI: Npc Support (@AIName "npc_support"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("npc_support")
public class Npc_SupportAI2 extends GeneralNpcAI2
{
    /**
	 * 打开对话窗口：天族/魔族支援 NPC 使用默认对话，其余打开通用对话。
	 * Opens the dialog window: default dialog for Elyos/Asmodian support NPCs, generic dialog otherwise.
	 */
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            // 天族。 / Elyos.
			case 831024: //天族支援 NPC：Ryoenniya / Ryoenniya.
			case 831025: //天族支援 NPC：Luella / Luella.
			case 831030: //天族支援 NPC：Netalion / Netalion.
			case 831031: //天族支援 NPC：Nebrith / Nebrith.
            // 魔族。 / Asmodians.
            case 831026: //魔族支援 NPC：Rikanellie / Rikanellie.
			case 831027: //魔族支援 NPC：Karzanke / Karzanke.
			case 831028: //魔族支援 NPC：Erdat / Erdat.
			case 831029: { //魔族支援 NPC：Edandos / Edandos.
				super.handleDialogStart(player);
				break;
			} default: {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
				break;
			}
		}
	}
	
	/**
	 * 对话选择处理：处理任务对话，满状态后为玩家施加成长祝福技能。
	 * Handles dialog selection: processes quest dialogs, restores full HP/MP and applies the Blessing Of Growth skill.
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
			    case 831024: //天族支援 NPC：Ryoenniya / Ryoenniya.
			    case 831025: //天族支援 NPC：Luella / Luella.
			    case 831030: //天族支援 NPC：Netalion / Netalion.
				case 831031: //天族支援 NPC：Nebrith / Nebrith.
				case 831026: //魔族支援 NPC：Rikanellie / Rikanellie.
			    case 831027: //魔族支援 NPC：Karzanke / Karzanke.
			    case 831028: //魔族支援 NPC：Erdat / Erdat.
				case 831029: //魔族支援 NPC：Edandos / Edandos.
				player.getLifeStats().setCurrentHpPercent(100);
				player.getLifeStats().setCurrentMpPercent(100);
				player.getLifeStats().updateCurrentStats();
						switch (Rnd.get(1, 2)) {
						case 1:
							skillId = 20950; //成长祝福 / Blessing Of Growth.
							effectController.removeEffect(20951);
						break;
						case 2:
							skillId = 20950; //成长祝福 / Blessing Of Growth.
							effectController.removeEffect(20951);
						break;
					}
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useWithoutPropSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
