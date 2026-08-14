package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.shield.ShieldTemplate;

/**
 * 护盾模板数据容器，持有全部 ShieldTemplate 列表。
 * Shield template data holder storing the full ShieldTemplate list.
 *
 * @author Wakizashi
 */
@XmlRootElement(name = "shields")
@XmlAccessorType(XmlAccessType.FIELD)
public class ShieldData {

	@XmlElement(name = "shield")
	private List<ShieldTemplate> shieldTemplates;

	/**
	 * 返回护盾模板数量；列表为空时初始化为空列表并返回 0。
	 * Returns the number of shield templates; initializes an empty list and returns 0 when null.
	 *
	 * @return shield templates; initializes an empty list and returns 0 when null数量 / Returns the number of shield templates; initializes an empty list and returns 0 when null.
	 */
	public int size() {
		if (shieldTemplates == null) {
			shieldTemplates = new ArrayList<ShieldTemplate>();
			return 0;
		}
		return shieldTemplates.size();
	}

	/**
	 * 返回护盾模板列表；为空时返回新空列表。
	 * Returns the shield template list; returns a new empty list when null.
	 *
	 * @return 护盾模板列表 / shield template list
	 */
	public List<ShieldTemplate> getShieldTemplates() {
		if (shieldTemplates == null) {
			return new ArrayList<ShieldTemplate>();
		}
		return shieldTemplates;
	}

	/**
	 * 批量追加护盾模板。
	 * Appends all given shield templates.
	 *
	 * @param templates 待追加的模板集合 / templates to append
	 */
	public void addAll(Collection<ShieldTemplate> templates) {
		if (shieldTemplates == null) {
			shieldTemplates = new ArrayList<ShieldTemplate>();
		}
		shieldTemplates.addAll(templates);
	}
}
