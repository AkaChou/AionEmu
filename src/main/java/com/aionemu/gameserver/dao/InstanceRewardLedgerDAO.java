package com.aionemu.gameserver.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.aionemu.commons.database.dao.DAO;

public abstract class InstanceRewardLedgerDAO implements DAO {
	public static final byte PENDING = 0;
	public static final byte COMPLETED = 1;

	@Override
	public final String getClassName() {
		return InstanceRewardLedgerDAO.class.getName();
	}

	public abstract boolean queue(long instanceUid, int playerId, String rewardKey, String payloadHash,
			String payloadJson, long createdAt);

	public abstract boolean lockOrCreate(Connection connection, long instanceUid, int playerId, String rewardKey,
			String payloadHash, String payloadJson, long createdAt) throws SQLException;

	public abstract void complete(Connection connection, long instanceUid, int playerId, String rewardKey,
			long completedAt) throws SQLException;

	public abstract List<PendingReward> loadPending(int playerId);

	public record PendingReward(long instanceUid, String rewardKey, String payloadHash, String payloadJson) {
	}
}
