package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate;

/**
 * 物品限制清理规则数据容器，持有全部 {@link ItemCleanupTemplate}。
 * Item restriction cleanup data holder containing all {@link ItemCleanupTemplate} entries.
 *
 * @author KID
 */
@XmlRootElement(name = "item_restriction_cleanups")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemRestrictionCleanupData {
	@XmlElement(name = "cleanup")
	private List<ItemCleanupTemplate> bplist;

	/**
	 * 返回清理规则数量。
	 * Returns the number of cleanup rules.
	 *
	 * @return 清理规则数量 / Returns the number of cleanup rules.
	 */
	public int size() {
		return bplist.size();
	}

	/**
	 * 返回全部清理规则列表。
	 * Returns the full list of cleanup rules.
	 *
	 * @return 清理模板列表 / cleanup template list
	 */
	public List<ItemCleanupTemplate> getList() {
		return this.bplist;
	}
}
