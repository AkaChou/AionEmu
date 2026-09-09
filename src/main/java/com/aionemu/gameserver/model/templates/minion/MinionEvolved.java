package com.aionemu.gameserver.model.templates.minion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 守护灵进化模板（静态数据/XML）。
 * Minion evolution template (static data/XML).
 *
 * @author Falke_34
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MinionEvolved")
public class MinionEvolved {

	/** 返回物品 ID / Returns the item id */
	@Getter
	@XmlAttribute(name = "itemId")
	private int itemId;

	/** 返回 evolved num / Returns the evolved num */
	@Getter
	@XmlAttribute(name = "evolvedNum")
	private int evolvedNum;

	/** 返回 evolved cost / Returns the evolved cost */
	@Getter
	@XmlAttribute(name = "evolvedCost")
	private int evolvedCost;
}
