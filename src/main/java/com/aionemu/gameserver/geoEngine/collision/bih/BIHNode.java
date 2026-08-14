package com.aionemu.gameserver.geoEngine.collision.bih;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 包围区间层次（BIH）节点。基于 Carsten Wächter 与 Alexander Keller 的
 * “Instant Ray Tracing: The Bounding Interval Hierarchy”。
 * Bounding Interval Hierarchy node. Based on: Instant Ray Tracing: The Bounding
 * Interval Hierarchy By Carsten Wächter and Alexander Keller.
 */
public final class BIHNode {

	/** 叶节点三角形区间左下标。 / Leaf triangle range left index. */
	private int leftIndex, rightIndex;
	/** 左子节点。 / Left child node. */
	private BIHNode left;
	/** 右子节点。 / Right child node. */
	private BIHNode right;
	/** 左分割平面。 / Left split plane. */
	private float leftPlane;
	/** 右分割平面。 / Right split plane. */
	private float rightPlane;
	/** 分割轴；3 表示叶节点。 / Split axis; 3 marks a leaf. */
	private int axis;

	/**
	 * 构造叶节点，覆盖三角形区间 [{@code l}, {@code r}]。
	 * Constructs a leaf covering triangle range [{@code l}, {@code r}].
	 *
	 * @param l 左下标 / left index
	 * @param r 右下标 / right index
	 */
	public BIHNode(int l, int r) {
		leftIndex = l;
		rightIndex = r;
		axis = 3; // indicates leaf
	}

	/**
	 * 构造内节点，指定分割轴。
	 * Constructs an inner node with the given split axis.
	 *
	 * @param axis 分割轴 0/1/2 / split axis 0/1/2
	 */
	public BIHNode(int axis) {
		this.axis = axis;
	}

	/**
	 * 空构造。
	 * Default constructor.
	 */
	public BIHNode() {
	}

	/**
	 * 返回左子节点。
	 * Returns the left child.
	 *
	 * @return 左子节点 / left child
	 */
	public BIHNode getLeftChild() {
		return left;
	}

	/**
	 * 设置左子节点。
	 * Sets the left child.
	 *
	 * @param left 左子节点 / left child
	 */
	public void setLeftChild(BIHNode left) {
		this.left = left;
	}

	/**
	 * 返回左分割平面。
	 * Returns the left split plane.
	 *
	 * @return 左分割平面 / left plane
	 */
	public float getLeftPlane() {
		return leftPlane;
	}

	/**
	 * 设置左分割平面。
	 * Sets the left split plane.
	 *
	 * @param leftPlane 左分割平面 / left plane
	 */
	public void setLeftPlane(float leftPlane) {
		this.leftPlane = leftPlane;
	}

	/**
	 * 返回右子节点。
	 * Returns the right child.
	 *
	 * @return 右子节点 / right child
	 */
	public BIHNode getRightChild() {
		return right;
	}

	/**
	 * 设置右子节点。
	 * Sets the right child.
	 *
	 * @param right 右子节点 / right child
	 */
	public void setRightChild(BIHNode right) {
		this.right = right;
	}

	/**
	 * 返回右分割平面。
	 * Returns the right split plane.
	 *
	 * @return 右分割平面 / right plane
	 */
	public float getRightPlane() {
		return rightPlane;
	}

	/**
	 * 设置右分割平面。
	 * Sets the right split plane.
	 *
	 * @param rightPlane 右分割平面 / right plane
	 */
	public void setRightPlane(float rightPlane) {
		this.rightPlane = rightPlane;
	}

	/**
	 * BIH 遍历栈数据，保存节点与当前 t 区间。
	 * Stack entry for BIH traversal holding a node and its t-range.
	 */
	public static final class BIHStackData {

		/** 待遍历节点。 / Node to visit. */
		private final BIHNode node;
		/** Range minimum t / Range minimum t */
		private final float min, max;

		/**
		 * 构造栈数据。
		 * Constructs stack data.
		 *
		 * @param node 待遍历节点 / node
		 * @param min 最小 t / min t
		 * @param max 最大 t / max t
		 */
		BIHStackData(BIHNode node, float min, float max) {
			this.node = node;
			this.min = min;
			this.max = max;
		}
	}

