package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.instance_bonusatrr.InstanceBonusAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 副本增益属性数据容器，按 Buff ID 索引 {@link InstanceBonusAttr}。
 * Instance bonus attribute data holder, indexing {@link InstanceBonusAttr} by buff id.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "instanceBonusattr" })
@XmlRootElement(name = "instance_bonusattrs")
public class InstanceBuffData {

	@XmlElement(name = "instance_bonusattr")
	protected List<InstanceBonusAttr> instanceBonusattr;
	@XmlTransient
	private IntObjectHashMap<InstanceBonusAttr> templates = new IntObjectHashMap<InstanceBonusAttr>();

	/**
	 * JAXB 反序列化完成后，按 Buff ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by buff id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (InstanceBonusAttr template : instanceBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		instanceBonusattr.clear();
		instanceBonusattr = null;
	}

	/**
	 * 返回已加载的副本增益模板数量。
	 * Returns the number of loaded instance bonus templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按 Buff ID 获取副本增益属性模板。
	 * Returns the instance bonus attribute template for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId @return 增益属性模板或 null / bonus attribute template or null
	 */
	public InstanceBonusAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
