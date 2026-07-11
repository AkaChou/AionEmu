package com.aionemu.gameserver.ai.instance.theEternalBastion;

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
 * The Eternal Bastion 副本 NPC AI：Defense Cannon（@AIName "defense_cannon"），继承 NpcAI2。
 * The Eternal Bastion instance NPC AI: Defense Cannon (@AIName "defense_cannon"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("defense_cannon")
public class Defense_CannonAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000136) != null) { //Aetheric Power Crystal.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5B_TD_DEFWeapon);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000136, 1)) { //Aetheric Power Crystal.
		    switch (getNpcId()) {
				// 防御加农 天族。 / Defense Cannon Elyos.
				case 701596:
				case 701597:
				case 701598:
				case 701599:
			    case 701600:
			    case 701601:
			    case 701602:
			    case 701603:
			    case 701604:
			    case 701605:
			    case 701606:
			    case 701607:
				    GameEngineServices.skillEngine().getSkill(player, 21065, 1, player).useNoAnimationSkill();
				break;
				// 防御加农 魔族。 / Defense Cannon Asmodians.
				case 701610:
				case 701611:
				case 701612:
				case 701613:
				case 701614:
				case 701615:
				case 701616:
				case 701617:
				case 701618:
				case 701619:
				case 701620:
				case 701621:
				    GameEngineServices.skillEngine().getSkill(player, 21066, 1, player).useNoAnimationSkill();
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
