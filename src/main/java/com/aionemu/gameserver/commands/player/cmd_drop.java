package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：查看指定 NPC 或当前目标的掉落表。
 * Player command: lists drop table of a given NPC id or current target.
 *
 * @author Eloann
 */
public class cmd_drop extends PlayerCommand {

	/**
	 * 注册命令别名 {@code cmd_drop}。
	 * Registers the command alias {@code cmd_drop}.
	 */
	public cmd_drop() {
		super("cmd_drop");
	}

	/**
	 * 按 NPC ID 或目标 NPC 输出掉落组与物品概率。
	 * Prints drop groups and item chances for an NPC id or target NPC.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 可选 NPC ID / optional NPC id
	 */
	@Override
	public void execute(Player player, String... params) {
		NpcDrop npcDrop = null;
		if (params.length > 0) {
			int npcId = Integer.parseInt(params[0]);
			NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(npcId);
			if (npcTemplate == null) {
				PacketSendUtility.sendMessage(player, "Incorrect npcId: " + npcId);
				return;
			}
			npcDrop = npcTemplate.getNpcDrop();
		}
		else {
			VisibleObject visibleObject = player.getTarget();

			if (visibleObject == null) {
				PacketSendUtility.sendMessage(player, "You have no target !");
				return;
			}

			if (visibleObject instanceof Player) {
				PacketSendUtility.sendMessage(player, "Your target must be a npc !");
				return;
			}

			if (visibleObject instanceof Npc) {
				npcDrop = ((Npc) visibleObject).getNpcDrop();
			}
		}
		if (npcDrop == null) {
			Npc npc = (Npc) player.getTarget();
			PacketSendUtility.sendMessage(player, "NPC ID :" + " " + npc.getNpcId() + " has no drops.");
			return;
		}

		int count = 0;
		PacketSendUtility.sendMessage(player, "[Mob Drops]\n");
		for (DropGroup dropGroup : npcDrop.getDropGroup()) {
			PacketSendUtility.sendMessage(player, "DropGroup: " + dropGroup.getGroupName());
			for (Drop drop : dropGroup.getDrop()) {
				PacketSendUtility.sendMessage(player, "[item:" + drop.getItemId() + "]" + " Rate: " + drop.getChance());
				count++;
			}
		}

		PacketSendUtility.sendMessage(player, "There are " + count + " drops on NPC.");
		Npc npc = (Npc) player.getTarget();
		PacketSendUtility.sendMessage(player, "NpcId :" + " " + npc.getNpcId());
	}

}
