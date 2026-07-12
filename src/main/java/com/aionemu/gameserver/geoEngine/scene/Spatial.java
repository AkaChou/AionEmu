package com.aionemu.gameserver.geoEngine.scene;

import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 场景图节点基类，维护父子关系、本地/世界变换相关状态，以及包围体与碰撞标志。
 * Base class for scene-graph nodes; maintains parent links, transform-related state, bounds and collision flags.
 * <p>
 * {@link Node} 与 {@link Geometry} 等均是其子类。
 * All other nodes such as {@link Node} and {@link Geometry} are subclasses of {@code Spatial}.
 *
 * @author Mark Powell
 * @author Joshua Slack
 * @author Rolandas - added materials
 * @version $Revision: 4075 $, $Data$
 */
public abstract class Spatial implements Collidable, Cloneable {

	/**
	 * 视锥剔除提示。
	 * View-frustum cull hint.
	 */
	public enum CullHint {

		/**
		 * 继承父节点策略；无父时默认为动态剔除。
		 * Do whatever the parent does. If no parent, default to dynamic.
		 */
		Inherit,
		/**
		 * 不完全在渲染相机视锥内时不绘制。
		 * Do not draw if not at least partially within the renderer's camera frustum.
		 */
		Dynamic,
		/**
		 * 始终从视图中剔除。
		 * Always cull this from view.
		 */
		Always,
		/**
		 * 永不主动剔除；若父节点被剔除仍会连带剔除。
		 * Never cull this from view. Note it is still culled if the parent is culled.
		 */
		Never;
	}

	/**
	 * 相对世界空间的包围体。
	 * Spatial's bounding volume relative to the world.
	 */
	protected BoundingVolume worldBound;
	/**
	 * 空间节点名称。
	 * This spatial's name.
	 */
	protected String name;
	/**
	 * 父节点；无父则为 null。
	 * Spatial's parent, or null if it has none.
	 */
	protected transient Node parent;

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public Spatial() {
	}

	/**
	 * 以给定名称构造空间节点（旋转/平移/缩放为默认值）。
	 * Constructs a spatial with the given name (rotation/translation/scale at defaults).
	 *
	 * @param name 场景元素名称，用于标识与比较 / name of the scene element for identification and comparison
	 */
	public Spatial(String name) {
		this();
		if (name != null) {
			this.name = name.intern();
		}
	}

	/**
	 * 设置名称（非 null 时 intern）。
	 * Sets the name (interned when non-null).
	 *
	 * spatial's new name
	 */
	public void setName(String name) {
		if (name != null) {
			this.name = name.intern();
		}
	}

	/**
	 * 返回名称。
	 * Returns the name of this spatial.
	 *
	 * this spatial's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 返回父节点；为 null 时表示根节点。
	 * Retrieves this node's parent. If null, this is a root node.
	 *
	 * parent of this node
	 */
	public Node getParent() {
		return parent;
	}

	/**
	 * {@link Node#detachChild(Spatial)} 调用，请勿直接调用。
	 * Called by {@link Node#attachChild(Spatial)} and {@link Node#detachChild(Spatial)} — do not call directly.
	 *
	 * parent of this node
	 */
	protected void setParent(Node parent) {
		this.parent = parent;
	}

	/**
	 * 从父节点上移除自身。
	 * Removes this spatial from its parent.
	 *
	 * @return 若存在父节点并完成移除则为 true / true if it had a parent and the remove was performed
	 */
	public boolean removeFromParent() {
		if (parent != null) {
			parent.detachChild(this);
			return true;
		}
		return false;
	}

	/**
	 * 判断给定节点是否为自身的祖先（父、祖父等）。
	 * Determines whether the provided node is a parent, grandparent, etc. of this spatial.
	 *
	 * @param ancestor 待查找的祖先 / ancestor object to look for
	 * @return 若 the ancestor is found 则为 true / true if the ancestor is found
	 */
	public boolean hasAncestor(Node ancestor) {
		if (parent == null) {
			return false;
		} else if (parent.equals(ancestor)) {
			return true;
		} else {
			return parent.hasAncestor(ancestor);
		}
	}

