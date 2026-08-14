package com.aionemu.gameserver.geoEngine.scene;

/**
 * 描述一个 GL 对象：对图形库原生侧某类对象的封装，用于跟踪其生命周期与更新状态。
 * Describes a GL object: encapsulation of a native graphics-library object, used to track its lifecycle and update state.
 */
public abstract class GLObject implements Cloneable {

	/**
	 * 对象 ID，通常取决于类型；典型来自 glGenTextures、glGenBuffers 等调用。
	 * Object ID, usually type-dependent; typically returned from calls such as glGenTextures or glGenBuffers.
	 */
	protected int id = -1;
	/**
	 * 硬引用句柄。通过硬引用可在 GL 对象不再使用时发现并删除图形库中的实例。
	 * Hard-reference handle. By hard-referencing an object it is possible to detect when a GLObject is unused and delete its native instance.
	 */
	protected Object handleRef = null;
	/**
	 * 若为 true，表示数据已变更，使用前需要更新。
	 * True if the data represented by this GLObject has changed and needs to be updated before use.
	 */
	protected boolean updateNeeded = true;
	/**
	 * GL 对象类型，通常由子类指定。
	 * Type of the GLObject, usually specified by a subclass.
	 */
	protected final Type type;

	/**
	 * GL 对象类型枚举。
	 * GL object type enumeration.
	 */
	public static enum Type {

		/**
		 * 顶点缓冲，用于描述几何数据及其属性。
		 * Vertex buffers describe geometry data and its attributes.
		 */
		VertexBuffer,
		/**
		 * 着色器源码，控制渲染管线某一阶段输出（如顶点位置或片元颜色）。
		 * Shader source code controlling the output of a rendering-pipeline stage (e.g. vertex position or fragment color).
		 */
		ShaderSource,
		/**
		 * 着色器，由多个 ShaderSource 聚合，共同控制顶点与片元处理器。
		 * A shader is an aggregation of ShaderSources that together control the vertex and fragment processors.
		 */
		Shader,
	}

	/**
	 * 按类型构造，并分配句柄引用。
	 * Constructs by type and allocates a handle reference.
	 *
	 * @param type GL 对象类型 / GL object type
	 */
	public GLObject(Type type) {
		this.type = type;
		this.handleRef = new Object();
	}

	/**
	 * 受保护构造：不分配句柄引用，供子类 createDestructableClone() 使用。
	 * Protected constructor that does not allocate a handle ref; used by subclasses for createDestructableClone().
	 *
	 * @param type GL 对象类型 / GL object type
	 * @param id 已有对象 ID / existing object ID
	 */
	protected GLObject(Type type, int id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * 设置 GL 对象 ID。由渲染器使用，用户代码通常不应调用。
	 * Sets the ID of the GLObject. Used by the renderer; must not be called by user code in most cases.
	 *
	 * @param id 要设置的 ID / ID to set
	 */
	public void setId(int id) {
		if (this.id != -1) {
			throw new IllegalStateException("ID has already been set for this GL object.");
		}

		this.id = id;
	}

	/**
	 * 返回对象 ID。多数情况下用户代码不应依赖此值。
	 * Returns the object ID. Should not be used by user code in most cases.
	 *
	 * @return 对象 ID / object ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * 标记为需要更新。
	 * Marks this object as needing an update.
	 */
	public void setUpdateNeeded() {
		updateNeeded = true;
	}

	/**
	 * 清除“需要更新”标记。
	 * Clears the update-needed flag.
	 */
	public void clearUpdateNeeded() {
		updateNeeded = false;
	}

	/**
	 * 是否需要更新。
	 * Whether an update is needed.
	 *
	 * @return 需要更新则为 true / true if update is needed
	 */
	public boolean isUpdateNeeded() {
		return updateNeeded;
	}

	@Override
	public String toString() {
		return type.name() + " " + Integer.toHexString(hashCode());
	}

	/**
	 * 创建深拷贝。浅拷贝请使用 createDestructableClone()。
	 * Creates a deep clone. For a shallow clone, use createDestructableClone().
	 *
	 * @return 深拷贝实例 / deep clone instance
	 */
	@Override
	protected GLObject clone() {
		try {
			GLObject obj = (GLObject) super.clone();
			obj.handleRef = new Object();
			obj.id = -1;
			obj.updateNeeded = true;
			return obj;
		} catch (CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	// @Override
	// public boolean equals(Object other){
	// if (this == other)
	// return true;
	// if (!(other instanceof GLObject))
	// return false;
	//
	// }
	// 仅供对象管理器使用的专用调用。 / Specialized calls to be used by object manager only.

	/**
	 * GL 上下文重启时重置所有 ID，避免显示重启后出现“白贴图”等问题。
	 * Called when the GL context is restarted to reset all IDs. Prevents issues such as "white textures" after a display restart.
	 */
	public abstract void resetObject();

	/**
	 * 创建本 GL 对象的浅拷贝；对该拷贝调用 deleteObject 应仍可用。
	 * Creates a shallow clone of this GL object. The deleteObject method should remain functional for this clone.
	 *
	 * @return 可销毁的浅拷贝 / destructable shallow clone
	 */
	public abstract GLObject createDestructableClone();
}
