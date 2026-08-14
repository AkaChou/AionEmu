package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.serial_guard.GuardRankRestriction;
import com.aionemu.gameserver.model.templates.serial_guard.GuardTypeRestriction;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 序列守卫限制数据容器，分别按等级与类型索引限制规则。
 * Serial guard restriction data holder, indexing rules by rank and type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "guardRankRestriction", "guardTypeRestriction" })
@XmlRootElement(name = "serial_guards")
public class SerialGuardData {
	@XmlElement(name = "guard_rank_restriction")
	protected List<GuardRankRestriction> guardRankRestriction;

	@XmlElement(name = "guard_type_restriction")
	protected List<GuardTypeRestriction> guardTypeRestriction;

	@XmlTransient
	private IntObjectHashMap<GuardRankRestriction> templates = new IntObjectHashMap<GuardRankRestriction>();

	@XmlTransient
	private IntObjectHashMap<GuardTypeRestriction> templatesType = new IntObjectHashMap<GuardTypeRestriction>();

	/**
	 * JAXB 反序列化完成后，分别写入等级/类型索引并释放列表。
	 * After JAXB unmarshalling, indexes rank/type restrictions and releases the lists.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (GuardRankRestriction template : guardRankRestriction) {
			templates.put(template.getRankNum(), template);
		}
		guardRankRestriction.clear();
		guardRankRestriction = null;
		////////////////////////////
		for (GuardTypeRestriction template : guardTypeRestriction) {
			templatesType.put(template.getTypeNum(), template);
		}
		guardTypeRestriction.clear();
		guardTypeRestriction = null;
	}

	/**
	 * 返回等级与类型限制的合计数量。
	 * Returns the total count of rank and type restrictions.
	 *
	 * @return 限制总数 / total restriction count
	 */
	public int size() {
		return templates.size() + templatesType.size();
	}

	/**
	 * 按等级编号获取守卫等级限制。
	 * Returns the guard rank restriction for the given rank number.
	 *
	 * @param rank 等级编号 / rank number
	 * @return 等级限制，不存在则为 null / rank restriction or null
	 */
	public GuardRankRestriction getGuardRankRestriction(int rank) {
		return templates.get(rank);
	}

	/**
	 * 按类型编号获取守卫类型限制。
	 * Returns the guard type restriction for the given type number.
	 *
	 * @param type 类型编号 / type number
	 * @return 类型限制，不存在则为 null / type restriction or null
	 */
	public GuardTypeRestriction getGuardTypeRestriction(int type) {
		return templatesType.get(type);
	}
}
