package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.FindGroup;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 寻找队伍/实例组列表包：按动作类型同步招募条目的增删改查。
 * instance-group list packet: CRUD of recruitment entries by action type. / instance-group list packet: CRUD of recruitment entries by action type.
 *
 * @author cura, MrPoke
 */
public class SM_FIND_GROUP extends AionServerPacket {

	private int action;
	private int lastUpdate;
	private Collection<FindGroup> findGroups;
	private int groupSize;
	private int unk;
	private int instanceId;

	/**
	 * 列表类动作：写入招募条目集合。
	 * List-style action: writes a collection of recruitment entries.
	 */
	public SM_FIND_GROUP(int action, int lastUpdate, Collection<FindGroup> findGroups) {
		this.lastUpdate = lastUpdate;
		this.action = action;
		this.findGroups = findGroups;
		this.groupSize = findGroups.size();
	}

	/**
	 * 单条更新/删除类动作。
	 * Single-entry update/delete style action.
	 */
	public SM_FIND_GROUP(int action, int lastUpdate, int unk) {
		this.action = action;
		this.lastUpdate = lastUpdate;
		this.unk = unk;
	}

	/**
	 * 实例相关动作。
	 * Instance-related action.
	 */
	public SM_FIND_GROUP(int action, int instanceId) {
		this.action = action;
		this.instanceId = instanceId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 0x00:
		case 0x02:
			writeH(groupSize); // groupSize
			writeH(groupSize); // groupSize
			writeD(lastUpdate); // objId?
			for (FindGroup findGroup : findGroups) {
				writeD(findGroup.getObjectId()); // player object id
				writeD(findGroup.getUnk()); // unk (0 or 65557)
				writeC(findGroup.getGroupType()); // 0:group, 1:alliance
				writeS(findGroup.getMessage()); // text
				writeS(findGroup.getName()); // writer name
				writeC(findGroup.getSize()); // members count
				writeC(findGroup.getMinLevel()); // members // level
				writeC(findGroup.getMaxLevel()); // members // level
				writeD(findGroup.getLastUpdate()); // objId?
			}
			break;
		case 0x01:
		case 0x03:
			writeD(lastUpdate); // player object id
			writeD(unk); // unk (0 or 65557)
			break;
		case 0x04:
		case 0x06:
			writeH(groupSize); // groupSize
			writeH(groupSize); // groupSize
			writeD(lastUpdate); // objId?
			for (FindGroup findGroup : findGroups) {
				writeD(findGroup.getObjectId()); // player object id
				writeC(findGroup.getGroupType()); // 0:group, 1:alliance
				writeS(findGroup.getMessage()); // text
				writeS(findGroup.getName()); // writer name
				writeC(findGroup.getClassId()); // player class id
				writeC(findGroup.getMinLevel()); // player level
				writeD(findGroup.getLastUpdate()); // objId?
			}
			break;
		case 0x05:
			writeD(lastUpdate); // player object id
			break;
		case 0x0A: // registered Groups
			writeH(groupSize);// size
			writeH(groupSize);// size
			writeD(lastUpdate);
			for (FindGroup findGroup : findGroups) {
				writeD(0);// groupregisteredId
				writeD(findGroup.getInstanceId());// instanceId
				writeD(1);// 未知 / unk
				writeC(findGroup.getSize());// currentMembers
				writeC(findGroup.getMinMembers());// minMembers
				writeH(0);// unk maybe spacer
				writeD(findGroup.getObjectId());// playerObjId
				writeD(1);// 未知 / unk
				writeD(0);// 未知 / unk
				writeC(findGroup.getMinLevel());// playerLevel
				writeC(findGroup.getMaxLevel());// playerLevel
				writeH(0);// unk maybe spacer?
				writeD(findGroup.getLastUpdate());// lastUpdate
				writeD(0);// 未知 / unk
				writeS(findGroup.getName());// writerName
				writeS(findGroup.getMessage());// Message
			}
			break;
		case 0x0E: // register new InstanceGroup
			writeC(1);// packetNumber 0 || 1 || 2
			for (FindGroup findGroup : findGroups) {
				writeD(0);// entryId? counts forwards every entry
				writeD(findGroup.getInstanceId());// instanceId
				writeD(1);// position?
				writeC(findGroup.getSize());// Maybe Members in Group?
				writeC(findGroup.getMinMembers());// min members to enter Instance(writer choose it)
				writeH(0);// unk maybe spacer
				writeD(findGroup.getObjectId());// playerObjId leader ID?
				writeC(1);// 未知 / unk
				writeC(0);// unkGroupType?
				writeD(1);// 未知 / unk
				writeH(0);// 未知 / unk
				writeC(findGroup.getMinLevel());// player level
				writeC(findGroup.getMaxLevel());// player level
				writeH(0);// 未知 / unk
				writeD(findGroup.getLastUpdate());// timestamp
				writeD(0);// 未知 / unk
				writeS(findGroup.getName());// writer name
				writeS(findGroup.getMessage());// register message
			}
			break;
		case 0x10:
			writeH(groupSize);// size
			writeH(groupSize);// size
			writeD(lastUpdate);// systemcurrentimemillis
			for (FindGroup findGroup : findGroups) {
				writeD(0);// groupId?
				writeD(findGroup.getInstanceId());// instanceId
				writeD(findGroup.getObjectId());// playerObjId
				writeD(findGroup.getMinLevel());// playerLevel
				writeD(1);// 未知 / unk
				writeH(1);// 未知 / unk
				writeC(findGroup.getGroupType());// groupType?
				writeC(findGroup.getClassId());// classId?
				writeS(findGroup.getName());// writerName
			}
		case 0x16:
			writeD(0);// GroupEntryId
			writeD(0);// instanceId
			break;
		case 0x18:
			writeD(0);// GroupObjId
			writeD(0);// instanceId
			writeC(0);// classId?
			for (FindGroup findGroup : findGroups) {
				writeD(0);// GroupRegisteredId
				writeD(findGroup.getInstanceId());// instanceId
				writeD(findGroup.getObjectId());// playerObjId
				writeD(findGroup.getMinLevel());// playerLevel
				writeD(1);// 未知 / unk
				writeH(1);// 未知 / unk
				writeC(findGroup.getGroupType());// groupType?
				writeC(findGroup.getClassId());// classId?
				writeS(findGroup.getName());// writerName
			}
			break;
		case 0x1A:
			writeH(1);// 未知 / unk
			writeD(instanceId);
			break;
		}
	}
}
