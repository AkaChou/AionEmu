package com.aionemu.gameserver.geoEngine.scene;

import java.util.BitSet;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;

/**
 * 可按实例/事件/攻城护盾等条件动态禁用碰撞的场景节点。
 * Scene node whose collisions can be dynamically disabled based on instance, event, siege shield, etc.
 */
public class DespawnableNode extends Node {

	/** 可消失类型。 / Despawnable type. */
	public DespawnableType type = DespawnableType.NONE;
	/** 关联业务 ID（事件主题、静态物件、攻城据点等）。 / Related business id (event theme, static object, siege location, etc.). */
	public int id;
	/** 等级位掩码。 / Level bit mask. */
	public byte levelBitMask;
	/** 各实例是否激活的位集。 / Bit set of which instances are active. */
	private final BitSet instances = new BitSet();

	/**
	 * 设置指定实例是否激活。
	 * Sets whether the given instance is active.
	 *
	 * @param instanceId 实例 ID / instance id
	 * @param active 是否激活 / whether active
	 */
	public void setActive(int instanceId, boolean active) {
		synchronized (instances) {
			instances.set(instanceId, active);
		}
	}

	/**
	 * 查询指定实例是否激活。
	 * Queries whether the given instance is active.
	 *
	 * @param instanceId 实例 ID / instance id
	 * @return 若激活则为 true / true if active
	 */
	public boolean isActive(int instanceId) {
		synchronized (instances) {
			return instances.get(instanceId);
		}
	}

	/**
	 * 从普通 {@link Node} 复制名称、碰撞标志与子节点结构。
	 * Copies name, collision flags and child structure from a plain {@link Node}.
	 *
	 * @param node 源节点 / source node
	 * @throws CloneNotSupportedException 遇到不支持的子类型时 / when an unsupported child type is encountered
	 */
	public void copyFrom(Node node) throws CloneNotSupportedException {
		name = node.name;
		collisionFlags = node.collisionFlags;
		for (Spatial spatial : node.getChildren()) {
			if (spatial instanceof Geometry) {
				attachChild(new Geometry(spatial.getName(), ((Geometry) spatial).getMesh()));
			} else if (spatial instanceof Node) {
				attachChild(((Node) spatial).clone());
			} else {
				throw new CloneNotSupportedException();
			}
		}
	}

	/**
	 * 按类型/实例/护盾/忽略属性过滤后委托父类碰撞检测。
	 * Filters by type/instance/shield/ignore properties, then delegates to the parent collision check.
	 *
	 * @param other 目标可碰撞对象 / target collidable
	 * @param results 碰撞结果收集器 / collision results collector
	 * @return 碰撞数量 / number of collisions
	 */
	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (type == DespawnableType.EVENT) {
			if (activeEventThemeId() != id) {
				return 0;
			}
		} else if (type == DespawnableType.SHIELD) {
			IgnoreProperties ignoreProperties = results.getIgnoreProperties();
			if (ignoreProperties == IgnoreProperties.ANY_RACE) {
				return 0;
			}
			SiegeLocation loc = getSiegeLocation();
			if (loc != null) {
				if (!loc.isUnderShield()) {
					return 0;
				}
				if (ignoreProperties != null && ignoreProperties.getRace() != null) {
					if (loc.getRace() != SiegeRace.BALAUR
							&& ignoreProperties.getRace().getRaceId() == loc.getRace().getRaceId()) {
						return 0;
					}
					if (loc.getRace() == SiegeRace.BALAUR
							&& ignoreProperties.getRace() == IgnoreProperties.BALAUR.getRace()) {
						return 0;
					}
				}
			}
		} else if (type != DespawnableType.EVENT && type != DespawnableType.HOUSE && !isActive(results.getInstanceId())) {
			return 0;
		} else if (results.getIgnoreProperties() != null && results.getIgnoreProperties().getStaticId() > 0
				&& results.getIgnoreProperties().getStaticId() == id) {
			return 0;
		}
		return super.collideWith(other, results);
	}

	/** 当前活动事件主题 ID；无数据时返回 0。 / Active event theme id; 0 when no data. */
	private int activeEventThemeId() {
		if (DataManager.EVENT_DATA == null) {
			return 0;
		}
		return GameEventServices.eventService().getEventType().getId();
	}

	/** 按 ID 查找攻城地点；失败返回 null / Looks up siege location by id; null on failure */
	private SiegeLocation getSiegeLocation() {
		try {
			return GameFeatureServices.siegeService().getSiegeLocation(id);
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * 深拷贝本节点及其类型/激活状态。
	 * Deep-clones this node including type and active-instance state.
	 *
	 * @return 克隆节点 / cloned node
	 */
	@Override
	public Node clone() throws CloneNotSupportedException {
		DespawnableNode node = new DespawnableNode();
		node.copyFrom(this);
		node.type = type;
		node.id = id;
		node.levelBitMask = levelBitMask;
		node.instances.or(instances);
		return node;
	}

	/**
	 * 设置可消失类型。
	 * Sets the despawnable type.
	 *
	 * @param type 可消失类型 / type
	 */
	public void setType(DespawnableType type) {
		this.type = type;
	}

	/**
	 * 设置关联业务 ID。
	 * Sets the related business id.
	 *
	 * @param id 业务 ID / business id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 可消失节点类型枚举。
	 * Despawnable node type enumeration.
	 */
	public enum DespawnableType {
		NONE(0),
		EVENT(1),
		PLACEABLE(2),
		HOUSE(3),
		HOUSE_DOOR(4),
		TOWN_OBJECT(5),
		DOOR_STATE1(6),
		DOOR_STATE2(7),
		SHIELD(8);

		/** 类型字节 ID / Type byte id */
		private final byte id;

		DespawnableType(int id) {
			this.id = (byte) id;
		}

		/**
		 * 按字节 ID 查找类型。
		 * Looks up a type by its byte id.
		 *
		 * @param id 类型 ID / type id
		 * @return 匹配的类型 / matching type
		 * @throws IllegalArgumentException 当 ID 无效时 / when id is invalid
		 */
		public static DespawnableType getById(byte id) {
			for (DespawnableType type : values()) {
				if (type.id == id) {
					return type;
				}
			}
			throw new IllegalArgumentException("Invalid despawnable type " + id);
		}

		/**
		 * 返回类型字节 ID。
		 * Returns the type byte id.
		 *
		 * @return 类型字节 ID / type id
		 */
		public byte getId() {
			return id;
		}
	}
}
