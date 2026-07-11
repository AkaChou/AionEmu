package com.aionemu.gameserver.geoEngine.scene;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.utils.BufferUtils;

/**
 * 顶点缓冲：封装某一顶点属性（或索引）的数据、格式与更新状态。
 * Vertex buffer encapsulating data, format and update state for a single vertex attribute (or index).
 */
public class VertexBuffer extends GLObject implements Cloneable {

	/**
	 * 缓冲类型，指定其定义的实际属性。
	 * Type of buffer; specifies the actual attribute it defines.
	 */
	public static enum Type {

		/**
		 * 顶点位置（3 个 float）。
		 * Position of the vertex (3 floats).
		 */
		Position,
		/**
		 * 使用点缓冲时的点大小。
		 * The size of the point when using point buffers.
		 */
		Size,
		/**
		 * 法线向量（已归一化）。
		 * Normal vector, normalized.
		 */
		Normal,
		/**
		 * 纹理坐标。
		 * Texture coordinate.
		 */
		TexCoord,
		/**
		 * 颜色与 Alpha（4 个 float）。
		 * Color and Alpha (4 floats).
		 */
		Color,
		/**
		 * 切线向量（已归一化）。
		 * Tangent vector, normalized.
		 */
		Tangent,
		/**
		 * 副法线向量（已归一化）。
		 * Binormal vector, normalized.
		 */
		Binormal,
		/**
		 * 交错时各顶点缓冲的源数据。
		 * Source data for various vertex buffers when interleaving is used.
		 */
		InterleavedData,
		/**
		 * 请勿使用。
		 * Do not use.
		 */
		@Deprecated
		MiscAttrib,
		/**
		 * 索引缓冲，须为整数数据。
		 * Index buffer; must contain integer data.
		 */
		Index,
		/**
		 * 动画用的初始顶点位置。
		 * Initial vertex position, used with animation.
		 */
		BindPosePosition,
		/**
		 * 动画用的初始顶点法线。
		 * Initial vertex normals, used with animation.
		 */
		BindPoseNormal,
		/**
		 * 动画用的骨骼权重。
		 * Bone weights, used with animation.
		 */
		BoneWeight,
		/**
		 * 动画用的骨骼索引。
		 * Bone indices, used with animation.
		 */
		BoneIndex,
		/**
		 * 第二套纹理坐标。
		 * Texture coordinate #2.
		 */
		TexCoord2;
	}

	/**
	 * 缓冲用途提示，可影响是否放入 VRAM，但不保证。
	 * Usage hint for the buffer; may influence VRAM placement, but no guarantees.
	 */
	public static enum Usage {

		/**
		 * 网格数据发送一次，极少更新。
		 * Mesh data is sent once and very rarely updated.
		 */
		Static,
		/**
		 * 网格数据偶尔更新（每帧一次或更少）。
		 * Mesh data is updated occasionally (once per frame or less).
		 */
		Dynamic,
		/**
		 * 网格数据每帧更新。
		 * Mesh data is updated every frame.
		 */
		Stream,
		/**
		 * 数据不发送到 GPU，仅 CPU 使用。
		 * Mesh data is not sent to the GPU at all; CPU only.
		 */
		CpuOnly;
	}

	/**
	 * 缓冲元素数据格式。
	 * Data format of buffer elements.
	 */
	public static enum Format {
		// 浮点格式 / Floating point formats

		Half(2), Float(4), Double(8),
		// 整数格式 / Integer formats
		Byte(1), UnsignedByte(1), Short(2), UnsignedShort(2), Int(4), UnsignedInt(4);

		/** 该格式每个分量的字节数。 / Bytes per component for this format. */
		private int componentSize = 0;

		Format(int componentSize) {
			this.componentSize = componentSize;
		}

		/**
		 * 返回该数据类型的字节大小。
		 * Returns the size in bytes of this data type.
		 *
		 * @return 分量字节数 / component size in bytes
		 */
		public int getComponentSize() {
			return componentSize;
		}
	}

	/** 交错缓冲中的字节偏移。 / Byte offset within an interleaved buffer. */
	protected int offset = 0;
	/** 交错缓冲中的字节步长。 / Byte stride within an interleaved buffer. */
	protected int stride = 0;
	/** 每顶点分量数。 / Components per vertex. */
	protected int components = 0;
	/**
	 * 由 components * format.getComponentSize() 推导的每顶点字节数。
	 * Bytes per vertex derived from components * format.getComponentSize().
	 */
	protected transient int componentsLength = 0;
	/** 底层 NIO 数据 / Underlying NIO data*/
	protected Buffer data = null;
	/** 映射的字节缓冲（若有）。 / Mapped byte buffer if any. */
	protected transient ByteBuffer mappedData;
	/** 用途提示。 / Usage hint. */
	protected Usage usage;
	/** 属性类型。 / Attribute type. */
	protected Type bufType;
	/** 数据格式。 / Data format. */
	protected Format format;
	/** 是否归一化整数数据。 / Whether integer data is normalized. */
	protected boolean normalized = false;
	/** 数据容量是否已变化。 / Whether data capacity has changed. */
	protected transient boolean dataSizeChanged = false;

