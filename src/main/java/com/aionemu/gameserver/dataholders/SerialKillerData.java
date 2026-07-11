package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.serial_killer.RankRestriction;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 连环杀手等级限制数据容器，按等级编号索引。
 * Serial killer rank restriction data holder, indexed by rank number.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "rankRestriction" })
@XmlRootElement(name = "serial_killers")
public class SerialKillerData {
	@XmlElement(name = "rank_restriction")
	protected List<RankRestriction> rankRestriction;

	@XmlTransient
	private IntObjectHashMap<RankRestriction> templates = new IntObjectHashMap<RankRestriction>();

	/**
	 * JAXB 反序列化完成后，将等级限制写入索引并释放列表。
	 * After JAXB unmarshalling, indexes rank restrictions and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (RankRestriction template : rankRestriction) {
			templates.put(template.getRankNum(), template);
		}
		rankRestriction.clear();
		rankRestriction = null;
	}

	/**
	 * 返回已加载的等级限制数量。
	 * Returns the number of loaded rank restrictions.
	 *
	 * restriction count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按等级编号获取连环杀手等级限制。
	 * Returns the serial killer rank restriction for the given rank.
	 *
	 * @param rank 等级编号 / rank number
	 * @return 等级限制，不存在则为 null / rank restriction or null
	 */
	public RankRestriction getRankRestriction(int rank) {
		return templates.get(rank);
	}
}
