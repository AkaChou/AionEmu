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
import lombok.Getter;
import lombok.Setter;

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
	/** 左分割平面。 / Left split plane.
	 * -- GETTER --
	 *  返回左分割平面。
	 *  Returns the left split plane.
	 * -- SETTER --
	 *  设置左分割平面。
	 *  Sets the left split plane.
	 */
	@Setter
	@Getter
	private float leftPlane;
	/** 右分割平面。 / Right split plane.
     * -- GETTER --
     *  返回右分割平面。
     *  Returns the right split plane.
	 * -- SETTER --
	 *  设置右分割平面。
	 *  Sets the right split plane.
     */
	@Setter
	@Getter
    private float rightPlane;
	/** 分割轴；3 表示叶节点。 / Split axis; 3 marks a leaf. */
	private int axis;

	/**
	 * 构造叶节点，覆盖三角形区间 [{@code l}, {@code r}]。
	 * Constructs a leaf covering triangle range [{@code l}, {@code r}].
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
	 * @return 左子节点 / left child
	 */
	public BIHNode getLeftChild() {
		return left;
	}

	/**
	 * 设置左子节点。
	 * Sets the left child.
	 * @param left 左子节点 / left child
	 */
	public void setLeftChild(BIHNode left) {
		this.left = left;
	}

	/**
	 * 返回右子节点。
	 * Returns the right child.
	 * @return 右子节点 / right child
	 */
	public BIHNode getRightChild() {
		return right;
	}

	/**
	 * 设置右子节点。
	 * Sets the right child.
	 * @param right 右子节点 / right child
	 */
	public void setRightChild(BIHNode right) {
		this.right = right;
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
	 * @param col 可碰撞对象 / collidable
	 * @param box 包围盒 / bounding box
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public int intersectWhere(Collidable col, BoundingBox box, Matrix4f worldMatrix, BIHTree tree,
							  CollisionResults results) {

		List<BIHStackData> stack = new ArrayList<>();

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
	 * @param r 射线 / ray
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param sceneMin 场景最小 t / scene min t
	 * @param sceneMax 场景最大 t / scene max t
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public int intersectBrute(Ray r, Matrix4f worldMatrix, BIHTree tree, float sceneMin, float sceneMax,
							  CollisionResults results) {
		float tHit = Float.POSITIVE_INFINITY;

		Vector3f v1 = new Vector3f(), v2 = new Vector3f(), v3 = new Vector3f();

		int cols = 0;

		List<BIHStackData> stack = new ArrayList<>();
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
	 * @param r 射线（结束后会恢复原 origin/direction） / ray (origin/direction restored after)
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param tree 所属 BIH 树 / owning BIH tree
	 * @param sceneMin 场景最小 t / scene min t
	 * @param sceneMax 场景最大 t / scene max t
	 * @param results 结果收集器 / collision results
	 * @return 命中数 / hit count
	 */
	public int intersectWhere(Ray r, Matrix4f worldMatrix, BIHTree tree, float sceneMin, float sceneMax,
							  CollisionResults results) {

		// 该遍历不再为每次分裂新建栈条目：改用每线程复用的数组栈。 / No per-split stack entries: reuse a per-thread array stack.
		RayTraversalStack stack = RAY_TRAVERSAL_STACK.get();
		stack.size = 0;

		// float tHit = Float.POSITIVE_INFINITY;
		// 临时向量改用每线程固定实例：既不再走对象池（池空时会新建，play-7 有 28.6MB 该站点的分配），
		// 也不再需要回收；Ray#setOrigin/setDirection 是拷贝，循环内的临时 Ray 用完即弃。
		// Scratch vectors now live in fixed per-thread instances: no pool interaction (which allocated when the
		// pool ran dry, 28.6MB in play-7) and no recycle step; Ray#setOrigin/setDirection copy and the in-loop
		// temporary Ray is discarded.
		RayScratch scratch = RAY_SCRATCH.get();
		Vector3f o = scratch.origin.set(r.getOrigin());
		Vector3f d = scratch.direction.set(r.getDirection());

		// 逆矩阵写入线程本地 scratch，避免每次查询分配 Matrix4f 与其内部数组。 / Invert into a per-thread scratch matrix instead of allocating one per query.
		Matrix4f inv = INVERSE_MATRIX.get();
		worldMatrix.invert(inv);

		inv.mult(r.getOrigin(), r.getOrigin());

		// 修复旋转碰撞缺陷 / Fixes rotation collision bug
		inv.multNormal(r.getDirection(), r.getDirection());
		// inv.multNormalAcross(r.getDirection(), r.getDirection());

		// 轴原点与方向倒数改为复用每线程数组：原实现每次遍历分配两个 float[3]
		// （play-12 该站点 9.8MB/300s 的 float[]）。
		// Axis origins and inverse directions now reuse per-thread arrays: the old code allocated two float[3]
		// per traversal (9.8MB/300s of float[] at this site in play-12).
		float[] origins = scratch.origins;
		origins[0] = r.getOrigin().x;
		origins[1] = r.getOrigin().y;
		origins[2] = r.getOrigin().z;

		float[] invDirections = scratch.invDirections;
		invDirections[0] = 1f / r.getDirection().x;
		invDirections[1] = 1f / r.getDirection().y;
		invDirections[2] = 1f / r.getDirection().z;

		r.getDirection().normalizeLocal();

		Vector3f v1 = scratch.v1, v2 = scratch.v2, v3 = scratch.v3;
		int cols = 0;

		pushRayStack(stack, this, sceneMin, sceneMax);
		stackloop: while (stack.size > 0) {

			int stackIndex = --stack.size;
			BIHNode node = stack.nodes[stackIndex];
			float tMin = stack.minTs[stackIndex], tMax = stack.maxTs[stackIndex];

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
					pushRayStack(stack, farNode, max(tMin, tFarSplit), tMax);
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

	/**
	 * 射线遍历用的数组栈（每线程一份，避免每次查询分配栈条目与列表扩容）。
	 * Array stack for ray traversal (one per thread; no per-entry allocation or list growth).
	 * <p>容量不足时翻倍扩容，因此不依赖“栈深必有硬上限”的假设。
	 * Doubles its capacity when full, so it does not rely on a hard depth bound.</p>
	 */
	private static final class RayTraversalStack {
		/** 初始容量。 / Initial capacity. */
		private BIHNode[] nodes = new BIHNode[32];
		private float[] minTs = new float[32];
		private float[] maxTs = new float[32];
		private int size;
	}

	/** 每线程的逆矩阵 scratch（只在本方法内使用，不逃逸）。 / Per-thread inverse-matrix scratch (local use only). */
	private static final ThreadLocal<Matrix4f> INVERSE_MATRIX = ThreadLocal.withInitial(Matrix4f::new);

	/**
	 * 射线查询用的固定 scratch（射线原值/方向与三角形顶点）。
	 * Fixed per-thread scratch for ray queries (saved ray origin/direction and triangle vertices).
	 */
	private static final class RayScratch {
		private final Vector3f origin = new Vector3f();
		private final Vector3f direction = new Vector3f();
		private final Vector3f v1 = new Vector3f();
		private final Vector3f v2 = new Vector3f();
		private final Vector3f v3 = new Vector3f();
		private final float[] origins = new float[3];
		private final float[] invDirections = new float[3];
	}

	/** 每线程的射线查询 scratch。 / Per-thread ray-query scratch. */
	private static final ThreadLocal<RayScratch> RAY_SCRATCH = ThreadLocal.withInitial(RayScratch::new);

	/** 每线程的射线遍历栈。 / Per-thread ray traversal stack. */
	private static final ThreadLocal<RayTraversalStack> RAY_TRAVERSAL_STACK =
			ThreadLocal.withInitial(RayTraversalStack::new);

	/**
	 * 压入数组栈，必要时翻倍扩容。
	 * Pushes onto the array stack, doubling its capacity when needed.
	 * @param stack 目标栈 / target stack
	 * @param node 待遍历节点 / node to visit
	 * @param minT 区间下限 / lower t bound
	 * @param maxT 区间上限 / upper t bound
	 */
	private static void pushRayStack(RayTraversalStack stack, BIHNode node, float minT, float maxT) {
		if (stack.size == stack.nodes.length) {
			int grown = stack.size << 1;
			stack.nodes = java.util.Arrays.copyOf(stack.nodes, grown);
			stack.minTs = java.util.Arrays.copyOf(stack.minTs, grown);
			stack.maxTs = java.util.Arrays.copyOf(stack.maxTs, grown);
		}
		stack.nodes[stack.size] = node;
		stack.minTs[stack.size] = minT;
		stack.maxTs[stack.size] = maxT;
		stack.size++;
	}
}
