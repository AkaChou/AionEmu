package com.aionemu.gameserver.model.templates.abyss_op;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 欧比斯 Op 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlType(name = "abyss_op")
@XmlAccessorType(XmlAccessType.NONE)
public class AbyssOp {
	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "npc_id", required = true)
	private int npcId;

	@XmlAttribute(name = "type", required = true)
	private AbyssOpType abyssOpType;

	@XmlAttribute(name = "siege_id", required = true)
	private int siegeId;

	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;

	@XmlAttribute(name = "group_id", required = true)
	private int groupId;

	@XmlAttribute(name = "points", required = true)
	private int points;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 返回 abyss op type / Returns the abyss op type */
	public AbyssOpType getAbyssOpType() {
		return abyssOpType;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回攻城 ID / Returns the siege id */
	public int getSiegeId() {
		return siegeId;
	}

	/** 返回组 ID / Returns the group id */
	public int getGroupId() {
		return groupId;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return points;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}
}
