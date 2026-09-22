package com.aionemu.gameserver.model.templates.atreian_bestiary;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 艾特里亚图鉴模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtreianBestiaryTemplate")
public class AtreianBestiaryTemplate {

	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	/**
	 * -- GETTER --
	 * 获取等级。 / Returns the level.
	 */
	@Getter
	@XmlAttribute(name = "level")
	private int level;

    /**
     * -- GETTER --
     * 获取名称。 / Returns the name.
     */
    @Getter
    @XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "npc_ids")
	private List<Integer> npc_ids;

	/**
	 * -- GETTER --
	 * 获取类型。 / Returns the type.
	 */
	@Getter
	@XmlAttribute(name = "type")
	private BookType type;

	@XmlElement(name = "achievement")
	private List<AtreianBestiaryAchievementTemplate> achievement;

	/** 返回 npc ids / Returns the npc ids */
	public List<Integer> getNpcIds() {
		if (npc_ids == null) {
			npc_ids = Collections.emptyList();
		}
		return this.npc_ids;
	}

	/**
	 * 获取艾特里亚图鉴 Achievement 模板。
	 * Returns the atreian bestiary achievement template.
	 */
	public List<AtreianBestiaryAchievementTemplate> getAtreianBestiaryAchievementTemplate() {
		return achievement;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AtreianBestiaryAchievementTemplate")
	public static class AtreianBestiaryAchievementTemplate {

		@XmlAttribute(name = "condition")
		private int condition;

		@XmlAttribute(name = "exp")
		private int exp;

		/** 返回 kill condition / Returns the kill condition */
		public int getKillCondition() {
			return condition;
		}

		/** 获取奖励经验。 / Returns the reward exp. */
		public int getRewardExp() {
			return exp;
		}
	}

	@XmlType(name = "BookType")
	@XmlEnum
	public enum BookType {
		/** 普通图鉴 / Normal book */
		NORMAL(),
		/** 英雄图鉴 / Hero book */
		HERO()
	}
}
