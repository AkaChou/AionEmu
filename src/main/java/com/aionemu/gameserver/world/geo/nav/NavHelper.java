package com.aionemu.gameserver.world.geo.nav;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.scene.NavGeometry;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.world.geo.nav.NavService.NavPathway;

/**
 * 基于 A* 变体的导航网格寻路辅助类，在 {@link NavGeometry} 上搜索走廊路径。
 * Pathfinding helper similar to A* that traverses {@link NavGeometry} corridors.
 *
 * @author Yon (Aion Reconstruction Project)
 */
@Slf4j
class NavHelper {


	/**
	 * 目标不在导航网格上时的容差阈值。
	 * 若目标估计距离在此阈值内，路径视为完成，并以直线补全剩余段。
	 * 过大可能导致实体穿墙。
	 * Tolerance used when pathfinding toward a target off the nav mesh.
	 * If the estimated distance is within this value, the path is considered complete
	 * and a straight-line finish is used. Too large a value may clip through geometry.
	 */
	public final static float ARBITRARY_SMALL_VALUE = 5;

	/**
	 * 路径走廊节点数上限；超过则寻路失败。
	 * 过小会使远距寻路失败，过大则可能耗尽内存。
	 * 同时作为防无限循环的操作上限。
	 * Maximum corridor segments; exceeding it fails the pathfinding attempt.
	 * Too small fails long paths; too large may exhaust memory.
	 * Also caps operations to prevent infinite loops from algorithm assumptions.
	 */
	public final static int ARBITRARY_LARGE_VALUE = 800;

	/**
	 * 下一节点远离目标时附加到 pathCost 的百分比权重。
	 * 若从对面顶点指向目标的向量不穿过路径边，则视为远离目标；
	 * 见 {@link NavGeometry#isTowardsEdge(byte, float[])}。
	 * Percentage of pathCost added when the next node moves away from the target.
	 * A node moves away if a vector from the opposite vertex toward the target
	 * does not cross the path edge; see {@link NavGeometry#isTowardsEdge(byte, float[])}.
	 */
	public final static float PATH_WEIGHT = 0.2F;

	/**
	 * 目标距离估计的乘数，使更接近目标的节点优先。
	 * Multiplier for {@link NavHeapNode#targetDist} so closer nodes get higher priority.
	 */
	public final static float TARGET_WEIGHT = 20;

	/**
	 * 堆节点：按 {@link #compareTo(NavHeapNode)} 以最低优先值置于堆顶。
	 * Heap node ordered by {@link #compareTo(NavHeapNode)}, lowest value at the top.
	 *
	 * @author Yon (Aion Reconstruction Project)
	 */
	private class NavHeapNode implements Comparable<NavHeapNode> {

		/**
		 * 是否已被寻路算法展开。
		 * Whether this node has been explored and opened.
		 */
		boolean open = false;

		/**
		 * 本节点对应的导航三角形。
		 * The {@link NavGeometry} this node represents.
		 */
		NavGeometry tile;

		/**
		 * 连到本节点且 pathCost 最短的父节点。
		 * Parent node with the shortest {@link #pathCost} connecting here.
		 */
		NavHeapNode parent;

		/**
		 * 在堆数组中的下标。
		 * Lookup index within the heap array.
		 */
		int heapIndex;

		/**
		 * 路径代价与目标距离估计；二者之和决定堆优先级。
		 * Path cost and target-distance estimate; their sum drives heap priority.
		 */
		float pathCost, targetDist;

		/**
		 * 仅用于路径起点节点的构造。
		 * Constructor used only for the initial starting node.
		 *
		 * @param node 对应的导航三角形 / represented nav geometry
		 */
		NavHeapNode(NavGeometry node) {
			this.tile = node;
			if (tile == endTile) {
				targetDist = 0;
			} else {
				targetDist = node.getPriority(x2, y2, z2) * targetWeight();
			}
		}

