package com.aionemu.gameserver.model.templates.itemset;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 套装部件模板（静态数据/XML）。
 * Item set part template (static data/XML).
 *
 * @author ATracer
 */
@XmlRootElement(name = "ItemPart")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemPart {

	/**
	 * @return the itemid
	 */
	@Getter
	@XmlAttribute
	protected int itemid;
}
