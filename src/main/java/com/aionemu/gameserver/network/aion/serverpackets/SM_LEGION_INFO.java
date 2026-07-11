package com.aionemu.gameserver.network.aion.serverpackets;

import java.sql.Timestamp;
import java.util.Map;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端下发军团完整信息（名称、等级、权限、公告等）的服务端包。
 * Server packet delivering full legion info (name, level, permissions, announcements, etc.) to the client.
 *
 * @author Simple
 */
public class SM_LEGION_INFO extends AionServerPacket {

	/** 军团信息 / Legion information */
	private Legion legion;

	/**
	 * 构造军团信息下发包。
	 * Creates a packet that delivers legion information.
	 *
	 * legion instance
	 */
	public SM_LEGION_INFO(Legion legion) {
		this.legion = legion;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(legion.getLegionName());
		writeC(legion.getLegionLevel());
		writeD(legion.getLegionRank());
		writeH(legion.getDeputyPermission());
		writeH(legion.getCenturionPermission());
		writeH(legion.getLegionaryPermission());
		writeH(legion.getVolunteerPermission());
		writeQ(legion.getContributionPoints());
		writeB(new byte[24]);
		writeS(legion.getLegionDescription());
		writeC(legion.getLegionJoinType());
		writeH(legion.getMinLevel());
		/**
	 * 获取 announcements 列表数据库按军团。
	 * Get Announcements List From DB By Legion *
	 */
		Map<Timestamp, String> announcementList = legion.getAnnouncementList().descendingMap();

		/** Show max 7 announcements  / Show max 7 announcements * */
		int i = 0;
		for (Timestamp unixTime : announcementList.keySet()) {
			writeS(announcementList.get(unixTime));
			writeD((int) (unixTime.getTime() / 1000));
			i++;
			if (i >= 7) {
				break;
			}
		}
	}
}
