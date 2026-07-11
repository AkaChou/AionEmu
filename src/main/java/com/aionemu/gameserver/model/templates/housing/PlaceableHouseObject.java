package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Placeable 房屋对象模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlaceableHouseObject")
@XmlSeeAlso({ HousingJukeBox.class, HousingPicture.class, HousingPostbox.class, HousingChair.class,
		HousingStorage.class, HousingNpc.class, HousingMoveableItem.class, HousingUseableItem.class,
		HousingPassiveItem.class })
public abstract class PlaceableHouseObject extends AbstractHouseObject {

	@XmlAttribute(name = "use_days")
	protected Integer useDays;

	@XmlAttribute
	protected LimitType limit;

	@XmlAttribute
	protected PlaceLocation location;

	@XmlAttribute
	protected PlaceArea area;

	 /**
	  * 获取 useDays 属性值。
	  * Gets the value of the useDays property
	  * @return null if not restricted
	  */
	public int getUseDays() {
		if (useDays == null) {
			return 0;
		}
		return useDays;
	}

	/**
	 * @return 对象允许放置在何处？ / Where the object is allowed to be placed on? @return {@link LimitType.NONE} if no restriction
	 */
	public LimitType getPlacementLimit() {
		if (limit == null) {
			return LimitType.NONE;
		}
		return limit;
	}

	/**
	 * 对象允许如何放置（堆叠、地面、墙面）？。 / How the object is allowed to be placed (stacks, ground, wall) ?.
	 */
	public PlaceLocation getLocation() {
		return location;
	}

	/**
	 * 对象允许放置的环境（室内、室外）。 / Environment where the object is allowed to be placed (interior, exterior).
	 */
	public PlaceArea getArea() {
		return area;
	}

	/** 返回类型 ID / Returns the type id */
	public abstract byte getTypeId();
}