	/**
	 * 以包围盒与可碰撞对象做树遍历相交测试（当前叶处理未计入命中）。
	 * Traverses the tree for intersection against a bounding box and collidable
	 * (leaf hits are currently not accumulated).
	 *
	 * @param col 可碰撞对象 / collidable
	 * @param box 包围盒 / bounding box
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public final int intersectWhere(Collidable col, BoundingBox box, Matrix4f worldMatrix, BIHTree tree,
			CollisionResults results) {

		List<BIHStackData> stack = new ArrayList<BIHStackData>();

		float[] minExts = { box.getCenter().x - box.getXExtent(), box.getCenter().y - box.getYExtent(),
				box.getCenter().z - box.getZExtent() };

		float[] maxExts = { box.getCenter().x + box.getXExtent(), box.getCenter().y + box.getYExtent(),
				box.getCenter().z + box.getZExtent() };

		stack.add(new BIHStackData(this, 0, 0));

		Triangle t = new Triangle();
		int cols = 0;

		stackloop: while (stack.size() > 0) {
			BIHNode node = stack.remove(stack.size() - 1).node;

			while (node.axis != 3) {
				int a = node.axis;

				float maxExt = maxExts[a];
				float minExt = minExts[a];

				if (node.leftPlane < node.rightPlane) {
					// 表示中间有间隙 / means there's a gap in the middle
					// 若盒子在该间隙中，则在此停止 / if the box is in that gap, we stop there
					if (minExt > node.leftPlane && maxExt < node.rightPlane) {
						continue stackloop;
					}
				}

				if (maxExt < node.rightPlane) {
					node = node.left;
				} else if (minExt > node.leftPlane) {
					node = node.right;
				} else {
					stack.add(new BIHStackData(node.right, 0, 0));
					node = node.left;
				}
			}

			for (int i = node.leftIndex; i <= node.rightIndex; i++) {
				tree.getTriangle(i, t.get1(), t.get2(), t.get3());
				if (worldMatrix != null) {
					worldMatrix.mult(t.get1(), t.get1());
					worldMatrix.mult(t.get2(), t.get2());
					worldMatrix.mult(t.get3(), t.get3());
				}

				/*
				 * Original code had this int added = col.collideWith(t, results, 1); if (added
				 * > 0) { cols += added; }
				 */
			}
		}
		return cols;
	}

	/**
	 * 暴力遍历所有叶三角形与射线求交（调试/对照用）。
	 * Brute-force traversal intersecting the ray with all leaf triangles (debug/reference).
	 *
	 * @param r 射线 / ray
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param sceneMin 场景最小 t / scene min t
	 * @param sceneMax 场景最大 t / scene max t
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public final int intersectBrute(Ray r, Matrix4f worldMatrix, BIHTree tree, float sceneMin, float sceneMax,
			CollisionResults results) {
		float tHit = Float.POSITIVE_INFINITY;

		Vector3f v1 = new Vector3f(), v2 = new Vector3f(), v3 = new Vector3f();

		int cols = 0;

		List<BIHStackData> stack = new ArrayList<BIHStackData>();
		stack.clear();
		stack.add(new BIHStackData(this, 0, 0));
		while (stack.size() > 0) {

			BIHStackData data = stack.remove(stack.size() - 1);
			BIHNode node = data.node;

			while (node.axis != 3) { // while node is not a leaf
				BIHNode nearNode, farNode;
				nearNode = node.left;
				farNode = node.right;

				stack.add(new BIHStackData(farNode, 0, 0));
				node = nearNode;
			}

			// 一片叶子 / a leaf
			for (int i = node.leftIndex; i <= node.rightIndex; i++) {
				tree.getTriangle(i, v1, v2, v3);

				if (worldMatrix != null) {
					worldMatrix.mult(v1, v1);
					worldMatrix.mult(v2, v2);
					worldMatrix.mult(v3, v3);
				}

				float t = r.intersects(v1, v2, v3);
				if (t < tHit) {
					tHit = t;
					Vector3f contactPoint = new Vector3f(r.direction).multLocal(tHit).addLocal(r.origin);
					CollisionResult cr = new CollisionResult(contactPoint, tHit);
					results.addCollision(cr);
					cols++;
				}
			}
		}
		return cols;
	}

	/**
	 * 射线与 BIH 树的精确相交测试：将射线变换到局部空间，剪枝遍历叶三角形并写回世界空间命中。
	 * Precise ray–BIH intersection: transforms the ray into local space, prunes
	 * traversal over leaf triangles and records hits in world space.
	 *
	 * @param r 射线（结束后会恢复原 origin/direction） / ray (origin/direction restored after)
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param sceneMin 场景最小 t / scene min t
	 * @param sceneMax 场景最大 t / scene max t
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public final int intersectWhere(Ray r, Matrix4f worldMatrix, BIHTree tree, float sceneMin, float sceneMax,
			CollisionResults results) {

		List<BIHStackData> stack = new ArrayList<BIHStackData>();

		// float tHit = Float.POSITIVE_INFINITY;
		Vector3f o = r.getOrigin().clone();
		Vector3f d = r.getDirection().clone();

		Matrix4f inv = worldMatrix.invert();

		inv.mult(r.getOrigin(), r.getOrigin());

		// 修复旋转碰撞缺陷 / Fixes rotation collision bug
		inv.multNormal(r.getDirection(), r.getDirection());
		// inv.multNormalAcross(r.getDirection(), r.getDirection());

		float[] origins = { r.getOrigin().x, r.getOrigin().y, r.getOrigin().z };

		float[] invDirections = { 1f / r.getDirection().x, 1f / r.getDirection().y, 1f / r.getDirection().z };

		r.getDirection().normalizeLocal();

		Vector3f v1 = new Vector3f(), v2 = new Vector3f(), v3 = new Vector3f();
		int cols = 0;

		stack.add(new BIHStackData(this, sceneMin, sceneMax));
		stackloop: while (stack.size() > 0) {

			BIHStackData data = stack.remove(stack.size() - 1);
			BIHNode node = data.node;
			float tMin = data.min, tMax = data.max;

			if (tMax < tMin) {
				continue;
			}

			while (node.axis != 3) { // while node is not a leaf
				int a = node.axis;

				// 查找给定轴的原点与方向值 / find the origin and direction value for the given axis
				float origin = origins[a];
				float invDirection = invDirections[a];

				float tNearSplit, tFarSplit;
				BIHNode nearNode, farNode;

				tNearSplit = (node.leftPlane - origin) * invDirection;
				tFarSplit = (node.rightPlane - origin) * invDirection;
				nearNode = node.left;
				farNode = node.right;

				if (invDirection < 0) {
					float tmpSplit = tNearSplit;
					tNearSplit = tFarSplit;
					tFarSplit = tmpSplit;

					BIHNode tmpNode = nearNode;
					nearNode = farNode;
					farNode = tmpNode;
				}

				if (tMin > tNearSplit && tMax < tFarSplit) {
					continue stackloop;
				}

				if (tMin > tNearSplit) {
					tMin = max(tMin, tFarSplit);
					node = farNode;
				} else if (tMax < tFarSplit) {
					tMax = min(tMax, tNearSplit);
					node = nearNode;
				} else {
					stack.add(new BIHStackData(farNode, max(tMin, tFarSplit), tMax));
					tMax = min(tMax, tNearSplit);
					node = nearNode;
				}
			}

			// 一片叶子 / a leaf
			for (int i = node.leftIndex; i <= node.rightIndex; i++) {
				tree.getTriangle(i, v1, v2, v3);

				float t = r.intersects(v1, v2, v3);
				if (!Float.isInfinite(t)) {
					if (worldMatrix != null) {
						worldMatrix.mult(v1, v1);
						worldMatrix.mult(v2, v2);
						worldMatrix.mult(v3, v3);
						float t_world = new Ray(o, d).intersects(v1, v2, v3);
						t = t_world;
					}

					Vector3f contactNormal = Triangle.computeTriangleNormal(v1, v2, v3, null);
					Vector3f contactPoint = new Vector3f(d).multLocal(t).addLocal(o);
					float worldSpaceDist = o.distance(contactPoint);
					// 修复隐形墙 / fix invisible walls
					if (worldSpaceDist > r.limit) {
						continue;
					}
					CollisionResult cr = new CollisionResult(contactPoint, worldSpaceDist);
					cr.setContactNormal(contactNormal);
					results.addCollision(cr);
					if (results.isOnlyFirst()) {
						return 1;
					}
					cols++;
				}
			}
		}

		r.setOrigin(o);
		r.setDirection(d);
		return cols;
	}
}
