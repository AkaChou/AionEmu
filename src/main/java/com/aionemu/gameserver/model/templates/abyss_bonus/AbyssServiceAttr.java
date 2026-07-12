package com.aionemu.gameserver.model.templates.abyss_bonus;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 欧比斯服务 Attr 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbyssServiceAttr", propOrder = { "bonusAttr" })
public class AbyssServiceAttr {
	@XmlElement(name = "bonus_attr")
	protected List<AbyssPenaltyAttr> bonusAttr;

	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlAttribute(name = "race", required = true)
	private Race race;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<AbyssPenaltyAttr> getPenaltyAttr() {
		if (bonusAttr == null) {
			bonusAttr = new ArrayList<AbyssPenaltyAttr>();
		}
		return bonusAttr;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 设置 buff id / Sets the buff id */
	public void setBuffId(int value) {
		buffId = value;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}
}
