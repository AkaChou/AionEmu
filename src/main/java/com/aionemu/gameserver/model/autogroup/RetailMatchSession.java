package com.aionemu.gameserver.model.autogroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class RetailMatchSession {
	public enum State {
		READY_CHECK,
		ENTERING,
		ACTIVE,
		FINISHED,
		CANCELLED
	}

	private final int matchmakerId;
	private final long instanceUid;
	private final long createdAt;
	private final long readyDeadline;
	private final long draftDeadline;
	private final List<Member> members;
	private State state;
	private int stateVersion;
	private String cancelReason = "";

	public RetailMatchSession(int matchmakerId, long instanceUid, long createdAt, long readyDeadline,
			long draftDeadline, List<Member> members) {
		this(matchmakerId, instanceUid, createdAt, readyDeadline, draftDeadline, State.READY_CHECK, 1, "", members);
	}

	private RetailMatchSession(int matchmakerId, long instanceUid, long createdAt, long readyDeadline,
			long draftDeadline, State state, int stateVersion, String cancelReason, List<Member> members) {
		this.matchmakerId = matchmakerId;
		this.instanceUid = instanceUid;
		this.createdAt = createdAt;
		this.readyDeadline = readyDeadline;
		this.draftDeadline = draftDeadline;
		this.state = state;
		this.stateVersion = stateVersion;
		this.cancelReason = cancelReason;
		this.members = new ArrayList<>(members);
	}

	public synchronized boolean add(Member member, long now) {
		if (!acceptsLateEntry(now) || member(member.playerId()) != null) {
			return false;
		}
		members.add(member);
		stateVersion++;
		return true;
	}

	public synchronized boolean remove(int playerId, String reason) {
		boolean removed = members.removeIf(member -> member.playerId() == playerId && !member.entered());
		if (removed) {
			cancelReason = reason == null ? "" : reason;
			stateVersion++;
		}
		return removed;
	}

	public synchronized boolean leave(int playerId) {
		boolean removed = members.removeIf(member -> member.playerId() == playerId);
		if (removed) {
			stateVersion++;
		}
		return removed;
	}

	public synchronized void resetPendingEntries() {
		boolean changed = false;
		for (Member member : members) {
			if (member.pressedEnter && !member.entered) {
				member.pressedEnter = false;
				changed = true;
			}
		}
		if (changed) {
			state = hasEnteredPlayers() ? State.ACTIVE : State.READY_CHECK;
			stateVersion++;
		}
	}

	public synchronized boolean pressEnter(int playerId) {
		Member member = member(playerId);
		if (member == null || member.pressedEnter() || state == State.CANCELLED || state == State.FINISHED) {
			return false;
		}
		member.pressedEnter = true;
		state = State.ENTERING;
		stateVersion++;
		return true;
	}

	public synchronized boolean markEntered(int playerId) {
		Member member = member(playerId);
		if (member == null) {
			return false;
		}
		member.pressedEnter = true;
		member.entered = true;
		member.online = true;
		state = State.ACTIVE;
		stateVersion++;
		return true;
	}

	public synchronized void markOnline(int playerId, boolean online) {
		Member member = member(playerId);
		if (member != null && member.online != online) {
			member.online = online;
			stateVersion++;
		}
	}

	public synchronized void cancel(String reason) {
		if (state != State.FINISHED && state != State.CANCELLED) {
			state = State.CANCELLED;
			cancelReason = reason == null ? "" : reason;
			stateVersion++;
		}
	}

	public synchronized void finish() {
		if (state != State.FINISHED) {
			state = State.FINISHED;
			stateVersion++;
		}
	}

	public synchronized boolean acceptsLateEntry(long now) {
		return (state == State.READY_CHECK || state == State.ENTERING || state == State.ACTIVE)
				&& (draftDeadline == 0 || now < draftDeadline);
	}

	public synchronized boolean hasEnteredPlayers() {
		return members.stream().anyMatch(Member::entered);
	}

	public synchronized Member member(int playerId) {
		return members.stream().filter(member -> member.playerId() == playerId).findFirst().orElse(null);
	}

	public synchronized List<Member> members() {
		return members.stream().map(Member::copy).toList();
	}

	public int matchmakerId() {
		return matchmakerId;
	}

	public long instanceUid() {
		return instanceUid;
	}

	public long createdAt() {
		return createdAt;
	}

	public long readyDeadline() {
		return readyDeadline;
	}

	public long draftDeadline() {
		return draftDeadline;
	}

	public synchronized State state() {
		return state;
	}

	public synchronized int stateVersion() {
		return stateVersion;
	}

	public synchronized String cancelReason() {
		return cancelReason;
	}

	public synchronized String encode() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
					output.writeInt(2);
				output.writeInt(matchmakerId);
				output.writeLong(instanceUid);
				output.writeLong(createdAt);
				output.writeLong(readyDeadline);
				output.writeLong(draftDeadline);
				output.writeByte(state.ordinal());
				output.writeInt(stateVersion);
				write(output, cancelReason);
				output.writeInt(members.size());
				for (Member member : members) {
					output.writeInt(member.playerId());
						write(output, member.name());
						output.writeByte(member.classId());
						output.writeByte(member.level());
						output.writeByte(member.raceId());
						output.writeByte(member.side());
						output.writeInt(member.teamId());
						output.writeInt(member.instanceGroupEntryId());
						output.writeLong(member.registeredAt());
					output.writeBoolean(member.pressedEnter());
					output.writeBoolean(member.entered());
					output.writeBoolean(member.online());
				}
			}
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to encode retail match session", e);
		}
	}

	public static RetailMatchSession decode(String value) {
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(value)))) {
			if (input.readInt() != 2) {
				throw new IllegalStateException("Unsupported retail match session version");
			}
			int matchmakerId = input.readInt();
			long instanceUid = input.readLong();
			long createdAt = input.readLong();
			long readyDeadline = input.readLong();
			long draftDeadline = input.readLong();
			int state = input.readUnsignedByte();
			int stateVersion = input.readInt();
			String cancelReason = read(input);
			int size = input.readInt();
			if (size < 0 || size > 1024) {
				throw new IllegalStateException("Invalid retail match member count " + size);
			}
			List<Member> members = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				members.add(new Member(input.readInt(), read(input), input.readByte(), input.readUnsignedByte(), input.readByte(),
						input.readByte(), input.readInt(), input.readInt(), input.readLong(), input.readBoolean(),
						input.readBoolean(), input.readBoolean()));
			}
			if (input.available() != 0 || state >= State.values().length) {
				throw new IllegalStateException("Invalid retail match session payload");
			}
			return new RetailMatchSession(matchmakerId, instanceUid, createdAt, readyDeadline, draftDeadline,
					State.values()[state], stateVersion, cancelReason, members);
		} catch (IOException | IllegalArgumentException e) {
			throw new IllegalStateException("Failed to decode retail match session", e);
		}
	}

	private static void write(DataOutputStream output, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	private static String read(DataInputStream input) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > 65535) {
			throw new IOException("Invalid retail match string length " + length);
		}
		return new String(input.readNBytes(length), StandardCharsets.UTF_8);
	}

	public static final class Member {
		private final int playerId;
		private final String name;
		private final byte classId;
		private final int level;
		private final byte raceId;
		private final byte side;
		private final int teamId;
		private final int instanceGroupEntryId;
		private final long registeredAt;
		private boolean pressedEnter;
		private boolean entered;
		private boolean online;

		public Member(int playerId, String name, byte classId, int level, byte raceId, byte side, int teamId,
				int instanceGroupEntryId, long registeredAt, boolean pressedEnter, boolean entered, boolean online) {
			this.playerId = playerId;
			this.name = name;
			this.classId = classId;
			this.level = level;
			this.raceId = raceId;
			this.side = side;
			this.teamId = teamId;
			this.instanceGroupEntryId = instanceGroupEntryId;
			this.registeredAt = registeredAt;
			this.pressedEnter = pressedEnter;
			this.entered = entered;
			this.online = online;
		}

		public int playerId() { return playerId; }
		public String name() { return name; }
		public byte classId() { return classId; }
		public int level() { return level; }
		public byte raceId() { return raceId; }
		public byte side() { return side; }
		public int teamId() { return teamId; }
		public int instanceGroupEntryId() { return instanceGroupEntryId; }
		public long registeredAt() { return registeredAt; }
		public boolean pressedEnter() { return pressedEnter; }
		public boolean entered() { return entered; }
		public boolean online() { return online; }

		private Member copy() {
			return new Member(playerId, name, classId, level, raceId, side, teamId, instanceGroupEntryId, registeredAt,
					pressedEnter, entered, online);
		}
	}
}
