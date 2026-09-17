package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 宠物商品条目模板（静态数据/XML）。
 * Pet merchandise entry template (static data / XML).
 *
 * @author Rinzler
 */

@Getter
@XmlType(name = "merch")
@XmlAccessorType(XmlAccessType.NONE)
@NoArgsConstructor
@AllArgsConstructor
public class PetMerchandEntry {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id", required = true)
	private int id;

	/** 获取比率价格。 / Returns the rate price. */
	@XmlAttribute(name = "rate_price")
	private int ratePrice;
}
