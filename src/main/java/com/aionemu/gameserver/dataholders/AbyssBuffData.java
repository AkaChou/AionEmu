package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.abyss_bonus.AbyssServiceAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 欧比斯增益属性数据容器，按 buff ID 索引服务端属性模板。
 * Abyss bonus attribute data holder, indexing service attribute templates by buff id.
 *
 * @Author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "abyssBonusattr" })
@XmlRootElement(name = "abyss_bonusattrs")
public class AbyssBuffData {
	@XmlElement(name = "abyss_bonusattr")
	protected List<AbyssServiceAttr> abyssBonusattr;

	@XmlTransient
	private IntObjectHashMap<AbyssServiceAttr> templates = new IntObjectHashMap<AbyssServiceAttr>();

	/**
	 * JAXB 反序列化完成后，按 buff ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by buff id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AbyssServiceAttr template : abyssBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		abyssBonusattr.clear();
		abyssBonusattr = null;
	}

	/**
	 * 返回已加载的模板数量。
	 * Returns the number of loaded templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按 buff ID 获取欧比斯服务属性模板。
	 * Returns the abyss service attribute template for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId
	 * @return 属性模板，不存在则为 null / attribute template or null
	 */
	public AbyssServiceAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