	/**
	 * 创建空的、未初始化缓冲；须调用 setupData() 初始化。
	 * Creates an empty, uninitialized buffer. Must call setupData() to initialize.
	 *
	 * @param type 属性类型 / attribute type
	 */
	public VertexBuffer(Type type) {
		super(GLObject.Type.VertexBuffer);
		this.bufType = type;
	}

	/**
	 * 仅用于序列化的空构造，请勿在业务代码中使用。
	 * Do not use this constructor. Serialization purposes only.
	 */
	public VertexBuffer() {
		super(GLObject.Type.VertexBuffer);
	}

	/**
	 * 带已有 ID 的受保护构造，供可销毁浅拷贝使用。
	 * Protected constructor with an existing id, for destructable shallow clones.
	 *
	 * @param id 对象 ID / object id
	 */
	protected VertexBuffer(int id) {
		super(GLObject.Type.VertexBuffer, id);
	}

	/**
	 * 返回交错偏移。
	 * Returns the interleaved offset.
	 *
	 * offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * 设置交错偏移。
	 * Sets the interleaved offset.
	 *
	 * offset
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * 返回交错步长。
	 * Returns the interleaved stride.
	 *
	 * stride
	 */
	public int getStride() {
		return stride;
	}

	/**
	 * 设置交错步长。
	 * Sets the interleaved stride.
	 *
	 * stride
	 */
	public void setStride(int stride) {
		this.stride = stride;
	}

	/**
	 * 返回底层数据缓冲。
	 * Returns the underlying data buffer.
	 *
	 * data buffer
	 */
	public Buffer getData() {
		return data;
	}

	/**
	 * 返回映射字节缓冲。
	 * Returns the mapped byte buffer.
	 *
	 * mapped buffer
	 */
	public ByteBuffer getMappedData() {
		return mappedData;
	}

	/**
	 * 设置映射字节缓冲。
	 * Sets the mapped byte buffer.
	 *
	 * mapped buffer
	 */
	public void setMappedData(ByteBuffer mappedData) {
		this.mappedData = mappedData;
	}

	/**
	 * 返回用途提示。
	 * Returns the usage hint.
	 *
	 * usage
	 */
	public Usage getUsage() {
		return usage;
	}

	/**
	 * 设置用途提示。
	 * Sets the usage hint.
	 *
	 * usage
	 */
	public void setUsage(Usage usage) {
		// if (id != -1)
		// throw new UnsupportedOperationException("Data has already been sent. Cannot
		// set usage.");

		this.usage = usage;
	}

	/**
	 * 设置是否归一化。
	 * Sets whether data is normalized.
	 *
	 * @param normalized 是否归一化 / whether normalized
	 */
	public void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	/**
	 * 是否归一化。
	 * Whether data is normalized.
	 *
	 * @return 归一化则为 true / true if normalized
	 */
	public boolean isNormalized() {
		return normalized;
	}

	/**
	 * 返回属性类型。
	 * Returns the attribute type.
	 *
	 * buffer type
	 */
	public Type getBufferType() {
		return bufType;
	}

	/**
	 * 返回数据格式。
	 * Returns the data format.
	 *
	 * format
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * 返回每顶点分量数。
	 * Returns the number of components per vertex.
	 *
	 * component count
	 */
	public int getNumComponents() {
		return components;
	}

	/**
	 * 返回元素（顶点）个数。
	 * Returns the number of elements (vertices).
	 *
	 * element count
	 */
	public int getNumElements() {
		int elements = data.capacity() / components;
		if (format == Format.Half) {
			elements /= 2;
		}
		return elements;
	}

	/**
	 * 初始化缓冲数据（数据已上传后不可再次调用）。
	 * Initializes buffer data (cannot be called again after data has been sent).
	 *
	 * usage
	 * @param components 每顶点分量数 / components per vertex
	 * data format
	 * @param data 数据缓冲 / data buffer
	 */
	public void setupData(Usage usage, int components, Format format, Buffer data) {
		if (id != -1) {
			throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");
		}

		this.data = data;
		this.components = components;
		this.usage = usage;
		this.format = format;
		this.componentsLength = components * format.getComponentSize();
		setUpdateNeeded();
	}

