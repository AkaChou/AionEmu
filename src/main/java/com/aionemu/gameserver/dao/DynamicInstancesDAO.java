package com.aionemu.gameserver.dao;

import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.DynamicInstanceMember;

public abstract class DynamicInstancesDAO implements DAO {
	@Override
	public final String getClassName() {
		return DynamicInstancesDAO.class.getName();
	}

	public abstract long create(DynamicInstance instance);
	public abstract void update(DynamicInstance instance);
	public abstract List<DynamicInstance> loadRecoverable(long now);
	public abstract Map<Integer, Integer> loadMaxRuntimeInstanceIds();
	public abstract void saveMember(DynamicInstanceMember member);
	public abstract void saveMatchReservation(DynamicInstanceMember member, PlayerInstanceLimit limit);
	public abstract int cancelMatchReservation(long instanceUid, int playerId);
	public abstract void markMemberLeft(long instanceUid, int playerId, long leftAt, long reentryUntil);
	public abstract void markMemberJoined(long instanceUid, int playerId, long joinedAt);
	public abstract boolean hasJoined(long instanceUid, int playerId);
	public abstract int countMembers(long instanceUid);
	public abstract void removeReservedMember(long instanceUid, int playerId);
	public abstract void revokeMember(long instanceUid, int playerId);
	public abstract List<DynamicInstanceMember> loadMembers(long instanceUid);
	public abstract Long findReentryInstanceUid(int playerId, int worldId, long now);
}
