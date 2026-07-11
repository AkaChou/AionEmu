package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.abyss_bonus.AbyssGroupAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 欧比斯组属性数据容器，按 buff ID 索引组属性模板。
 * Abyss group attribute data holder, indexing group attribute templates by buff id.
 *
 * @Author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "abyssGroupattr" })
@XmlRootElement(name = "abyss_groupattrs")
public class AbyssGroupData {
	@XmlElement(name = "abyss_groupattr")
	protected List<AbyssGroupAttr> abyssGroupattr;

	@XmlTransient
	private IntObjectHashMap<AbyssGroupAttr> templates = new IntObjectHashMap<AbyssGroupAttr>();

	/**
	 * JAXB 反序列化完成后，按 buff ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by buff id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AbyssGroupAttr template : abyssGroupattr) {
			templates.put(template.getBuffId(), template);
		}
		abyssGroupattr.clear();
		abyssGroupattr = null;
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
	 * 按 buff ID 获取欧比斯组属性模板。
	 * Returns the abyss group attribute template for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId @return 组属性模板，不存在则为 null / group attribute template or null
	 */
	public AbyssGroupAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
