package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.globaldrops.GlobalRule;

/**
 * 全局掉落规则配置数据容器，维护全部全局掉落规则。
 * Global drop rule configuration data holder for all global drop rules.
 *
 * Created by wanke on 19/02/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropData", propOrder = { "globalDropRules" })
@XmlRootElement(name = "global_rules")
public class GlobalDropData {
	@XmlElementWrapper(name = "gd_rules")
	@XmlElement(name = "gd_rule")
	protected List<GlobalRule> globalDropRules;

	/**
	 * 返回全部全局掉落规则列表；若尚未初始化则创建空列表。
	 * Returns all global drop rules; creates an empty list if not yet initialized.
	 *
	 * @return 全局掉落规则列表 / global drop rule list
	 */
	public List<GlobalRule> getAllRules() {
		if (globalDropRules == null) {
			globalDropRules = new ArrayList<GlobalRule>();
		}
		return this.globalDropRules;
	}

	/**
	 * 返回全局掉落规则数量。
	 * Returns the number of global drop rules.
	 *
	 * rule count
	 */
	public int size() {
		return globalDropRules.size();
	}
}
