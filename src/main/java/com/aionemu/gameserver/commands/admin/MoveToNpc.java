package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 按 NPC 模板 ID 或名称传送到该 NPC 的管理员命令。
 * Admin command to teleport to an NPC by template id or name.
 *
 * @author MrPoke, lord_rex and ginho1
 */
public class MoveToNpc extends AdminCommand {

	/**
	 * 以别名 {@code movetonpc} 构造命令。
	 * Construct the command with alias {@code movetonpc}.
	 */
	public MoveToNpc() {
		super("movetonpc");
	}

	/**
	 * 按 NPC Id 或名称解析并传送；名称匹配多个时列出其余 Id。
	 * Resolve NPC by id or name and teleport; list alternate ids when the name matches more than one.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		int npcId = 0;
		String message = "";
		try {
			npcId = Integer.valueOf(params[0]);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			onFail(player, e.getMessage());
		}
		catch (NumberFormatException e) {
			String npcName = "";

			for(int i = 0; i < params.length; i++)
				npcName += params[i]+" ";
			npcName = npcName.substring(0, npcName.length() - 1);

			for(NpcTemplate template : DataManager.NPC_DATA.getNpcData().values()) {
				if(template.getName().equalsIgnoreCase(npcName)) {
					if(npcId == 0)
						npcId = template.getTemplateId();
					else {
						if(message.equals(""))
							message += "Found others ("+npcName+"): \n";
						message += "Id: "+template.getTemplateId()+"\n";
					}
				}
			}
			if(npcId == 0) {
				PacketSendUtility.sendMessage(player, "NPC " + npcName + " cannot be found");
			}
		}

		if(npcId > 0) {
			message = "Teleporting to Npc: "+npcId+"\n"+message;
			PacketSendUtility.sendMessage(player, message);
			TeleportService2.teleportToNpc(player, npcId);
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //movetonpc <npc_id|npc name>");
	}
}
