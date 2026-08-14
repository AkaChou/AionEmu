package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：按 NPC ID 或名称描述传送到该 NPC 附近。
 * GM command handler that teleports the admin to an NPC by id or name description.
 *
 * @author Alcapwnd
 */
public class CmdTeleportToNamed extends AbstractGMHandler {

	/**
	 * 创建处理器并立即按 NPC 传送。
	 * Creates the handler and immediately teleports by NPC.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params NPC ID 或名称描述 / NPC id or name description
	 */
	public CmdTeleportToNamed(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 解析 NPC ID 或按名称描述查找并传送到该 NPC。
	 * Parses an NPC id or looks up by name description and teleports to that NPC.
	 */
	public void run() {
		int npcId = 0;
		String message = "";
		try {
			npcId = Integer.valueOf(params);
		} catch (ArrayIndexOutOfBoundsException e) {
			onFail(admin, e.getMessage());
		} catch (NumberFormatException e) {
			String npcDesc = params;

			for (NpcTemplate template : DataManager.NPC_DATA.getNpcData().values()) {
				if (template.getNamedesc() != null && template.getNamedesc().equalsIgnoreCase(npcDesc)) {
					TeleportService2.teleportToNpc(admin, template.getTemplateId());
					message = "Teleporting to Npc: " + template.getTemplateId();
					PacketSendUtility.sendMessage(admin, message);
				}
			}
		}

		if (npcId > 0) {
			if (!message.equals(""))
				message = "Teleporting to Npc: " + npcId + "\n" + message;
			else
				message = "Teleporting to Npc: " + npcId;
			PacketSendUtility.sendMessage(admin, message);
			TeleportService2.teleportToNpc(admin, npcId);
		}
	}

	/**
	 * @param admin 管理员 / the admin
	 * @param message 失败信息 / failure message
	 */
	public void onFail(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "syntax //movetonpc <npc_id|npc name>");
	}
}
