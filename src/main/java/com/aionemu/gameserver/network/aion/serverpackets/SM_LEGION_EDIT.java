package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.RequiredArgsConstructor;

/**
 * 向客户端同步军团属性变更（等级、权限、公告、解散等）的服务端包。
 * Server packet synchronizing legion property changes (level, permissions, announcement, disband, etc.) to the client.
 * @author Simple
 */
@RequiredArgsConstructor
public class SM_LEGION_EDIT extends AionServerPacket {

	private final int type;
	private Legion legion;
	private int unixTime;
	private String announcement;

	/**
	 * 按变更类型与军团数据构造（等级、军阶、权限、贡献、仓库、描述、加入方式、最低等级等）。
	 * Creates a packet with type and legion data (level, rank, permissions, contribution, warehouse, description, join type, min level, etc.).
	 * @param type 变更类型 / edit type
	 * legion instance
	 */
	public SM_LEGION_EDIT(int type, Legion legion) {
		this.type = type;
		this.legion = legion;
	}

	/**
	 * 按变更类型与 Unix 时间构造（如解散军团倒计时）。
	 * Creates a packet with type and a Unix timestamp (e.g. legion disband countdown).
	 * @param type 变更类型 / edit type
	 * Unix timestamp
	 */
	public SM_LEGION_EDIT(int type, int unixTime) {
		this.type = type;
		this.unixTime = unixTime;
	}

	/**
	 * 按变更类型、Unix 时间与公告文本构造（军团公告变更）。
	 * Creates a packet with type, Unix timestamp and announcement text (legion announcement change).
	 * @param type 变更类型 / edit type
	 * Unix timestamp
	 * announcement text
	 */
	public SM_LEGION_EDIT(int type, int unixTime, String announcement) {
		this.type = type;
		this.announcement = announcement;
		this.unixTime = unixTime;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);
		switch (type) {
			case 0x00:
			writeC(legion.getLegionLevel());
			break;
			case 0x01:
			writeD(legion.getLegionRank());
			break;
			case 0x02:
			writeH(legion.getDeputyPermission());
			writeH(legion.getCenturionPermission());
			writeH(legion.getLegionaryPermission());
			writeH(legion.getVolunteerPermission());
			break;
			case 0x03:
			writeQ(legion.getContributionPoints()); // get Contributions
			break;
		case 0x04:
			writeQ(legion.getLegionWarehouse().getKinah());
			break;
			case 0x05:
			writeS(announcement);
			writeD(unixTime);
			break;
			case 0x06:
			writeD(unixTime);
			break;
			case 0x07:
			break;
            case 0x08:
			break;
            case 0x10:
			break;
		case 0x0C:
			writeS(legion.getLegionDescription());
			break;
		case 0x0D:
			writeC(legion.getLegionJoinType());
			break;
		case 0x0E:
			writeH(legion.getMinLevel());
			break;
		}
	}
}
