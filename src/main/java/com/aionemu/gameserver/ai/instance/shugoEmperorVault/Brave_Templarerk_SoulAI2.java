package com.aionemu.gameserver.ai.instance.shugoEmperorVault;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Shugo Emperor Vault 副本 NPC AI：Brave Templarerk Soul（@AIName "templarerk"），继承 NpcAI2。
 * Shugo Emperor Vault instance NPC AI: Brave Templarerk Soul (@AIName "templarerk"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("templarerk")
public class Brave_Templarerk_SoulAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		PlayerEffectController effectController = player.getEffectController();
		if (dialogId == 10000) {
			switch (getNpcId()) {
			    case 833491: //Brave Templarerk's Soul.
				    if (player.getCommonData().getRace() == Race.ELYOS) {
				        effectController.removeEffect(21830);
				        effectController.removeEffect(21831);
					    GameEngineServices.skillEngine().applyEffectDirectly(21829, player, player, 1200000 * 1); //Brave Templarerk's Soul.
					}
			    break;
				case 833494: //Brave Templarerk's Soul.
				    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						effectController.removeEffect(21833);
				        effectController.removeEffect(21834);
					    GameEngineServices.skillEngine().applyEffectDirectly(21832, player, player, 1200000 * 1); //Brave Templarerk's Soul.
					}
			    break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
