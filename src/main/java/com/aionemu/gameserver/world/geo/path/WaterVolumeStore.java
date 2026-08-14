package com.aionemu.gameserver.world.geo.path;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 水体体积存储：从 water-volumes.bin 加载并查询各世界的水体。
 * Water volume store: loads water-volumes.bin and queries water volumes per world.
 */
@Slf4j
final class WaterVolumeStore {

	private static final int MAGIC = 0x41495756; // AIWV
	private static final int VERSION = 1;
	private static final String FILE_NAME = "water-volumes.bin";
	private volatile Map<Integer, List<Volume>> volumesByWorld = Map.of();

	int load() {
		File file = Config.geoFile(FILE_NAME);
		if (!file.isFile()) {
			volumesByWorld = Map.of();
			return 0;
		}
		try (InputStream input = new FileInputStream(file)) {
			return load(input);
		} catch (IOException | RuntimeException e) {
			volumesByWorld = Map.of();
			log.error(I18n.get("log.path.water_volumes_load_failed", file), e);
			return 0;
		}
	}

	int load(InputStream input) throws IOException {
		DataInputStream data = new DataInputStream(new BufferedInputStream(input));
		if (data.readInt() != MAGIC) {
			throw new IOException("Invalid water volume magic");
		}
		if (data.readInt() != VERSION) {
			throw new IOException("Unsupported water volume version");
		}
		int count = data.readInt();
		if (count < 0 || count > 100_000) {
			throw new IOException("Invalid water volume count: " + count);
		}
		Map<Integer, List<Volume>> loaded = new HashMap<>();
		for (int i = 0; i < count; i++) {
			int worldId = data.readInt();
			int id = data.readInt();
			int pointCount = data.readInt();
			if (pointCount < 3 || pointCount > 10_000) {
				throw new IOException("Invalid point count for water volume " + id + ": " + pointCount);
			}
			float[] x = new float[pointCount];
			float[] y = new float[pointCount];
			float[] z = new float[pointCount];
			for (int point = 0; point < pointCount; point++) {
				x[point] = data.readFloat();
				y[point] = data.readFloat();
				z[point] = data.readFloat();
			}
			loaded.computeIfAbsent(worldId, ignored -> new ArrayList<>()).add(new Volume(id, x, y, z));
		}
		loaded.replaceAll((ignored, volumes) -> List.copyOf(volumes));
		volumesByWorld = Map.copyOf(loaded);
		return count;
	}

	Volume find(int worldId, float x, float y, float z) {
		List<Volume> volumes = volumesByWorld.get(worldId);
		if (volumes == null) {
			return null;
		}
		Volume nearest = null;
		float nearestSurface = Float.POSITIVE_INFINITY;
		for (Volume volume : volumes) {
			if (!volume.contains(x, y)) {
				continue;
			}
			float surface = volume.surfaceZ(x, y);
			if (z <= surface + 0.5f && surface < nearestSurface) {
				nearest = volume;
				nearestSurface = surface;
			}
		}
		return nearest;
	}

	static final class Volume {

		private final int id;
		private final float[] x;
		private final float[] y;
		private final float[] z;
		private final float minX;
		private final float minY;
		private final float maxX;
		private final float maxY;
		private final float flatZ;
		private final int[] triangles;

		private Volume(int id, float[] x, float[] y, float[] z) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.z = z;
			float minX = Float.POSITIVE_INFINITY;
			float minY = Float.POSITIVE_INFINITY;
			float maxX = Float.NEGATIVE_INFINITY;
			float maxY = Float.NEGATIVE_INFINITY;
			float minZ = Float.POSITIVE_INFINITY;
			float maxZ = Float.NEGATIVE_INFINITY;
			float totalZ = 0;
			for (int i = 0; i < x.length; i++) {
				minX = Math.min(minX, x[i]);
				minY = Math.min(minY, y[i]);
				maxX = Math.max(maxX, x[i]);
				maxY = Math.max(maxY, y[i]);
				minZ = Math.min(minZ, z[i]);
				maxZ = Math.max(maxZ, z[i]);
				totalZ += z[i];
			}
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.flatZ = maxZ - minZ < 0.05f ? totalZ / z.length : Float.NaN;
			this.triangles = Float.isFinite(flatZ) ? new int[0] : triangulate(x, y);
		}

