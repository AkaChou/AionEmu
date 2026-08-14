package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 军团综合操作（创建、邀请、权限、公告等）的客户端包。
 * Client packet for general legion operations (create, invite, rights, announcements, etc.).
 *
 * @author Simple
 */
@Slf4j
public class CM_LEGION extends AionClientPacket {

	/**
	 * 扩展操作码及后续数据 / exOpcode and the rest
	 */
	private int exOpcode;
	private short deputyPermission;
	private short centurionPermission;
	private short legionarPermission;
	private short volunteerPermission;
	private int rank;
	private String legionName;
	private String charName;
	private String newNickname;
	private String announcement;
	private String newSelfIntro;
	private String joinDescription;
	private int joinType;
	private int minLevel;
	private int playerId;
	private int creatorNpcObjectId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 按扩展操作码读取军团操作参数。
	 * Reads legion operation parameters by extended opcode.
	 */
	@Override
	protected void readImpl() {
		exOpcode = readC();

		switch (exOpcode) {
		/** 创建军团。 / Create a legion */
		case 0x00:
			creatorNpcObjectId = readD();
			legionName = readS();
			break;
		/** 邀请加入军团 / Invite to legion */
		case 0x01:
			readD(); // 空 / empty
			charName = readS();
			break;
		/** 离开军团 / Leave legion */
		case 0x02:
			readD(); // 空 / empty
			readH(); // 空 / empty
			break;
		/** 将成员踢出军团 / Kick member from legion */
		case 0x04:
			readD(); // 空 / empty
			charName = readS();
			break;
		/** 任命新军团长 / Appoint a new Brigade General */
		case 0x05:
			readD();
			charName = readS();
			break;
		/** 任命百夫长 / Appoint Centurion */
		case 0x06:
			rank = readD();
			charName = readS();
			break;
		/** 降为军团兵 / Demote to Legionary */
		case 0x07:
			readD(); // 空或角色 ID？00 78 19 00 40 / char id? 00 78 19 00 40
			charName = readS();
			break;
		/** 刷新军团信息 / Refresh legion info */
		case 0x08:
			readD();
			readH();
			break;
		/** 编辑公告 / Edit announcements */
		case 0x09:
			readD(); // 空或角色 ID？ / empty or char id?
			announcement = readS();
			break;
		/** 修改自我介绍 / Change self introduction */
		case 0x0A:
			readD(); // 空或角色 ID？ / empty char id?
			newSelfIntro = readS();
			break;
		/** 编辑权限 / Edit permissions */
		case 0x0D:
			deputyPermission = (short) readH();
			centurionPermission = (short) readH();
			legionarPermission = (short) readH();
			volunteerPermission = (short) readH();
			break;
		/** 提升军团等级 / Level legion up */
		case 0x0E:
			readD(); // 空 / empty
			readH(); // 空 / empty
			break;
		case 0x0F:
			charName = readS();
			newNickname = readS();
			break;
		/** 石矛之地 / Stonespear Reach */
		case 0x10:
			readD();
			break;
		case 0x11:
			joinDescription = readS();
			break;
		case 0x12:
			joinType = readC();
			break;
		case 0x13:
			minLevel = readH();
			break;
		case 0x14:
			playerId = readD();
			break;
		case 0x15:
			playerId = readD();
			break;
		default:
			log.info(I18n.get("log.31ee2653cc50", Integer.toHexString(exOpcode).toUpperCase()));
			break;
		}
	}
	/**
	 * 分发军团创建、邀请、权限、公告等操作。
	 * Dispatches legion create, invite, rights, announcement, and related ops.
	 */
	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();
		if (activePlayer.isLegionMember()) {
			final Legion legion = activePlayer.getLegion();

			if (charName != null) {
				GameCoreGameplayServices.legionService().handleCharNameRequest(exOpcode, activePlayer, charName, newNickname, rank);
			} else {
				switch (exOpcode) {
				/** 刷新军团信息 / Refresh legion info */
				case 0x08:
					sendPacket(new SM_LEGION_INFO(legion));
					break;
				/** 编辑公告 / Edit announcements */
				case 0x09:
					GameCoreGameplayServices.legionService().handleLegionRequest(exOpcode, activePlayer, announcement);
					break;
				/** 石矛之地 / Stonespear Reach */
				case 0x10:
					break;
				/** 修改自我介绍 / Change self introduction */
				case 0x0A:
					GameCoreGameplayServices.legionService().handleLegionRequest(exOpcode, activePlayer, newSelfIntro);
					break;
				/** 编辑权限 / Edit permissions */
				case 0x0D:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().changePermissions(legion, deputyPermission, centurionPermission,
								legionarPermission, volunteerPermission);
					break;
				case 0x11:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().setJoinDescription(activePlayer, joinDescription);
					break;
				case 0x12:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().setJoinType(activePlayer, joinType);
					break;
				case 0x13:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().setJoinMinLevel(activePlayer, minLevel);
					break;
				case 0x14:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().handleJoinRequestGiveAnswer(activePlayer, playerId, true);
					break;
				case 0x15:
					if (activePlayer.getLegionMember().isBrigadeGeneral())
						GameCoreGameplayServices.legionService().handleJoinRequestGiveAnswer(activePlayer, playerId, false);
					break;
				/** 杂项 / Misc */
				default:
					GameCoreGameplayServices.legionService().handleLegionRequest(exOpcode, activePlayer);
					break;
				}
			}
		} else {
			switch (exOpcode) {
			/** 创建军团。 / Create a legion */
			case 0x00:
				if (NameRestrictionService.isForbiddenWord(legionName)) {
					PacketSendUtility.sendMessage(activePlayer,
							"You are trying to use a forbidden name. Choose another one!");
				} else {
					VisibleObject creator = activePlayer.getKnownList().getObject(creatorNpcObjectId);
					GameCoreGameplayServices.legionService().createLegion(activePlayer, legionName,
							creator instanceof Npc ? (Npc) creator : null);
				}
				break;
			}
		}
	}
}
