package com.aionemu.gameserver.world.geo.path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

final class SpatialPathfinder {

	private static final int[] DELTAS = {-1, 0, 1};
	private static final float MAX_STAGE_STEP = 32;

	@FunctionalInterface
	interface PointAllowed {
		boolean test(float x, float y, float z);
	}

	@FunctionalInterface
	interface EdgeAllowed {
		boolean test(float startX, float startY, float startZ, float endX, float endY, float endZ);
	}

	static List<float[]> find(float startX, float startY, float startZ, float targetX, float targetY, float targetZ,
			float step, int maxNodes, PointAllowed pointAllowed, EdgeAllowed edgeAllowed) {
		return find(startX, startY, startZ, targetX, targetY, targetZ, step, maxNodes, 24, 12, pointAllowed, edgeAllowed);
	}

	static List<float[]> findProgressive(float startX, float startY, float startZ, float targetX, float targetY, float targetZ,
			float step, int maxNodes, PointAllowed pointAllowed, EdgeAllowed edgeAllowed) {
		float horizontalDistance = (float) Math.hypot(targetX - startX, targetY - startY);
		return findProgressive(startX, startY, startZ, targetX, targetY, targetZ, step, maxNodes,
				Math.max(96, horizontalDistance), Math.max(48, Math.abs(targetZ - startZ)), pointAllowed, edgeAllowed);
	}

	static List<float[]> findProgressive(float startX, float startY, float startZ, float targetX, float targetY, float targetZ,
			float step, int maxNodes, float maxHorizontalDetour, float maxVerticalDetour,
			PointAllowed pointAllowed, EdgeAllowed edgeAllowed) {
		float stageStep = Math.min(step, MAX_STAGE_STEP);
		float horizontalDetour = Math.min(24, Math.max(4 * stageStep, maxHorizontalDetour));
		float verticalDetour = Math.min(12, Math.max(3 * stageStep, maxVerticalDetour));
		while (true) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			List<float[]> path = find(startX, startY, startZ, targetX, targetY, targetZ, stageStep, maxNodes,
					horizontalDetour, verticalDetour, pointAllowed, edgeAllowed);
			if (path != null) {
				return path;
			}
			if (horizontalDetour >= maxHorizontalDetour && verticalDetour >= maxVerticalDetour) {
				break;
			}
			stageStep = Math.min(stageStep * 2, MAX_STAGE_STEP);
			horizontalDetour = Math.min(maxHorizontalDetour, horizontalDetour * 2);
			verticalDetour = Math.min(maxVerticalDetour, verticalDetour * 2);
		}
		return null;
	}

	static List<float[]> find(float startX, float startY, float startZ, float targetX, float targetY, float targetZ,
			float step, int maxNodes, float horizontalDetour, float verticalDetour, PointAllowed pointAllowed,
			EdgeAllowed edgeAllowed) {
		if (pointAllowed.test(targetX, targetY, targetZ)
				&& edgeAllowed.test(startX, startY, startZ, targetX, targetY, targetZ)) {
			return List.of(new float[] {targetX, targetY, targetZ});
		}
		int detour = Math.max(4, Math.round(horizontalDetour / step));
		int verticalCells = Math.max(3, Math.round(verticalDetour / step));
		int targetCellX = Math.round((targetX - startX) / step);
		int targetCellY = Math.round((targetY - startY) / step);
		int targetCellZ = Math.round((targetZ - startZ) / step);
		int minX = Math.min(0, targetCellX) - detour;
		int maxX = Math.max(0, targetCellX) + detour;
		int minY = Math.min(0, targetCellY) - detour;
		int maxY = Math.max(0, targetCellY) + detour;
		int minZ = Math.min(0, targetCellZ) - verticalCells;
		int maxZ = Math.max(0, targetCellZ) + verticalCells;

		Cell start = new Cell(0, 0, 0);
		Map<Cell, SearchNode> visited = new HashMap<>();
		PriorityQueue<OpenNode> open = new PriorityQueue<>(Comparator.comparingDouble(OpenNode::score));
		SearchNode first = new SearchNode(start, null, 0, distance(startX, startY, startZ, targetX, targetY, targetZ));
		visited.put(start, first);
		open.add(new OpenNode(start, 0, first.score));
		int processed = 0;
		int budget = Math.max(1, maxNodes);
		while (!open.isEmpty() && processed < budget && !Thread.currentThread().isInterrupted()) {
			OpenNode queued = open.poll();
			SearchNode current = visited.get(queued.cell());
			if (current == null || current.closed || Float.compare(current.cost, queued.cost()) != 0) {
				continue;
			}
			processed++;
			current.closed = true;
			float currentX = startX + current.cell.x * step;
			float currentY = startY + current.cell.y * step;
			float currentZ = startZ + current.cell.z * step;
			if (distance(currentX, currentY, currentZ, targetX, targetY, targetZ) <= step * 1.75f
					&& pointAllowed.test(targetX, targetY, targetZ)
					&& edgeAllowed.test(currentX, currentY, currentZ, targetX, targetY, targetZ)) {
				return reconstruct(current, startX, startY, startZ, targetX, targetY, targetZ, step);
			}
			for (int dx : DELTAS) {
				for (int dy : DELTAS) {
					for (int dz : DELTAS) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						Cell next = new Cell(current.cell.x + dx, current.cell.y + dy, current.cell.z + dz);
						if (next.x < minX || next.x > maxX || next.y < minY || next.y > maxY || next.z < minZ || next.z > maxZ) {
							continue;
						}
						float nextX = startX + next.x * step;
						float nextY = startY + next.y * step;
						float nextZ = startZ + next.z * step;
						if (!pointAllowed.test(nextX, nextY, nextZ)
								|| !edgeAllowed.test(currentX, currentY, currentZ, nextX, nextY, nextZ)) {
							continue;
						}
						float cost = current.cost + step * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
						SearchNode known = visited.get(next);
						if (known == null || cost < known.cost) {
							float score = cost + distance(nextX, nextY, nextZ, targetX, targetY, targetZ);
							if (known == null) {
								known = new SearchNode(next, current, cost, score);
								visited.put(next, known);
							} else {
								known.parent = current;
								known.cost = cost;
								known.score = score;
								known.closed = false;
							}
							open.add(new OpenNode(next, cost, score));
						}
					}
				}
			}
		}
		return null;
	}

	private static List<float[]> reconstruct(SearchNode end, float startX, float startY, float startZ,
			float targetX, float targetY, float targetZ, float step) {
		List<float[]> reverse = new ArrayList<>();
		for (SearchNode node = end; node.parent != null; node = node.parent) {
			reverse.add(new float[] {startX + node.cell.x * step, startY + node.cell.y * step, startZ + node.cell.z * step});
		}
		List<float[]> result = new ArrayList<>(reverse.size() + 1);
		for (int i = reverse.size() - 1; i >= 0; i--) {
			result.add(reverse.get(i));
		}
		result.add(new float[] {targetX, targetY, targetZ});
		return result;
	}

	private static float distance(float x, float y, float z, float targetX, float targetY, float targetZ) {
		float dx = x - targetX;
		float dy = y - targetY;
		float dz = z - targetZ;
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private record Cell(int x, int y, int z) {}

	private static final class SearchNode {
		private final Cell cell;
		private SearchNode parent;
		private float cost;
		private float score;
		private boolean closed;

		private SearchNode(Cell cell, SearchNode parent, float cost, float score) {
			this.cell = cell;
			this.parent = parent;
			this.cost = cost;
			this.score = score;
		}
	}

	private record OpenNode(Cell cell, float cost, float score) {}
}
