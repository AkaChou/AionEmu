package com.aionemu.gameserver.geoEngine.collision.bih;

import static java.lang.Math.max;

import java.nio.FloatBuffer;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.UnsupportedCollisionException;
import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.CollisionData;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;
import com.aionemu.gameserver.geoEngine.scene.mesh.IndexBuffer;

/**
 * 包围区间层次（BIH）加速结构，实现 {@link CollisionData}，用于网格与射线/包围体的快速碰撞检测。
 * Bounding Interval Hierarchy acceleration structure implementing {@link CollisionData}
 * for fast mesh collisions against rays and bounding volumes.
 */
public class BIHTree implements CollisionData {

	/** 最大树深度。 / Maximum tree depth. */
	public static final int MAX_TREE_DEPTH = 100;
	/** 每节点最大三角形数。 / Maximum triangles per node. */
	public static final int MAX_TRIS_PER_NODE = 21;
	/** 根节点。 / Root node. */
	private BIHNode root;
	/** 每节点最大三角形数配置。 / Configured max triangles per node. */
	private int maxTrisPerNode;
	/** 三角形总数。 / Total triangle count. */
	private int numTris;
	/** 展平顶点数据（每三角 9 个 float） / Flattened vertex data (9 floats per triangle) */
	private float[] pointData;
	/** 三角形原始下标映射。 / Original triangle index mapping. */
	private int[] triIndices;
	/** 交换三角形时的临时缓冲。 / Temporary buffer for triangle swaps. */
	private transient float[] bihSwapTmp;
	/** 按轴的三角形比较器。 / Per-axis triangle comparators. */
	private static final TriangleAxisComparator[] comparators = new TriangleAxisComparator[3];

	static {
		comparators[0] = new TriangleAxisComparator(0);
		comparators[1] = new TriangleAxisComparator(1);
		comparators[2] = new TriangleAxisComparator(2);
	}

	/**
	 * 从顶点/索引缓冲初始化展平三角形列表与下标映射。
	 * Initializes the flattened triangle list and index mapping from vertex/index buffers.
	 *
	 * @param vb 顶点缓冲 / vertex buffer
	 * @param ib 索引缓冲 / index buffer
	 */
	private void initTriList(FloatBuffer vb, IndexBuffer ib) {
		pointData = new float[numTris * 3 * 3];
		int p = 0;
		for (int i = 0; i < numTris * 3; i += 3) {
			int vert = ib.get(i) * 3;
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert);

			vert = ib.get(i + 1) * 3;
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert);

