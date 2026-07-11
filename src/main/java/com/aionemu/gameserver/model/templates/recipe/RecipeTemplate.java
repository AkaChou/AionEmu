package com.aionemu.gameserver.model.templates.recipe;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 配方模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecipeTemplate")
public class RecipeTemplate {
	protected List<ComboProduct> comboproduct;
	@XmlAttribute(name = "max_production_count")
	protected Integer maxProductionCount;
	@XmlAttribute(name = "craft_delay_time")
	protected Integer craftDelayTime;
	@XmlAttribute(name = "craft_delay_id")
	protected Integer craftDelayId;
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
	@XmlAttribute
	protected Race race;
	@XmlAttribute
	protected int skillid;
	@XmlAttribute
	protected int itemid;
	@XmlAttribute
	protected int nameid;
	@XmlAttribute
	protected int id;
	@XmlElement(name = "component_panel")
	private List<Component> component_panel;

	/** 返回 component / Returns the component */
	public List<Component> getComponent() {
		return component_panel;
	}

	/** 返回 combo product / Returns the combo product */
	public Integer getComboProduct(int num) {
		if (comboproduct == null || comboproduct.get(num - 1) == null) {
			return null;
		}
		return comboproduct.get(num - 1).getItemid();
	}

	/** 返回 combo product size / Returns the combo product size */
	public Integer getComboProductSize() {
		if (comboproduct == null) {
			return 0;
		}
		return comboproduct.size();
	}

	/** 返回 quantity / Returns the quantity */
	public Integer getQuantity() {
		return quantity;
	}

	/** 返回 productid / Returns the productid */
	public Integer getProductid() {
		return productid;
	}

	/** 返回 auto learn / Returns the auto learn */
	public int getAutoLearn() {
		return autolearn;
	}

	/** 获取神圣能量。 / Returns the dp. */
	public Integer getDp() {
		return dp;
	}

	/** 返回 skillpoint / Returns the skillpoint */
	public Integer getSkillpoint() {
		return skillpoint;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回 skillid / Returns the skillid */
	public Integer getSkillid() {
		return skillid;
	}

	/** 返回物品 ID / Returns the itemid */
	public Integer getItemid() {
		return itemid;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 nameid / Returns the nameid */
	public int getNameid() {
		return nameid;
	}

	/** 返回 ID / Returns the id */
	public Integer getId() {
		return id;
	}

	/** 返回 max production count / Returns the max production count */
	public Integer getMaxProductionCount() {
		return maxProductionCount;
	}

	/** 返回制作延迟时间 / Returns the craft delay time*/
	public Integer getCraftDelayTime() {
		return craftDelayTime;
	}

	/** 返回 craft delay id / Returns the craft delay id */
	public Integer getCraftDelayId() {
		return craftDelayId;
	}

	/**
	 * @return Whether arch daeva / Whether arch daeva
	 */
	public boolean isArchDaeva() {
		return archdaeva;
	}

	/** 设置 arch daeva / Sets the arch daeva */
	public void setArchDaeva(boolean value) {
		archdaeva = value;
	}
}