	/**
	 * 更新数据缓冲；容量变化时标记 dataSizeChanged。
	 * Updates the data buffer; marks dataSizeChanged when capacity differs.
	 *
	 * new data
	 */
	public void updateData(Buffer data) {
		if (id != -1) {
			// 更新数据请求可以 / request to update data is okay
		}

		// 将强制渲染器再次调用 glBufferData / will force renderer to call glBufferData again
		if (this.data.capacity() != data.capacity()) {
			dataSizeChanged = true;
		}
		this.data = data;
		setUpdateNeeded();
	}

	/**
	 * 数据容量是否已变化。
	 * Whether data capacity has changed.
	 *
	 * @return 已变化则为 true / true if capacity changed
	 */
	public boolean hasDataSizeChanged() {
		return dataSizeChanged;
	}

	/**
	 * 清除更新标记及容量变化标记。
	 * Clears the update-needed flag and the data-size-changed flag.
	 */
	@Override
	public void clearUpdateNeeded() {
		super.clearUpdateNeeded();
		dataSizeChanged = false;
	}

	/**
	 * 将 Float 数据转换为 Half 格式（须在上传前）。
	 * Converts Float data to Half format (must be before upload).
	 */
	public void convertToHalf() {
		if (id != -1) {
			throw new UnsupportedOperationException("Data has already been sent.");
		}

		if (format != Format.Float) {
			throw new IllegalStateException("Format must be float!");
		}

		int numElements = data.capacity() / components;
		format = Format.Half;
		this.componentsLength = components * format.getComponentSize();

		ByteBuffer halfData = BufferUtils.createByteBuffer(componentsLength * numElements);
		halfData.rewind();

		FloatBuffer floatData = (FloatBuffer) data;
		floatData.rewind();

		for (int i = 0; i < floatData.capacity(); i++) {
			float f = floatData.get(i);
			short half = FastMath.convertFloatToHalf(f);
			halfData.putShort(half);
		}
		this.data = halfData;
		setUpdateNeeded();
		dataSizeChanged = true;
	}

	/**
	 * 将数据压缩为仅保留 numElements 个元素。
	 * Compacts the data to retain only numElements elements.
	 *
	 * @param numElements 保留的元素个数 / number of elements to keep
	 */
	public void compact(int numElements) {
		int total = components * numElements;
		data.clear();
		switch (format) {
		case Byte:
		case UnsignedByte:
		case Half:
			ByteBuffer bbuf = (ByteBuffer) data;
			bbuf.limit(total);
			ByteBuffer bnewBuf = BufferUtils.createByteBuffer(total);
			bnewBuf.put(bbuf);
			data = bnewBuf;
			break;
		case Short:
		case UnsignedShort:
			ShortBuffer sbuf = (ShortBuffer) data;
			sbuf.limit(total);
			ShortBuffer snewBuf = BufferUtils.createShortBuffer(total);
			snewBuf.put(sbuf);
			data = snewBuf;
			break;
		case Int:
		case UnsignedInt:
			IntBuffer ibuf = (IntBuffer) data;
			ibuf.limit(total);
			IntBuffer inewBuf = BufferUtils.createIntBuffer(total);
			inewBuf.put(ibuf);
			data = inewBuf;
			break;
		case Float:
			FloatBuffer fbuf = (FloatBuffer) data;
			fbuf.limit(total);
			FloatBuffer fnewBuf = BufferUtils.createFloatBuffer(total);
			fnewBuf.put(fbuf);
			data = fnewBuf;
			break;
		default:
			throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
		}
		data.clear();
		setUpdateNeeded();
		dataSizeChanged = true;
	}

	/**
	 * 将本缓冲第 inIndex 个元素拷贝到 outVb 的 outIndex（格式与分量数须一致）。
	 * Copies element inIndex from this buffer into outVb at outIndex (format and component count must match).
	 *
	 * @param inIndex 源元素索引 / source element index
	 * @param outVb 目标缓冲 / destination buffer
	 * @param outIndex 目标元素索引 / destination element index
	 */
	public void copyElement(int inIndex, VertexBuffer outVb, int outIndex) {
		if (outVb.format != format || outVb.components != components) {
			throw new IllegalArgumentException("Buffer format mismatch. Cannot copy");
		}

		int inPos = inIndex * components;
		int outPos = outIndex * components;
		int elementSz = components;
		if (format == Format.Half) {
			// 因为 half 存为 bytebuf 但实际 2 字节长 / because half is stored as bytebuf but its 2 bytes long
			inPos *= 2;
			outPos *= 2;
			elementSz *= 2;
		}
		data.clear();
		outVb.data.clear();

		switch (format) {
		case Byte:
		case UnsignedByte:
		case Half:
			ByteBuffer bin = (ByteBuffer) data;
			ByteBuffer bout = (ByteBuffer) outVb.data;
			bin.position(inPos).limit(inPos + elementSz);
			bout.position(outPos).limit(outPos + elementSz);
			bout.put(bin);
			break;
		case Short:
		case UnsignedShort:
			ShortBuffer sin = (ShortBuffer) data;
			ShortBuffer sout = (ShortBuffer) outVb.data;
			sin.position(inPos).limit(inPos + elementSz);
			sout.position(outPos).limit(outPos + elementSz);
			sout.put(sin);
			break;
		case Int:
		case UnsignedInt:
			IntBuffer iin = (IntBuffer) data;
			IntBuffer iout = (IntBuffer) outVb.data;
			iin.position(inPos).limit(inPos + elementSz);
			iout.position(outPos).limit(outPos + elementSz);
			iout.put(iin);
			break;
		case Float:
			FloatBuffer fin = (FloatBuffer) data;
			FloatBuffer fout = (FloatBuffer) outVb.data;
			fin.position(inPos).limit(inPos + elementSz);
			fout.position(outPos).limit(outPos + elementSz);
			fout.put(fin);
			break;
		default:
			throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
		}

		data.clear();
		outVb.data.clear();
	}

