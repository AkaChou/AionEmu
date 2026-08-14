package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.PlayerScript;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Map;

/**
 * 房屋相关 NPC AI：Butler（@AIName "butler"），继承 GeneralNpcAI2。
 * Housing-related NPC AI: Butler (@AIName "butler"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("butler")
public class ButlerAI2 extends GeneralNpcAI2
{
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return kickDialog(player, DialogPage.getPageByAction(dialogId));
	}
	
	private boolean kickDialog(Player player, DialogPage page) {
		if (page == DialogPage.NULL)
		return false;
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), page.id()));
		return true;
	}
	
	@Override
	/**
	 * 玩家可见管家时，将房屋脚本数据按 8141 字节分包上限切块，逐包发送给玩家。
	 * When a player sees the butler, sends the house's script data in chunks up to the 8141-byte packet limit.
	 */
	protected void handleCreatureSee(Creature creature) {
		if (creature instanceof Player) {
			Player player = (Player) creature;
			House house = (House) getCreator();
			Map<Integer, PlayerScript> scriptMap = house.getPlayerScripts().getScripts();
			try {
				for (int position = 0; position < 8; position++) {
					scriptMap.get(position).writeLock();
				}
				int totalSize = 0;
				int position = 0;
				int from = 0;
				while (position != 7) {
					for (; position < 8; position++) {
						PlayerScript script = scriptMap.get(position);
						byte[] bytes = script.getCompressedBytes();
						if (bytes == null) {
							continue;
						} if (bytes.length > 8141) {
							return;
						} if (totalSize + bytes.length > 8141) {
							position--;
							PacketSendUtility.sendPacket(player, new SM_HOUSE_SCRIPTS(house.getAddress().getId(), house.getPlayerScripts(), from, position));
							from = position + 1;
							totalSize = 0;
							continue;
						}
						totalSize += bytes.length + 8;
					}
					position--;
					if (totalSize > 0 || from == 0 && position == 7)
						PacketSendUtility.sendPacket(player, new SM_HOUSE_SCRIPTS(house.getAddress().getId(), house.getPlayerScripts(), from, position));
				}
			} finally {
				for (int position = 0; position < 8; position++) {
					scriptMap.get(position).writeUnlock();
				}
			}
		}
	}
}
