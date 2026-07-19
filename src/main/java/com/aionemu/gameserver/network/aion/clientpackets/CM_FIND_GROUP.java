package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/** 5.8 查找队伍与跨服副本队伍客户端协议。 */
public class CM_FIND_GROUP extends AionClientPacket {
	private int action;
	private int playerOrTeamId;
	private int bannedPlayerId;
	private String message;
	private int groupType;
	private int classId;
	private int level;
	private byte serverId;
	private byte unk1;
	private byte unk2;
	private byte unk3;
	private int instanceMaskId;
	private int minMembers;
	private byte applicationReply;

	public CM_FIND_GROUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readC();
		switch (action) {
			case 0x00, 0x04, 0x0A, 0x0D, 0x14 -> { }
			case 0x01, 0x03 -> {
				playerOrTeamId = readD();
				serverId = (byte) readC();
				unk1 = (byte) readC();
				unk2 = (byte) readC();
				unk3 = (byte) readC();
				if (action == 0x03) {
					message = readS();
					groupType = readC();
				}
			}
			case 0x02 -> {
				playerOrTeamId = readD();
				message = readS();
				groupType = readC();
			}
			case 0x05 -> playerOrTeamId = readD();
			case 0x06, 0x07 -> {
				playerOrTeamId = readD();
				message = readS();
				groupType = readC();
				classId = readC();
				level = readC();
			}
			case 0x08 -> {
				instanceMaskId = readD();
				readC();
				message = readS();
				minMembers = readC();
			}
			case 0x09, 0x0B, 0x0F -> {
				playerOrTeamId = readD();
				instanceMaskId = readD();
			}
			case 0x0C -> {
				playerOrTeamId = readD();
				applicationReply = (byte) readC();
			}
			case 0x11 -> {
				playerOrTeamId = readD();
				instanceMaskId = readD();
				message = readS();
			}
			case 0x19 -> {
				playerOrTeamId = readD();
				instanceMaskId = readD();
				bannedPlayerId = readD();
			}
			default -> { }
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		var service = GameRuntimeServices.findGroupService();
		switch (action) {
			case 0x00, 0x04 -> service.sendFindGroups(player, action);
			case 0x01, 0x05 -> service.removeFindGroup(player.getRace(), action - 1, playerOrTeamId);
			case 0x02, 0x06 -> service.addFindGroupList(player, action, message, groupType);
			case 0x03, 0x07 -> service.updateFindGroupList(player, action, message, playerOrTeamId);
			case 0x08 -> service.registerInstanceGroup(player, 0x0E, instanceMaskId, message, minMembers, groupType);
			case 0x09 -> service.removeInstanceGroup(player);
			case 0x0A -> service.sendFindGroups(player, action);
			case 0x0B -> service.applyToInstanceGroup(player, playerOrTeamId, instanceMaskId);
			case 0x0C -> service.replyInstanceGroupApplication(player, playerOrTeamId, applicationReply);
			case 0x0D -> service.quickApply(player);
			case 0x0F -> service.sendInstanceGroupMembers(player, playerOrTeamId, instanceMaskId);
			case 0x11 -> service.updateInstanceGroup(player, message);
			case 0x14 -> service.enterTeamMatch(player);
			case 0x19 -> service.banInstanceGroupMember(player, playerOrTeamId, instanceMaskId, bannedPlayerId);
			default -> { }
		}
	}
}