	/**
	 * 按格式与分量数创建可容纳 numElements 个元素的 NIO 缓冲。
	 * Creates an NIO buffer for the given format/components that holds numElements elements.
	 *
	 * data format
	 * @param components 每顶点分量数（1–4） / components per vertex (1–4)
	 * element count
	 * newly created buffer
	 */
	public static final Buffer createBuffer(Format format, int components, int numElements) {
		if (components < 1 || components > 4) {
			throw new IllegalArgumentException("Num components must be between 1 and 4");
		}

		int total = numElements * components;

		switch (format) {
		case Byte:
		case UnsignedByte:
			return BufferUtils.createByteBuffer(total);
		case Half:
			return BufferUtils.createByteBuffer(total * 2);
		case Short:
		case UnsignedShort:
			return BufferUtils.createShortBuffer(total);
		case Int:
		case UnsignedInt:
			return BufferUtils.createIntBuffer(total);
		case Float:
			return BufferUtils.createFloatBuffer(total);
		case Double:
			return BufferUtils.createDoubleBuffer(total);
		default:
			throw new UnsupportedOperationException("Unrecoginized buffer format: " + format);
		}
	}

	/**
	 * 深拷贝本缓冲（含数据克隆，新 ID）。
	 * Deep-clones this buffer (data cloned, new id).
	 *
	 * clone instance
	 */
	@Override
	public VertexBuffer clone() {
		// 注意：超类 GLObject 自动创建浅克隆 / NOTE: Superclass GLObject automatically creates shallow clone
		// 例如重用 ID。 / e.g re-use ID.
		VertexBuffer vb = (VertexBuffer) super.clone();
		if (data != null) {
			vb.updateData(BufferUtils.clone(data));
		}
		return vb;
	}

	/**
	 * 以覆盖类型克隆（数据拷贝，新 ID，未上传）。
	 * Clones with an overridden type (data copied, new id, not uploaded).
	 *
	 * @param overrideType 覆盖的属性类型 / overridden attribute type
	 * clone instance
	 */
	public VertexBuffer clone(Type overrideType) {
		VertexBuffer vb = new VertexBuffer(overrideType);
		vb.components = components;
		vb.componentsLength = componentsLength;
		vb.data = BufferUtils.clone(data);
		vb.format = format;
		vb.handleRef = new Object();
		vb.id = -1;
		vb.normalized = normalized;
		vb.offset = offset;
		vb.stride = stride;
		vb.updateNeeded = true;
		vb.usage = usage;
		return vb;
	}

	/**
	 * 返回格式/类型/用途与元素数的摘要字符串。
	 * Returns a summary string of format/type/usage and element count.
	 *
	 * @return 描述字符串 / descriptive string
	 */
	@Override
	public String toString() {
		String dataTxt = null;
		if (data != null) {
			dataTxt = ", elements=" + data.capacity();
		}
		return getClass().getSimpleName() + "[fmt=" + format.name() + ", type=" + bufType.name() + ", usage="
				+ usage.name() + dataTxt + "]";
	}

	/**
	 * 重置对象 ID 并标记需要更新（GL 上下文重启时）。
	 * Resets the object id and marks update needed (on GL context restart).
	 */
	@Override
	public void resetObject() {
		// assert this.id != -1;
		this.id = -1;
		setUpdateNeeded();
	}

	/**
	 * 创建仅含 ID 的可销毁浅拷贝。
	 * Creates a destructable shallow clone holding only the id.
	 *
	 * shallow clone
	 */
	@Override
	public GLObject createDestructableClone() {
		return new VertexBuffer(id);
	}
}
