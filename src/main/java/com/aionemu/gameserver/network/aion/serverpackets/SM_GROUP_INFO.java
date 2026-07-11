package com.aionemu.gameserver.network.aion.serverpackets;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送小队基本信息（队长、地图、拾取规则与队伍类型）的服务端包。
 * Server packet that sends group basics (leader, map, loot rules, and team type) to the client.
 */
public class SM_GROUP_INFO extends AionServerPacket {

	private LootGroupRules lootRules;
	private int groupId;
	private int leaderId;
	private int groupmapid;
	private TeamType type;

	/**
	 * @param group 玩家小队 / Player group
	 */
	public SM_GROUP_INFO(PlayerGroup group) {
		groupId = group.getObjectId();
		leaderId = group.getLeader().getObjectId();
		groupmapid = group.getLeaderObject().getWorldId();
		lootRules = group.getLootGroupRules();
		type = group.getTeamType();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(groupId);
		writeD(leaderId);
		writeD(groupmapid);
		writeD(lootRules.getLootRule().getId());
		writeD(lootRules.getMisc());
		writeD(lootRules.getCommonItemAbove());
		writeD(lootRules.getSuperiorItemAbove());
		writeD(lootRules.getHeroicItemAbove());
		writeD(lootRules.getFabledItemAbove());
		writeD(lootRules.getEthernalItemAbove());
		writeD(lootRules.getAutodistribution().getId());
		writeD(2);
		writeC(0);
		writeD(type.getType());
		writeD(type.getSubType());
		writeH(0); // 未知 / unk
		writeH(0); // message id
		writeS(StringUtils.EMPTY); // name
	}
}
