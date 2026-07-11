package com.aionemu.gameserver.ai.instance.dredgionDefense;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Dredgion Defense 副本 NPC AI：Korunerk（@AIName "Korunerk"），继承 NpcAI2。
 * Dredgion Defense instance NPC AI: Korunerk (@AIName "Korunerk"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Korunerk")
public class KorunerkAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {

		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {

		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 834305: //Korunerk.
					spawn(220820, 1390.711f, 1692.9382f, 573.28613f, (byte) 105); //Sanctum Tank A.
				break;
			}
		}
		AI2Actions.deleteOwner(this);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
