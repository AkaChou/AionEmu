package com.aionemu.gameserver.geoEngine.scene;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.bih.BIHTree;
import com.aionemu.gameserver.geoEngine.math.Matrix4f;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Format;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Usage;
import com.aionemu.gameserver.geoEngine.scene.mesh.IndexBuffer;
import com.aionemu.gameserver.geoEngine.scene.mesh.IndexByteBuffer;
import com.aionemu.gameserver.geoEngine.scene.mesh.IndexIntBuffer;
import com.aionemu.gameserver.geoEngine.scene.mesh.IndexShortBuffer;
import com.aionemu.gameserver.geoEngine.utils.BufferUtils;
import com.aionemu.gameserver.geoEngine.utils.IntMap;
import com.aionemu.gameserver.geoEngine.utils.IntMap.Entry;

/**
 * 网格：管理顶点/索引缓冲、图元模式、包围体与 BIH 碰撞树。
 * Mesh that manages vertex/index buffers, primitive mode, bounds and a BIH collision tree.
 */
public class Mesh {

	/**
	 * 图元绘制模式。
	 * Primitive draw mode.
	 */
	public enum Mode {

		/** 点列表。 / Point list. */
		Points,
		/** 线段列表。 / Line segment list. */
		Lines,
		/** 线环（首尾相连）。 / Closed line loop. */
		LineLoop,
		/** 连续折线。 / Connected line strip. */
		LineStrip,
		/** 独立三角形列表。 / Independent triangle list. */
		Triangles,
		/** 三角形带。 / Triangle strip. */
		TriangleStrip,
		/** 三角形扇。 / Triangle fan. */
		TriangleFan,
		/** 混合模式（多段不同图元）。 / Hybrid mode with mixed primitive segments. */
		Hybrid
	}

	// private static final int BUFFERS_SIZE = VertexBuffer.Type.BoneIndex.ordinal()
	// + 1;
	/**
	 * 完全包含网格的包围体，默认 AABB。
	 * Bounding volume that contains the mesh entirely. By default a BoundingBox (AABB).
	 */
	private BoundingVolume meshBound = new BoundingBox();
	/** Collision acceleration structure (BIH tree) / Collision acceleration structure (BIH tree) */
	private CollisionData collisionTree = null;
	// private EnumMap<VertexBuffer.Type, VertexBuffer> buffers = new EnumMap<Type,
	// VertexBuffer>(VertexBuffer.Type.class);
	// private VertexBuffer[] buffers = new VertexBuffer[BUFFERS_SIZE];
	/** 按类型序数索引的顶点缓冲映射。 / Vertex buffers keyed by type ordinal. */
	private IntMap<VertexBuffer> buffers = new IntMap<VertexBuffer>();
	/** 点大小。 / Point size. */
	private float pointSize = 1;
	/** 线宽。 / Line width. */
	private float lineWidth = 1;
	/** 顶点数组对象 ID / Vertex-array object id */
	private transient int vertexArrayID = -1;
	/** 顶点数。 / Vertex count. */
	private int vertCount = -1;
	/** 图元（元素）数。 / Primitive (element) count. */
	private int elementCount = -1;
	/** 骨骼动画最大权重数。 / Max bone weights (skeletal animation only). */
	private int maxNumWeights = -1; // only if using skeletal animation
	/** 混合模式下各段起始索引。 / Mode-start indices for hybrid mode. */
	private int[] modeStart;
	/** 当前图元模式，默认三角形。 / Current primitive mode; default Triangles. */
	private Mode mode = Mode.Triangles;
	/** 碰撞标志（低 8 位材质，高 8 位意图）。 / Collision flags (low 8 material, high 8 intentions). */
	private short collisionFlags = -1;

	/**
	 * 默认构造空网格。
	 * Default constructor for an empty mesh.
	 */
	public Mesh() {
	}

	/**
	 * 返回混合模式起始索引数组。
	 * Returns the hybrid mode-start index array.
	 *
	 * @return 起始索引数组 / mode-start array
	 */
	public int[] getModeStart() {
		return modeStart;
	}

	/**
	 * 设置混合模式起始索引数组。
	 * Sets the hybrid mode-start index array.
	 *
	 * @param modeStart 起始索引数组 / mode-start array
	 */
	public void setModeStart(int[] modeStart) {
		this.modeStart = modeStart;
	}

