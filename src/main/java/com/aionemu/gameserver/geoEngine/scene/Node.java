package com.aionemu.gameserver.geoEngine.scene;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

import lombok.extern.slf4j.Slf4j;

/**
 * 场景图内部节点：维护子节点集合，并将子包围体合并以便快速剔除。
 * Internal scene-graph node that maintains children and merges their bounds for fast culling.
 * <p>
 * 可挂载任意数量的子节点。
 * A node may have any number of children attached.
 *
 * @author Mark Powell
 * @author Gregg Patton
 * @author Joshua Slack
 */
@Slf4j
public class Node extends Spatial implements Cloneable {

	/**
	 * 子节点列表。
	 * This node's children.
	 */
	protected ArrayList<Spatial> children = new ArrayList<Spatial>(1);
	/** 碰撞标志。 / Collision flags. */
	protected short collisionFlags;

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public Node() {
	}

	/**
	 * 以给定名称构造空子列表节点，碰撞标志默认为 ALL。
	 * Constructs a node with the given name, empty children, and ALL collision flags.
	 *
	 * @param name 场景元素名称，用于标识与比较 / name of the scene element for identification and comparison
	 */
	public Node(String name) {
		super(name);
		collisionFlags = CollisionIntention.ALL.getId();
	}

	/**
	 * 返回维护的子节点数量。
	 * Returns the number of children this node maintains.
	 *
	 * @return 子节点数量 / child count
	 */
	public int getQuantity() {
		return children.size();
	}

