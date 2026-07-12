package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.item.ItemQuality;

/**
 * 抽象房屋对象模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractHouseObject")
@XmlSeeAlso({ PlaceableHouseObject.class })
public abstract class AbstractHouseObject extends VisibleObjectTemplate {

	@XmlAttribute(name = "talking_distance", required = true)
	protected float talkingDistance;

	@XmlAttribute(required = true)
	protected ItemQuality quality;

	@XmlAttribute(required = true)
	protected HousingCategory category;

	@XmlAttribute(name = "name_id", required = true)
	protected int nameId;

	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(name = "can_dye")
	protected boolean canDye;

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return id;
	}

	/** 返回 talking distance / Returns the talking distance */
	public float getTalkingDistance() {
		return talkingDistance;
	}

	/** 返回 quality / Returns the quality */
	public ItemQuality getQuality() {
		return quality;
	}

	/** 获取分类。 / Returns the category. */
	public HousingCategory getCategory() {
		return category;
	}

	/** 返回 can dye / Returns the can dye */
	public boolean getCanDye() {
		return canDye;
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return nameId;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return null;
	}
}
