package com.aionemu.gameserver.geoEngine.bounding;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Plane;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 包围体与三角形相交检测的工具类。
 * Utility methods for computing intersection between bounding volumes and triangles.
 *
 * @author Kirill
 */
public class Intersection {

	/**
	 * 在给定轴上求三点最小/最大值，结果写入 minMax（x=min, y=max）。
	 * Finds min/max of three values on one axis; stores min in x and max in y of minMax.
	 *
	 * @param x0 第一分量 / first component
	 * @param x1 第二分量 / second component
	 * @param x2 第三分量 / third component
	 * @param minMax 输出向量 / output vector
	 */
	private static final void findMinMax(float x0, float x1, float x2, Vector3f minMax) {
		minMax.set(x0, x0, 0);
		if (x1 < minMax.x) {
			minMax.setX(x1);
		}
		if (x1 > minMax.y) {
			minMax.setY(x1);
		}
		if (x2 < minMax.x) {
			minMax.setX(x2);
		}
		if (x2 > minMax.y) {
			minMax.setY(x2);
		}
	}

	// private boolean axisTest(float a, float b, float fa, float fb, Vector3f v0,
	// Vector3f v1, )
	// private boolean axisTestX01(float a, float b, float fa, float fb,
	// Vector3f center, Vector3f ext,
	// Vector3f v1, Vector3f v2, Vector3f v3){
	// float p0 = a * v0.y - b * v0.z;
	// float p2 = a * v2.y - b * v2.z;
	// if(p0 < p2){
	// min = p0;
	// max = p2;
	// } else {
	// min = p2;
	// max = p0;
	// }
	// float rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
	// if(min > rad || max < -rad)
	// return false;
	// }

