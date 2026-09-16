package com.aionemu.gameserver.geoEngine.bounding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

class BoundingBoxRayIntersectionTest {

	private final BoundingBox box = new BoundingBox(new Vector3f(0, 0, 0), 1, 1, 1);

	@Test
	void detectsForwardAndInsideRayIntersections() {
		assertTrue(box.intersects(ray(-2, 0, 0, 1, 0, 0)));
		assertTrue(box.intersects(ray(0, 0, 0, 0, 1, 0)));
		assertTrue(box.intersects(ray(-2, -2, 0, 1, 1, 0)));
	}

	@Test
	void rejectsRaysPointingAwayOrPassingOutsideTheBox() {
		assertFalse(box.intersects(ray(-2, 0, 0, -1, 0, 0)));
		assertFalse(box.intersects(ray(-2, 2, 0, 1, 0, 0)));
		assertFalse(box.intersects(ray(-2, -2, 2, 1, 1, 0)));
	}

	/**
	 * {@code clipRayRange} 必须与旧的“收集 {@link CollisionResults}”路径给出相同的 t 区间——BIH 树预剪枝依赖这一等价性
	 * （它只取最近/最远距离，不再分配结果对象）。
	 * {@code clipRayRange} must produce the same t range as the former CollisionResults path, which the BIH tree
	 * pre-cull relies on (it only needs the closest/farthest distance and no longer allocates result objects).
	 */
	@Test
	void clipRayRangeMatchesCollisionResultsPath() {
		List<Ray> rays = List.of(
				ray(0, 0, 0, 0, 1, 0), // 起点在盒内 / origin inside
				ray(-2, 0, 0, 1, 0, 0), // 正面穿盒 / straight through
				ray(-2, -2, 0, 1, 1, 0), // 斜穿 / diagonal
				ray(-2, 1, 0, 1, 0, 0), // 贴面掠过 / grazes a face
				ray(-2, 0, 0, -1, 0, 0), // 背向 / pointing away
				ray(-2, 2, 0, 1, 0, 0)); // 从外侧掠过 / passes outside

		for (Ray ray : rays) {
			CollisionResults results = new CollisionResults((byte) 0, false, 0);
			box.collideWith(ray, results);

			float[] range = new float[2];
			boolean clipped = box.clipRayRange(ray, range);

			if (results.size() > 0) {
				assertTrue(clipped, "有碰撞结果时区间必须被裁剪: " + ray);
				assertEquals(results.getClosestCollision().getDistance(), range[0], 0f);
				assertEquals(results.getFarthestCollision().getDistance(), range[1], 0f);
			} else {
				assertFalse(clipped, "无碰撞结果时不应报出裁剪后的区间: " + ray);
			}
		}
	}

	private static Ray ray(float originX, float originY, float originZ, float directionX, float directionY,
			float directionZ) {
		return new Ray(new Vector3f(originX, originY, originZ), new Vector3f(directionX, directionY, directionZ));
	}
}
