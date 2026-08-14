package com.aionemu.gameserver.ai.instance.archivesOfEternity;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Archives Of Eternity 副本 NPC AI：Cryptograph Cube（@AIName "cryptograph_cube"），继承 GeneralNpcAI2。
 * Archives Of Eternity instance NPC AI: Cryptograph Cube (@AIName "cryptograph_cube"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("cryptograph_cube")
public class Cryptograph_CubeAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.isArchDaeva()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 806152: //密码背包。 / Cryptograph Cube.
				case 806153: //密码背包。 / Cryptograph Cube.
				    ItemService.addItem(player, 125004516, 1); // 发放密码背包 / Grant the Cryptograph Cube
			    break;
			}
		}
		AI2Actions.deleteOwner(this);
		return true;
	}
}