		int id() {
			return id;
		}

		boolean contains(float px, float py) {
			if (px < minX || px > maxX || py < minY || py > maxY) {
				return false;
			}
			boolean inside = false;
			for (int i = 0, j = x.length - 1; i < x.length; j = i++) {
				if (onSegment(px, py, x[j], y[j], x[i], y[i])) {
					return true;
				}
				if ((y[i] > py) != (y[j] > py)
						&& px < (x[j] - x[i]) * (py - y[i]) / (y[j] - y[i]) + x[i]) {
					inside = !inside;
				}
			}
			return inside;
		}

		float surfaceZ(float px, float py) {
			if (Float.isFinite(flatZ)) {
				return flatZ;
			}
			// ponytail: 对导出轮廓插值；若沼泽水面不准确再采样 WaterGeometry CGF。
			// ponytail: interpolate the exported contour; sample WaterGeometry CGFs only if swamp surfaces prove inaccurate.
			for (int i = 0; i < triangles.length; i += 3) {
				float value = triangleZ(px, py, triangles[i], triangles[i + 1], triangles[i + 2]);
				if (Float.isFinite(value)) {
					return value;
				}
			}
			float weightedZ = 0;
			float weights = 0;
			for (int i = 0; i < x.length; i++) {
				float dx = px - x[i];
				float dy = py - y[i];
				float weight = 1 / Math.max(0.01f, dx * dx + dy * dy);
				weightedZ += z[i] * weight;
				weights += weight;
			}
			return weightedZ / weights;
		}

