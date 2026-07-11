package com.aionemu.gameserver.ai.instance.dredgion;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_SETTINGS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Dredgion 副本 NPC AI：Shulack Drudge（@AIName "shulack_drudge"），继承 GeneralNpcAI2。
 * Dredgion instance NPC AI: Shulack Drudge (@AIName "shulack_drudge"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("shulack_drudge")
public class Shulack_DrudgeAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogFinish(Player player) {
		addItems(player);
		super.handleDialogFinish(player);
	}
	
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	private void addItems(Player player) {
	    int itemId = player.getRace() == Race.ELYOS ? 182212606 : 182212607;
	    Item dredgionSupplies = player.getInventory().getFirstItemByItemId(itemId);
	    if (dredgionSupplies == null) {
	  	    ItemService.addItem(player, itemId, 1);
	  	    getOwner().setNpcType(NpcType.NON_ATTACKABLE);
	  	    getKnownList().doOnAllPlayers(new Visitor<Player>() {
			    @Override
			    public void visit(Player player) {
				    PacketSendUtility.sendPacket(player, new SM_CUSTOM_SETTINGS(getOwner().getObjectId(), 0, NpcType.NON_ATTACKABLE.getId(), 0));
			    }
		    });
	    }
	}
}
