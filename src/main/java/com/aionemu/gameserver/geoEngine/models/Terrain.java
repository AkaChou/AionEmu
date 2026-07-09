package com.aionemu.gameserver.geoEngine.models;

import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

public class Terrain {

	private static final int HEIGHTMAP_UNIT_SIZE = 2;
	private static final int HEIGHTMAP_MAX_Z_EXCLUSIVE = 2048;

	private int heightmapXSize, heightmapYSize;
	private short[] heightmap;
	private int materialsXSize, materialsYSize;
	private byte[] materials;

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

	public boolean hasHeightmap() {
		return heightmap != null;
	}

	public boolean hasMaterials() {
		return materials != null;
	}

	public void collideAtOrigin(Ray ray, CollisionResults results) {
		collideNearXY(ray.origin.x, ray.origin.y, ray, new Vector3f(), new Vector3f(), new Vector3f(), results);
	}

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

	private float getZ(int index) {
		return heightmap[index] == -1 ? Float.NaN : Short.toUnsignedInt(heightmap[index]) * HEIGHTMAP_MAX_Z_EXCLUSIVE / (0xFFFF + 1f);
	}

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

	private boolean isLeft(float startX, float startY, float endX, float endY, float targetX, float targetY) {
		return (endX - startX) * (targetY - startY) > (endY - startY) * (targetX - startX);
	}

	private float getMaximumZDiff(Vector3f v1, Vector3f v2, Vector3f v3) {
		return Math.max(v1.z, Math.max(v2.z, v3.z)) - Math.min(v1.z, Math.min(v2.z, v3.z));
	}
}
