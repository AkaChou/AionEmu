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
 * 攻城战相关 NPC AI：SWAA Gun Dark（@AIName "SWAAGun_Dark"），继承 NpcAI2。
 * Siege-related NPC AI: SWAA Gun Dark (@AIName "SWAAGun_Dark"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("SWAAGun_Dark")
public class SWAAGun_DarkAI2 extends NpcAI2
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
				// 执政官炮台【雷珊塔】 / Archon Battery [Reshanta]
				case 251735:
				case 251755:
				case 251775:
				// 魔族防御炮塔【卡尔多】 / Asmodian Defense Turret [Kaldor]
				case 252171:
				case 252172:
				case 252173:
				case 252174:
				case 252175:
				case 252176:
				case 252177:
				// 空以太加农【雷珊塔】 / Empty Aetheric Cannon [Reshanta]
				case 881982:
				    GameEngineServices.skillEngine().getSkill(player, 21518, 1, player).useNoAnimationSkill();
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