		/**
		 * 由父节点展开创建子节点，并估计 pathCost / targetDist。
		 * Creates a child node opened from a parent, estimating pathCost and targetDist.
		 *
		 * @param node 对应的导航三角形 / represented nav geometry
		 * @param parent 展开到本节点的父节点 / parent that opened this node
		 * whether to apply PATH_WEIGHT
		 */
		NavHeapNode(NavGeometry node, NavHeapNode parent, boolean useWeight) {
			this(node);
			this.parent = parent;
			float basePriority = parent.pathCost + parent.tile.getInRad();
			if (useWeight) {
				pathCost = basePriority + basePriority * pathWeight();
			} else {
				pathCost = basePriority;
			}
		}

		/**
		 * 若新父节点 pathCost 更低则接受并更新本节点代价。
		 * Accepts newParent when it has a lower pathCost and updates this node's cost.
		 *
		 * @param newParent 候选父节点 / candidate parent
		 * @param useWeight 接受后是否叠加 PATH_WEIGHT / whether to apply PATH_WEIGHT after accept
		 */
		void checkAndUpdateParent(NavHeapNode newParent, boolean useWeight) {
			assert newParent != null;
			if (parent == null) return;
			for (NavHeapNode ancestor = newParent; ancestor != null; ancestor = ancestor.parent) {
				if (ancestor == this) return;
			}
			if (parent.pathCost > newParent.pathCost) {
				parent = newParent;
				if (useWeight) {
					pathCost = parent.pathCost + parent.tile.getInRad();
					pathCost += pathCost * pathWeight();
					onUpdateNode(this);
				} else {
					pathCost = parent.pathCost + parent.tile.getInRad();
					onUpdateNode(this);
				}
			}
		}

		/**
		 * 返回 pathCost + targetDist 作为整体优先级（越小越高）。
		 * Returns pathCost + targetDist as overall priority (lower is better).
		 *
		 * priority value
		 */
		float getPriority() {
			return pathCost + targetDist;
		}

		/**
		 * 展开本节点：检查三条边连接，新建或更新邻居堆节点。
		 * Opens this node by exploring edge connections and adding/updating neighbor heap nodes.
		 * Sets {@link #open} to true.
		 */
		void open() {
			if (open) return;
			// 检查连接是否属于堆 / Check connections to see if they are part of the heap
			float[] vec = {x2, y2};

			// 这段注释代码反而更糟。 / This commented code made things worse.
//			if (parent != null) {
//				vec = new float[] {x2 - parent.tile.incenter[0], y2 - parent.tile.incenter[1]};
//			} else {
//				vec = new float[] {x2 - x1, y2 - y1};
//			}
			if (tile.getEdge1() != null) if (!contains(tile.getEdge1())) {
				// 若不是，则创建并添加它们 / If they aren't, then create and add them
				NavHeapNode newNode = new NavHeapNode(tile.getEdge1(), this, !tile.isTowardsEdge((byte) 1, vec));
				add(newNode);
			} else {
				// If they are, run checkAndUpdateParent
				NavHeapNode child = getNode(tile.getEdge1());
				if (child != parent) child.checkAndUpdateParent(this, !tile.isTowardsEdge((byte) 1, vec));
			}

			if (tile.getEdge2() != null) if (!contains(tile.getEdge2())) {
				NavHeapNode newNode = new NavHeapNode(tile.getEdge2(), this, !tile.isTowardsEdge((byte) 2, vec));
				add(newNode);
			} else {
				NavHeapNode child = getNode(tile.getEdge2());
				if (child != parent) child.checkAndUpdateParent(this, !tile.isTowardsEdge((byte) 2, vec));
			}

			if (tile.getEdge3() != null) if (!contains(tile.getEdge3())) {
				NavHeapNode newNode = new NavHeapNode(tile.getEdge3(), this, !tile.isTowardsEdge((byte) 3, vec));
				add(newNode);
			} else {
				NavHeapNode child = getNode(tile.getEdge3());
				if (child != parent) child.checkAndUpdateParent(this, !tile.isTowardsEdge((byte) 3, vec));
			}
			open = true;
		}

