package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.bonus_service.BonusServiceAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 服务加成数据容器，按 buffId 索引 BonusServiceAttr。
 * Service bonus data holder, indexing BonusServiceAttr by buff id.
 *
 * @author Ranastic (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "serviceBonusattr" })
@XmlRootElement(name = "service_bonusattrs")
public class ServiceBuffData {
	@XmlElement(name = "service_bonusattr")
	protected List<BonusServiceAttr> serviceBonusattr;

	@XmlTransient
	private IntObjectHashMap<BonusServiceAttr> templates = new IntObjectHashMap<BonusServiceAttr>();

	/**
	 * JAXB 反序列化完成后，将加成模板写入 buffId 索引并释放列表。
	 * After JAXB unmarshalling, indexes bonus templates by buff id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BonusServiceAttr template : serviceBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		serviceBonusattr.clear();
		serviceBonusattr = null;
	}

	/**
	 * 返回已加载的加成模板数量。
	 * Returns the number of loaded bonus templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按增益 ID 获取服务加成属性。
	 * Returns the service bonus attribute for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId @return 加成属性，不存在则为 null / bonus attribute or null
	 */
	public BonusServiceAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
