package com.aionemu.gameserver.geoEngine.models;

import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 高度图地形，支持射线碰撞与材质采样。
 * Heightmap terrain supporting ray collision and material sampling.
 */
public class Terrain {

	/** 高度图网格单位尺寸（世界单位）。 / Heightmap grid unit size in world units. */
	private static final int HEIGHTMAP_UNIT_SIZE = 2;
	/** Exclusive 最大 Zforheightscaling / Exclusive max Z for height scaling */
	private static final int HEIGHTMAP_MAX_Z_EXCLUSIVE = 2048;

	/** 沿 X 的高度图尺寸 / Heightmap size along X */
	private int heightmapXSize, heightmapYSize;
	/** 高度图数据（全相同高度时压缩为单元素）。 / Height samples (compressed to one if uniform). */
	private short[] heightmap;
	/** 沿 X 的材质图尺寸 / Material map size along X */
	private int materialsXSize, materialsYSize;
	/** 材质图数据。 / Material map bytes. */
	private byte[] materials;

	/**
	 * 设置高度图；全同高度时压缩存储。
	 * Sets the heightmap; compresses to a single value when all samples match.
	 *
	 * height samples
	 * size along X
	 * size along Y
	 */
	public void setHeightmap(short[] heightmap, int heightmapXSize, int heightmapYSize) {
		if (materials != null && (heightmapXSize < materialsXSize || heightmapYSize < materialsYSize)) {
			throw new IllegalArgumentException("Terrain heightmap must not be smaller than terrain materials");
		}
		if (heightmap.length != heightmapXSize * heightmapYSize) {
			throw new IllegalArgumentException("Expected terrain heightmap length differs by " + (heightmap.length - heightmapXSize * heightmapYSize) + " bytes");
		}
		boolean allSameZValues = heightmap.length > 0;
		for (short z : heightmap) {
			if (z != heightmap[0]) {
				allSameZValues = false;
				break;
			}
		}
		this.heightmap = allSameZValues ? new short[] { heightmap[0] } : heightmap;
		this.heightmapXSize = heightmapXSize;
		this.heightmapYSize = heightmapYSize;
	}

	/**
	 * 设置地形材质图。
	 * Sets the terrain material map.
	 *
	 * material bytes
	 * size along X
	 * size along Y
	 */
	public void setMaterials(byte[] materials, int materialsXSize, int materialsYSize) {
		if (heightmap != null && (materialsXSize > heightmapXSize || materialsYSize > heightmapYSize)) {
			throw new IllegalArgumentException("Terrain materials need a terrain heightmap of at least the same size");
		}
		if (materials.length != materialsXSize * materialsYSize) {
			throw new IllegalArgumentException("Expected terrain materials length differs by " + (materials.length - materialsXSize * materialsYSize) + " bytes");
		}
		this.materials = materials;
		this.materialsXSize = materialsXSize;
		this.materialsYSize = materialsYSize;
	}

	/**
	 * 是否已设置高度图。
	 * Whether a heightmap is present.
	 *
	 * @return 若 heightmap set 则为 true / true if heightmap set
	 */
	public boolean hasHeightmap() {
		return heightmap != null;
	}

	/**
	 * 是否已设置材质图。
	 * Whether a material map is present.
	 *
	 * @return 若 materials set 则为 true / true if materials set
	 */
	public boolean hasMaterials() {
		return materials != null;
	}

	public float getPathHeight(float x, float y) {
		if (heightmap == null) {
			return Float.NaN;
		}
		float sampleX = x / HEIGHTMAP_UNIT_SIZE;
		float sampleY = y / HEIGHTMAP_UNIT_SIZE;
		int x0 = (int) Math.floor(sampleX);
		int y0 = (int) Math.floor(sampleY);
		if (x0 < 0 || y0 < 0 || x0 + 1 >= heightmapXSize || y0 + 1 >= heightmapYSize) {
			return Float.NaN;
		}
		float fx = sampleX - x0;
		float fy = sampleY - y0;
		float top = pathHeight(x0, y0) * (1 - fx) + pathHeight(x0 + 1, y0) * fx;
		float bottom = pathHeight(x0, y0 + 1) * (1 - fx) + pathHeight(x0 + 1, y0 + 1) * fx;
		return top * (1 - fy) + bottom * fy;
	}

