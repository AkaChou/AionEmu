package com.aionemu.gameserver.geoEngine.collision.bih;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;

/**
 * {@link BIHTree} 射线遍历的行为契约：命中集合、距离顺序、`onlyFirst` 提前返回、limit 截断与射线状态还原。
 * Behaviour contract for {@link BIHTree} ray traversal: hit set, distance order, `onlyFirst` early return, limit culling
 * and ray restoration.
 * <p>用途：遍历栈（{@code BIHStackData} → 原始数组栈）这类改动会改变内部数据结构，但**不允许改变命中结果**，
 * 因此这里把当前实现的可观察结果固定下来，改造前后都必须一致。
 * Purpose: internal changes to the traversal stack must not change observable results, so this test pins them.</p>
 */
class BIHTreeRayTraversalTest {

	/**
	 * 三面互相平行的墙（x = 2/6/10），穿过它们的射线会经历多次分裂压栈。
	 * Three parallel walls (x = 2/6/10); a ray crossing them exercises several split pushes.
	 */
	private static final float[] WALLS = {
			2, -1, -1, 2, 1, -1, 2, 1, 2, 2, -1, 2,
			6, -1, -1, 6, 1, -1, 6, 1, 2, 6, -1, 2,
			10, -1, -1, 10, 1, -1, 10, 1, 2, 10, -1, 2};

	private static final int[] WALL_INDICES = {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11};

	/** 依次命中三面墙，距离按升序返回。 / Hits all three walls, ordered by distance. */
	@Test
	void collectsEveryCrossedWallInDistanceOrder() {
		CollisionResults results = new CollisionResults((byte) 0, false, 1);

		int hits = collide(ray(-1, 0, 0.5f, 1, 0, 0), 0f, results);

		// 每面墙由两个三角形组成，射线同时命中两者，因此每个距离出现两次。 / Each wall is two triangles, so every distance appears twice.
		assertEquals(6, hits);
		assertEquals(6, results.size());
		assertEquals(3f, results.getCollision(0).getDistance(), 1e-4f);
		assertEquals(3f, results.getCollision(1).getDistance(), 1e-4f);
		assertEquals(7f, results.getCollision(2).getDistance(), 1e-4f);
		assertEquals(7f, results.getCollision(3).getDistance(), 1e-4f);
		assertEquals(11f, results.getCollision(4).getDistance(), 1e-4f);
		assertEquals(11f, results.getCollision(5).getDistance(), 1e-4f);
	}

	/** `onlyFirst` 时在首个命中处提前返回，只收集一条结果。 / With onlyFirst the traversal stops at the first hit. */
	@Test
	void onlyFirstStopsAtTheNearestWall() {
		CollisionResults results = new CollisionResults((byte) 0, true, 1);

		int hits = collide(ray(-1, 0, 0.5f, 1, 0, 0), 0f, results);

		assertEquals(1, hits);
		assertEquals(1, results.size());
		assertEquals(3f, results.getClosestCollision().getDistance(), 1e-4f);
	}

	/** 射线 limit 之外的命中被丢弃。 / Hits beyond the ray limit are dropped. */
	@Test
	void respectsRayLimit() {
		CollisionResults results = new CollisionResults((byte) 0, false, 1);

		int hits = collide(ray(-1, 0, 0.5f, 1, 0, 0), 5f, results);

		assertEquals(2, hits);
		assertEquals(2, results.size());
		assertEquals(3f, results.getClosestCollision().getDistance(), 1e-4f);
		assertEquals(7f, results.getFarthestCollision().getDistance(), 4f);
	}

	/** 未命中任何墙面时无结果。 / No hits when the ray misses every wall. */
	@Test
	void missesWhenNothingIsInTheWay() {
		CollisionResults results = new CollisionResults((byte) 0, false, 1);

		int hits = collide(ray(-1, 0, 9f, 1, 0, 0), 0f, results);

		assertEquals(0, hits);
		assertEquals(0, results.size());
	}

	/** 遍历会临时改写射线的原值/方向，返回前必须还原（scratch 复用后尤其重要）。 / Traversal must restore the ray. */
	@Test
	void restoresRayOriginAndDirection() {
		Ray ray = ray(-1, 0, 0.5f, 1, 0, 0);
		Vector3f originBefore = ray.getOrigin().clone();
		Vector3f directionBefore = ray.getDirection().clone();

		collide(ray, 0f, new CollisionResults((byte) 0, false, 1));

		assertEquals(originBefore.x, ray.getOrigin().x, 1e-6f);
		assertEquals(originBefore.y, ray.getOrigin().y, 1e-6f);
		assertEquals(originBefore.z, ray.getOrigin().z, 1e-6f);
		assertEquals(directionBefore.x, ray.getDirection().x, 1e-6f);
		assertEquals(directionBefore.y, ray.getDirection().y, 1e-6f);
		assertEquals(directionBefore.z, ray.getDirection().z, 1e-6f);
	}

	/**
	 * 用当前线程的墙网格做一次射线查询。
	 * Runs one ray query against the wall mesh.
	 * @param ray 射线 / the ray
	 * @param limit 射线长度上限，0 表示不限 / ray limit, 0 means unlimited
	 * @param results 结果收集器 / results collector
	 * @return 命中数 / hit count
	 */
	private static int collide(Ray ray, float limit, CollisionResults results) {
		if (limit > 0f) {
			ray.setLimit(limit);
		}
		return tree().collideWith(ray, new Matrix4f(), bound(), results);
	}

	/**
	 * 构建墙网格对应的 BIH 树。
	 * Builds the BIH tree for the wall mesh.
	 * @return BIH 树 / the tree
	 */
	private static BIHTree tree() {
		Mesh mesh = new Mesh();
		mesh.setBuffer(Type.Position, 3, WALLS);
		mesh.setBuffer(Type.Index, 3, WALL_INDICES);
		mesh.setCollisionFlags((short) (CollisionIntention.PHYSICAL.getId() << 8));
		Geometry geometry = new Geometry("walls", mesh);
		geometry.updateModelBound();
		BIHTree tree = new BIHTree(mesh);
		tree.construct();
		return tree;
	}

	/**
	 * 覆盖三面墙的世界包围盒。
	 * World bound covering the three walls.
	 * @return 包围盒 / the bounding box
	 */
	private static BoundingBox bound() {
		return new BoundingBox(new Vector3f(6, 0, 0.5f), 5.5f, 1.5f, 2f);
	}

	/**
	 * 构造一条射线。
	 * Builds a ray.
	 * @param originX 起点 X / origin x
	 * @param originY 起点 Y / origin y
	 * @param originZ 起点 Z / origin z
	 * @param directionX 方向 X / direction x
	 * @param directionY 方向 Y / direction y
	 * @param directionZ 方向 Z / direction z
	 * @return 射线 / the ray
	 */
	private static Ray ray(float originX, float originY, float originZ, float directionX, float directionY,
			float directionZ) {
		return new Ray(new Vector3f(originX, originY, originZ), new Vector3f(directionX, directionY, directionZ));
	}
}
