package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 查看 NPC 掉落信息的管理命令（{@code //dropinfo}）。
 * Admin command that shows NPC drop information ({@code //dropinfo}).
 *
 * @author Oliver
 */
public class DropInfo extends AdminCommand {

	/**
	 * 注册命令名为 {@code dropinfo}。
	 * Registers the command name {@code dropinfo}.
	 */
	public DropInfo() {
		super("dropinfo");
	}

	/**
	 * 按 NPC ID 或当前目标列出掉落组与掉落率。
	 * Lists drop groups and chances by NPC id or current target.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		NpcDrop npcDrop = null;
		if (params.length > 0) {
			int npcId = Integer.parseInt(params[0]);
			NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(npcId);
			if (npcTemplate == null){
				PacketSendUtility.sendMessage(player, "Incorrect npcId: "+ npcId);
				return;
			}
			npcDrop = npcTemplate.getNpcDrop();
		}
		else {
			VisibleObject visibleObject = player.getTarget();

			if (visibleObject == null) {
				PacketSendUtility.sendMessage(player, "You should target some NPC first !");
				return;
			}

			if (visibleObject instanceof Npc) {
				npcDrop = ((Npc)visibleObject).getNpcDrop();
			}
		}
		if (npcDrop == null){
			PacketSendUtility.sendMessage(player, "No drops for the selected NPC");
			return;
		}

		int count = 0;
		PacketSendUtility.sendMessage(player, "[Drop Info for the specified NPC]\n");
		for (DropGroup dropGroup: npcDrop.getDropGroup()){
			PacketSendUtility.sendMessage(player, "DropGroup: "+ dropGroup.getGroupName());
			for (Drop drop : dropGroup.getDrop()){
				float adjustedChance = Math.min(dropGroup.getAdjustedChance(drop), 100f);
				PacketSendUtility.sendMessage(player, "[item:" + drop.getItemId() + "]" + "	Rate: " + adjustedChance);
				count ++;
			}
		}
		PacketSendUtility.sendMessage(player, count + " drops available for the selected NPC");
	}

}
