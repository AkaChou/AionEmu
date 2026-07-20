package com.aionemu.gameserver.model.instance;

public final class DynamicInstanceMember {
	private final long instanceUid;
	private final int playerId;
	private final int teamIdAtEntry;
	private final byte side;
	private final boolean permitted;
	private final long joinedAt;
	private final long leftAt;
	private final long reentryUntil;
	private final int exitWorldId;
	private final String exitAlias;
	private final int entryLimitKey;
	private final boolean entryConsumed;

	public DynamicInstanceMember(long instanceUid, int playerId, int teamIdAtEntry, byte side, boolean permitted,
			long joinedAt, long leftAt, long reentryUntil, int exitWorldId, String exitAlias, int entryLimitKey,
			boolean entryConsumed) {
		this.instanceUid = instanceUid;
		this.playerId = playerId;
		this.teamIdAtEntry = teamIdAtEntry;
		this.side = side;
		this.permitted = permitted;
		this.joinedAt = joinedAt;
		this.leftAt = leftAt;
		this.reentryUntil = reentryUntil;
		this.exitWorldId = exitWorldId;
		this.exitAlias = exitAlias;
		this.entryLimitKey = entryLimitKey;
		this.entryConsumed = entryConsumed;
	}

	public long getInstanceUid() { return instanceUid; }
	public int getPlayerId() { return playerId; }
	public int getTeamIdAtEntry() { return teamIdAtEntry; }
	public byte getSide() { return side; }
	public boolean isPermitted() { return permitted; }
	public long getJoinedAt() { return joinedAt; }
	public long getLeftAt() { return leftAt; }
	public long getReentryUntil() { return reentryUntil; }
	public int getExitWorldId() { return exitWorldId; }
	public String getExitAlias() { return exitAlias; }
	public int getEntryLimitKey() { return entryLimitKey; }
	public boolean isEntryConsumed() { return entryConsumed; }
}