		/**
		 * 按整体优先级比较；相等时以 targetDist 决胜。
		 * 注意：自然顺序与 equals 不一致。
		 * Compares by overall priority, breaking ties with targetDist.
		 * Note: natural ordering is inconsistent with equals.
		 *
		 * @param other 另一节点 / other node
		 * comparison result
		 */
		@Override
		public int compareTo(NavHeapNode other) {
			float pThis = getPriority();
			float pOther = other.getPriority();
			if (pThis > pOther) return 1;
			if (pThis < pOther) return -1;
			if (targetDist > other.targetDist) return 1;
			if (targetDist < other.targetDist) return -1;
			// 若优先级与目标距离相等，则路径代价也相等。 / If priority is equal, and targetDist is equal, then pathCost is also equal.
//			if (pathCost > other.pathCost) return 1;
//			if (pathCost < other.pathCost) return -1;
			return 0;
		}

	}

	/** 起点坐标分量。 / Starting coordinate components. */
	final float x1, y1, z1;

	/** 终点坐标分量。 / Target coordinate components. */
	final float x2, y2, z2;

	/**
	 * 寻路目标导航三角形。
	 * Target {@link NavGeometry} to pathfind toward.
	 */
	NavGeometry endTile;

	/**
	 * 已探索导航三角形到堆节点的映射（含已出堆节点）。
	 * Map of explored nav geometry to heap nodes (including those removed from the heap).
	 */
	private Map<NavGeometry, NavHeapNode> list;

	/**
	 * 作为堆底层结构的数组；由本类维护顺序，按需扩容。
	 * Underlying heap array maintained by this helper; expanded as needed.
	 */
	private NavHeapNode[] heap;

	/**
	 * 当前堆中元素个数。
	 * Current number of items stored on the heap.
	 */
	private int currentHeapCount = 0;

	/**
	 * 构造就绪的寻路助手，可调用 {@link #createPathway()} 生成走廊。
	 * 用毕请调用 {@link #destroy()}。
	 * Creates a ready-to-run helper that can {@link #createPathway() construct a path}.
	 * Callers should run {@link #destroy()} when done.
	 *
	 * @param startTile 起点三角形，不可为 null / start geometry; must not be null
	 * @param endTile 终点三角形，可为 null / end geometry; may be null
	 * @param x1 起点 X / start x
	 * @param y1 起点 Y / start y
	 * @param z1 起点 Z / start z
	 * @param x2 终点 X / end x
	 * @param y2 终点 Y / end y
	 * @param z2 终点 Z / end z
	 */
	NavHelper(NavGeometry startTile, NavGeometry endTile, float x1, float y1, float z1, float x2, float y2, float z2) {
		assert startTile != null;
		heap = new NavHeapNode[100];
		list = new ConcurrentHashMap<NavGeometry, NavHeapNode>();
		this.endTile = endTile;
		this.x1 = x1; this.y1 = y1; this.z1 = z1;
		this.x2 = x2; this.y2 = y2; this.z2 = z2;
		init(startTile);
	}

	/**
	 * 以起点三角形创建首个堆节点并入堆。
	 * Creates the first heap node from the start geometry and adds it.
	 *
	 * @param tile 起点三角形 / start geometry
	 */
	private void init(NavGeometry tile) {
		NavHeapNode startNode = new NavHeapNode(tile);
		add(startNode);
	}

	/**
	 * 异步清理 list 中节点的 parent 引用，防止内存泄漏。
	 * Schedules cleanup that nulls parent refs in list and clears it to avoid leaks.
	 */
	public void destroy() {
		GameThreadPoolServices.threadPoolManager().executeLongRunning(new Runnable() {
			public void run() {
				for (NavHeapNode node : list.values()) {
					node.parent = null;
				}
				list.clear();
			}
		});
	}

