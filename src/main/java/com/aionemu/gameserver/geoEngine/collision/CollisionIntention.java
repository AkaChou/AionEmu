package com.aionemu.gameserver.geoEngine.collision;

import java.util.EnumSet;

/**
 * 碰撞意图位掩码枚举，用于过滤几何体在射线/包围体检测中是否参与碰撞。
 * Bitmask enum of collision intentions used to filter which geometries
 * participate in ray/bounding-volume collision tests.
 *
 * @author Rolandas
 */
public enum CollisionIntention {

	/** 无意图。 / No intention. */
	NONE(0),
	/** 物理碰撞。 / Physical collision. */
	PHYSICAL(1 << 0),
	/** 带技能的网格材质。 / Mesh 材料 with skills. */
	MATERIAL(1 << 1),
	/** 技能障碍。 / Skill obstacles. */
	SKILL(1 << 2),
	/** 可行走/不可行走障碍。 / Walk/NoWalk obstacles. */
	WALK(1 << 3),
	/** 可开关的门。 / Doors which have a state opened/closed. */
	DOOR(1 << 4),
	/** 仅活动期间出现。 / Appear on event only. */
	EVENT(1 << 5),
	/** 可移动物体（船、术古箱等）。 / Moveable objects (ships, shugo boxes). */
	MOVEABLE(1 << 6),
	/** 物理但可透视。 / Physical but see-through. */
	PHYSICAL_SEE_THROUGH(1 << 7),
	/**
	 * 默认碰撞组合：物理 + 门 + 可透视物理。
	 * Default collision set: physical + door + physical see-through.
	 */
	DEFAULT_COLLISIONS(PHYSICAL.getId() | DOOR.getId() | PHYSICAL_SEE_THROUGH.getId()),
	/**
	 * 视线遮挡组合：物理 + 门。
	 * Line-of-sight blocking set: physical + door.
	 */
	CANT_SEE_COLLISIONS(PHYSICAL.getId() | DOOR.getId()),
	/**
	 * 全部意图位（含节点遍历）。仅节点使用时可枚举子几何体；
	 * 未指定该标志的节点不会枚举子节点，以加速处理。
	 * All intention bits (including node traversal). On nodes only, means children
	 * geometries may be enumerated; nodes without it skip children for speed.
	 */
	ALL(PHYSICAL.getId() | MATERIAL.getId() | SKILL.getId() | WALK.getId() | DOOR.getId() | EVENT.getId()
			| MOVEABLE.getId() | PHYSICAL_SEE_THROUGH.getId());

	/** 意图位 id / Intention bit id. */
	private byte id;

	/**
	 * 以整型位值构造意图。
	 * Constructs an intention from an integer bit value.
	 *
	 * @param id 位值 / bit value
	 */
	private CollisionIntention(int id) {
		this.id = (byte) id;
	}

	/**
	 * 返回意图位 id。
	 * Returns the intention bit id.
	 *
	 * @return 意图位 id / bit id
	 */
	public byte getId() {
		return id;
	}

	/**
	 * 将整型掩码解析为意图枚举集合（跳过 {@link #NONE} 与 {@link #ALL}）。
	 * Parses an integer mask into an EnumSet of intentions (skips {@link #NONE} and {@link #ALL}).
	 *
	 * @param value 整型掩码 / integer bitmask
	 * @return 匹配的意图集合 / matched intention set
	 */
	public static EnumSet<CollisionIntention> getFlagsFormValue(int value) {
		EnumSet<CollisionIntention> result = EnumSet.noneOf(CollisionIntention.class);
		for (CollisionIntention m : CollisionIntention.values()) {
			if ((value & m.getId()) == m.getId()) {
				if (m == NONE || m == ALL) {
					continue;
				}
				result.add(m);
			}
		}
		return result;
	}

	/**
	 * 将整型掩码格式化为逗号分隔的意图名字符串。
	 * Formats an integer mask as a comma-separated list of intention names.
	 *
	 * @param value 整型掩码 / integer bitmask
	 * @return 意图名列表字符串 / comma-separated intention names
	 */
	public static String toString(int value) {
		String str = "";
		for (CollisionIntention m : CollisionIntention.values()) {
			if (m == NONE || m == ALL) {
				continue;
			}
			if ((value & m.getId()) == m.getId()) {
				str += m.toString();
				str += ", ";
			}
		}
		if (str.length() > 0) {
			str = str.substring(0, str.length() - 2);
		}
		return str;
	}
}
