package com.aionemu.gameserver.model.templates.recipe;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 月华模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaTemplate")
public class LunaTemplate {
	protected List<LunaComponent> luna_component_panel_1;
	protected List<LunaComponent> luna_component_panel_2;
	protected List<LunaComponent> luna_component_panel_3;
	protected List<LunaComponent> luna_component_panel_4;
	protected List<LunaComponent> luna_component_panel_5;

	@XmlAttribute(name = "max_production_count")
	protected Integer maxProductionCount;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute
	protected int quantity;

	@XmlAttribute
	protected int group;

	@XmlAttribute(name = "success_rate")
	protected int success_rate;

	@XmlAttribute
	protected int productid;

	@XmlAttribute
	protected Race race;

	@XmlAttribute
	protected int itemid;

	@XmlAttribute
	protected int nameid;

	@XmlAttribute
	protected int id;

	/** 返回 luna component / Returns the luna component */
	public List<LunaComponent> getLunaComponent() {
		if (luna_component_panel_1 == null) {
			luna_component_panel_1 = new ArrayList<LunaComponent>();
		}
		return this.luna_component_panel_1;
	}

	/** 返回 luna component 2 / Returns the luna component 2 */
	public List<LunaComponent> getLunaComponent2() {
		if (luna_component_panel_2 == null) {
			luna_component_panel_2 = new ArrayList<LunaComponent>();
		}
		return this.luna_component_panel_2;
	}

	/** 返回 luna component 3 / Returns the luna component 3 */
	public List<LunaComponent> getLunaComponent3() {
		if (luna_component_panel_3 == null) {
			luna_component_panel_3 = new ArrayList<LunaComponent>();
		}
		return this.luna_component_panel_3;
	}

	/** 返回 luna component 4 / Returns the luna component 4 */
	public List<LunaComponent> getLunaComponent4() {
		if (luna_component_panel_4 == null) {
			luna_component_panel_4 = new ArrayList<LunaComponent>();
		}
		return this.luna_component_panel_4;
	}

	/** 返回 luna component 5 / Returns the luna component 5 */
	public List<LunaComponent> getLunaComponent5() {
		if (luna_component_panel_5 == null) {
			luna_component_panel_5 = new ArrayList<LunaComponent>();
		}
		return this.luna_component_panel_5;
	}

	/** 返回 quantity / Returns the quantity */
	public Integer getQuantity() {
		return quantity;
	}

	/** 获取队伍。 / Returns the group. */
	public Integer getGroup() {
		return group;
	}

	/** 获取比率。 / Returns the rate. */
	public int getRate() {
		return success_rate;
	}

	/** 返回 productid / Returns the productid */
	public Integer getProductid() {
		return productid;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
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
}
