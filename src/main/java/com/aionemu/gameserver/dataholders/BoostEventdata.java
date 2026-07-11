package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.event.BoostEvents;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 增益活动数据容器，按 ID 索引增益活动模板。
 * Boost-event data holder, indexing boost event templates by id.
 *
 * Created by wanke on 02/03/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "boost_events")
public class BoostEventdata {
	@XmlElement(name = "boost_event")
	protected List<BoostEvents> bonusServiceBonusattr;

	@XmlTransient
	private IntObjectHashMap<BoostEvents> templates = new IntObjectHashMap<BoostEvents>();

	@XmlTransient
	private Map<Integer, BoostEvents> templatesMap = new HashMap<Integer, BoostEvents>();

	/**
	 * JAXB 反序列化完成后，将列表写入双索引映射并释放列表。
	 * After JAXB unmarshalling, populates both index maps and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BoostEvents template : bonusServiceBonusattr) {
			templates.put(template.getId(), template);
			templatesMap.put(template.getId(), template);
		}
		bonusServiceBonusattr.clear();
		bonusServiceBonusattr = null;
	}

	/**
	 * 返回已加载的增益活动数量。
	 * Returns the number of loaded boost events.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按 ID 获取增益活动模板。
	 * Returns the boost event template for the given id.
	 *
	 * event id
	 *
	 * @param buffId @return 模板，不存在则为 null / template or null
	 */
	public BoostEvents getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}

	/**
	 * 返回全部增益活动映射。
	 * Returns the full boost event map.
	 *
	 * @return ID 到模板的映射 / map of id to template
	 */
	public Map<Integer, BoostEvents> getAll() {
		return templatesMap;
	}
}
