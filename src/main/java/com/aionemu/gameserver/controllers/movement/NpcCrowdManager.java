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
	private static final long SIDE_HOLD_MILLIS = 667;
	private static final long STALE_MILLIS = 5000;
	private static final int[] CANDIDATE_ANGLES = {15, 30, 45};
	private static final Map<WorldKey, CrowdWorld> WORLDS = new ConcurrentHashMap<>();

	@FunctionalInterface
	interface Passability {
		boolean test(float x, float y, float z);
	}

	@FunctionalInterface
	interface Projection {
		float[] project(float x, float y, float z);
	}

	static float[] choose(Agent owner, float desiredX, float desiredY, float desiredZ, Passability passability, long now,
			long elapsedMillis) {
		return choose(owner, desiredX, desiredY, desiredZ, (x, y, z) -> new float[] {x, y, z}, passability, now,
				elapsedMillis);
	}

	static float[] choose(Agent owner, float desiredX, float desiredY, float desiredZ, Projection projection,
			Passability passability, long now, long elapsedMillis) {
		WorldKey key = new WorldKey(owner.worldId, owner.instanceId);
		return WORLDS.computeIfAbsent(key, ignored -> new CrowdWorld()).choose(owner, desiredX, desiredY, desiredZ,
				projection, passability, now, Math.max(1, elapsedMillis));
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

	static List<float[]> candidates(int preferredSide, float x, float y, float z, float vx, float vy, float vz) {
		List<float[]> result = new ArrayList<>(7);
		result.add(new float[] {x + vx, y + vy, z + vz});
		if (vx * vx + vy * vy > 0.000001f) {
			int side = preferredSide < 0 ? -1 : 1;
			for (int degrees : CANDIDATE_ANGLES) {
				addCandidate(result, side * degrees, x, y, z, vx, vy, vz);
				addCandidate(result, -side * degrees, x, y, z, vx, vy, vz);
			}
		}
		return result;
	}

	private static void addCandidate(List<float[]> result, int degrees, float x, float y, float z, float vx, float vy,
			float vz) {
		double angle = Math.toRadians(degrees);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		result.add(new float[] {x + vx * cos - vy * sin, y + vx * sin + vy * cos, z + vz});
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
			float relativeX = vx - other.vx * otherScale;
			float relativeY = vy - other.vy * otherScale;
			float relativeZ = vz - other.vz * otherScale;
			float separating = dx * relativeX + dy * relativeY + dz * relativeZ;
			float relativeSpeed = relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ;
			if (currentDistance < clearance && (separating > 0 || relativeSpeed < 0.0001f)) {
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

		private float[] choose(Agent owner, float desiredX, float desiredY, float desiredZ, Projection projection,
				Passability passability, long now, long elapsedMillis) {
			List<AgentMotion> neighbors;
			int preferredSide;
			synchronized (this) {
				AgentState state = update(owner, now);
				neighbors = neighbors(state.bucket, owner.id, now);
				preferredSide = state.sideUntil > now && state.preferredSide != 0
						? state.preferredSide : (owner.id & 1) == 0 ? 1 : -1;
			}
			// 先廉价碰撞，再投影 PATH 高度和检查通行，避免无效候选触发 geo。
			float[] direct = new float[] {desiredX, desiredY, desiredZ};
			boolean directPreferred = collisionFree(owner, desiredX - owner.x, desiredY - owner.y, desiredZ - owner.z,
					neighbors, elapsedMillis);
			if (directPreferred
					&& passability.test(desiredX, desiredY, desiredZ)) {
				synchronized (this) {
					AgentState state = agents.get(owner.id);
					if (state == null) {
						return null;
					}
					updateMovement(state, owner, direct, desiredX, desiredY, elapsedMillis, now);
					cleanup(now, false);
				}
				return direct;
			}
			List<float[]> candidates = candidates(preferredSide, owner.x, owner.y, owner.z,
					desiredX - owner.x, desiredY - owner.y, desiredZ - owner.z);
			float[] selected = null;
			for (int i = 1; i < candidates.size(); i++) {
				float[] candidate = candidates.get(i);
				float vx = candidate[0] - owner.x;
				float vy = candidate[1] - owner.y;
				float vz = candidate[2] - owner.z;
				if (!collisionFree(owner, vx, vy, vz, neighbors, elapsedMillis)) {
					continue;
				}
				float[] projected = projection.project(candidate[0], candidate[1], candidate[2]);
				if (projected != null && passability.test(projected[0], projected[1], projected[2])) {
					selected = projected;
					break;
				}
			}
			if (selected == null && !directPreferred && passability.test(desiredX, desiredY, desiredZ)) {
				selected = direct;
			}
			synchronized (this) {
				AgentState state = agents.get(owner.id);
				if (state == null) {
					return null;
				}
				updateMovement(state, owner, selected, desiredX, desiredY, elapsedMillis, now);
				cleanup(now, false);
			}
			return selected;
		}

		private static void updateMovement(AgentState state, Agent owner, float[] selected, float desiredX, float desiredY,
				long elapsedMillis, long now) {
			state.vx = selected == null ? 0 : selected[0] - owner.x;
			state.vy = selected == null ? 0 : selected[1] - owner.y;
			state.vz = selected == null ? 0 : selected[2] - owner.z;
			state.elapsedMillis = elapsedMillis;
			state.updatedAt = now;
			if (selected != null) {
				float cross = (desiredX - owner.x) * (selected[1] - owner.y)
						- (desiredY - owner.y) * (selected[0] - owner.x);
				if (Math.abs(cross) > 0.0001f) {
					state.preferredSide = cross > 0 ? 1 : -1;
					state.sideUntil = now + SIDE_HOLD_MILLIS;
				}
			}
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
		private int preferredSide;
		private long sideUntil;

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
