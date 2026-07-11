package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Cygnea 区域 NPC AI：Besta（@AIName "besta"），继承 NpcAI2。
 * Cygnea zone NPC AI: Besta (@AIName "besta"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("besta")
public class BestaAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        // 艾德拉领地村庄渗透裂隙走廊钥匙。 / Aedra Territory Village Infiltration Rift Corridor Key.
		if (player.getInventory().getFirstItemByItemId(185000231) != null) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		// 艾德拉领地村庄渗透裂隙走廊钥匙。 / Aedra Territory Village Infiltration Rift Corridor Key.
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000231, 1)) {
		    switch (getNpcId()) {
				case 804843: //Besta
				    announceLightLegionPortal();
					spawn(702721, 1214.9753f, 1564.4226f, 468.49017f, (byte) 50);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
						    despawnNpc(702721);
				        }
			        }, 300000); //5 分钟。 / 5 Minutes.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
	
	private void announceLightLegionPortal() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LIGHT_SIDE_LEGION_DIRECT_PORTAL_OPEN);
			}
		});
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
