package com.aionemu.gameserver.model.templates.gather;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;

/**
 * 可采集物模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer, KID
 */

@XmlRootElement(name = "gatherable_template")
@XmlAccessorType(XmlAccessType.FIELD)
public class GatherableTemplate extends VisibleObjectTemplate {
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
	@XmlAttribute
	protected String sourceType;
	@XmlAttribute
	protected int harvestCount;
	@XmlAttribute
	protected int skillLevel;
	@XmlAttribute
	protected int harvestSkill;
	@XmlAttribute
	protected int successAdj;
	@XmlAttribute
	protected int failureAdj;
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
	@XmlAttribute
	protected int checkType;
	@XmlAttribute
	protected int eraseValue;

	 /**
	  * 获取 materials 属性值。
	  * Gets the value of the materials property
	  * @return possible object is {@link Materials }
	  */
	public Materials getMaterials() {
		return materials;
	}

	/** 返回 extra 材料 / Returns the extra 材料 */
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
	  * 获取 aerialAdj 属性值。
	  * Gets the value of the aerialAdj property
	  * @return possible object is {@link Integer }
	  */
	public int getAerialAdj() {
		return aerialAdj;
	}

	 /**
	  * 获取 failureAdj 属性值。
	  * Gets the value of the failureAdj property
	  * @return possible object is {@link Integer }
	  */
	public int getFailureAdj() {
		return failureAdj;
	}

	 /**
	  * 获取 successAdj 属性值。
	  * Gets the value of the successAdj property
	  * @return possible object is {@link Integer }
	  */
	public int getSuccessAdj() {
		return successAdj;
	}

	 /**
	  * 获取 harvestSkill 属性值。
	  * Gets the value of the harvestSkill property
	  * @return possible object is {@link Integer }
	  */
	public int getHarvestSkill() {
		return harvestSkill;
	}

	 /**
	  * 获取 skillLevel 属性值。
	  * Gets the value of the skillLevel property
	  * @return possible object is {@link Integer }
	  */
	public int getSkillLevel() {
		return skillLevel;
	}

	 /**
	  * 获取 harvestCount 属性值。
	  * Gets the value of the harvestCount property
	  * @return possible object is {@link Integer }
	  */
	public int getHarvestCount() {
		return harvestCount;
	}

	 /**
	  * 获取 sourceType 属性值。
	  * Gets the value of the sourceType property
	  * @return possible object is {@link String }
	  */
	public String getSourceType() {
		return sourceType;
	}

	/**
	 * 获取 value 的名称 property。 / Gets the value of the name property
	 *
	 * @return possible object is {@link String }
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the nameId
	 */
	@Override
	public int getNameId() {
		return nameId;
	}

	/** 返回 captcha rate / Returns the captcha rate */
	public int getCaptchaRate() {
		return captcha;
	}

	/** 获取等级限制。 / Returns the level limit. */
	public int getLevelLimit() {
		return lvlLimit;
	}

	/** 返回 required item id / Returns the required item id */
	public int getRequiredItemId() {
		return reqItem;
	}

	/** 返回 required item name id / Returns the required item name id */
	public int getRequiredItemNameId() {
		return reqItemNameId * 2 + 1;
	}

	/** 返回检查类型 / Returns the check type*/
	public int getCheckType() {
		return checkType;
	}

	/** 返回 erase value / Returns the erase value */
	public int getEraseValue() {
		return eraseValue;
	}
}