	/**
	 * 构造连接起点到终点的走廊路径；超过节点上限则返回空路径。
	 * Builds a corridor path from start to goal; returns empty pathway if node limit is exceeded.
	 *
	 * path corridor
	 */
	public NavPathway[] createPathway() {
		boolean finished = false;
		int maxNodes = maxNodes();
		if (endTile == null) {
			NavHeapNode current;
			int opCount = 0;
			do {
				current = removeFirst();
				if (current.targetDist < targetThreshold() * targetWeight()) {
					finished = true;
					break;
				}
				current.open();
				if (opCount++ > maxNodes) break;
			} while (currentHeapCount > 0);
			if (finished) return retrace(current);
		} else {
			NavHeapNode current;
			int opCount = 0;
			do {
				current = removeFirst();
				if (current.tile == endTile) {
					finished = true;
					break;
				}
				current.open();
				if (opCount++ > maxNodes) break;
			} while (currentHeapCount > 0);
			if (finished) return retrace(current);
		}
		return new NavPathway[0];
	}

	/**
	 * 沿 parent 回溯构造走廊；节点数超限则返回空路径。
	 * Retraces parents to build the corridor; returns empty pathway if length is exceeded.
	 *
	 * @param node 终点堆节点 / final heap node on the path
	 * path corridor
	 */
	private NavPathway[] retrace(NavHeapNode node) {
		ArrayList<NavPathway> ret = new ArrayList<NavPathway>();
		int corridorLength = corridorLength();
		NavHeapNode child = node;
		NavHeapNode parent = node;
		while (parent.parent != null) {
			parent = parent.parent;
			assert parent.parent == null || parent.parent != child: "Parent of parent node is child node! Infinite Loop!";
			NavPathway port;
			if (parent.tile.getEdge1() == child.tile) {
				port = new NavPathway(parent.tile, (byte) 1);
			} else if (parent.tile.getEdge2() == child.tile) {
				port = new NavPathway(parent.tile, (byte) 2);
			} else {
				assert parent.tile.getEdge3() == child.tile;
				port = new NavPathway(parent.tile, (byte) 3);
			}
			ret.add(port);
			if (ret.size() > corridorLength) {
				log.error(I18n.get("log.df7236389a8e", x1, y1, z1, x2, y2, z2));
				return new NavPathway[0];
			}
			child = parent;
		}
		return ret.toArray(new NavPathway[0]);
	}

	/**
	 * 将节点加入堆并上浮维护堆序。
	 * Adds a node to the heap and sorts it up.
	 *
	 * @param node 待加入节点 / node to add
	 */
	private void add(NavHeapNode node) {
		list.put(node.tile, node);
		node.heapIndex = currentHeapCount;
		heap[currentHeapCount++] = node;
		if (currentHeapCount == heap.length) {
			NavHeapNode[] tempHeap = new NavHeapNode[heap.length + 50];
			System.arraycopy(heap, 0, tempHeap, 0, currentHeapCount);
			heap = tempHeap;
		}
		sortUp(node);
	}

	/**
	 * 判断给定三角形是否已探索过。
	 * Whether the given geometry has already been considered.
	 *
	 * @param tile 导航三角形 / nav geometry
	 * @return 已探索则为 true / true if already considered
	 */
	private boolean contains(NavGeometry tile) {
		return list.containsKey(tile);
	}

	/**
	 * 获取表示给定三角形的堆节点。
	 * Returns the heap node representing the given geometry.
	 *
	 * @param tile 导航三角形 / nav geometry
	 * @return 对应堆节点 / matching heap node
	 */
	private NavHeapNode getNode(NavGeometry tile) {
		return list.get(tile);
	}

