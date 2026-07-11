package com.aionemu.gameserver.ai.siege;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 攻城战相关 NPC AI：SWAA Gun Light（@AIName "SWAAGun_Light"），继承 NpcAI2。
 * Siege-related NPC AI: SWAA Gun Light (@AIName "SWAAGun_Light"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("SWAAGun_Light")
public class SWAAGun_LightAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(186000246) != null) { //Magic Cannonball.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_AB1_PCTank_NoItem);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				// 守护者炮台【雷珊塔】 / Guardian Battery [Reshanta]
				case 251725:
				case 251745:
				case 251765:
				// 天族防御炮塔【卡尔多】 / Elyos Defense Turret [Kaldor]
				case 252164:
				case 252165:
				case 252166:
				case 252167:
				case 252168:
				case 252169:
				case 252170:
				// 空以太加农【雷珊塔】 / Empty Aetheric Cannon [Reshanta]
				case 881981:
				    GameEngineServices.skillEngine().getSkill(player, 21517, 1, player).useNoAnimationSkill();
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
		return true;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
