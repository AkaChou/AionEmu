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
 * 仓库扩展模板（静态数据/XML）。
 * Warehouse expand template (static data / XML).
 *
 * @author Simple
 */
@XmlRootElement(name = "warehouse_npc")
@XmlAccessorType(XmlAccessType.FIELD)
public class WarehouseExpandTemplate {

	@XmlElement(name = "expand", required = true)
	protected List<Expand> warehouseExpands;

	/**
	 * 仓库 NPC 的 ID。
	 * NPC ID of the warehouse NPC.
	 */
	@XmlAttribute(name = "id", required = true)
	protected int id;

	/**
	 * NPC 名称。
	 * NPC name.
	 */
	@XmlAttribute(name = "name", required = true)
	protected String name = "";

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return id;
	}

	/**
	 * 获取仓库扩展列表。
	 * Gets the warehouse expand list.
	 */
	public List<Expand> getWarehouseExpand() {
		return this.warehouseExpands;
	}

	/**
	 * 获取 NPC 名称。
	 * Gets the value of the name property
	 *
	 * @return NPC 名称字符串 / Possible object is {@link String }
	 */
	public String getName() {
		return Util.convertName(name);
	}

	/**
	 * 判断扩展列表是否包含指定等级。
	 * Returns true if list contains level
	 *
	 * @param level 要检查的等级 / Level to check
	 * @return 包含则为 true / True or false
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
	 * 按等级获取扩展项。
	 * Returns the expand for the given level
	 *
	 * @param level 要查找的等级 / Level to look up
	 * @return 扩展项 / Expand
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
