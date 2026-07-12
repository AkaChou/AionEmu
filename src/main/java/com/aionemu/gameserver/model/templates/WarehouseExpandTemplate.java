package com.aionemu.gameserver.model.templates;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.expand.Expand;
import com.aionemu.gameserver.utils.Util;

/**
 * 仓库 Expand 模板（静态数据/XML）。
 * XML template.
 *
 * @author Simple
 */
@XmlRootElement(name = "warehouse_npc")
@XmlAccessorType(XmlAccessType.FIELD)
public class WarehouseExpandTemplate {

	@XmlElement(name = "expand", required = true)
	protected List<Expand> warehouseExpands;

	/**
	 * NPC ID
	 */
	@XmlAttribute(name = "id", required = true)
	protected int id;

	/**
	 * NPC name
	 */
	@XmlAttribute(name = "name", required = true)
	protected String name = "";

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return id;
	}

	 /**
	  * 获取 material 属性值。
	  * Gets the value of the material property
	  */
	public List<Expand> getWarehouseExpand() {
		return this.warehouseExpands;
	}

	/**
	 * 获取 value 的名称 property。 / Gets the value of the name property
	 *
	 * @return possible object is {@link String }
	 */
	public String getName() {
		return Util.convertName(name);
	}

	/**
	 * 返回若为真则列表 containslevel。 / Returns true if list contains level
	 *
	 * @return true or false
	 */
	public boolean contains(int level) {
		for (Expand expand : warehouseExpands) {
			if (expand.getLevel() == level) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回若为真则列表 containslevel。 / Returns true if list contains level
	 *
	 * @return expand
	 */
	public Expand get(int level) {
		for (Expand expand : warehouseExpands) {
			if (expand.getLevel() == level) {
				return expand;
			}
		}
		return null;
	}
}
