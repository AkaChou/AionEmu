package com.aionemu.gameserver.model.templates.gather;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import lombok.Getter;

/**
 * 可采集物模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer, KID
 */

@Getter
@XmlRootElement(name = "gatherable_template")
@XmlAccessorType(XmlAccessType.FIELD)
public class GatherableTemplate extends VisibleObjectTemplate {
	/**
	 * 获取 materials 属性值。
	 * Gets the value of the materials property
	 * @return 可能的返回对象 / possible object is {@link Materials }
	 */
	@XmlElement(required = true)
	protected Materials materials;
	@XmlElement(required = true)
	protected ExMaterials exmaterials;
	@XmlAttribute
	protected int id;
	@XmlAttribute
	protected String name;
	@XmlAttribute
	protected int nameId;
	/**
	 * 获取 sourceType 属性值。
	 * Gets the value of the sourceType property
	 * @return 可能的返回对象 / possible object is {@link String }
	 */
	@XmlAttribute
	protected String sourceType;
	/**
	 * 获取 harvestCount 属性值。
	 * Gets the value of the harvestCount property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int harvestCount;
	/**
	 * 获取 skillLevel 属性值。
	 * Gets the value of the skillLevel property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int skillLevel;
	/**
	 * 获取 harvestSkill 属性值。
	 * Gets the value of the harvestSkill property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int harvestSkill;
	/**
	 * 获取 successAdj 属性值。
	 * Gets the value of the successAdj property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int successAdj;
	/**
	 * 获取 failureAdj 属性值。
	 * Gets the value of the failureAdj property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int failureAdj;
	/**
	 * 获取 aerialAdj 属性值。
	 * Gets the value of the aerialAdj property
	 * @return 可能的返回对象 / possible object is {@link Integer }
	 */
	@XmlAttribute
	protected int aerialAdj;
	@XmlAttribute
	protected int captcha;
	@XmlAttribute
	protected int lvlLimit;
	@XmlAttribute
	protected int reqItem;
	@XmlAttribute
	protected int reqItemNameId;
	/** 返回检查类型 / Returns the check type */
	@XmlAttribute
	protected int checkType;
	/** 返回消除值 / Returns the erase value */
	@XmlAttribute
	protected int eraseValue;

	/** 返回额外材料 / Returns the extra materials */
	public ExMaterials getExtraMaterials() {
		return exmaterials;
	}

	 /**
	  * 获取 id 属性值。
	  * Gets the value of the id property
	  */
	@Override
	public int getTemplateId() {
		return id;
	}

	/**
	 * 获取名称属性值。 / Gets the value of the name property
	 *
	 * @return 可能的返回对象 / possible object is {@link String }
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * 获取名称 ID。
	 * Gets the name id.
	 *
	 * @return 名称 ID / the nameId
	 */
	@Override
	public int getNameId() {
		return nameId;
	}

	/** 返回验证码比率 / Returns the captcha rate */
	public int getCaptchaRate() {
		return captcha;
	}

	/** 获取等级限制。 / Returns the level limit. */
	public int getLevelLimit() {
		return lvlLimit;
	}

	/** 返回所需物品 ID / Returns the required item id */
	public int getRequiredItemId() {
		return reqItem;
	}

	/** 返回所需物品名称 ID / Returns the required item name id */
	public int getRequiredItemNameId() {
		return reqItemNameId * 2 + 1;
	}
}