		boolean allowsSegment(float startX, float startY, float startZ, float endX, float endY, float endZ,
				float clearance) {
			float dx = endX - startX;
			float dy = endY - startY;
			float dz = endZ - startZ;
			if (!containsSegment(startX, startY, endX, endY)) {
				return false;
			}
			int samples = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 2));
			for (int i = 0; i <= samples; i++) {
				float amount = (float) i / samples;
				float px = startX + dx * amount;
				float py = startY + dy * amount;
				float pz = startZ + dz * amount;
				float surface = surfaceZ(px, py);
				if (!Float.isFinite(surface) || pz > surface - clearance) {
					return false;
				}
			}
			return true;
		}

		private boolean containsSegment(float startX, float startY, float endX, float endY) {
			if (!contains(startX, startY) || !contains(endX, endY)) {
				return false;
			}
			double dx = endX - startX;
			double dy = endY - startY;
			if (dx == 0 && dy == 0) {
				return true;
			}
			List<Double> intersections = null;
			for (int i = 0, previous = x.length - 1; i < x.length; previous = i++) {
				double edgeX = x[i] - x[previous];
				double edgeY = y[i] - y[previous];
				double denominator = dx * edgeY - dy * edgeX;
				if (denominator == 0) {
					continue;
				}
				double offsetX = x[previous] - startX;
				double offsetY = y[previous] - startY;
				double segmentAmount = (offsetX * edgeY - offsetY * edgeX) / denominator;
				double edgeAmount = (offsetX * dy - offsetY * dx) / denominator;
				if (segmentAmount >= 0 && segmentAmount <= 1 && edgeAmount >= 0 && edgeAmount <= 1) {
					if (intersections == null) {
						intersections = new ArrayList<>();
						intersections.add(0d);
						intersections.add(1d);
					}
					intersections.add(segmentAmount);
				}
			}
			if (intersections == null) {
				return true;
			}
			intersections.sort(Double::compare);
			for (int i = 1; i < intersections.size(); i++) {
				double from = intersections.get(i - 1);
				double to = intersections.get(i);
				if (to > from) {
					double middle = (from + to) * 0.5;
					if (!contains((float) (startX + dx * middle), (float) (startY + dy * middle))) {
						return false;
					}
				}
			}
			return true;
		}

		private float triangleZ(float px, float py, int a, int b, int c) {
			float denominator = (y[b] - y[c]) * (x[a] - x[c]) + (x[c] - x[b]) * (y[a] - y[c]);
			if (Math.abs(denominator) < 0.0001f) {
				return Float.NaN;
			}
			float wa = ((y[b] - y[c]) * (px - x[c]) + (x[c] - x[b]) * (py - y[c])) / denominator;
			float wb = ((y[c] - y[a]) * (px - x[c]) + (x[a] - x[c]) * (py - y[c])) / denominator;
			float wc = 1 - wa - wb;
			return wa >= -0.0001f && wb >= -0.0001f && wc >= -0.0001f ? wa * z[a] + wb * z[b] + wc * z[c] : Float.NaN;
		}

		private static int[] triangulate(float[] x, float[] y) {
			int pointCount = x.length;
			if (pointCount > 3 && x[0] == x[pointCount - 1] && y[0] == y[pointCount - 1]) {
				pointCount--;
			}
			List<Integer> vertices = new ArrayList<>(pointCount);
			if (signedArea(x, y, pointCount) >= 0) {
				for (int i = 0; i < pointCount; i++) {
					vertices.add(i);
				}
			} else {
				for (int i = pointCount - 1; i >= 0; i--) {
					vertices.add(i);
				}
			}
			List<Integer> result = new ArrayList<>(Math.max(0, pointCount - 2) * 3);
			while (vertices.size() > 3) {
				boolean clipped = false;
				for (int i = 0; i < vertices.size(); i++) {
					int a = vertices.get((i + vertices.size() - 1) % vertices.size());
					int b = vertices.get(i);
					int c = vertices.get((i + 1) % vertices.size());
					if (cross(x[a], y[a], x[b], y[b], x[c], y[c]) <= 0.000001f) {
						continue;
					}
					boolean containsPoint = false;
					for (int point : vertices) {
						if (point != a && point != b && point != c && pointInTriangle(x[point], y[point], a, b, c, x, y)) {
							containsPoint = true;
							break;
						}
					}
					if (containsPoint) {
						continue;
					}
					result.add(a);
					result.add(b);
					result.add(c);
					vertices.remove(i);
					clipped = true;
					break;
				}
				if (!clipped) {
					return new int[0];
				}
			}
			if (vertices.size() == 3) {
				result.addAll(vertices);
			}
			int[] triangles = new int[result.size()];
			for (int i = 0; i < triangles.length; i++) {
				triangles[i] = result.get(i);
			}
			return triangles;
		}

		private static float signedArea(float[] x, float[] y, int count) {
			float area = 0;
			for (int i = 0, previous = count - 1; i < count; previous = i++) {
				area += x[previous] * y[i] - x[i] * y[previous];
			}
			return area * 0.5f;
		}

		private static boolean pointInTriangle(float px, float py, int a, int b, int c, float[] x, float[] y) {
			return cross(x[a], y[a], x[b], y[b], px, py) >= -0.000001f
					&& cross(x[b], y[b], x[c], y[c], px, py) >= -0.000001f
					&& cross(x[c], y[c], x[a], y[a], px, py) >= -0.000001f;
		}

		private static float cross(float ax, float ay, float bx, float by, float cx, float cy) {
			return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
		}

		private static boolean onSegment(float px, float py, float ax, float ay, float bx, float by) {
			float cross = (px - ax) * (by - ay) - (py - ay) * (bx - ax);
			return Math.abs(cross) < 0.001f && px >= Math.min(ax, bx) - 0.001f && px <= Math.max(ax, bx) + 0.001f
					&& py >= Math.min(ay, by) - 0.001f && py <= Math.max(ay, by) + 0.001f;
		}
	}
}