	/**
	 * 使用分离轴定理判断 AABB 与三角形是否相交。
	 * Tests AABB-triangle overlap using the separating axis theorem.
	 * <p>
	 * 检测方向：三角形边与坐标轴的叉积（9 次）、三角形 AABB 轴（3 次）、三角形法线。
	 * Tests: 9 cross-products of tri edges with axes, 3 AABB axes, and the triangle plane.
	 *
	 * @param bbox 轴对齐包围盒 / axis-aligned bounding box
	 * @param v1 三角形顶点 1 / triangle vertex 1
	 * @param v2 三角形顶点 2 / triangle vertex 2
	 * @param v3 三角形顶点 3 / triangle vertex 3
	 * @return 若 box and triangle overlap 则为 true / true if box and triangle overlap
	 */
	public static final boolean intersect(BoundingBox bbox, Vector3f v1, Vector3f v2, Vector3f v3) {
		// 用分离轴定理检测三角形与盒子是否重叠 / use separating axis theorem to test overlap between triangle and box
		// 需在这些方向测试重叠： / need to test for overlap in these directions:
		// 1) {x,y,z} 方向（实际上因使用三角形的 AABB / 1) the {x,y,z}-directions (actually, since we use the AABB of the triangle
		// 我们甚至不需要测试这些） / we do not even need to test these)
		// 2) 三角形法线 / 2) normal of the triangle
		// 3) 叉积（三角形边，{x,y,z} 方向） / 3) crossproduct(edge from tri, {x,y,z}-directin)
		// 这给出 3x3=9 项额外测试 / this gives 3x3=9 more tests

		Vector3f tmp0 = new Vector3f(), tmp1 = new Vector3f(), tmp2 = new Vector3f();

		Vector3f e0 = new Vector3f(), e1 = new Vector3f(), e2 = new Vector3f();

		Vector3f center = bbox.getCenter();
		Vector3f extent = bbox.getExtent(null);

		// float min,max,p0,p1,p2,rad,fex,fey,fez;
		// float normal[3]
		// 这是 Sun 上最快的分支 / This is the fastest branch on Sun
		// 移动使盒子中心位于 (0,0,0) / move everything so that the boxcenter is in (0,0,0)
		v1.subtract(center, tmp0);
		v2.subtract(center, tmp1);
		v3.subtract(center, tmp2);

		// 计算三角形边 / compute triangle edges
		tmp1.subtract(tmp0, e0); // tri edge 0
		tmp2.subtract(tmp1, e1); // tri edge 1
		tmp0.subtract(tmp2, e2); // tri edge 2

		// 子弹 3： / Bullet 3:
		// 先做 9 项测试（这样更快） / test the 9 tests first (this was faster)
		float min, max;
		float p0, p1, p2, rad;
		float fex = FastMath.abs(e0.x);
		float fey = FastMath.abs(e0.y);
		float fez = FastMath.abs(e0.z);

		// AXISTEST_X01(e0[Z], e0[Y], fez, fey);
		p0 = e0.z * tmp0.y - e0.y * tmp0.z;
		p2 = e0.z * tmp2.y - e0.y * tmp2.z;
		min = min(p0, p2);
		max = max(p0, p2);
		rad = fez * extent.y + fey * extent.z;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Y02(e0[Z], e0[X], fez, fex);
		p0 = -e0.z * tmp0.x + e0.x * tmp0.z;
		p2 = -e0.z * tmp2.x + e0.x * tmp2.z;
		min = min(p0, p2);
		max = max(p0, p2);
		rad = fez * extent.x + fex * extent.z;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Z12(e0[Y], e0[X], fey, fex);
		p1 = e0.y * tmp1.x - e0.x * tmp1.y;
		p2 = e0.y * tmp2.x - e0.x * tmp2.y;
		min = min(p1, p2);
		max = max(p1, p2);
		rad = fey * extent.x + fex * extent.y;
		if (min > rad || max < -rad) {
			return false;
		}

		fex = FastMath.abs(e1.x);
		fey = FastMath.abs(e1.y);
		fez = FastMath.abs(e1.z);

		// AXISTEST_X01(e1[Z], e1[Y], fez, fey);
		p0 = e1.z * tmp0.y - e1.y * tmp0.z;
		p2 = e1.z * tmp2.y - e1.y * tmp2.z;
		min = min(p0, p2);
		max = max(p0, p2);
		rad = fez * extent.y + fey * extent.z;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Y02(e1[Z], e1[X], fez, fex);
		p0 = -e1.z * tmp0.x + e1.x * tmp0.z;
		p2 = -e1.z * tmp2.x + e1.x * tmp2.z;
		min = min(p0, p2);
		max = max(p0, p2);
		rad = fez * extent.x + fex * extent.z;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Z0(e1[Y], e1[X], fey, fex);
		p0 = e1.y * tmp0.x - e1.x * tmp0.y;
		p1 = e1.y * tmp1.x - e1.x * tmp1.y;
		min = min(p0, p1);
		max = max(p0, p1);
		rad = fey * extent.x + fex * extent.y;
		if (min > rad || max < -rad) {
			return false;
		}
		//
		fex = FastMath.abs(e2.x);
		fey = FastMath.abs(e2.y);
		fez = FastMath.abs(e2.z);

		// AXISTEST_X2(e2[Z], e2[Y], fez, fey);
		p0 = e2.z * tmp0.y - e2.y * tmp0.z;
		p1 = e2.z * tmp1.y - e2.y * tmp1.z;
		min = min(p0, p1);
		max = max(p0, p1);
		rad = fez * extent.y + fey * extent.z;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Y1(e2[Z], e2[X], fez, fex);
		p0 = -e2.z * tmp0.x + e2.x * tmp0.z;
		p1 = -e2.z * tmp1.x + e2.x * tmp1.z;
		min = min(p0, p1);
		max = max(p0, p1);
		rad = fez * extent.x + fex * extent.y;
		if (min > rad || max < -rad) {
			return false;
		}

		// AXISTEST_Z12(e2[Y], e2[X], fey, fex);
		p1 = e2.y * tmp1.x - e2.x * tmp1.y;
		p2 = e2.y * tmp2.x - e2.x * tmp2.y;
		min = min(p1, p2);
		max = max(p1, p2);
		rad = fey * extent.x + fex * extent.y;
		if (min > rad || max < -rad) {
			return false;
		}

		// 子弹 1： / Bullet 1:
		// 首先在 {x,y,z} 方向测试重叠 / first test overlap in the {x,y,z}-directions
		// 找三角形各方向 min/max，并测试重叠。 / find min, max of the triangle each direction, and test for overlap in
		// 该方向——等价于测试周围最小 AABB。 / that direction -- this is equivalent to testing a minimal AABB around
		// 三角形对 AABB / the triangle against the AABB
		Vector3f minMax = new Vector3f();

		// 在 X 方向测试 / test in X-direction
		findMinMax(tmp0.x, tmp1.x, tmp2.x, minMax);
		if (minMax.x > extent.x || minMax.y < -extent.x) {
			return false;
		}

		// 在 Y 方向测试 / test in Y-direction
		findMinMax(tmp0.y, tmp1.y, tmp2.y, minMax);
		if (minMax.x > extent.y || minMax.y < -extent.y) {
			return false;
		}

		// 在 Z 方向测试 / test in Z-direction
		findMinMax(tmp0.z, tmp1.z, tmp2.z, minMax);
		if (minMax.x > extent.z || minMax.y < -extent.z) {
			return false;
		}

		// Bullet 2:
		// 测试盒子是否与三角形平面相交。 / test if the box intersects the plane of the triangle
		// 计算三角形平面方程：normal * x + d = 0。 / compute plane equation of triangle: normal * x + d = 0
		// Vector3f normal = new Vector3f();
		// e0.cross(e1, normal);
		Plane p = new Plane();
		p.setPlanePoints(v1, v2, v3);
		if (bbox.whichSide(p) == Plane.Side.Negative) {
			return false;
		}
		//
		// if(!planeBoxOverlap(normal,v0,boxhalfsize)) return false;
		return true; /* box and triangle overlaps */

	}
}
