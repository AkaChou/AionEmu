package com.aionemu.gameserver.model.templates.pet;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物奖励模板（静态数据/XML）。
 * Pet rewards template (static data / XML).
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetRewards", propOrder = { "results" })
public class PetRewards {

	@XmlElement(name = "result")
	protected List<PetFeedResult> results;

	@XmlAttribute(name = "group", required = true)
	protected FoodType type;

	@XmlAttribute
	protected boolean loved = false;

	/** 返回奖励结果列表 / Returns the results */
	public List<PetFeedResult> getResults() {
		if (results == null) {
			results = new ArrayList<PetFeedResult>();
		}
		return this.results;
	}

	/** 获取类型。 / Returns the type. */
	public FoodType getType() {
		return type;
	}

	/**
	 * @return 是否为喜爱食物 / Whether loved
	 */
	public boolean isLoved() {
		return loved;
	}
}
