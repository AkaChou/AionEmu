package com.aionemu.gameserver.model.templates.abyss_bonus;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 欧比斯服务 Attr 模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbyssServiceAttr", propOrder = { "bonusAttr" })
public class AbyssServiceAttr {
	@XmlElement(name = "bonus_attr")
	protected List<AbyssPenaltyAttr> bonusAttr;

	/** 返回增益 ID / Returns the buff id */
	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name", required = true)
	private String name;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race", required = true)
	private Race race;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<AbyssPenaltyAttr> getPenaltyAttr() {
		if (bonusAttr == null) {
			bonusAttr = new ArrayList<AbyssPenaltyAttr>();
		}
		return bonusAttr;
	}
}
