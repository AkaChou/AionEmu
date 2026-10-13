package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.HousingService;

/**
 * 好友列表包：下发全部好友的在线状态、地图、备注与房屋信息。
 * Friend list packet: online status, map, note and housing info for all friends.
 */
public class SM_FRIEND_LIST extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		FriendList list = con.getActivePlayer().getFriendList();
		writeH((-list.getSize()));
		writeC(0);
		for (Friend friend : list) {
			writeD(friend.getOid());
			writeS(friend.getName());
			writeD(friend.getLevel());
			writeD(friend.getPlayerClass().getClassId());
			writeC(friend.isOnline() ? 1 : 0);
			writeD(friend.getMapId());
			writeD(friend.getLastOnlineTime());
			writeS(friend.getNote());
			writeC(friend.getStatus().getId());
			int address = HousingService.getInstance().getPlayerAddress(friend.getOid());
			if (address > 0) {
				House house = HousingService.getInstance().getPlayerStudio(friend.getOid());
				if (house == null) {
					house = HousingService.getInstance().getHouseByAddress(address);
					writeD(house.getAddress().getId());
				} else {
					writeD(address);
				}
				writeC(house.getDoorState().getPacketValue());
			} else {
				writeD(0);
				writeC(0);
			}
			writeS(friend.getFriendNote());
		}
	}
}
