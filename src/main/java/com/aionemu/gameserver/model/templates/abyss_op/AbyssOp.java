package com.aionemu.gameserver.model.templates.abyss_op;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 欧比斯 Op 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlType(name = "abyss_op")
@XmlAccessorType(XmlAccessType.NONE)
public class AbyssOp {
	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id", required = true)
	private int id;

	/** 返回 NPC ID / Returns the npc id */
	@Getter
	@XmlAttribute(name = "npc_id", required = true)
	private int npcId;

	/** 返回 abyss op type / Returns the abyss op type */
	@Getter
	@XmlAttribute(name = "type", required = true)
	private AbyssOpType abyssOpType;

	/** 返回攻城 ID / Returns the siege id */
	@Getter
	@XmlAttribute(name = "siege_id", required = true)
	private int siegeId;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;

	/** 返回组 ID / Returns the group id */
	@Getter
	@XmlAttribute(name = "group_id", required = true)
	private int groupId;

	/** 获取点。 / Returns the points. */
	@Getter
	@XmlAttribute(name = "points", required = true)
	private int points;
}