	/**
	 * 返回图元模式。
	 * Returns the primitive mode.
	 *
	 * primitive mode
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * 设置图元模式并刷新计数。
	 * Sets the primitive mode and refreshes counts.
	 *
	 * @param mode 图元模式 / primitive mode
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
		updateCounts();
	}

	/**
	 * 返回最大骨骼权重数。
	 * Returns the max number of bone weights.
	 *
	 * @return 最大权重数 / max weights
	 */
	public int getMaxNumWeights() {
		return maxNumWeights;
	}

	/**
	 * 设置最大骨骼权重数。
	 * Sets the max number of bone weights.
	 *
	 * @param maxNumWeights 最大权重数 / max weights
	 */
	public void setMaxNumWeights(int maxNumWeights) {
		this.maxNumWeights = maxNumWeights;
	}

	/**
	 * 返回点大小。
	 * Returns the point size.
	 *
	 * point size
	 */
	public float getPointSize() {
		return pointSize;
	}

	/**
	 * 设置点大小。
	 * Sets the point size.
	 *
	 * point size
	 */
	public void setPointSize(float pointSize) {
		this.pointSize = pointSize;
	}

	/**
	 * 返回线宽。
	 * Returns the line width.
	 *
	 * line width
	 */
	public float getLineWidth() {
		return lineWidth;
	}

