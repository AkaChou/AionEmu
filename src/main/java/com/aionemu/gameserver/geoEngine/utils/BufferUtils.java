package com.aionemu.gameserver.geoEngine.utils;


import com.aionemu.boot.i18n.I18n;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import java.util.concurrent.ConcurrentHashMap;
import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

import lombok.extern.slf4j.Slf4j;

/**
 * NIO 缓冲工具，从向量等 jME 数据类型生成缓冲。
 * Helper for generating nio buffers from jME data classes such as Vectors and ColorRGBA.
 *
 * @author Joshua Slack
 * @version $Id: BufferUtils.java,v 1.16 2007/10/29 16:56:18 nca Exp $
 */
@Slf4j
public final class BufferUtils {

	//// -- 临时数据对象 / TEMP DATA OBJECTS -- ////
	// private static final Vector2f _tempVec2 = new Vector2f();
	// private static final Vector3f _tempVec3 = new Vector3f();
	// private static final ColorRGBA _tempColor = new ColorRGBA();
	//// -- 追踪哈希 / TRACKER HASH -- ////
	/** 直接内存缓冲弱引用追踪表。 / Weak-reference tracker for direct memory buffers. */
	private static final Map<Buffer, Object> trackingHash = new ConcurrentHashMap<>(new WeakHashMap<Buffer, Object>());
	/** 追踪表占位引用对象。 / Sentinel reference object stored in the tracking map. */
	private static final Object ref = new Object();
	/** 是否启用直接内存追踪。 / Whether direct-memory tracking is enabled. */
	private static final boolean trackDirectMemory = false;

	//// -- 通用克隆 / GENERIC CLONE -- ////

