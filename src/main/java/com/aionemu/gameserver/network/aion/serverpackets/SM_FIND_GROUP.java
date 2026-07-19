package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.autogroup.RetailMatchSession;
import com.aionemu.gameserver.model.gameobjects.FindGroup;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/** 5.8 查找队伍与跨服副本队伍协议。 */
public class SM_FIND_GROUP extends AionServerPacket {
	private final int action;
	private int lastUpdate;
	private int unk;
	private Collection<FindGroup> findGroups = List.of();
	private Player applicant;
	private boolean showEnterMessage;
	private List<Integer> instanceMaskIds = List.of();
	private int instanceGroupEntryId;
	private int instanceMaskId;
	private List<RetailMatchSession.Member> matchMembers = List.of();

	public SM_FIND_GROUP(int action, int lastUpdate, Collection<FindGroup> findGroups) {
		this.action = action;
		this.lastUpdate = lastUpdate;
		this.findGroups = findGroups == null ? List.of() : findGroups;
	}

	public SM_FIND_GROUP(int action, int lastUpdate, int unk) {
		this.action = action;
		this.lastUpdate = lastUpdate;
		this.unk = unk;
	}

	public SM_FIND_GROUP(int action, int instanceMaskId) {
		this.action = action;
		this.instanceMaskIds = List.of(instanceMaskId);
	}

	public SM_FIND_GROUP(Player applicant) {
		this.action = 0x0B;
		this.applicant = applicant;
	}

	public SM_FIND_GROUP(int action, FindGroup group) {
		this(action, (int) (System.currentTimeMillis() / 1000), List.of(group));
	}

	public SM_FIND_GROUP(int action, FindGroup group, boolean showEnterMessage) {
		this(action, group);
		this.showEnterMessage = showEnterMessage;
	}

	public SM_FIND_GROUP(int action, int instanceGroupEntryId, int instanceMaskId, boolean showEnterMessage) {
		this.action = action;
		this.instanceGroupEntryId = instanceGroupEntryId;
		this.instanceMaskId = instanceMaskId;
		this.showEnterMessage = showEnterMessage;
	}

	public SM_FIND_GROUP(int action, int instanceGroupEntryId, int instanceMaskId,
			List<RetailMatchSession.Member> matchMembers) {
		this(action, instanceGroupEntryId, instanceMaskId, false);
		this.matchMembers = List.copyOf(matchMembers);
	}

