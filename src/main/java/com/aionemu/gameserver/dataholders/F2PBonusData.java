package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.bonus_service.F2pBonusAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * F2P 加成配置数据容器，按 buffId 索引 F2P 加成属性模板。
 * F2P bonus configuration data holder, indexed by buff id.
 *
 * Created by wanke on 12/02/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "f2pBonusattr" })
@XmlRootElement(name = "f2p_bonus")
public class F2PBonusData {

	@XmlElement(name = "f2p")
	protected List<F2pBonusAttr> f2pBonusattr;

	@XmlTransient
	private IntObjectHashMap<F2pBonusAttr> templates = new IntObjectHashMap<F2pBonusAttr>();

	/**
	 * JAXB 反序列化完成后，按 buffId 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes templates by buff id and releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (F2pBonusAttr template : f2pBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		f2pBonusattr.clear();
		f2pBonusattr = null;
	}

	/**
	 * 返回 F2P 加成模板数量。
	 * Returns the number of F2P bonus templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按 buffId 获取 F2P 加成属性模板。
	 * Returns the F2P bonus attribute template for the given buff id.
	 *
	 * bonus buff id
	 *
	 * @param buffId
	 * @return 加成属性模板，不存在则为 null / bonus attribute template, or null if absent
	 */
	public F2pBonusAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