	/**
	 * 返回本分支下所有几何中的三角形总数。
	 * Returns the number of triangles contained in all geometry sub-branches of this node.
	 *
	 * @return 三角形总数 / triangle count of this branch
	 */
	@Override
	public int getTriangleCount() {
		int count = 0;
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				count += children.get(i).getTriangleCount();
			}
		}
		return count;
	}

	/**
	 * 返回本分支下所有几何中的顶点总数。
	 * Returns the number of vertices contained in all geometry sub-branches of this node.
	 *
	 * vertex count of this branch
	 */
	@Override
	public int getVertexCount() {
		int count = 0;
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				count += children.get(i).getVertexCount();
			}
		}

		return count;
	}

	/**
	 * 挂载子节点；本节点成为其父。若子节点原有父节点则先从其父上拆下。
	 * Attaches a child; this node becomes its parent. If the child already had a parent it is detached first.
	 *
	 * @param child 待挂载子节点 / child to attach
	 * @return 挂载后的子节点数量 / number of children maintained after attach
	 * if child is null。 / if child is null.
	 */
	public int attachChild(Spatial child) {
		if (child == null) {
			throw new NullPointerException();
		}

		if (child.getParent() != this && child != this) {
			if (child.getParent() != null) {
				child.getParent().detachChild(child);
			}
			child.setParent(this);
			children.add(child);
		}
		return children.size();
	}

	/**
	 * 在指定索引处挂载子节点；本节点成为其父。若子节点原有父节点则先从其父上拆下。
	 * Attaches a child at an index; this node becomes its parent. If the child already had a parent it is detached first.
	 *
	 * @param child 待挂载子节点 / child to attach
	 * @param index 插入索引 / insert index
	 * @return 挂载后的子节点数量 / number of children maintained after attach
	 * if child is null。 / if child is null.
	 */
	public int attachChildAt(Spatial child, int index) {
		if (child == null) {
			throw new NullPointerException();
		}

		if (child.getParent() != this && child != this) {
			if (child.getParent() != null) {
				child.getParent().detachChild(child);
			}
			child.setParent(this);
			children.add(index, child);
		}
		return children.size();
	}

	/**
	 * 从子列表移除给定子节点。
	 * Removes the given child from this node's list.
	 *
	 * @param child 待移除子节点 / child to remove
	 * @return 子节点原索引；不在列表中则为 -1 / former index, or -1 if not present
	 */
	public int detachChild(Spatial child) {
		if (child == null) {
			throw new NullPointerException();
		}

		if (child.getParent() == this) {
			int index = children.indexOf(child);
			if (index != -1) {
				detachChildAt(index);
			}
			return index;
		}
		return -1;
	}

	/**
	 * 按名称移除第一个匹配的子节点。
	 * Removes the first child whose name matches.
	 *
	 * @param childName 子节点名称 / child name
	 * @return 子节点原索引；未找到则为 -1 / former index, or -1 if not found
	 */
	public int detachChildNamed(String childName) {
		if (childName == null) {
			throw new NullPointerException();
		}

		for (int x = 0, max = children.size(); x < max; x++) {
			Spatial child = children.get(x);
			if (childName.equals(child.getName())) {
				detachChildAt(x);
				return x;
			}
		}
		return -1;
	}

	/**
	 * 移除指定索引处的子节点并返回该节点。
	 * Removes the child at the given index and returns it.
	 *
	 * @param index 子节点索引 / child index
	 * @return 被移除的子节点 / removed child
	 */
	public Spatial detachChildAt(int index) {
		Spatial child = children.remove(index);
		if (child != null) {
			child.setParent(null);
		}
		return child;
	}

	/**
	 * 移除全部子节点。
	 * Removes all children attached to this node.
	 */
	public void detachAllChildren() {
		for (int i = children.size() - 1; i >= 0; i--) {
			detachChildAt(i);
		}
		log.info(I18n.get("log.12859bc60155"));
	}

	/**
	 * 返回给定子节点的索引。
	 * Returns the index of the given child.
	 *
	 * @param sp 子节点 / child spatial
	 * @return 索引；不存在则为 -1 / index, or -1 if absent
	 */
	public int getChildIndex(Spatial sp) {
		return children.indexOf(sp);
	}

	/**
	 * 交换两个索引处的子节点（比先拆后挂更高效，无需额外更新）。
	 * Swaps children at two indices (more efficient than detach/attach; no extra updates needed).
	 *
	 * @param index1 第一个索引 / first index
	 * @param index2 第二个索引 / second index
	 */
	public void swapChildren(int index1, int index2) {
		Spatial c2 = children.get(index2);
		Spatial c1 = children.remove(index1);
		children.add(index1, c2);
		children.remove(index2);
		children.add(index2, c1);
	}

	/**
	 * 返回指定索引处的子节点。
	 * Returns the child at the given index.
	 *
	 * @param i 索引 / index
	 * child at the index
	 */
	public Spatial getChild(int i) {
		return children.get(i);
	}

	/**
	 * 按精确名称（区分大小写）查找第一个匹配的子节点，递归进入子 Node。
	 * Returns the first child with exactly the given name (case sensitive), recursing into child nodes.
	 *
	 * @param name 子节点名称；null 时返回 null / child name; null yields null
	 * @return 找到的子节点，或 null / child if found, or null
	 */
	public Spatial getChild(String name) {
		if (name == null) {
			return null;
		}

		for (int x = 0, cSize = getQuantity(); x < cSize; x++) {
			Spatial child = children.get(x);
			if (name.equals(child.getName())) {
				return child;
			} else if (child instanceof Node) {
				Spatial out = ((Node) child).getChild(name);
				if (out != null) {
					return out;
				}
			}
		}
		return null;
	}

	/**
	 * 判断给定 Spatial 是否在本节点子树中。
	 * Determines whether the provided spatial is contained in this node's children (recursively).
	 *
	 * @param spat 待查找的子对象 / child object to look for
	 * @return 若 contained 则为 true / true if contained
	 */
	public boolean hasChild(Spatial spat) {
		if (children.contains(spat)) {
			return true;
		}

		for (int i = 0, max = getQuantity(); i < max; i++) {
			Spatial child = children.get(i);
			if (child instanceof Node && ((Node) child).hasChild(spat)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回全部子节点列表。
	 * Returns all children of this node.
	 *
	 * @return 子节点列表 / list of all children
	 */
	public List<Spatial> getChildren() {
		return children;
	}

	/**
	 * 子几何变更时向上传递给父节点。
	 * Propagates a child-geometry change to the parent.
	 *
	 * related geometry
	 * index 1
	 * index 2
	 */
	public void childChange(Geometry geometry, int index1, int index2) {
		// 仅传递给父级 / just pass to parent
		if (parent != null) {
			parent.childChange(geometry, index1, index2);
		}
	}

	/**
	 * 按意图与包围体过滤后，对子节点递归碰撞检测。
	 * After intention/bound filtering, recursively collides children.
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * total collisions
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if ((getIntentions() & results.getIntentions()) == 0) {
			return 0;
		}

		if (other instanceof Ray) {
			if (worldBound == null || !worldBound.intersects(((Ray) other))) {
				return 0;
			}
		}

		int total = 0;
		for (int i = 0; i < children.size(); i++) {
			Spatial child = children.get(i);
			if (child instanceof Geometry) {

				// 未使用的 materialId 未设置材质碰撞意图 / not used materialIds do not have collision intention for materials set
				// 并非所有材质网格都设置了物理碰撞 / not all material meshes have physical collisions set
				if ((child.getIntentions() & results.getIntentions()) == 0) {
					continue;
				}
				if ((results.getIntentions() & CollisionIntention.MATERIAL.getId()) != 0
						&& child.getMaterialId() <= 0) {
					continue;
				}
			}
			total += child.collideWith(other, results);
			if (total > 0 && results.isOnlyFirst()) {
				break;
			}
		}
		return total;
	}

	/**
	 * 返回实现指定类且名称匹配正则的后代 Spatial 扁平列表（不含自身）。
	 * Returns a flat list of descendant Spatials implementing the class and matching the name pattern (self excluded).
	 * <p>
	 * 正则为整串匹配；可用 (?X) 模式。按设计可安全用于 for-each。
	 * The pattern is a full match; (?X) modes are allowed. Safe for for-each by design.
	 *
	 * @param spatialSubclass 必须实现的子类；null 表示任意 / required subclass; null matches all
	 * @param nameRegex 名称正则；null 表示任意名称 / name regex; null matches all names
	 * @return 非 null 列表（可能为空） / non-null, possibly empty list of matches
	 * @see java.util.regex.Pattern
	 * @see Spatial#matches(Class, String)
	 */
	@SuppressWarnings("unchecked")
	public <T extends Spatial> List<T> descendantMatches(Class<T> spatialSubclass, String nameRegex) {
		List<T> newList = new ArrayList<T>();
		if (getQuantity() < 1) {
			return newList;
		}
		for (int i = 0; i < children.size(); i++) {
			Spatial child = children.get(i);
			if (child.matches(spatialSubclass, nameRegex)) {
				newList.add((T) child);
			}
			if (child instanceof Node) {
				newList.addAll(((Node) child).descendantMatches(spatialSubclass, nameRegex));
			}
		}
		return newList;
	}

	/**
	 * 按子类筛选后代的便捷重载。
	 * Convenience overload filtering descendants by subclass only.
	 *
	 * @param spatialSubclass 必须实现的子类 / required subclass
	 * matching list
	 * @see #descendantMatches(Class, String)
	 */
	public <T extends Spatial> List<T> descendantMatches(Class<T> spatialSubclass) {
		return descendantMatches(spatialSubclass, null);
	}

	/**
	 * 按名称正则筛选后代的便捷重载。
	 * Convenience overload filtering descendants by name regex only.
	 *
	 * name regex
	 * matching list
	 * @see #descendantMatches(Class, String)
	 */
	public <T extends Spatial> List<T> descendantMatches(String nameRegex) {
		return descendantMatches(null, nameRegex);
	}

	/**
	 * 将模型包围体克隆后下发到所有子节点。
	 * Clones the model bound and assigns it to all children.
	 *
	 * @param modelBound 模型包围体；null 表示清空 / model bound; null clears
	 */
	@Override
	public void setModelBound(BoundingVolume modelBound) {
		if (children != null) {
			for (int i = 0, max = children.size(); i < max; i++) {
				children.get(i).setModelBound(modelBound != null ? modelBound.clone(null) : null);
			}
		}
	}

	/**
	 * 更新子节点包围体并合并为本节点世界包围体。
	 * Updates child bounds and merges them into this node's world bound.
	 */
	@Override
	public void updateModelBound() {
		BoundingVolume resultBound = null;
		if (children != null) {
			for (int i = 0, max = children.size(); i < max; i++) {
				Spatial child = children.get(i);
				child.updateModelBound();
				if (resultBound != null) {
					// 合并当前世界边界与子世界边界 / merge current world bound with child world bound
					resultBound.mergeLocal(child.getWorldBound());
				} else {
					// 将世界边界设为第一个非空子世界边界 / set world bound to first non-null child world bound
					if (child.getWorldBound() != null) {
						resultBound = child.getWorldBound().clone(this.worldBound);
					}
				}
			}
		}
		this.worldBound = resultBound;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * aionjHungary.geoEngine.scene.Spatial#setTransform(aionjHungary.geoEngine.math
	 * .Matrix3f, aionjHungary.geoEngine.math.Vector3f)
	 */
	/**
	 * 将均匀缩放变换下发到所有子节点。
	 * Propagates a uniform-scale transform to all children.
	 *
	 * rotation
	 * translation
	 * @param scale 均匀缩放 / uniform scale
	 */
	@Override
	public void setTransform(Matrix3f rotation, Vector3f loc, float scale) {
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				children.get(i).setTransform(rotation, loc, scale);
			}
		}
	}

	/**
	 * 将非均匀缩放变换下发到所有子节点。
	 * Propagates a non-uniform-scale transform to all children.
	 *
	 * rotation
	 * translation
	 * @param scale 各轴缩放 / per-axis scale
	 */
	@Override
	public void setTransform(Matrix3f rotation, Vector3f loc, Vector3f scale) {
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				children.get(i).setTransform(rotation, loc, scale);
			}
		}
	}

	/**
	 * 克隆本节点及其子树（Geometry 共享原 Mesh 引用）。
	 * Clones this node and its subtree (Geometry shares the original Mesh reference).
	 *
	 * cloned node
	 */
	@Override
	public Node clone() throws CloneNotSupportedException {
		Node node = new Node(name);
		node.collisionFlags = collisionFlags;
		for (Spatial spatial : children) {
			if (spatial instanceof Geometry) {
				Geometry geom = new Geometry(spatial.getName(), ((Geometry) spatial).getMesh());
				node.attachChild(geom);
			} else if (spatial instanceof Node) {
				node.attachChild(((Node) (spatial)).clone());
			}
		}
		return node;
	}

	/**
	 * 返回碰撞标志。
	 * Returns collision flags.
	 *
	 * collision flags
	 */
	@Override
	public short getCollisionFlags() {
		return collisionFlags;
	}

	/**
	 * 设置碰撞标志。
	 * Sets collision flags.
	 *
	 * @param flags 碰撞标志 / collision flags
	 */
	@Override
	public void setCollisionFlags(short flags) {
		collisionFlags = flags;
	}
}
