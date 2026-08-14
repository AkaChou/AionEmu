package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 好友条目更新包：刷新好友列表中单个好友的等级、在线、备注等。
 * Friend entry update: refreshes one friend entry (level, online, note, …).
 *
 * @author Ben
 */
@Slf4j
public class SM_FRIEND_UPDATE extends AionServerPacket {

	private int friendObjId;

	/**
	 * @param friendObjId 好友 objectId / friend's object ID
	 */
	public SM_FRIEND_UPDATE(int friendObjId) {
		this.friendObjId = friendObjId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Friend f = con.getActivePlayer().getFriendList().getFriend(friendObjId);
		if (f == null) {
			log.debug("Attempted to update friend list status of " + friendObjId + " for "
					+ con.getActivePlayer().getName() + " - object ID not found on friend list");
		} else {
			writeS(f.getName());
			writeD(f.getLevel());
			writeD(f.getPlayerClass().getClassId());
			writeC(f.isOnline() ? 1 : 0); // 在线状态——不清楚为何与 f.getStatus 并用 / Online status - No idea why this and f.getStatus are used
			writeD(f.getMapId());
			writeD(f.getLastOnlineTime()); // 好友上次在线时间（Unix 时间戳）/ Date friend was last online as a Unix timestamp
			writeS(f.getNote());
			writeC(f.getStatus().getId());
		}
	}
}
