package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gm.GmCommands;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * GM 面板书签/快捷操作的客户端包。
 * Client packet for GM panel bookmark and quick actions.
 */
public class CM_GM_BOOKMARK extends AionClientPacket {
	private GmCommands command;
	private String playerName;
	private String[] parts;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GM_BOOKMARK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		playerName = readS();
		parts = playerName.split(" ");
		command = GmCommands.getValue(parts[0]);
		playerName = parts[1];
	}

	@Override
	protected void runImpl() {
		Player admin = getConnection().getActivePlayer();
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(playerName));
		if (admin == null) {
			return;
		}
		if (admin.getAccessLevel() < AdminConfig.GM_PANEL) {
			return;
		}
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "Could not find an online player with that name.");
			return;
		}
		switch (command) {
		case GM_DIALOG_TELEPORTTO:
			TeleportService2.teleportTo(admin, player.getWorldId(), player.getX(), player.getY(), player.getZ());
			break;
		case GM_DIALOG_RECALL:
			TeleportService2.teleportTo(player, admin.getWorldId(), admin.getX(), admin.getY(), admin.getZ());
			break;
		//
		case GM_DIALOG:
		case GM_DIALOG_POS:
		case GM_DIALOG_MEMO:
		case GM_DIALOG_BOOKMARK:
		case GM_DIALOG_INVENTORY:
		case GM_DIALOG_SKILL:
		case GM_DIALOG_STATUS:
		case GM_DIALOG_QUEST:
		case GM_DIALOG_REFRESH:
		case GM_DIALOG_WAREHOUSE:
		case GM_DIALOG_MAIL:
		case GM_POLL_DIALOG:
		case GM_POLL_DIALOG_SUBMIT:
		case GM_BOOKMARK_DIALOG:
		case GM_BOOKMARK_DIALOG_ADD_BOOKMARK:
		case GM_MEMO_DIALOG:
		case GM_MEMO_DIALOG_ADD_MEMO:
		case GM_DIALOG_CHECK_BOT1:
		case GM_DIALOG_CHECK_BOT99:
		case GM_INDICATOR_DIALOG_TOOLTIP_HOUSING_MODE:
		case GM_DIALOG_CHARACTER:
		case GM_DIALOG_OPTION:
		case GM_DIALOG_BUILDER_CONTROL:
		case GM_DIALOG_BUILDER_COMMAND:
			break;
		default:
			PacketSendUtility.sendMessage(admin, "Invalid command: " + command.name());
			break;
		}
	}
}