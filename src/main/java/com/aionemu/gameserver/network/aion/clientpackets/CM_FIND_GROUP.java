package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.services.FindGroupService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 查找队伍/申请组队相关操作的客户端包。
 * Client packet for find-group recruit and apply operations.
 *
 * @author cura, MrPoke
 */
@Slf4j
public class CM_FIND_GROUP extends AionClientPacket {


	private int action;
	private int playerObjId;
	private String message;
	private int groupType;
	@SuppressWarnings("unused")
	private int classId;
	@SuppressWarnings("unused")
	private int level;
	private int unk;
	private int instanceId;
	private int minMembers;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_FIND_GROUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readC();

		switch (action) {
		case 0x00: // recruit list
			break;
		case 0x01: // offer delete
			playerObjId = readD();
			unk = readD(); // unk(65557)
			break;
		case 0x02: // send offer
			playerObjId = readD();
			message = readS();
			groupType = readC();
			break;
		case 0x03: // recruit update
			playerObjId = readD();
			unk = readD(); // unk(65557)
			message = readS();
			groupType = readC();
			break;
		case 0x04: // apply list
			break;
		case 0x05: // post delete
			playerObjId = readD();
			break;
		case 0x06: // apply create
		case 0x07: // apply update
			playerObjId = readD();
			message = readS();
			groupType = readC();
			classId = readC();
			level = readC();
			break;
		case 0x08: // register InstanceGroup
			instanceId = readD();
			groupType = readC();// need to be tested
			message = readS();// text
			minMembers = readC();// minMembers chosen by writer
			break;
		case 0x0A:
			break;
		default:
			// log.error(I18n.get("log.8239177b4631", // Integer.toHexString(action).toUpperCase()));
			break;
		}
	}

	@Override
	protected void runImpl() {
		final Player player = this.getConnection().getActivePlayer();
		switch (action) {
		case 0x00:
		case 0x04:
			GameRuntimeServices.findGroupService().sendFindGroups(player, action);
			break;
		case 0x01:
		case 0x05:
			GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), action - 1, playerObjId);
			break;
		case 0x02:
		case 0x06:
			GameRuntimeServices.findGroupService().addFindGroupList(player, action, message, groupType);
			break;
		case 0x03:
		case 0x07:
			GameRuntimeServices.findGroupService().updateFindGroupList(player, action, message, playerObjId);
			break;
		case 0x08:
			GameRuntimeServices.findGroupService().registerInstanceGroup(player, 0x0E, instanceId, message, minMembers,
					groupType);
			break;
		case 0x0A:
			GameRuntimeServices.findGroupService().sendFindGroups(player, action);
			break;
		default:
			PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(action, playerObjId, unk));
			break;
		}
	}
}
