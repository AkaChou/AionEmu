package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.util.List;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端下发军团成员列表（含在线状态与房屋信息）的服务端包。
 * Server packet delivering the legion member list (including online status and housing info) to the client.
 */
public class SM_LEGION_MEMBERLIST extends AionServerPacket {
	private static final int OFFLINE = 0x00, ONLINE = 0x01;
	private boolean isFirst;
	private boolean result;
	private List<LegionMemberEx> legionMembers;

	/**
	 * 构造军团成员列表包。
	 * Creates a legion member list packet.
	 *
	 * @param legionMembers 成员扩展信息列表 / extended legion member list
	 * @param result 结果标记（影响写入的成员数量符号） / result flag (affects signed member count written)
	 * @param isFirst 是否为列表首包 / whether this is the first packet of the list
	 */
	public SM_LEGION_MEMBERLIST(List<LegionMemberEx> legionMembers, boolean result, boolean isFirst) {
		this.legionMembers = legionMembers;
		this.result = result;
		this.isFirst = isFirst;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		int size = legionMembers.size();
		writeC(isFirst ? 1 : 0);
		writeH(result ? size : -size);
		for (LegionMemberEx legionMember : legionMembers) {
			writeD(legionMember.getObjectId());
			writeS(legionMember.getName());
			writeC(legionMember.getPlayerClass().getClassId());
			writeD(legionMember.getLevel());
			writeC(legionMember.getRank().getRankId());
			writeD(legionMember.getWorldId());
			writeC(legionMember.isOnline() ? ONLINE : OFFLINE);
			writeS(legionMember.getSelfIntro());
			writeS(legionMember.getNickname());
			writeD(legionMember.getLastOnline());
			int address = GameHousingServices.housingService().getPlayerAddress(legionMember.getObjectId());
			if (address > 0) {
				House house = GameHousingServices.housingService().getPlayerStudio(legionMember.getObjectId());
				if (house == null) {
					house = GameHousingServices.housingService().getHouseByAddress(address);
				}
				writeD(address);
				writeD(house.getDoorState().getPacketValue());
			} else {
				writeD(0);
				writeD(0);
			}
			writeD(NetworkConfig.GAMESERVER_ID);
		}
	}
}