	/**
	 * 按具体缓冲类型分派并克隆缓冲。
	 * Clone a buffer by dispatching to the matching typed clone method.
	 *
	 * @param buf 待克隆的缓冲 / buffer to clone
	 * @return 克隆后的缓冲 / cloned buffer
	 * @throws UnsupportedOperationException 不支持的缓冲类型 / unsupported buffer type
	 */
	public static Buffer clone(Buffer buf) {
		if (buf instanceof FloatBuffer) {
			return clone((FloatBuffer) buf);
		} else if (buf instanceof ShortBuffer) {
			return clone((ShortBuffer) buf);
		} else if (buf instanceof ByteBuffer) {
			return clone((ByteBuffer) buf);
		} else if (buf instanceof IntBuffer) {
			return clone((IntBuffer) buf);
		} else if (buf instanceof DoubleBuffer) {
			return clone((DoubleBuffer) buf);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	//VECTOR3F METHODS -- ////

	/**
	 * 用 Vector3f 数组生成 FloatBuffer，长度为 3 * data.length，顺序为 x,y,z。
	 * Generate a FloatBuffer from Vector3f objects; length is 3 * data.length as x,y,z.
	 *
	 * @param data 要写入的 Vector3f 数组 / array of Vector3f objects to place into a new FloatBuffer
	 * @return 新的 FloatBuffer，data 为 null 时返回 null / new FloatBuffer, or null if data is null
	 */
	public static FloatBuffer createFloatBuffer(Vector3f... data) {
		if (data == null) {
			return null;
		}
		FloatBuffer buff = createFloatBuffer(3 * data.length);
		for (int x = 0; x < data.length; x++) {
			if (data[x] != null) {
				buff.put(data[x].x).put(data[x].y).put(data[x].z);
			} else {
				buff.put(0).put(0).put(0);
			}
		}
		buff.flip();
		return buff;
	}

	/**
	 * 用 float 数组生成 FloatBuffer。
	 * Generate a FloatBuffer from float primitives.
	 *
	 * @param data 要写入的 float 数组 / array of float primitives to place into a new FloatBuffer
	 * @return 新的 FloatBuffer，data 为 null 时返回 null / new FloatBuffer, or null if data is null
	 */
	public static FloatBuffer createFloatBuffer(float... data) {
		if (data == null) {
			return null;
		}
		FloatBuffer buff = createFloatBuffer(data.length);
		buff.clear();
		buff.put(data);
		buff.flip();
		return buff;
	}

	/**
	 * 创建可容纳指定数量 Vector3f 的 FloatBuffer。
	 * Create a FloatBuffer sized to hold the specified number of Vector3f entries.
	 *
	 * @param vertices 需要容纳的顶点数量 / number of vertices to hold
	 * @return 请求的新 FloatBuffer / the requested new FloatBuffer
	 */
	public static FloatBuffer createVector3Buffer(int vertices) {
		FloatBuffer vBuff = createFloatBuffer(3 * vertices);
		return vBuff;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建可容纳指定数量 Vector3f 的 FloatBuffer。
	 * Reuse the given buffer if it already has the right size; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param vertices 需要容纳的顶点数量 / number of vertices to hold
	 * @return 合适大小的 FloatBuffer / the requested FloatBuffer
	 */
	public static FloatBuffer createVector3Buffer(FloatBuffer buf, int vertices) {
		if (buf != null && buf.limit() == 3 * vertices) {
			buf.rewind();
			return buf;
		}
		return createFloatBuffer(3 * vertices);
	}

	/**
	 * 将给定颜色数据写入 FloatBuffer 指定索引处。
	 * Sets the data contained in the given color into the FloatBuffer at the specified index.
	 *
	 * @param color 要插入的数据 / the data to insert
	 * @param buf 目标缓冲 / the buffer to insert into
	 * @param index 写入位置（按颜色计，非 float） / position in terms of colors not floats
	 */
	/*
	 * public static void setInBuffer(ColorRGBA color, FloatBuffer buf, int index) {
	 * buf.position(index*4); buf.put(color.r); buf.put(color.g); buf.put(color.b);
	 * buf.put(color.a); }
	 */

	/**
	 * 将 Vector3f 数据写入 FloatBuffer 指定索引处。
	 * Sets the Vector3f data into the FloatBuffer at the specified index.
	 *
	 * @param vector 要插入的向量 / the data to insert
	 * @param buf 目标缓冲 / the buffer to insert into
	 * @param index 写入位置（按向量计，非 float） / position in terms of vectors not floats
	 */
	public static void setInBuffer(Vector3f vector, FloatBuffer buf, int index) {
		if (buf == null) {
			return;
		}
		if (vector == null) {
			buf.put(index * 3, 0);
			buf.put((index * 3) + 1, 0);
			buf.put((index * 3) + 2, 0);
		} else {
			buf.put(index * 3, vector.x);
			buf.put((index * 3) + 1, vector.y);
			buf.put((index * 3) + 2, vector.z);
		}
	}

	/**
	 * 从缓冲指定索引读取数据填充到向量。
	 * Updates the given vector from the buffer at the provided index.
	 *
	 * @param vector 要写入数据的向量 / the vector to set data on
	 * @param buf 读取来源缓冲 / the buffer to read from
	 * @param index 读取位置（按向量计，非 float） / position in terms of vectors not floats
	 */
	public static void populateFromBuffer(Vector3f vector, FloatBuffer buf, int index) {
		vector.x = buf.get(index * 3);
		vector.y = buf.get(index * 3 + 1);
		vector.z = buf.get(index * 3 + 2);
	}

	/**
	 * 从 FloatBuffer 生成 Vector3f 数组。
	 * Generates a Vector3f array from the given FloatBuffer.
	 *
	 * @param buff 读取来源的 FloatBuffer / the FloatBuffer to read from
	 * @return 新生成的 Vector3f 数组 / a newly generated array of Vector3f objects
	 */
	public static Vector3f[] getVector3Array(FloatBuffer buff) {
		buff.clear();
		Vector3f[] verts = new Vector3f[buff.limit() / 3];
		for (int x = 0; x < verts.length; x++) {
			Vector3f v = new Vector3f(buff.get(), buff.get(), buff.get());
			verts[x] = v;
		}
		return verts;
	}

	/**
	 * 在同一缓冲内复制一个 Vector3f 到另一位置（索引按向量计）。
	 * Copies a Vector3f from one position in the buffer to another (indices in vector units).
	 *
	 * @param buf 源 / 目标缓冲 / the buffer to copy from/to
	 * @param fromPos 源向量索引 / the index of the vector to copy
	 * @param toPos 目标向量索引 / the index to copy the vector to
	 */
	public static void copyInternalVector3(FloatBuffer buf, int fromPos, int toPos) {
		copyInternal(buf, fromPos * 3, toPos * 3, 3);
	}

	/**
	 * 原地归一化缓冲中指定位置的 Vector3f。
	 * Normalize a Vector3f in-buffer.
	 *
	 * @param buf 含向量的缓冲 / the buffer containing the Vector3f
	 * @param index 向量位置（按向量计，非 float） / position in terms of vectors not floats
	 */
	public static void normalizeVector3(FloatBuffer buf, int index) {
		Vector3f tempVec3 = Vector3f.newInstance();
		populateFromBuffer(tempVec3, buf, index);
		tempVec3.normalizeLocal();
		setInBuffer(tempVec3, buf, index);
		Vector3f.recycle(tempVec3);
	}

	/**
	 * 将向量加到缓冲中指定位置的 Vector3f 上。
	 * Add to a Vector3f in-buffer.
	 *
	 * @param toAdd 要累加的向量 / the vector to add from
	 * @param buf 含向量的缓冲 / the buffer containing the Vector3f
	 * @param index 目标向量位置（按向量计，非 float） / position in terms of vectors not floats
	 */
	public static void addInBuffer(Vector3f toAdd, FloatBuffer buf, int index) {
		Vector3f tempVec3 = Vector3f.newInstance();
		populateFromBuffer(tempVec3, buf, index);
		tempVec3.addLocal(toAdd);
		setInBuffer(tempVec3, buf, index);
		Vector3f.recycle(tempVec3);
	}

	/**
	 * 将缓冲中指定位置的 Vector3f 与给定向量分量相乘并写回。
	 * Multiply and store a Vector3f in-buffer.
	 *
	 * @param toMult 要乘的向量 / the vector to multiply against
	 * @param buf 含向量的缓冲 / the buffer containing the Vector3f
	 * @param index 目标向量位置（按向量计，非 float） / position in terms of vectors not floats
	 */
	public static void multInBuffer(Vector3f toMult, FloatBuffer buf, int index) {
		Vector3f tempVec3 = Vector3f.newInstance();
		populateFromBuffer(tempVec3, buf, index);
		tempVec3.multLocal(toMult);
		setInBuffer(tempVec3, buf, index);
		Vector3f.recycle(tempVec3);
	}

	/**
	 * 判断给定 Vector3f 是否与缓冲指定索引处的数据相等。
	 * Checks whether the given Vector3f equals the data stored in the buffer at the index.
	 *
	 * @param check 用于比较的向量，null 返回 false / vector to check against; null returns false
	 * @param buf 比较用缓冲 / the buffer to compare data with
	 * @param index 缓冲中向量位置（按向量计，非 float） / position in terms of vectors not floats
	 * @return 若 equal 则为 true / true if equal
	 */
	public static boolean equals(Vector3f check, FloatBuffer buf, int index) {
		Vector3f tempVec3 = Vector3f.newInstance();
		populateFromBuffer(tempVec3, buf, index);
		boolean eq = tempVec3.equals(check);
		Vector3f.recycle(tempVec3);
		return eq;
	}

	// VECTOR2F METHODS -- ////

	/**
	 * 用 Vector2f 数组生成 FloatBuffer，长度为 2 * data.length，顺序为 x,y。
	 * Generate a FloatBuffer from Vector2f objects; length is 2 * data.length as x,y.
	 *
	 * @param data 要写入的 Vector2f 数组 / array of Vector2f objects to place into a new FloatBuffer
	 * @return 新的 FloatBuffer，data 为 null 时返回 null / new FloatBuffer, or null if data is null
	 */
	public static FloatBuffer createFloatBuffer(Vector2f... data) {
		if (data == null) {
			return null;
		}
		FloatBuffer buff = createFloatBuffer(2 * data.length);
		for (int x = 0; x < data.length; x++) {
			if (data[x] != null) {
				buff.put(data[x].x).put(data[x].y);
			} else {
				buff.put(0).put(0);
			}
		}
		buff.flip();
		return buff;
	}

	/**
	 * 创建可容纳指定数量 Vector2f 的 FloatBuffer。
	 * Create a FloatBuffer sized to hold the specified number of Vector2f entries.
	 *
	 * @param vertices 需要容纳的顶点数量 / number of vertices to hold
	 * @return 请求的新 FloatBuffer / the requested new FloatBuffer
	 */
	public static FloatBuffer createVector2Buffer(int vertices) {
		FloatBuffer vBuff = createFloatBuffer(2 * vertices);
		return vBuff;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建可容纳指定数量 Vector2f 的 FloatBuffer。
	 * Reuse the given buffer if it already has the right size; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param vertices 需要容纳的顶点数量 / number of vertices to hold
	 * @return 合适大小的 FloatBuffer / the requested FloatBuffer
	 */
	public static FloatBuffer createVector2Buffer(FloatBuffer buf, int vertices) {
		if (buf != null && buf.limit() == 2 * vertices) {
			buf.rewind();
			return buf;
		}

		return createFloatBuffer(2 * vertices);
	}

	//// -- INT 方法 / INT METHODS -- ////

	/**
	 * 用 int 数组生成 IntBuffer。
	 * Generate an IntBuffer from the given int array.
	 *
	 * @param data 要写入的 int 数组 / array of ints to place into a new IntBuffer
	 * @return 新的 IntBuffer，data 为 null 时返回 null / new IntBuffer, or null if data is null
	 */
	public static IntBuffer createIntBuffer(int... data) {
		if (data == null) {
			return null;
		}
		IntBuffer buff = createIntBuffer(data.length);
		buff.clear();
		buff.put(data);
		buff.flip();
		return buff;
	}

	/**
	 * 从 IntBuffer 内容生成 int 数组。
	 * Create a new int[] populated with the given IntBuffer's contents.
	 *
	 * @param buff 读取来源的 IntBuffer / the IntBuffer to read from
	 * @return 新的 int 数组，buff 为 null 时返回 null / new int array, or null if buff is null
	 */
	public static int[] getIntArray(IntBuffer buff) {
		if (buff == null) {
			return null;
		}
		buff.clear();
		int[] inds = new int[buff.limit()];
		for (int x = 0; x < inds.length; x++) {
			inds[x] = buff.get();
		}
		return inds;
	}

	/**
	 * 从 FloatBuffer 内容生成 float 数组。
	 * Create a new float[] populated with the given FloatBuffer's contents.
	 *
	 * @param buff 读取来源的 FloatBuffer / the FloatBuffer to read from
	 * @return 新的 float 数组，buff 为 null 时返回 null / new float array, or null if buff is null
	 */
	public static float[] getFloatArray(FloatBuffer buff) {
		if (buff == null) {
			return null;
		}
		buff.clear();
		float[] inds = new float[buff.limit()];
		for (int x = 0; x < inds.length; x++) {
			inds[x] = buff.get();
		}
		return inds;
	}

	//GENERAL DOUBLE ROUTINES -- ////

	/**
	 * 创建指定容量的直接 DoubleBuffer（本地字节序）。
	 * Create a direct DoubleBuffer of the specified size (native byte order).
	 *
	 * @param size 需要存储的 double 数量 / required number of doubles to store
	 * @return 新生成的 DoubleBuffer / the new DoubleBuffer
	 */
	public static DoubleBuffer createDoubleBuffer(int size) {
		DoubleBuffer buf = ByteBuffer.allocateDirect(8 * size).order(ByteOrder.nativeOrder()).asDoubleBuffer();
		buf.clear();
		if (trackDirectMemory) {
			trackingHash.put(buf, ref);
		}
		return buf;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建指定容量的 DoubleBuffer。
	 * Reuse the given DoubleBuffer if sized correctly; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param size 需要容纳的 double 数量 / number of doubles to hold
	 * @return 合适大小的 DoubleBuffer / the requested DoubleBuffer
	 */
	public static DoubleBuffer createDoubleBuffer(DoubleBuffer buf, int size) {
		if (buf != null && buf.limit() == size) {
			buf.rewind();
			return buf;
		}
		buf = createDoubleBuffer(size);
		return buf;
	}

	/**
	 * 深拷贝 DoubleBuffer 内容（独立副本，变更不互相反映）。
	 * Creates a separate DoubleBuffer with the same contents; use Buffer.duplicate() to share changes.
	 *
	 * @param buf 要拷贝的 DoubleBuffer / the DoubleBuffer to copy
	 * @return 拷贝结果，buf 为 null 时返回 null / the copy, or null if buf is null
	 */
	public static DoubleBuffer clone(DoubleBuffer buf) {
		if (buf == null) {
			return null;
		}
		buf.rewind();

		DoubleBuffer copy;
		if (buf.isDirect()) {
			copy = createDoubleBuffer(buf.limit());
		} else {
			copy = DoubleBuffer.allocate(buf.limit());
		}
		copy.put(buf);
		return copy;
	}

	//// -- 通用 FLOAT 例程 / GENERAL FLOAT ROUTINES -- ////

	/**
	 * 创建指定容量的直接 FloatBuffer（本地字节序）。
	 * Create a direct FloatBuffer of the specified size (native byte order).
	 *
	 * @param size 需要存储的 float 数量 / required number of floats to store
	 * @return 新生成的 FloatBuffer / the new FloatBuffer
	 */
	public static FloatBuffer createFloatBuffer(int size) {
		FloatBuffer buf = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder()).asFloatBuffer();
		buf.clear();
		if (trackDirectMemory) {
			trackingHash.put(buf, ref);
		}
		return buf;
	}

	/**
	 * 在同一 FloatBuffer 内将一段 float 从 fromPos 复制到 toPos。
	 * Copies floats from one position in the buffer to another.
	 *
	 * @param buf 源 / 目标缓冲 / the buffer to copy from/to
	 * @param fromPos 源起始位置 / the starting point to copy from
	 * @param toPos 目标起始位置 / the starting point to copy to
	 * @param length 复制的 float 数量 / the number of floats to copy
	 */
	public static void copyInternal(FloatBuffer buf, int fromPos, int toPos, int length) {
		float[] data = new float[length];
		buf.position(fromPos);
		buf.get(data);
		buf.position(toPos);
		buf.put(data);
	}

	/**
	 * 深拷贝 FloatBuffer 内容（独立副本，变更不互相反映）。
	 * Creates a separate FloatBuffer with the same contents; use Buffer.duplicate() to share changes.
	 *
	 * @param buf 要拷贝的 FloatBuffer / the FloatBuffer to copy
	 * @return 拷贝结果，buf 为 null 时返回 null / the copy, or null if buf is null
	 */
	public static FloatBuffer clone(FloatBuffer buf) {
		if (buf == null) {
			return null;
		}
		buf.rewind();

		FloatBuffer copy;
		if (buf.isDirect()) {
			copy = createFloatBuffer(buf.limit());
		} else {
			copy = FloatBuffer.allocate(buf.limit());
		}
		copy.put(buf);

		return copy;
	}

	//// -- 通用 INT 例程 / GENERAL INT ROUTINES -- ////

	/**
	 * 创建指定容量的直接 IntBuffer（本地字节序）。
	 * Create a direct IntBuffer of the specified size (native byte order).
	 *
	 * @param size 需要存储的 int 数量 / required number of ints to store
	 * @return 新生成的 IntBuffer / the new IntBuffer
	 */
	public static IntBuffer createIntBuffer(int size) {
		IntBuffer buf = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder()).asIntBuffer();
		buf.clear();
		if (trackDirectMemory) {
			trackingHash.put(buf, ref);
		}
		return buf;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建指定容量的 IntBuffer。
	 * Reuse the given IntBuffer if sized correctly; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param size 需要容纳的 int 数量 / number of ints to hold
	 * @return 合适大小的 IntBuffer / the requested IntBuffer
	 */
	public static IntBuffer createIntBuffer(IntBuffer buf, int size) {
		if (buf != null && buf.limit() == size) {
			buf.rewind();
			return buf;
		}
		buf = createIntBuffer(size);
		return buf;
	}

	/**
	 * 深拷贝 IntBuffer 内容（独立副本，变更不互相反映）。
	 * Creates a separate IntBuffer with the same contents; use Buffer.duplicate() to share changes.
	 *
	 * @param buf 要拷贝的 IntBuffer / the IntBuffer to copy
	 * @return 拷贝结果，buf 为 null 时返回 null / the copy, or null if buf is null
	 */
	public static IntBuffer clone(IntBuffer buf) {
		if (buf == null) {
			return null;
		}
		buf.rewind();

		IntBuffer copy;
		if (buf.isDirect()) {
			copy = createIntBuffer(buf.limit());
		} else {
			copy = IntBuffer.allocate(buf.limit());
		}
		copy.put(buf);
		return copy;
	}

	//// -- 通用 BYTE 例程 / GENERAL BYTE ROUTINES -- ////

	/**
	 * 创建指定容量的直接 ByteBuffer（本地字节序）。
	 * Create a direct ByteBuffer of the specified size (native byte order).
	 *
	 * @param size 需要存储的字节数 / required number of bytes to store
	 * @return 新生成的 ByteBuffer / the new ByteBuffer
	 */
	public static ByteBuffer createByteBuffer(int size) {
		ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		buf.clear();
		if (trackDirectMemory) {
			trackingHash.put(buf, ref);
		}
		return buf;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建指定容量的 ByteBuffer。
	 * Reuse the given ByteBuffer if sized correctly; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param size 需要容纳的字节数 / number of bytes to hold
	 * @return 合适大小的 ByteBuffer / the requested ByteBuffer
	 */
	public static ByteBuffer createByteBuffer(ByteBuffer buf, int size) {
		if (buf != null && buf.limit() == size) {
			buf.rewind();
			return buf;
		}
		buf = createByteBuffer(size);
		return buf;
	}

	/**
	 * 用 byte 数组生成 ByteBuffer。
	 * Generate a ByteBuffer from the given byte array.
	 *
	 * @param data 要写入的字节数据 / byte data to place into a new ByteBuffer
	 * @return 新生成的 ByteBuffer / the new ByteBuffer
	 */
	public static ByteBuffer createByteBuffer(byte... data) {
		ByteBuffer bb = createByteBuffer(data.length);
		bb.put(data);
		bb.flip();
		return bb;
	}

	/**
	 * 用字符串默认编码字节生成 ByteBuffer。
	 * Generate a ByteBuffer from the string's default-charset bytes.
	 *
	 * @param data 源字符串 / source string
	 * @return 新生成的 ByteBuffer / the new ByteBuffer
	 */
	public static ByteBuffer createByteBuffer(String data) {
		byte[] bytes = data.getBytes();
		ByteBuffer bb = createByteBuffer(bytes.length);
		bb.put(bytes);
		bb.flip();
		return bb;
	}

	/**
	 * 深拷贝 ByteBuffer 内容（独立副本，变更不互相反映）。
	 * Creates a separate ByteBuffer with the same contents; use Buffer.duplicate() to share changes.
	 *
	 * @param buf 要拷贝的 ByteBuffer / the ByteBuffer to copy
	 * @return 拷贝结果，buf 为 null 时返回 null / the copy, or null if buf is null
	 */
	public static ByteBuffer clone(ByteBuffer buf) {
		if (buf == null) {
			return null;
		}
		buf.rewind();

		ByteBuffer copy;
		if (buf.isDirect()) {
			copy = createByteBuffer(buf.limit());
		} else {
			copy = ByteBuffer.allocate(buf.limit());
		}
		copy.put(buf);
		return copy;
	}

	//// -- 通用 SHORT 例程 / GENERAL SHORT ROUTINES -- ////

	/**
	 * 创建指定容量的直接 ShortBuffer（本地字节序）。
	 * Create a direct ShortBuffer of the specified size (native byte order).
	 *
	 * @param size 需要存储的 short 数量 / required number of shorts to store
	 * @return 新生成的 ShortBuffer / the new ShortBuffer
	 */
	public static ShortBuffer createShortBuffer(int size) {
		ShortBuffer buf = ByteBuffer.allocateDirect(2 * size).order(ByteOrder.nativeOrder()).asShortBuffer();
		buf.clear();
		if (trackDirectMemory) {
			trackingHash.put(buf, ref);
		}
		return buf;
	}

	/**
	 * 若已有缓冲大小合适则回绕复用，否则新建指定容量的 ShortBuffer。
	 * Reuse the given ShortBuffer if sized correctly; otherwise create a new one.
	 *
	 * @param buf 先检查并回绕的缓冲 / buffer to first check and rewind
	 * @param size 需要容纳的 short 数量 / number of shorts to hold
	 * @return 合适大小的 ShortBuffer / the requested ShortBuffer
	 */
	public static ShortBuffer createShortBuffer(ShortBuffer buf, int size) {
		if (buf != null && buf.limit() == size) {
			buf.rewind();
			return buf;
		}
		buf = createShortBuffer(size);
		return buf;
	}

	/**
	 * 用 short 数组生成 ShortBuffer。
	 * Generate a ShortBuffer from the given short array.
	 *
	 * @param data 要写入的 short 数组 / array of shorts to place into a new ShortBuffer
	 * @return 新的 ShortBuffer，data 为 null 时返回 null / new ShortBuffer, or null if data is null
	 */
	public static ShortBuffer createShortBuffer(short... data) {
		if (data == null) {
			return null;
		}
		ShortBuffer buff = createShortBuffer(data.length);
		buff.clear();
		buff.put(data);
		buff.flip();
		return buff;
	}

	/**
	 * 深拷贝 ShortBuffer 内容（独立副本，变更不互相反映）。
	 * Creates a separate ShortBuffer with the same contents; use Buffer.duplicate() to share changes.
	 *
	 * @param buf 要拷贝的 ShortBuffer / the ShortBuffer to copy
	 * @return 拷贝结果，buf 为 null 时返回 null / the copy, or null if buf is null
	 */
	public static ShortBuffer clone(ShortBuffer buf) {
		if (buf == null) {
			return null;
		}
		buf.rewind();

		ShortBuffer copy;
		if (buf.isDirect()) {
			copy = createShortBuffer(buf.limit());
		} else {
			copy = ShortBuffer.allocate(buf.limit());
		}
		copy.put(buf);
		return copy;
	}

	/**
	 * 确保 FloatBuffer 当前位置之后至少还有 required 个空位，不足则扩容并拷贝。
	 * Ensures at least the required number of entries remain after the current position; grows if needed.
	 *
	 * @param buffer 待检查 / 拷贝的缓冲，可为 null / buffer that should be checked/copied (may be null)
	 * @param required 返回缓冲中至少应剩余的元素数 / minimum remaining elements required
	 * @return 足够大的缓冲，位置与输入一致，非 null / buffer large enough with same position, never null
	 */
	public static FloatBuffer ensureLargeEnough(FloatBuffer buffer, int required) {
		if (buffer == null || (buffer.remaining() < required)) {
			int position = (buffer != null ? buffer.position() : 0);
			FloatBuffer newVerts = createFloatBuffer(position + required);
			if (buffer != null) {
				buffer.rewind();
				newVerts.put(buffer);
				newVerts.position(position);
			}
			buffer = newVerts;
		}
		return buffer;
	}

	/**
	 * 确保 ShortBuffer 当前位置之后至少还有 required 个空位，不足则扩容并拷贝。
	 * Ensures at least the required number of entries remain after the current position; grows if needed.
	 *
	 * @param buffer 待检查 / 拷贝的缓冲，可为 null / buffer that should be checked/copied (may be null)
	 * @param required 返回缓冲中至少应剩余的元素数 / minimum remaining elements required
	 * @return 足够大的缓冲，位置与输入一致，非 null / buffer large enough with same position, never null
	 */
	public static ShortBuffer ensureLargeEnough(ShortBuffer buffer, int required) {
		if (buffer == null || (buffer.remaining() < required)) {
			int position = (buffer != null ? buffer.position() : 0);
			ShortBuffer newVerts = createShortBuffer(position + required);
			if (buffer != null) {
				buffer.rewind();
				newVerts.put(buffer);
				newVerts.position(position);
			}
			buffer = newVerts;
		}
		return buffer;
	}

	/**
	 * 确保 ByteBuffer 当前位置之后至少还有 required 个空位，不足则扩容并拷贝。
	 * Ensures at least the required number of entries remain after the current position; grows if needed.
	 *
	 * @param buffer 待检查 / 拷贝的缓冲，可为 null / buffer that should be checked/copied (may be null)
	 * @param required 返回缓冲中至少应剩余的元素数 / minimum remaining elements required
	 * @return 足够大的缓冲，位置与输入一致，非 null / buffer large enough with same position, never null
	 */
	public static ByteBuffer ensureLargeEnough(ByteBuffer buffer, int required) {
		if (buffer == null || (buffer.remaining() < required)) {
			int position = (buffer != null ? buffer.position() : 0);
			ByteBuffer newVerts = createByteBuffer(position + required);
			if (buffer != null) {
				buffer.rewind();
				newVerts.put(buffer);
				newVerts.position(position);
			}
			buffer = newVerts;
		}
		return buffer;
	}

	/**
	 * 统计并输出当前直接内存与堆内存占用情况。
	 * Summarizes and reports current direct-memory and heap usage of tracked buffers.
	 *
	 * @param store 结果写入的 StringBuilder；为 null 时新建并经 log 输出 / StringBuilder to append into; if null, builds one and logs it
	 */
	public static void printCurrentDirectMemory(StringBuilder store) {
		long totalHeld = 0;
		// 新建集合保存键，避免并发问题。 / make a new set to hold the keys to prevent concurrency issues.
		ArrayList<Buffer> bufs = new ArrayList<Buffer>(trackingHash.keySet());
		int fBufs = 0, bBufs = 0, iBufs = 0, sBufs = 0, dBufs = 0;
		int fBufsM = 0, bBufsM = 0, iBufsM = 0, sBufsM = 0, dBufsM = 0;
		for (Buffer b : bufs) {
			if (b instanceof ByteBuffer) {
				totalHeld += b.capacity();
				bBufsM += b.capacity();
				bBufs++;
			} else if (b instanceof FloatBuffer) {
				totalHeld += b.capacity() * 4;
				fBufsM += b.capacity() * 4;
				fBufs++;
			} else if (b instanceof IntBuffer) {
				totalHeld += b.capacity() * 4;
				iBufsM += b.capacity() * 4;
				iBufs++;
			} else if (b instanceof ShortBuffer) {
				totalHeld += b.capacity() * 2;
				sBufsM += b.capacity() * 2;
				sBufs++;
			} else if (b instanceof DoubleBuffer) {
				totalHeld += b.capacity() * 8;
				dBufsM += b.capacity() * 8;
				dBufs++;
			}
		}
		long heapMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		boolean printStout = store == null;
		if (store == null) {
			store = new StringBuilder();
		}
		store.append("Existing buffers: ").append(bufs.size()).append("\n");
		store.append("(b: ").append(bBufs).append("  f: ").append(fBufs).append("  i: ").append(iBufs).append("  s: ")
				.append(sBufs).append("  d: ").append(dBufs).append(")").append("\n");
		store.append("Total   heap memory held: ").append(heapMem / 1024).append("kb\n");
		store.append("Total direct memory held: ").append(totalHeld / 1024).append("kb\n");
		store.append("(b: ").append(bBufsM / 1024).append("kb  f: ").append(fBufsM / 1024).append("kb  i: ")
				.append(iBufsM / 1024).append("kb  s: ").append(sBufsM / 1024).append("kb  d: ").append(dBufsM / 1024)
				.append("kb)").append("\n");
		if (printStout) {
			log.info(I18n.get("log.bf21a9e8fbc5", store));
		}
	}
}