	/**
	 * 弹出堆顶最高优先级节点并重整堆。
	 * Removes the highest-priority node from the heap and reorders it.
	 *
	 * top heap node
	 */
	private NavHeapNode removeFirst() {
		if (currentHeapCount == 0) return null;
		NavHeapNode ret = heap[0];
		currentHeapCount--;
		heap[0] = heap[currentHeapCount];
		heap[0].heapIndex = 0;
		sortDown(heap[0]);
		return ret;
	}

	/**
	 * 节点代价更新后校验其在堆中的位置。
	 * Revalidates the node's heap position after its values change.
	 *
	 * @param node 已更新节点 / updated node
	 */
	private void onUpdateNode(NavHeapNode node) {
		if (node.open) return;
		sortUp(node);
		sortDown(node); //Unneeded for this application
	}

	/**
	 * 下沉调整，确保节点不高于其子节点应有位置。
	 * Sorts a node down so it is not above its proper child position.
	 *
	 * @param node 待调整节点 / node to validate
	 */
	private void sortDown(NavHeapNode node) {
		do {
			// 子索引 / Child Index
			int ciLeft = node.heapIndex * 2 + 1;
			int ciRight = node.heapIndex * 2 + 2;
			int swapIndex;
			if (ciLeft < currentHeapCount) {
				swapIndex = ciLeft;
				if (ciRight < currentHeapCount) {
					if (heap[ciRight].compareTo(heap[ciLeft]) < 0) {
						swapIndex = ciRight;
					}
				}
				if (node.compareTo(heap[swapIndex]) > 0) {
					swap(node, heap[swapIndex]);
				} else {
					break;
				}
			} else {
				break;
			}
		} while (true);
	}

	/**
	 * 上浮调整，确保节点不低于其父节点应有位置。
	 * Sorts a node up so it is not below its proper parent position.
	 *
	 * @param node 待调整节点 / node to validate
	 */
	private void sortUp(NavHeapNode node) {
		int pi; //Parent Index
		do {
			pi = (node.heapIndex-1)/2;
			if (heap[pi].compareTo(node) > 0) {
				swap(heap[pi], node);
			} else {
				break;
			}
		} while (true);
	}

	/**
	 * 交换两节点在堆中的位置。
	 * Swaps the positions of two nodes on the heap.
	 *
	 * node 1
	 * node 2
	 */
	private void swap(NavHeapNode node1, NavHeapNode node2) {
		heap[node1.heapIndex] = node2;
		heap[node2.heapIndex] = node1;
		int heapIndex1 = node1.heapIndex;
		node1.heapIndex = node2.heapIndex;
		node2.heapIndex = heapIndex1;
	}

	/**
	 * 最大展开节点数配置。
	 * Configured maximum nodes to expand.
	 *
	 * max nodes
	 */
	private static int maxNodes() {
		return Math.max(1, GeoDataConfig.GEO_NAV_MAX_NODES);
	}

	/**
	 * 走廊最大长度配置。
	 * Configured maximum corridor length.
	 *
	 * @return 走廊长度上限 / corridor length limit
	 */
	private static int corridorLength() {
		return Math.max(1, GeoDataConfig.GEO_NAV_CORRIDOR_LENGTH);
	}

	/**
	 * 无终点三角形时的目标距离阈值。
	 * Target-distance threshold when no end tile is set.
	 *
	 * threshold
	 */
	private static float targetThreshold() {
		return Math.max(0F, GeoDataConfig.GEO_NAV_TARGET_THRESHOLD);
	}

	/**
	 * 路径权重配置。
	 * Configured path weight.
	 *
	 * path weight
	 */
	private static float pathWeight() {
		return Math.max(0F, GeoDataConfig.GEO_NAV_PATH_WEIGHT);
	}

	/**
	 * 目标距离权重配置。
	 * Configured target-distance weight.
	 *
	 * target weight
	 */
	private static float targetWeight() {
		return Math.max(0F, GeoDataConfig.GEO_NAV_TARGET_WEIGHT);
	}

}