	private float pathHeight(int x, int y) {
		short value = heightmap.length == 1 ? heightmap[0] : heightmap[y + x * heightmapYSize];
		return value == -1 ? Float.NaN : (Short.toUnsignedInt(value) & 0xfffc) / 32f;
	}

	/**
	 * 在射线起点附近与地形做碰撞检测。
	 * Collides the ray against terrain near the ray origin.
	 *
	 * ray
	 * @param results 碰撞结果收集器 / collision results collector
	 */
	public void collideAtOrigin(Ray ray, CollisionResults results) {
		collideNearXY(ray.origin.x, ray.origin.y, ray, new Vector3f(), new Vector3f(), new Vector3f(), results);
	}

	/**
	 * 沿射线 2D 投影路径逐步检测地形碰撞。
	 * Walks the ray's 2D projection and tests terrain collision stepwise.
	 *
	 * ray
	 * target X
	 * target Y
	 * @param results 碰撞结果（可为 null） / results (may be null)
	 * @return 是否发生碰撞 / true if a hit was found
	 */
	public boolean collide(Ray ray, float targetX, float targetY, CollisionResults results) {
		float distanceX = targetX - ray.origin.x;
		float distanceY = targetY - ray.origin.y;
		float distance2D = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
		if (distance2D == 0) {
			return false;
		}
		float checkDistanceLimit = distance2D + HEIGHTMAP_UNIT_SIZE;
		Vector3f p1or4 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
		for (int checkDistance = 0; checkDistance < checkDistanceLimit; checkDistance += HEIGHTMAP_UNIT_SIZE) {
			float distanceFactor = checkDistance / distance2D;
			float x = ray.origin.x + distanceX * distanceFactor;
			float y = ray.origin.y + distanceY * distanceFactor;
			if (collideNearXY(x, y, ray, p1or4, p2, p3, results)
				|| collideNearXY(x + HEIGHTMAP_UNIT_SIZE, y, ray, p1or4, p2, p3, results)
				|| collideNearXY(x, y + HEIGHTMAP_UNIT_SIZE, ray, p1or4, p2, p3, results)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 在 (x,y) 附近网格单元与射线做三角面相交检测。
	 * Tests ray intersection with the terrain cell near (x, y).
	 *
	 * @param x 世界 X / world X
	 * @param y 世界 Y / world Y
	 * ray
	 * @param p1or4 复用顶点缓冲 / reusable vertex
	 * @param p2 复用顶点缓冲 / reusable vertex
	 * @param p3 复用顶点缓冲 / reusable vertex
	 * @param results 结果收集器（可为 null） / results (may be null)
	 * 若 hit 则为 true / true if hit
	 */
	private boolean collideNearXY(float x, float y, Ray ray, Vector3f p1or4, Vector3f p2, Vector3f p3, CollisionResults results) {
		int xIndexNorth = (int) (x / HEIGHTMAP_UNIT_SIZE);
		int yIndexWest = (int) (y / HEIGHTMAP_UNIT_SIZE);
		int yIndexEast = yIndexWest + 1;
		float z2 = getZ(xIndexNorth, yIndexEast);
		if (Float.isNaN(z2)) {
			return false;
		}
		int xIndexSouth = xIndexNorth + 1;
		float z3 = getZ(xIndexSouth, yIndexWest);
		if (Float.isNaN(z3)) {
			return false;
		}
		float z1 = getZ(xIndexNorth, yIndexWest);
		float z4 = getZ(xIndexSouth, yIndexEast);
		int xNorth = xIndexNorth * HEIGHTMAP_UNIT_SIZE;
		int yWest = yIndexWest * HEIGHTMAP_UNIT_SIZE;
		int yEast = yWest + HEIGHTMAP_UNIT_SIZE;
		int xSouth = xNorth + HEIGHTMAP_UNIT_SIZE;
		p2.set(xNorth, yEast, z2);
		p3.set(xSouth, yWest, z3);
		Vector3f contactPoint = new Vector3f();
		if ((Float.isNaN(z1) || !ray.intersectWhere(p1or4.set(xNorth, yWest, z1), p2, p3, contactPoint))
			&& (Float.isNaN(z4) || !ray.intersectWhere(p1or4.set(xSouth, yEast, z4), p2, p3, contactPoint))) {
			return false;
		}
		float distance = contactPoint.distance(ray.origin);
		if (distance > ray.getLimit()) {
			return false;
		}
		if (results != null) {
			if (results.shouldInvalidateSlopingSurface() && getMaximumZDiff(p1or4, p2, p3) > HEIGHTMAP_UNIT_SIZE) {
				contactPoint.setZ(Float.NaN);
			}
			results.addCollision(new CollisionResult(contactPoint, distance));
		}
		return true;
	}

	/**
	 * 按网格索引取高度；越界返回 NaN，边界返回 0。
	 * Height at grid index; NaN if out of bounds, 0 on the outer border.
	 *
	 * X index
	 * Y index
	 * @return 世界高度或 NaN / world Z or NaN
	 */
	private float getZ(int xIndex, int yIndex) {
		if (xIndex < 0 || yIndex < 0 || xIndex > heightmapXSize || yIndex > heightmapYSize) {
			return Float.NaN;
		}
		if (xIndex == 0 || yIndex == 0 || xIndex == heightmapXSize || yIndex == heightmapYSize) {
			return 0;
		}
		if (heightmap.length == 1) {
			return getZ(0);
		}
		return getZ(yIndex + xIndex * heightmapYSize);
	}

	/**
	 * 将 short 高度采样映射为世界 Z（-1 表示空洞）。
	 * Maps an unsigned short height sample to world Z (-1 means hole).
	 *
	 * @param index 高度图索引 / heightmap index
	 * @return 世界高度或 NaN / world Z or NaN
	 */
	private float getZ(int index) {
		return heightmap[index] == -1 ? Float.NaN : Short.toUnsignedInt(heightmap[index]) * HEIGHTMAP_MAX_Z_EXCLUSIVE / (0xFFFF + 1f);
	}

	/**
	 * 采样 (x,y) 处的地形材质 ID。
	 * Samples the terrain material id at (x, y).
	 *
	 * @param x 世界 X / world X
	 * @param y 世界 Y / world Y
	 * material id, or 0
	 */
	public int getTerrainMaterialAt(float x, float y) {
		if (materials == null) {
			return 0;
		}
		int mat1x = (int) (x / HEIGHTMAP_UNIT_SIZE);
		int mat1y = (int) (y / HEIGHTMAP_UNIT_SIZE);
		if (mat1x < 0 || mat1y < 0 || mat1x + 1 >= materialsXSize || mat1y + 1 >= materialsYSize) {
			return 0;
		}
		int mat1Index = mat1y + mat1x * materialsYSize;
		int mat3Index = mat1Index + materialsYSize;
		int mat = Byte.toUnsignedInt(materials[mat1Index]);
		if (mat != 0 && mat == Byte.toUnsignedInt(materials[mat1Index + 1]) && mat == Byte.toUnsignedInt(materials[mat3Index])) {
			if (isLeft(x + HEIGHTMAP_UNIT_SIZE, y, x, y + HEIGHTMAP_UNIT_SIZE, x, y)) {
				return mat;
			}
		}
		if (mat3Index + 1 < materials.length) {
			mat = Byte.toUnsignedInt(materials[mat3Index + 1]);
			if (mat != 0 && mat == Byte.toUnsignedInt(materials[mat3Index]) && mat == Byte.toUnsignedInt(materials[mat1Index + 1])
				&& !isLeft(x + HEIGHTMAP_UNIT_SIZE, y, x, y + HEIGHTMAP_UNIT_SIZE, x, y)) {
				return mat;
			}
		}
		return 0;
	}

	/**
	 * 判断目标点是否在有向线段左侧。
	 * Whether the target point lies to the left of the directed segment.
	 *
	 * start X
	 * start Y
	 * end X
	 * end Y
	 * target X
	 * target Y
	 *
	 * @return 是否在左侧 / true if left of the segment
	 */
	private boolean isLeft(float startX, float startY, float endX, float endY, float targetX, float targetY) {
		return (endX - startX) * (targetY - startY) > (endY - startY) * (targetX - startX);
	}

	/**
	 * 三个顶点的最大 Z 差。
	 * Maximum Z difference among three vertices.
	 *
	 * @param v1 顶点 1 / vertex 1
	 * @param v2 顶点 2 / vertex 2
	 * @param v3 顶点 3 / vertex 3
	 * Z delta
	 */
	private float getMaximumZDiff(Vector3f v1, Vector3f v2, Vector3f v3) {
		return Math.max(v1.z, Math.max(v2.z, v3.z)) - Math.min(v1.z, Math.min(v2.z, v3.z));
	}
}
