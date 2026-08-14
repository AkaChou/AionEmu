package com.aionemu.gameserver.ai.npcSupport;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NPC 支援/增益 AI：Blessed Relics（@AIName "blessed_relic"），继承 NpcAI2。
 * NPC support/buff AI: Blessed Relics (@AIName "blessed_relic"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("blessed_relic")
public class Blessed_RelicsAI2 extends NpcAI2
{
    /**
	 * 打开对话窗口：持有威名水晶时提供祝福对话，否则提示需要威名水晶。
	 * Opens the dialog window: offers the blessing dialog when the player holds a Prestige Crystal, otherwise asks for one.
	 */
	@Override
	protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(186000344) != null) { //威名水晶 / Prestige Crystal.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player,
			"You must have 1 <Prestige Crystal>", ChatType.BRIGHT_YELLOW_CENTER), true);
        }
    }
	
	/**
	 * 对话选择处理：消耗一颗威名水晶并对玩家施加威名祝福技能效果。
	 * Handles dialog selection: consumes one Prestige Crystal and applies the Prestigious Blessing skill effect.
	 */
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(186000344, 1)) { //威名水晶 / Prestige Crystal.
			switch (getNpcId()) {
			    case 831987: //次级祝福遗物 / Lesser Blessed Relics.
				case 831988: //小型祝福遗物 / Minor Blessed Relics.
				case 831989: //大型祝福遗物 / Major Blessed Relics.
				case 831990: //高级祝福遗物 / Greater Blessed Relics.
					GameEngineServices.skillEngine().applyEffectDirectly(21650, player, player, 1800000 * 1); //威名祝福 / Prestigious Blessing.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
}
