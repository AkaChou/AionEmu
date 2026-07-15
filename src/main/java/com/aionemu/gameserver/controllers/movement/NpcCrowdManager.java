package com.aionemu.gameserver.controllers.movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class NpcCrowdManager {

	private static final float BUCKET_SIZE = 4;
	private static final long PREDICTION_MILLIS = 667;
	private static final long STALE_MILLIS = 5000;
	private static final Map<WorldKey, CrowdWorld> WORLDS = new ConcurrentHashMap<>();

	@FunctionalInterface
	interface Passability {
		boolean test(float x, float y, float z);
	}

	static float[] choose(Agent owner, float desiredX, float desiredY, float desiredZ, Passability passability, long now,
			long elapsedMillis) {
		WorldKey key = new WorldKey(owner.worldId, owner.instanceId);
		return WORLDS.computeIfAbsent(key, ignored -> new CrowdWorld()).choose(owner, desiredX, desiredY, desiredZ, passability, now,
				Math.max(1, elapsedMillis));
	}

	static void remove(int ownerId) {
		for (CrowdWorld world : WORLDS.values()) {
			world.remove(ownerId);
		}
		WORLDS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	static void clear() {
		WORLDS.clear();
	}

	static void cleanup(long now) {
		for (CrowdWorld world : WORLDS.values()) {
			world.cleanup(now, true);
		}
		WORLDS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	static int agentCount(int worldId, int instanceId) {
		CrowdWorld world = WORLDS.get(new WorldKey(worldId, instanceId));
		return world == null ? 0 : world.agentCount();
	}

	private static List<float[]> candidates(float x, float y, float z, float vx, float vy, float vz) {
		List<float[]> result = new ArrayList<>(12);
		result.add(new float[] {x + vx, y + vy, z + vz});
		if (vx * vx + vy * vy > 0.000001f) {
			for (int direction = 1; direction < 12; direction++) {
				double angle = direction * Math.PI / 6;
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);
				result.add(new float[] {x + vx * cos - vy * sin, y + vx * sin + vy * cos, z + vz});
			}
		}
		return result;
	}

	static float predictedDistance(float x, float y, float z, float vx, float vy, float vz,
			float otherX, float otherY, float otherZ, float otherVx, float otherVy, float otherVz,
			long elapsedMillis, long otherElapsedMillis) {
		float px = x - otherX;
		float py = y - otherY;
		float pz = z - otherZ;
		float otherScale = elapsedMillis / (float) Math.max(1, otherElapsedMillis);
		float rvx = vx - otherVx * otherScale;
		float rvy = vy - otherVy * otherScale;
		float rvz = vz - otherVz * otherScale;
		float speed = rvx * rvx + rvy * rvy + rvz * rvz;
		float horizon = PREDICTION_MILLIS / (float) Math.max(1, elapsedMillis);
		float time = speed < 0.0001f ? 0 : Math.max(0, Math.min(horizon, -(px * rvx + py * rvy + pz * rvz) / speed));
		float dx = px + rvx * time;
		float dy = py + rvy * time;
		float dz = pz + rvz * time;
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static boolean collisionFree(Agent owner, float vx, float vy, float vz, List<AgentMotion> neighbors,
			long elapsedMillis) {
		for (AgentMotion other : neighbors) {
			float clearance = Math.max(0.5f, owner.collision + other.agent.collision);
			float otherScale = elapsedMillis / (float) Math.max(1, other.elapsedMillis);
			float dx = owner.x - other.agent.x;
			float dy = owner.y - other.agent.y;
			float dz = owner.z - other.agent.z;
			float currentDistance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			float separating = dx * (vx - other.vx * otherScale) + dy * (vy - other.vy * otherScale)
					+ dz * (vz - other.vz * otherScale);
			if (currentDistance < clearance && separating > 0) {
				continue;
			}
			if (predictedDistance(owner.x, owner.y, owner.z, vx, vy, vz, other.agent.x, other.agent.y, other.agent.z,
					other.vx, other.vy, other.vz, elapsedMillis, other.elapsedMillis) < clearance) {
				return false;
			}
		}
		return true;
	}

	private static Bucket bucket(float x, float y, float z) {
		return new Bucket((int) Math.floor(x / BUCKET_SIZE), (int) Math.floor(y / BUCKET_SIZE), (int) Math.floor(z / BUCKET_SIZE));
	}

	private static final class CrowdWorld {

		private final Map<Integer, AgentState> agents = new HashMap<>();
		private final Map<Bucket, Set<Integer>> buckets = new HashMap<>();
		private int cleanupTick;

		private float[] choose(Agent owner, float desiredX, float desiredY, float desiredZ,
				Passability passability, long now, long elapsedMillis) {
			List<AgentMotion> neighbors;
			synchronized (this) {
				AgentState state = update(owner, now);
				neighbors = neighbors(state.bucket, owner.id, now);
			}
			// 先廉价碰撞，再对真正可能采用的候选做 passability，避免先扫 12 次 geo。
			float[] direct = new float[] {desiredX, desiredY, desiredZ};
			if (collisionFree(owner, desiredX - owner.x, desiredY - owner.y, desiredZ - owner.z, neighbors, elapsedMillis)
					&& passability.test(desiredX, desiredY, desiredZ)) {
				synchronized (this) {
					AgentState state = agents.get(owner.id);
					if (state == null) {
						return null;
					}
					updateMovement(state, owner, direct, elapsedMillis, now);
					cleanup(now, false);
				}
				return direct;
			}
			List<float[]> candidates = candidates(owner.x, owner.y, owner.z,
					desiredX - owner.x, desiredY - owner.y, desiredZ - owner.z);
			float[] selected = null;
			for (int i = 1; i < candidates.size(); i++) {
				float[] candidate = candidates.get(i);
				float vx = candidate[0] - owner.x;
				float vy = candidate[1] - owner.y;
				float vz = candidate[2] - owner.z;
				if (collisionFree(owner, vx, vy, vz, neighbors, elapsedMillis)
						&& passability.test(candidate[0], candidate[1], candidate[2])) {
					selected = candidate;
					break;
				}
			}
			synchronized (this) {
				AgentState state = agents.get(owner.id);
				if (state == null) {
					return null;
				}
				updateMovement(state, owner, selected, elapsedMillis, now);
				cleanup(now, false);
			}
			return selected;
		}

		private static void updateMovement(AgentState state, Agent owner, float[] selected, long elapsedMillis, long now) {
			state.vx = selected == null ? 0 : selected[0] - owner.x;
			state.vy = selected == null ? 0 : selected[1] - owner.y;
			state.vz = selected == null ? 0 : selected[2] - owner.z;
			state.elapsedMillis = elapsedMillis;
			state.updatedAt = now;
		}

		private AgentState update(Agent owner, long now) {
			AgentState state = agents.get(owner.id);
			Bucket nextBucket = bucket(owner.x, owner.y, owner.z);
			if (state == null) {
				state = new AgentState(owner, nextBucket, now);
				agents.put(owner.id, state);
				buckets.computeIfAbsent(nextBucket, ignored -> new HashSet<>()).add(owner.id);
				return state;
			}
			if (!state.bucket.equals(nextBucket)) {
				removeFromBucket(state.bucket, owner.id);
				buckets.computeIfAbsent(nextBucket, ignored -> new HashSet<>()).add(owner.id);
				state.bucket = nextBucket;
			}
			state.agent = owner;
			state.updatedAt = now;
			return state;
		}

		private List<AgentMotion> neighbors(Bucket center, int ownerId, long now) {
			List<AgentMotion> result = new ArrayList<>();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						Set<Integer> ids = buckets.get(new Bucket(center.x + dx, center.y + dy, center.z + dz));
						if (ids == null) {
							continue;
						}
						for (int id : ids) {
							AgentState state = agents.get(id);
							if (id != ownerId && state != null && now - state.updatedAt <= STALE_MILLIS) {
								result.add(new AgentMotion(state.agent, state.vx, state.vy, state.vz, state.elapsedMillis));
							}
						}
					}
				}
			}
			return result;
		}

		private synchronized void remove(int ownerId) {
			AgentState state = agents.remove(ownerId);
			if (state != null) {
				removeFromBucket(state.bucket, ownerId);
			}
		}

		private void removeFromBucket(Bucket bucket, int ownerId) {
			Set<Integer> ids = buckets.get(bucket);
			if (ids != null) {
				ids.remove(ownerId);
				if (ids.isEmpty()) {
					buckets.remove(bucket);
				}
			}
		}

		private synchronized void cleanup(long now, boolean force) {
			if (!force && (++cleanupTick & 255) != 0) {
				return;
			}
			List<Integer> stale = new ArrayList<>();
			for (AgentState state : agents.values()) {
				if (now - state.updatedAt > STALE_MILLIS) {
					stale.add(state.agent.id);
				}
			}
			for (int id : stale) {
				remove(id);
			}
		}

		private synchronized int agentCount() {
			return agents.size();
		}

		private synchronized boolean isEmpty() {
			return agents.isEmpty();
		}
	}

	private static final class AgentState {
		private Agent agent;
		private Bucket bucket;
		private float vx;
		private float vy;
		private float vz;
		private long elapsedMillis = 1;
		private long updatedAt;

		private AgentState(Agent agent, Bucket bucket, long updatedAt) {
			this.agent = agent;
			this.bucket = bucket;
			this.updatedAt = updatedAt;
		}
	}

	private record WorldKey(int worldId, int instanceId) {}

	private record Bucket(int x, int y, int z) {}

	private record AgentMotion(Agent agent, float vx, float vy, float vz, long elapsedMillis) {}

	record Agent(int id, int worldId, int instanceId, float x, float y, float z, float collision) {}
}