	public SM_FIND_GROUP(List<Integer> instanceMaskIds) {
		this.action = 0x1A;
		this.instanceMaskIds = List.copyOf(instanceMaskIds);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
			case 0x00, 0x02 -> writeRecruitments();
			case 0x01, 0x03 -> writeRecruitmentRemoval();
			case 0x04, 0x06 -> writeApplications();
			case 0x05 -> writeD(lastUpdate);
			case 0x0A -> writeInstanceGroups();
			case 0x0B -> writeApplication();
			case 0x0E -> writeRegisteredInstanceGroups();
			case 0x10 -> writeInstanceGroupMembers();
			case 0x12, 0x16 -> writePrepareWindow();
			case 0x17 -> writeDestroyPrepareWindow();
			case 0x18 -> writePrepareWindowMembers();
			case 0x1A -> writeInstanceRegistrationOptions();
		}
	}

	private void writeRecruitments() {
		writeH(findGroups.size());
		writeH(findGroups.size());
		writeD(lastUpdate);
		for (FindGroup group : findGroups) {
			writeD(group.getObjectId());
			writeC(NetworkConfig.GAMESERVER_ID);
			writeC(0);
			writeC(0);
			writeC(group.getUnk() == 65557 ? 16 : 0);
			writeC(group.getGroupType());
			writeS(group.getMessage());
			writeS(group.getName());
			writeC(group.getSize());
			writeC(group.getMinLevel());
			writeC(group.getMaxLevel());
			writeD(group.getLastUpdate());
		}
	}

	private void writeRecruitmentRemoval() {
		writeD(lastUpdate);
		writeC(NetworkConfig.GAMESERVER_ID);
		writeC(0);
		writeC(0);
		writeC(unk == 65557 ? 16 : 0);
	}

	private void writeApplications() {
		writeH(findGroups.size());
		writeH(findGroups.size());
		writeD(lastUpdate);
		for (FindGroup group : findGroups) {
			writeD(group.getObjectId());
			writeC(group.getGroupType());
			writeS(group.getMessage());
			writeS(group.getName());
			writeC(group.getClassId());
			writeC(group.getMinLevel());
			writeD(group.getLastUpdate());
		}
	}

	private void writeInstanceGroups() {
		writeH(findGroups.size());
		writeH(findGroups.size());
		writeD(lastUpdate);
		for (FindGroup group : findGroups) {
			writeD(group.getObjectId());
			writeD(group.getInstanceId());
			writeD(1);
			writeC(group.getSize());
			writeC(group.getMinMembers());
			writeH(0);
			writeD(group.getObjectId());
			writeD(1);
			writeD(0);
			writeC(group.getMinLevel());
			writeC(group.getMaxLevel());
			writeH(0);
			writeD(group.getLastUpdate());
			writeD(0);
			writeS(group.getName());
			writeS(group.getMessage());
		}
	}

	private void writeApplication() {
		writeD(applicant.getObjectId());
		writeD(0);
		writeD(0);
		writeH(0);
		writeC(0);
		writeC(applicant.getPlayerClass().getClassId());
		writeD(applicant.getLevel());
		writeS(applicant.getName());
	}

	private void writeRegisteredInstanceGroups() {
		writeC(1);
		for (FindGroup group : findGroups) {
			writeD(group.getObjectId());
			writeD(group.getInstanceId());
			writeD(1);
			writeC(group.getSize());
			writeC(group.getMinMembers());
			writeH(0);
			writeD(group.getObjectId());
			writeC(1);
			writeC(0);
			writeD(1);
			writeH(0);
			writeC(group.getMinLevel());
			writeC(group.getMaxLevel());
			writeH(0);
			writeD(group.getLastUpdate());
			writeD(0);
			writeS(group.getName());
			writeS(group.getMessage());
		}
	}

	private void writeInstanceGroupMembers() {
		List<Player> members = findGroups.iterator().next().getMembers();
		writeH(members.size());
		writeH(members.size());
		writeD(lastUpdate);
		for (Player member : members) {
			writeD(0);
			writeD(member.getWorldId());
			writeD(member.getObjectId());
			writeD(member.getLevel());
			writeD(member.getPlayerClass().getClassId());
			writeH(1);
			writeC(0);
			writeC(0);
			writeS(member.getName());
		}
	}

	private void writePrepareWindow() {
		if (findGroups.isEmpty()) {
			writeD(instanceGroupEntryId);
			writeD(instanceMaskId);
		} else {
			FindGroup group = findGroups.iterator().next();
			writeD(group.getObjectId());
			writeD(group.getInstanceId());
		}
	}

	private void writeDestroyPrepareWindow() {
		writePrepareWindow();
		writeC(showEnterMessage ? 1 : 0);
	}

	private void writePrepareWindowMembers() {
		if (!matchMembers.isEmpty()) {
			writeD(instanceGroupEntryId);
			writeD(instanceMaskId);
			writeC(matchMembers.size());
			for (RetailMatchSession.Member member : matchMembers) {
				writeD(0);
				writeD(0);
				writeD(member.playerId());
				writeD(member.level());
				writeD(member.classId());
				writeH(0);
				writeC(1);
				writeC(member.online() ? 1 : 0);
				writeS(member.name());
			}
			return;
		}
		FindGroup group = findGroups.iterator().next();
		writeD(group.getObjectId());
		writeD(group.getInstanceId());
		writeC(group.getMembers().size());
		for (Player member : group.getMembers()) {
			writeD(0);
			writeD(0);
			writeD(member.getObjectId());
			writeD(member.getLevel());
			writeD(member.getPlayerClass().getClassId());
			writeH(0);
			writeC(1);
			writeC(member.isOnline() ? 1 : 0);
			writeS(member.getName());
		}
	}

	private void writeInstanceRegistrationOptions() {
		writeH(instanceMaskIds.size());
		for (int instanceMaskId : instanceMaskIds) {
			writeD(instanceMaskId);
		}
	}
}
