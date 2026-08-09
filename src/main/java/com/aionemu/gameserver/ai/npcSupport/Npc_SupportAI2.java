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
    @Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            // 天族。 / Elyos.
			case 831024: //Ryoenniya.
			case 831025: //Luella.
			case 831030: //Netalion.
			case 831031: //Nebrith.
            // 魔族。 / Asmodians.
            case 831026: //Rikanellie.
			case 831027: //Karzanke.
			case 831028: //Erdat.
			case 831029: { //Edandos.
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
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		PlayerEffectController effectController = player.getEffectController();
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
			int skillId = 0;
			switch (getNpcId()) {
			    case 831024: //Ryoenniya.
			    case 831025: //Luella.
			    case 831030: //Netalion.
				case 831031: //Nebrith.
				case 831026: //Rikanellie.
			    case 831027: //Karzanke.
			    case 831028: //Erdat.
				case 831029: //Edandos.
				player.getLifeStats().setCurrentHpPercent(100);
				player.getLifeStats().setCurrentMpPercent(100);
				player.getLifeStats().updateCurrentStats();
						switch (Rnd.get(1, 2)) {
						case 1:
							skillId = 20950; //Blessing Of Growth.
							effectController.removeEffect(20951);
						break;
						case 2:
							skillId = 20950; //Blessing Of Growth.
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