	/**
	 * 重新计算本空间节点的包围体。
	 * Recalculates the bounding object for this spatial.
	 */
	public abstract void updateModelBound();

	/**
	 * 设置本空间节点的包围体。
	 * Sets the bounding object for this spatial.
	 *
	 * bounding object for this spatial
	 */
	public abstract void setModelBound(BoundingVolume modelBound);

	/**
	 * 返回本节点下所有顶点数量之和。
	 * Returns the sum of all vertices under this spatial.
	 *
	 * vertex count
	 */
	public abstract int getVertexCount();

	/**
	 * 返回本节点下所有三角形数量之和。
	 * Returns the sum of all triangles under this spatial.
	 *
	 * triangle count
	 */
	public abstract int getTriangleCount();

	/**
	 * 从碰撞标志低 8 位取得材质 ID。
	 * Returns the material id from the low 8 bits of collision flags.
	 *
	 * material id
	 */
	public byte getMaterialId() {
		return (byte) (getCollisionFlags() & 0xFF);
	}

	/**
	 * 从碰撞标志高 8 位取得碰撞意图掩码。
	 * Returns the intention mask from the high 8 bits of collision flags.
	 *
	 * intention mask
	 */
	public byte getIntentions() {
		return (byte) (getCollisionFlags() >> 8);
	}

	/**
	 * 返回碰撞标志。
	 * Returns collision flags.
	 *
	 * collision flags
	 */
	public abstract short getCollisionFlags();

	/**
	 * 设置碰撞标志。
	 * Sets collision flags.
	 *
	 * @param flags 碰撞标志 / collision flags
	 */
	public abstract void setCollisionFlags(short flags);

	/**
	 * 判断本节点是否匹配给定子类与名称正则（整串匹配，可用 (?X) 模式）。
	 * Returns true if this implements the specified class and its name matches the pattern (full match; (?X) modes allowed).
	 *
	 * @param spatialSubclass 必须实现的子类；null 表示任意 Spatial / subclass that must be implemented; null matches all
	 * @param nameRegex 名称正则；null 表示任意名称 / name regex; null matches all names
	 * 若 class and name both match 则为 true / true if class and name both match
	 * @see java.util.regex.Pattern
	 */
	public boolean matches(Class<? extends Spatial> spatialSubclass, String nameRegex) {
		if (spatialSubclass != null && !spatialSubclass.isInstance(this)) {
			return false;
		}

		if (nameRegex != null && (name == null || !name.matches(nameRegex))) {
			return false;
		}
		return true;
	}

	/**
	 * 返回本节点层级上的世界包围体。
	 * Retrieves the world bound at this node level.
	 *
	 * @return 世界包围体 / world bound at this level
	 */
	public BoundingVolume getWorldBound() {
		return worldBound;
	}

	/**
	 * 返回“名称 (简单类名) use 意图”形式的字符串。
	 * Returns a string of the form "name (SimpleClassName) use intentions".
	 *
	 * @return 描述字符串 / descriptive string
	 */
	@Override
	public String toString() {
		return name + " (" + this.getClass().getSimpleName() + ") use " + CollisionIntention.toString(getIntentions());
	}

	/**
	 * 设置变换（旋转、平移、均匀缩放）。
	 * Sets transform from rotation, translation and uniform scale.
	 *
	 * rotation
	 * translation
	 * @param scale 均匀缩放 / uniform scale
	 */
	public abstract void setTransform(Matrix3f rotation, Vector3f loc, float scale);

	/**
	 * 设置变换（旋转、平移、向量缩放；默认取 scale.x 作为均匀缩放）。
	 * Sets transform from rotation, translation and vector scale (default uses scale.x as uniform scale).
	 *
	 * rotation
	 * translation
	 * @param scale 缩放向量 / scale vector
	 */
	public void setTransform(Matrix3f rotation, Vector3f loc, Vector3f scale) {
		setTransform(rotation, loc, scale.x);
	}

	/**
	 * 浅克隆。
	 * Shallow clone.
	 *
	 * clone instance
	 */
	@Override
	public Spatial clone() throws CloneNotSupportedException {
		return (Spatial) super.clone();
	}
}
