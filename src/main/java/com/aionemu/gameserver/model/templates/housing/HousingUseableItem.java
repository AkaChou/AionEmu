package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 住房 Useable 物品模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HousingUseableItem", propOrder = { "action" })
public class HousingUseableItem extends PlaceableHouseObject {

	@XmlElement(required = true)
	protected UseItemAction action;

	@XmlAttribute(required = true)
	protected boolean owner;

	@XmlAttribute
	protected Integer cd;

	@XmlAttribute(required = true)
	protected int delay;

	@XmlAttribute(name = "use_count")
	protected Integer useCount;

	@XmlAttribute(name = "required_item")
	protected Integer requiredItem;

	/** 获取动作。 / Returns the action. */
	public UseItemAction getAction() {
		return action;
	}

	/**
	 * Can the object be used only by the owner or visitors too
	 */
	public boolean isOwnerOnly() {
		return owner;
	}

	/**
	 * @return null if no Cooltime is used
	 */
	public Integer getCd() {
		return cd;
	}

	/** 返回延迟 / Returns the delay*/
	public int getDelay() {
		return delay;
	}

	/**
	 * @return null if use is not restricted
	 */
	public Integer getUseCount() {
		return useCount;
	}

	/**
	 * @return null if no item is required
	 */
	public Integer getRequiredItem() {
		return requiredItem;
	}

	/** 返回类型 ID / Returns the type id */
	@Override
	public byte getTypeId() {
		return 1;
	}
}