			vert = ib.get(i + 2) * 3;
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert++);
			pointData[p++] = vb.get(vert);
		}

		triIndices = new int[numTris];
		for (int i = 0; i < numTris; i++) {
			triIndices[i] = i;
		}
	}

	/**
	 * 以网格与每节点最大三角形数构造 BIH 树（尚未建树，需调用 {@link #construct()}）。
	 * Constructs a BIH tree from a mesh and max-tris-per-node (call {@link #construct()} to build).
	 *
	 * mesh
	 * @param maxTrisPerNode 每节点最大三角形数 / max triangles per node
	 */
	public BIHTree(Mesh mesh, int maxTrisPerNode) {
		this.maxTrisPerNode = maxTrisPerNode;

		if (maxTrisPerNode < 1 || mesh == null) {
			throw new IllegalArgumentException();
		}

		bihSwapTmp = new float[9];

		FloatBuffer vb = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
		IndexBuffer ib = mesh.getIndexBuffer();

		numTris = ib.size() / 3;
		initTriList(vb, ib);
	}

	/**
	 * 以网格与默认每节点最大三角形数构造。
	 * Constructs from a mesh with the default max triangles per node.
	 *
	 * mesh
	 */
	public BIHTree(Mesh mesh) {
		this(mesh, MAX_TRIS_PER_NODE);
	}

	/**
	 * 空构造。
	 * Default constructor.
	 */
	public BIHTree() {
	}

	/**
	 * 构建 BIH 树根节点。
	 * Builds the BIH tree root.
	 */
	public void construct() {
		BoundingBox sceneBbox = createBox(0, numTris - 1);
		root = createNode(0, numTris - 1, sceneBbox, 0);
	}

	/**
	 * 为三角形区间 [{@code l}, {@code r}] 创建轴对齐包围盒。
	 * Creates an AABB for triangle range [{@code l}, {@code r}].
	 *
	 * @param l 左下标 / left index
	 * @param r 右下标 / right index
	 * bounding box
	 */
	private BoundingBox createBox(int l, int r) {
		Vector3f min = Vector3f.newInstance();
		Vector3f max = Vector3f.newInstance();
		min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Vector3f v1 = Vector3f.newInstance();
		Vector3f v2 = Vector3f.newInstance();
		Vector3f v3 = Vector3f.newInstance();

		for (int i = l; i <= r; i++) {
			getTriangle(i, v1, v2, v3);
			BoundingBox.checkMinMax(min, max, v1);
			BoundingBox.checkMinMax(min, max, v2);
			BoundingBox.checkMinMax(min, max, v3);
		}

		BoundingBox bbox = new BoundingBox(min, max);
		Vector3f.recycle(min);
		Vector3f.recycle(max);
		Vector3f.recycle(v1);
		Vector3f.recycle(v2);
		Vector3f.recycle(v3);
		return bbox;
	}

	/**
	 * 返回给定树内下标对应的原始三角形下标。
	 * Returns the original triangle index for the given in-tree index.
	 *
	 * in-tree index
	 * original index
	 */
	int getTriangleIndex(int triIndex) {
		return triIndices[triIndex];
	}

	/**
	 * 按分割值将区间内三角形划分为左右两侧，返回枢轴。
	 * Partitions triangles in range by split value along an axis; returns the pivot.
	 *
	 * @param l 左下标 / left index
	 * @param r 右下标 / right index
	 * split value
	 * axis
	 * pivot index
	 */
	private int sortTriangles(int l, int r, float split, int axis) {
		int pivot = l;
		int j = r;

		Vector3f v1 = Vector3f.newInstance(), v2 = Vector3f.newInstance(), v3 = Vector3f.newInstance();

		while (pivot <= j) {
			getTriangle(pivot, v1, v2, v3);
			v1.addLocal(v2).addLocal(v3).multLocal(FastMath.ONE_THIRD);
			if (v1.get(axis) > split) {
				swapTriangles(pivot, j);
				--j;
			} else {
				++pivot;
			}
		}

		Vector3f.recycle(v1);
		Vector3f.recycle(v2);
		Vector3f.recycle(v3);
		pivot = (pivot == l && j < pivot) ? j : pivot;
		return pivot;
	}

	/**
	 * 设置包围盒在指定轴上的最小或最大边界。
	 * Sets the min or max bound of a bounding box on the given axis.
	 *
	 * bounding box
	 * @param doMin {@code true} 设置最小，否则最大 / {@code true} for min, else max
	 * axis
	 * bound value
	 */
	private void setMinMax(BoundingBox bbox, boolean doMin, int axis, float value) {
		Vector3f min = bbox.getMin(null);
		Vector3f max = bbox.getMax(null);

		if (doMin) {
			min.set(axis, value);
		} else {
			max.set(axis, value);
		}

		bbox.setMinMax(min, max);
	}

	/**
	 * 读取包围盒在指定轴上的最小或最大边界。
	 * Reads the min or max bound of a bounding box on the given axis.
	 *
	 * bounding box
	 * @param doMin {@code true} 读最小，否则最大 / {@code true} for min, else max
	 * axis
	 * bound value
	 */
	private float getMinMax(BoundingBox bbox, boolean doMin, int axis) {
		if (doMin) {
			return bbox.getMin(null).get(axis);
		} else {
			return bbox.getMax(null).get(axis);
		}
	}

	// private BIHNode createNode2(int l, int r, BoundingBox nodeBbox, int depth){
	// if ((r - l) < maxTrisPerNode || depth > 100)
	// return createLeaf(l, r);
	//
	// BoundingBox currentBox = createBox(l, r);
	// int axis = depth % 3;
	// float split = currentBox.getCenter().get(axis);
	//
	// TriangleAxisComparator comparator = comparators[axis];
	// Arrays.sort(tris, l, r, comparator);
	// int splitIndex = -1;
	//
	// float leftPlane, rightPlane = Float.POSITIVE_INFINITY;
	// leftPlane = tris[l].getExtreme(axis, false);
	// for (int i = l; i <= r; i++){
	// BIHTriangle tri = tris[i];
	// if (splitIndex == -1){
	// float v = tri.getCenter().get(axis);
	// if (v > split){
	// if (i == 0){
	// 无左平面。 / no left plane
	// splitIndex = -2;
	// }else{
	// splitIndex = i;
	// 首个三角形分配到右侧。 / first triangle assigned to right
	// rightPlane = tri.getExtreme(axis, true);
	// }
	// }else{
	// 三角形分配到左侧。 / triangle assigned to left
	// float ex = tri.getExtreme(axis, false);
	// if (ex > leftPlane)
	// leftPlane = ex;
	// }
	// }else{
	// float ex = tri.getExtreme(axis, true);
	// if (ex < rightPlane)
	// rightPlane = ex;
	// }
	// }
	//
	// if (splitIndex < 0){
	// 2;
	//
	// leftPlane = Float.NEGATIVE_INFINITY;
	// rightPlane = Float.POSITIVE_INFINITY;
	//
	// for (int i = l; i < splitIndex; i++){
	// float ex = tris[i].getExtreme(axis, false);
	// if (ex > leftPlane){
	// leftPlane = ex;
	// }
	// }
	// for (int i = splitIndex; i <= r; i++){
	// float ex = tris[i].getExtreme(axis, true);
	// if (ex < rightPlane){
	// rightPlane = ex;
	// }
	// }
	// }
	//
	// BIHNode node = new BIHNode(axis);
	// node.leftPlane = leftPlane;
	// node.rightPlane = rightPlane;
	//
	// node.leftIndex = l;
	// node.rightIndex = r;
	//
	// BoundingBox leftBbox = new BoundingBox(currentBox);
	// setMinMax(leftBbox, false, axis, split);
	// node.left = createNode2(l, splitIndex-1, leftBbox, depth+1);
	//
	// BoundingBox rightBbox = new BoundingBox(currentBox);
	// setMinMax(rightBbox, true, axis, split);
	// node.right = createNode2(splitIndex, r, rightBbox, depth+1);
	//
	// return node;
	// }

	/**
	 * 递归创建 BIH 节点：按外延差选轴、按中心分割三角形并构建左右子树。
	 * Recursively creates a BIH node: picks axis by exterior extent, partitions
	 * triangles by center and builds left/right children.
	 *
	 * @param l 左下标 / left index
	 * @param r 右下标 / right index
	 * @param nodeBbox 节点包围盒 / node bounding box
	 * @param depth 当前深度 / current depth
	 * new node
	 */
	private BIHNode createNode(int l, int r, BoundingBox nodeBbox, int depth) {
		if ((r - l) < maxTrisPerNode || depth > MAX_TREE_DEPTH) {
			return new BIHNode(l, r);
		}

		BoundingBox currentBox = createBox(l, r);

		Vector3f exteriorExt = nodeBbox.getExtent(null);
		Vector3f interiorExt = currentBox.getExtent(null);
		exteriorExt.subtractLocal(interiorExt);

		int axis = 0;
		if (exteriorExt.x > exteriorExt.y) {
			if (exteriorExt.x > exteriorExt.z) {
				axis = 0;
			} else {
				axis = 2;
			}
		} else {
			if (exteriorExt.y > exteriorExt.z) {
				axis = 1;
			} else {
				axis = 2;
			}
		}
		if (exteriorExt.equals(Vector3f.ZERO)) {
			axis = 0;
		}

		// Arrays.sort(tris, l, r, comparators[axis]);
		float split = currentBox.getCenter().get(axis);
		int pivot = sortTriangles(l, r, split, axis);
		if (pivot == l || pivot == r) {
			pivot = (r + l) / 2;
		}

		// 若某一分区为空，继续递归：同级但 / If one of the partitions is empty, continue with recursion: same level but
		// 不同包围盒 / different bbox
		if (pivot < l) {
			// 仅右 / Only right
			BoundingBox rbbox = new BoundingBox(currentBox);
			setMinMax(rbbox, true, axis, split);
			return createNode(l, r, rbbox, depth + 1);
		} else if (pivot > r) {
			// 仅左 / Only left
			BoundingBox lbbox = new BoundingBox(currentBox);
			setMinMax(lbbox, false, axis, split);
			return createNode(l, r, lbbox, depth + 1);
		} else {
			// 构建节点 / Build the node
			BIHNode node = new BIHNode(axis);

			// 左子节点 / Left child
			BoundingBox lbbox = new BoundingBox(currentBox);
			setMinMax(lbbox, false, axis, split);

			// 左节点右边界是最右侧平面 / The left node right border is the plane most right
			node.setLeftPlane(getMinMax(createBox(l, max(l, pivot - 1)), false, axis));
			node.setLeftChild(createNode(l, max(l, pivot - 1), lbbox, depth + 1)); // Recursive call

			// 右子节点 / Right Child
			BoundingBox rbbox = new BoundingBox(currentBox);
			setMinMax(rbbox, true, axis, split);
			// 右节点左边界是最左侧平面 / The right node left border is the plane most left
			node.setRightPlane(getMinMax(createBox(pivot, r), true, axis));
			node.setRightChild(createNode(pivot, r, rbbox, depth + 1)); // Recursive call

			return node;
		}
	}

	/**
	 * 将树内下标对应的三角形三顶点写入输出向量。
	 * Writes the three vertices of the triangle at the given in-tree index into the outputs.
	 *
	 * @param index 树内下标 / in-tree index
	 * @param v1 顶点 1 输出 / vertex 1 output
	 * @param v2 顶点 2 输出 / vertex 2 output
	 * @param v3 顶点 3 输出 / vertex 3 output
	 */
	public void getTriangle(int index, Vector3f v1, Vector3f v2, Vector3f v3) {
		int pointIndex = index * 9;

		v1.x = pointData[pointIndex++];
		v1.y = pointData[pointIndex++];
		v1.z = pointData[pointIndex++];

		v2.x = pointData[pointIndex++];
		v2.y = pointData[pointIndex++];
		v2.z = pointData[pointIndex++];

		v3.x = pointData[pointIndex++];
		v3.y = pointData[pointIndex++];
		v3.z = pointData[pointIndex++];
	}

	/**
	 * 交换两个树内下标的三角形顶点数据与原始下标映射。
	 * Swaps triangle vertex data and original-index mapping for two in-tree indices.
	 *
	 * index 1
	 * index 2
	 */
	public void swapTriangles(int index1, int index2) {
		int p1 = index1 * 9;
		int p2 = index2 * 9;

		// 将 p1 存入 tmp / store p1 in tmp
		System.arraycopy(pointData, p1, bihSwapTmp, 0, 9);

		// 将 p2 复制到 p1 / copy p2 to p1
		System.arraycopy(pointData, p2, pointData, p1, 9);

		// 将 tmp 复制到 p2 / copy tmp to p2
		System.arraycopy(bihSwapTmp, 0, pointData, p2, 9);

		// 交换索引 / swap indices
		int tmp2 = triIndices[index1];
		triIndices[index1] = triIndices[index2];
		triIndices[index2] = tmp2;
	}

	/**
	 * 与射线做碰撞：先测世界包围体，再以 t 区间查询根节点。
	 * Collides with a ray: tests the world bound first, then queries the root with a t-range.
	 *
	 * @param r 射线 / ray
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param worldBound 世界包围体 / world bound
	 * @param results 结果收集器 / collision results
	 * hit count
	 */
	private int collideWithRay(Ray r, Matrix4f worldMatrix, BoundingVolume worldBound, CollisionResults results) {

		CollisionResults boundResults = new CollisionResults(results.getIntentions(), results.isOnlyFirst(),
				results.getInstanceId(), results.getIgnoreProperties());
		worldBound.collideWith(r, boundResults);
		if (boundResults.size() > 0 || worldBound.contains(r.getOrigin())) {
			float tMin = 0;
			float tMax = r.getLimit();
			if (boundResults.size() > 0) {
				tMin = boundResults.getClosestCollision().getDistance();
				tMax = boundResults.getFarthestCollision().getDistance();

				if (tMax <= 0) {
					tMax = Float.POSITIVE_INFINITY;
				} else if (tMin == tMax) {
					tMin = 0;
				}

				if (tMin <= 0) {
					tMin = 0;
				}

				if (r.getLimit() < Float.POSITIVE_INFINITY) {
					tMax = Math.min(tMax, r.getLimit());
				}
			}
			// return root.intersectBrute(r, worldMatrix, this, tMin, tMax, results);
			return root.intersectWhere(r, worldMatrix, this, tMin, tMax, results);
		}
		return 0;
	}

	/**
	 * 与包围体做碰撞（目前仅支持 {@link BoundingBox}）。
	 * Collides with a bounding volume (currently {@link BoundingBox} only).
	 *
	 * @param bv 包围体 / bounding volume
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param results 结果收集器 / collision results
	 * hit count
	 * not a BoundingBox
	 */
	private int collideWithBoundingVolume(BoundingVolume bv, Matrix4f worldMatrix, CollisionResults results) {
		BoundingBox bbox;
		if (bv instanceof BoundingBox) {
			bbox = new BoundingBox((BoundingBox) bv);
		} else {
			throw new UnsupportedCollisionException();
		}

		bbox.transform(worldMatrix.invert(), bbox);
		return root.intersectWhere(bv, bbox, worldMatrix, this, results);
	}

	/**
	 * 与射线或包围体做碰撞检测。
	 * Performs collision against a ray or bounding volume.
	 *
	 * @param other 另一可碰撞对象 / other collidable
	 * @param worldMatrix 世界变换矩阵 / world matrix
	 * @param worldBound 世界包围体 / world bound
	 * @param results 结果收集器 / collision results
	 * hit count
	 * unsupported type。 / unsupported type.
	 */
	@Override
	public int collideWith(Collidable other, Matrix4f worldMatrix, BoundingVolume worldBound,
			CollisionResults results) {

		if (other instanceof Ray) {
			Ray ray = (Ray) other;
			return collideWithRay(ray, worldMatrix, worldBound, results);
		} else if (other instanceof BoundingVolume) {
			BoundingVolume bv = (BoundingVolume) other;
			return collideWithBoundingVolume(bv, worldMatrix, results);
		} else {
			throw new UnsupportedCollisionException();
		}
	}
}