	/**
	 * 设置线宽。
	 * Sets the line width.
	 *
	 * line width
	 */
	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
	}

	/**
	 * 将所有缓冲标记为 Static，锁定网格以优化数据。
	 * Marks all buffers Static, locking the mesh so it can no longer be modified (optimizes data).
	 */
	@SuppressWarnings("unchecked")
	public void setStatic() {
		for (Entry<VertexBuffer> entry : buffers) {
			entry.getValue().setUsage(Usage.Static);
		}
	}

	/**
	 * 将所有缓冲标记为 Stream。
	 * Marks all buffers as Stream usage.
	 */
	@SuppressWarnings("unchecked")
	public void setStreamed() {
		for (Entry<VertexBuffer> entry : buffers) {
			entry.getValue().setUsage(Usage.Stream);
		}
	}

	/**
	 * 将各属性缓冲交错打包为单一 InterleavedData 缓冲。
	 * Interleaves attribute buffers into a single InterleavedData buffer.
	 */
	@SuppressWarnings("unchecked")
	public void setInterleaved() {
		ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>();
		for (Entry<VertexBuffer> entry : buffers) {
			vbs.add(entry.getValue());
		}
		// ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>(buffers.values());
		// 交错时不包含索引缓冲 / index buffer not included when interleaving
		vbs.remove(getBuffer(Type.Index));

		int stride = 0; // aka bytes per vertex
		for (int i = 0; i < vbs.size(); i++) {
			VertexBuffer vb = vbs.get(i);
			// if (vb.getFormat() != Format.Float){
			// throw new UnsupportedOperationException("Cannot interleave vertex buffer.\n"
			// +
			// “包含非 float 数据。”); / "Contains not-float data.");
			// }
			stride += vb.componentsLength;
			vb.getData().clear(); // reset position & limit (used later)
		}

		VertexBuffer allData = new VertexBuffer(Type.InterleavedData);
		ByteBuffer dataBuf = BufferUtils.createByteBuffer(stride * getVertexCount());
		allData.setupData(Usage.Static, -1, Format.UnsignedByte, dataBuf);
		setBuffer(allData);

		for (int vert = 0; vert < getVertexCount(); vert++) {
			for (int i = 0; i < vbs.size(); i++) {
				VertexBuffer vb = vbs.get(i);
				switch (vb.getFormat()) {
				case Float:
					FloatBuffer fb = (FloatBuffer) vb.getData();
					for (int comp = 0; comp < vb.components; comp++) {
						dataBuf.putFloat(fb.get());
					}
					break;
				case Byte:
				case UnsignedByte:
					ByteBuffer bb = (ByteBuffer) vb.getData();
					for (int comp = 0; comp < vb.components; comp++) {
						dataBuf.put(bb.get());
					}
					break;
				case Half:
				case Short:
				case UnsignedShort:
					ShortBuffer sb = (ShortBuffer) vb.getData();
					for (int comp = 0; comp < vb.components; comp++) {
						dataBuf.putShort(sb.get());
					}
					break;
				case Int:
				case UnsignedInt:
					IntBuffer ib = (IntBuffer) vb.getData();
					for (int comp = 0; comp < vb.components; comp++) {
						dataBuf.putInt(ib.get());
					}
					break;
				default:
					break;
				}
			}
		}

		int offset = 0;
		for (VertexBuffer vb : vbs) {
			vb.setOffset(offset);
			vb.setStride(stride);

			// 丢弃旧缓冲 / discard old buffer
			vb.setupData(vb.usage, vb.components, vb.format, null);
			offset += vb.componentsLength;
		}
	}

	/**
	 * 按图元模式将缓冲大小换算为元素个数。
	 * Converts a buffer size to an element count based on the primitive mode.
	 *
	 * @param bufSize 缓冲元素规模 / buffer element size
	 * primitive count
	 */
	private int computeNumElements(int bufSize) {
		switch (mode) {
		case Triangles:
			return bufSize / 3;
		case TriangleFan:
		case TriangleStrip:
			return bufSize - 2;
		case Points:
			return bufSize;
		case Lines:
			return bufSize / 2;
		case LineLoop:
			return bufSize;
		case LineStrip:
			return bufSize - 1;
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * 根据位置/索引缓冲刷新顶点数与图元数（须在交错前调用）。
	 * Refreshes vertex and element counts from position/index buffers (must be called before interleaving).
	 */
	public void updateCounts() {
		if (getBuffer(Type.InterleavedData) != null) {
			throw new IllegalStateException("Should update counts before interleave");
		}

		VertexBuffer pb = getBuffer(Type.Position);
		VertexBuffer ib = getBuffer(Type.Index);
		if (pb != null) {
			vertCount = pb.getData().capacity() / pb.getNumComponents();
		}
		if (ib != null) {
			elementCount = computeNumElements(ib.getData().capacity());
		} else {
			elementCount = computeNumElements(vertCount);
		}
	}

	/**
	 * 返回指定 LOD 的三角形数（当前实现忽略 lod，等同 {@link #getTriangleCount()}）。
	 * Returns the triangle count for the given LOD (currently ignores lod; same as {@link #getTriangleCount()}).
	 *
	 * @param lod LOD 级别（未使用） / LOD level (unused)
	 * triangle count
	 */
	public int getTriangleCount(int lod) {
		return elementCount;
	}

	/**
	 * 返回三角形（图元）数。
	 * Returns the triangle (element) count.
	 *
	 * element count
	 */
	public int getTriangleCount() {
		return elementCount;
	}

	/**
	 * 返回顶点数。
	 * Returns the vertex count.
	 *
	 * vertex count
	 */
	public int getVertexCount() {
		return vertCount;
	}

	/**
	 * 手动设置三角形数。
	 * Manually sets the triangle count.
	 *
	 * @param count 三角形数 / triangle count
	 */
	public void setTriangleCount(int count) {
		this.elementCount = count;
	}

	/**
	 * 手动设置顶点数。
	 * Manually sets the vertex count.
	 *
	 * vertex count
	 */
	public void setVertexCount(int count) {
		this.vertCount = count;
	}

	/**
	 * 将第 index 个三角形的三个顶点写入 v1/v2/v3（要求 Float 位置 + UnsignedShort 索引）。
	 * Writes the three vertices of triangle index into v1/v2/v3 (requires Float positions and UnsignedShort indices).
	 *
	 * @param index 三角形索引 / triangle index
	 * @param v1 顶点 1 输出 / vertex 1 out
	 * @param v2 顶点 2 输出 / vertex 2 out
	 * @param v3 顶点 3 输出 / vertex 3 out
	 */
	public void getTriangle(int index, Vector3f v1, Vector3f v2, Vector3f v3) {
		VertexBuffer pb = getBuffer(Type.Position);
		VertexBuffer ib = getBuffer(Type.Index);

		if (pb.getFormat() == Format.Float) {
			FloatBuffer fpb = (FloatBuffer) pb.getData();

			if (ib.getFormat() == Format.UnsignedShort) {
				// 缓冲区接受的格式 / accepted format for buffers
				ShortBuffer sib = (ShortBuffer) ib.getData();

				// 获取三角形顶点索引 / aquire triangle's vertex indices
				int vertIndex = index * 3;
				int vert1 = sib.get(vertIndex);
				int vert2 = sib.get(vertIndex + 1);
				int vert3 = sib.get(vertIndex + 2);

				BufferUtils.populateFromBuffer(v1, fpb, vert1);
				BufferUtils.populateFromBuffer(v2, fpb, vert2);
				BufferUtils.populateFromBuffer(v3, fpb, vert3);
			}
		}
	}

	/**
	 * 将第 index 个三角形写入 {@link Triangle}。
	 * Writes triangle index into a {@link Triangle}.
	 *
	 * @param index 三角形索引 / triangle index
	 * @param tri 输出三角形 / output triangle
	 */
	public void getTriangle(int index, Triangle tri) {
		getTriangle(index, tri.get1(), tri.get2(), tri.get3());
		tri.setIndex(index);
	}

	/**
	 * 将第 index 个三角形的三个顶点索引写入数组（UnsignedShort 索引）。
	 * Writes the three vertex indices of triangle index into the array (UnsignedShort indices).
	 *
	 * @param index 三角形索引 / triangle index
	 * @param indices 长度至少 3 的输出数组 / output array of length ≥ 3
	 */
	public void getTriangle(int index, int[] indices) {
		VertexBuffer ib = getBuffer(Type.Index);
		if (ib.getFormat() == Format.UnsignedShort) {
			// 缓冲区接受的格式 / accepted format for buffers
			ShortBuffer sib = (ShortBuffer) ib.getData();

			// 获取三角形顶点索引 / aquire triangle's vertex indices
			int vertIndex = index * 3;
			indices[0] = sib.get(vertIndex);
			indices[1] = sib.get(vertIndex + 1);
			indices[2] = sib.get(vertIndex + 2);
		}
	}

	/**
	 * 返回顶点数组对象 ID。
	 * Returns the vertex-array object id.
	 *
	 * VAO id
	 */
	public int getId() {
		return vertexArrayID;
	}

	/**
	 * 设置顶点数组对象 ID（仅可设置一次）。
	 * Sets the vertex-array object id (only once).
	 *
	 * VAO id
	 */
	public void setId(int id) {
		if (vertexArrayID != -1) {
			throw new IllegalStateException("ID has already been set.");
		}

		vertexArrayID = id;
	}

	/**
	 * 若尚无碰撞树则构建 BIH 树。
	 * Builds a BIH tree if no collision tree exists yet.
	 */
	public void createCollisionData() {
		if (collisionTree != null) {
			return;
		}
		BIHTree tree = new BIHTree(this);
		tree.construct();
		collisionTree = tree;
	}

	/**
	 * 使用碰撞树与可碰撞对象检测，结果写入 results。
	 * Collides via the collision tree and writes hits into results.
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * world transform
	 * @param worldBound 世界包围体 / world bound
	 * @param results 结果收集器 / results collector
	 * number of collisions
	 */
	public int collideWith(Collidable other, Matrix4f worldMatrix, BoundingVolume worldBound,
			CollisionResults results) {

		if (collisionTree == null) {
			createCollisionData();
		}
		return collisionTree.collideWith(other, worldMatrix, worldBound, results);
	}

	/**
	 * 设置或更新 Float 类型顶点缓冲。
	 * Sets or updates a Float vertex buffer.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, FloatBuffer buf) {
		VertexBuffer vb = buffers.get(type.ordinal());
		if (vb == null) {
			if (buf == null) {
				return;
			}

			vb = new VertexBuffer(type);
			vb.setupData(Usage.Dynamic, components, Format.Float, buf);
			// buffers.put(type, vb);
			buffers.put(type.ordinal(), vb);
		} else {
			vb.setupData(Usage.Dynamic, components, Format.Float, buf);
		}
		updateCounts();
	}

	/**
	 * 以 float 数组设置 Float 缓冲。
	 * Sets a Float buffer from a float array.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, float[] buf) {
		setBuffer(type, components, BufferUtils.createFloatBuffer(buf));
	}

	/**
	 * 设置 UnsignedInt 类型缓冲（仅在该类型尚不存在时）。
	 * Sets an UnsignedInt buffer (only if that type is not already present).
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, IntBuffer buf) {
		VertexBuffer vb = buffers.get(type.ordinal());
		if (vb == null) {
			vb = new VertexBuffer(type);
			vb.setupData(Usage.Dynamic, components, Format.UnsignedInt, buf);
			buffers.put(type.ordinal(), vb);
			updateCounts();
		}
	}

	/**
	 * 以 int 数组设置 UnsignedInt 缓冲。
	 * Sets an UnsignedInt buffer from an int array.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, int[] buf) {
		setBuffer(type, components, BufferUtils.createIntBuffer(buf));
	}

	/**
	 * 设置 UnsignedShort 类型缓冲（仅在该类型尚不存在时）。
	 * Sets an UnsignedShort buffer (only if that type is not already present).
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, ShortBuffer buf) {
		VertexBuffer vb = buffers.get(type.ordinal());
		if (vb == null) {
			vb = new VertexBuffer(type);
			vb.setupData(Usage.Dynamic, components, Format.UnsignedShort, buf);
			buffers.put(type.ordinal(), vb);
			updateCounts();
		}
	}

	/**
	 * 以 byte 数组设置 UnsignedByte 缓冲。
	 * Sets an UnsignedByte buffer from a byte array.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, byte[] buf) {
		setBuffer(type, components, BufferUtils.createByteBuffer(buf));
	}

	/**
	 * 设置 UnsignedByte 类型缓冲（仅在该类型尚不存在时）。
	 * Sets an UnsignedByte buffer (only if that type is not already present).
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, ByteBuffer buf) {
		VertexBuffer vb = buffers.get(type.ordinal());
		if (vb == null) {
			vb = new VertexBuffer(type);
			vb.setupData(Usage.Dynamic, components, Format.UnsignedByte, buf);
			buffers.put(type.ordinal(), vb);
			updateCounts();
		}
	}

	/**
	 * 放入已构造好的 {@link VertexBuffer}（类型不可重复）。
	 * Puts a prebuilt {@link VertexBuffer} (type must not already be set).
	 *
	 * @param vb 顶点缓冲 / vertex buffer
	 */
	public void setBuffer(VertexBuffer vb) {
		if (buffers.containsKey(vb.getBufferType().ordinal())) {
			throw new IllegalArgumentException("Buffer type already set: " + vb.getBufferType());
		}

		buffers.put(vb.getBufferType().ordinal(), vb);
	}

	/**
	 * 移除指定类型的缓冲。
	 * Removes the buffer of the given type.
	 *
	 * @param type 缓冲类型 / buffer type
	 */
	public void clearBuffer(VertexBuffer.Type type) {
		buffers.remove(type.ordinal());
	}

	/**
	 * 以 short 数组设置 UnsignedShort 缓冲。
	 * Sets an UnsignedShort buffer from a short array.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @param components 每顶点分量数 / components per vertex
	 * data
	 */
	public void setBuffer(Type type, int components, short[] buf) {
		setBuffer(type, components, BufferUtils.createShortBuffer(buf));
	}

	/**
	 * 按类型获取顶点缓冲。
	 * Gets the vertex buffer of the given type.
	 *
	 * @param type 缓冲类型 / buffer type
	 * @return 顶点缓冲，可能为 null / vertex buffer, or null
	 */
	public VertexBuffer getBuffer(Type type) {
		return buffers.get(type.ordinal());
	}

	/**
	 * 按类型获取 Float 数据缓冲。
	 * Gets the Float data buffer of the given type.
	 *
	 * @param type 缓冲类型 / buffer type
	 * FloatBuffer, or null
	 */
	public FloatBuffer getFloatBuffer(Type type) {
		VertexBuffer vb = getBuffer(type);
		if (vb == null) {
			return null;
		}
		return (FloatBuffer) vb.getData();
	}

	/**
	 * 按类型获取 Short 数据缓冲。
	 * Gets the Short data buffer of the given type.
	 *
	 * @param type 缓冲类型 / buffer type
	 * ShortBuffer, or null
	 */
	public ShortBuffer getShortBuffer(Type type) {
		VertexBuffer vb = getBuffer(type);
		if (vb == null) {
			return null;
		}
		return (ShortBuffer) vb.getData();
	}

	/**
	 * 返回包装后的索引缓冲抽象（按底层类型选择实现）。
	 * Returns a typed {@link IndexBuffer} wrapper over the index data.
	 *
	 * @return 索引缓冲，或无索引时为 null / index buffer, or null if none
	 */
	public IndexBuffer getIndexBuffer() {
		VertexBuffer vb = getBuffer(Type.Index);
		if (vb == null) {
			return null;
		}

		Buffer buf = vb.getData();
		if (buf instanceof ByteBuffer) {
			return new IndexByteBuffer((ByteBuffer) buf);
		} else if (buf instanceof ShortBuffer) {
			return new IndexShortBuffer((ShortBuffer) buf);
		} else if (buf instanceof IntBuffer) {
			return new IndexIntBuffer((IntBuffer) buf);
		} else {
			throw new UnsupportedOperationException("Index buffer type unsupported: " + buf.getClass());
		}
	}

	/**
	 * 按比例缩放 2D 纹理坐标（仅支持 Float、2 分量）。
	 * Scales 2D texture coordinates by the given factor (Float format, 2 components only).
	 *
	 * scale factor
	 */
	public void scaleTextureCoordinates(Vector2f scaleFactor) {
		VertexBuffer tc = getBuffer(Type.TexCoord);
		if (tc == null) {
			throw new IllegalStateException("The mesh has no texture coordinates");
		}

		if (tc.getFormat() != VertexBuffer.Format.Float) {
			throw new UnsupportedOperationException("Only float texture coord format is supported");
		}

		if (tc.getNumComponents() != 2) {
			throw new UnsupportedOperationException("Only 2D texture coords are supported");
		}

		FloatBuffer fb = (FloatBuffer) tc.getData();
		fb.clear();
		for (int i = 0; i < fb.capacity() / 2; i++) {
			float x = fb.get();
			float y = fb.get();
			fb.position(fb.position() - 2);
			x *= scaleFactor.getX();
			y *= scaleFactor.getY();
			fb.put(x).put(y);
		}
		fb.clear();
	}

	/**
	 * 由位置缓冲重算网格包围体。
	 * Recomputes the mesh bound from the position buffer.
	 */
	public void updateBound() {
		VertexBuffer posBuf = getBuffer(VertexBuffer.Type.Position);
		if (meshBound == null) {
			meshBound = new BoundingBox();
		}
		if (posBuf != null) {
			meshBound.computeFromPoints((FloatBuffer) posBuf.getData());
		}
	}

	/**
	 * 返回网格包围体。
	 * Returns the mesh bounding volume.
	 *
	 * bounding volume
	 */
	public BoundingVolume getBound() {
		return meshBound;
	}

	/**
	 * 设置网格包围体。
	 * Sets the mesh bounding volume.
	 *
	 * bounding volume
	 */
	public void setBound(BoundingVolume modelBound) {
		meshBound = modelBound;
	}

	/**
	 * 返回全部顶点缓冲映射。
	 * Returns the map of all vertex buffers.
	 *
	 * buffer map
	 */
	public IntMap<VertexBuffer> getBuffers() {
		return buffers;
	}

	/**
	 * 返回碰撞标志。
	 * Returns collision flags.
	 *
	 * collision flags
	 */
	public short getCollisionFlags() {
		return collisionFlags;
	}

	/**
	 * 设置碰撞标志。
	 * Sets collision flags.
	 *
	 * collision flags
	 */
	public void setCollisionFlags(short collisionFlags) {
		this.collisionFlags = collisionFlags;
	}

	/**
	 * 从碰撞标志低 8 位取得材质 ID。
	 * Returns the material id from the low 8 bits of collision flags.
	 *
	 * material id
	 */
	public byte getMaterialId() {
		return (byte) (collisionFlags & 0xFF);
	}

	/**
	 * 从碰撞标志高 8 位取得碰撞意图掩码。
	 * Returns the intention mask from the high 8 bits of collision flags.
	 *
	 * intention mask
	 */
	public byte getIntentions() {
		return (byte) (collisionFlags >> 8);
	}
}
