package com.aionemu.gameserver.ai.instance.theHexway;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * The Hexway 副本 NPC AI：The Hexway Entrance（@AIName "hexway_portal"），继承 NpcAI2。
 * The Hexway instance NPC AI: The Hexway Entrance (@AIName "hexway_portal"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("hexway_portal")
public class TheHexwayEntranceAI2 extends NpcAI2
{
	/**
	 * 打开对话窗口：55 级以上玩家可进入六角道，否则提示等级不足。
	 * Opens the dialog window: players level 55+ may enter The Hexway, otherwise a level requirement message.
	 */
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 55) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
        }
	}
	
	@Override
    protected void handleSpawned() {
        announceHexwayOpen();
		super.handleSpawned();
    }
	
	private void announceHexwayOpen() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 通往六角道的入口已开启。 / The entrance to The Hexway has opened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_Underpass_IDUnderpassRe_Open);
			}
		});
	}
}
