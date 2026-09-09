package com.aionemu.gameserver.model.templates.recipe;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 配方模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecipeTemplate")
public class RecipeTemplate {
	protected List<ComboProduct> comboproduct;
	/** 返回最大制作数量 / Returns the max production count */
	@Getter
	@XmlAttribute(name = "max_production_count")
	protected Integer maxProductionCount;
	/** 返回制作延迟时间 / Returns the craft delay time*/
	@Getter
	@XmlAttribute(name = "craft_delay_time")
	protected Integer craftDelayTime;
	/** 返回制作延迟 ID / Returns the craft delay id */
	@Getter
	@XmlAttribute(name = "craft_delay_id")
	protected Integer craftDelayId;
	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(name = "name")
	protected String name;
	@XmlAttribute(name = "archdaeva")
	protected boolean archdaeva;
	@XmlAttribute
	protected int quantity;
	@XmlAttribute
	protected int productid;
	@XmlAttribute
	protected int autolearn;
	@XmlAttribute
	protected int dp;
	@XmlAttribute
	protected int skillpoint;
	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute
	protected Race race;
	@XmlAttribute
	protected int skillid;
	@XmlAttribute
	protected int itemid;
	/** 返回名称 ID / Returns the name id */
	@Getter
	@XmlAttribute
	protected int nameid;
	@XmlAttribute
	protected int id;
	@XmlElement(name = "component_panel")
	private List<Component> component_panel;

	/** 返回组件面板 / Returns the component panel */
	public List<Component> getComponent() {
		return component_panel;
	}

	/** 返回组合产品 / Returns the combo product */
	public Integer getComboProduct(int num) {
		if (comboproduct == null || comboproduct.get(num - 1) == null) {
			return null;
		}
		return comboproduct.get(num - 1).getItemid();
	}

	/** 返回组合产品数量 / Returns the combo product size */
	public Integer getComboProductSize() {
		if (comboproduct == null) {
			return 0;
		}
		return comboproduct.size();
	}

	/** 返回数量 / Returns the quantity */
	public Integer getQuantity() {
		return quantity;
	}

	/** 返回产品 ID / Returns the product id */
	public Integer getProductid() {
		return productid;
	}

	/** 返回自动学习 / Returns the auto learn */
	public int getAutoLearn() {
		return autolearn;
	}

	/** 获取神圣能量。 / Returns the dp. */
	public Integer getDp() {
		return dp;
	}

	/** 返回技能点数 / Returns the skill point */
	public Integer getSkillpoint() {
		return skillpoint;
	}

	/** 返回技能 ID / Returns the skill id */
	public Integer getSkillid() {
		return skillid;
	}

	/** 返回物品 ID / Returns the item id */
	public Integer getItemid() {
		return itemid;
	}

	/** 返回 ID / Returns the id */
	public Integer getId() {
		return id;
	}

	/**
	 * @return 是否为高阶守护者配方 / whether arch daeva recipe
	 */
	public boolean isArchDaeva() {
		return archdaeva;
	}

	/** 设置高阶守护者标志 / Sets the arch daeva flag */
	public void setArchDaeva(boolean value) {
		archdaeva = value;
	}
}
