package com.aionemu.gameserver.model.instance;

public final class DynamicInstance {
	public static final byte OWNER_NONE = 0;
	public static final byte OWNER_PLAYER = 1;
	public static final byte OWNER_GROUP = 2;
	public static final byte OWNER_ALLIANCE = 3;
	public static final byte OWNER_LEAGUE = 4;
	public static final byte OWNER_MATCH = 5;

	public static final byte ACTIVE = 1;
	public static final byte EMPTY = 2;
	public static final byte DESTROYED = 3;

	private long instanceUid;
	private final int worldId;
	private final int creationId;
	private final int clientInstanceId;
	private final int runtimeInstanceId;
	private final byte ownerType;
	private final int ownerId;
	private final byte difficulty;
	private byte status;
	private final byte spawnPage;
	private final long createdAt;
	private long activeUntil;
	private long emptyUntil;
	private long destroyAt;
	private final int stateVersion;
	private String stateJson;
	private long updatedAt;

	public DynamicInstance(long instanceUid, int worldId, int creationId, int clientInstanceId,
			int runtimeInstanceId, byte ownerType, int ownerId, byte difficulty, byte status, byte spawnPage,
			long createdAt, long activeUntil, long emptyUntil, long destroyAt, int stateVersion, String stateJson,
			long updatedAt) {
		this.instanceUid = instanceUid;
		this.worldId = worldId;
		this.creationId = creationId;
		this.clientInstanceId = clientInstanceId;
		this.runtimeInstanceId = runtimeInstanceId;
		this.ownerType = ownerType;
		this.ownerId = ownerId;
		this.difficulty = difficulty;
		this.status = status;
		this.spawnPage = spawnPage;
		this.createdAt = createdAt;
		this.activeUntil = activeUntil;
		this.emptyUntil = emptyUntil;
		this.destroyAt = destroyAt;
		this.stateVersion = stateVersion;
		this.stateJson = stateJson;
		this.updatedAt = updatedAt;
	}

	public long getInstanceUid() {
		return instanceUid;
	}

	public void setInstanceUid(long instanceUid) {
		this.instanceUid = instanceUid;
	}

	public int getWorldId() {
		return worldId;
	}

	public int getCreationId() {
		return creationId;
	}

	public int getClientInstanceId() {
		return clientInstanceId;
	}

	public int getRuntimeInstanceId() {
		return runtimeInstanceId;
	}

	public byte getOwnerType() {
		return ownerType;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public byte getDifficulty() {
		return difficulty;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public byte getSpawnPage() {
		return spawnPage;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public long getActiveUntil() {
		return activeUntil;
	}

	public void setActiveUntil(long activeUntil) {
		this.activeUntil = activeUntil;
	}

	public long getEmptyUntil() {
		return emptyUntil;
	}

	public void setEmptyUntil(long emptyUntil) {
		this.emptyUntil = emptyUntil;
	}

	public long getDestroyAt() {
		return destroyAt;
	}

	public void setDestroyAt(long destroyAt) {
		this.destroyAt = destroyAt;
	}

	public int getStateVersion() {
		return stateVersion;
	}

	public String getStateJson() {
		return stateJson;
	}

	public void setStateJson(String stateJson) {
		this.stateJson = stateJson;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}
}
