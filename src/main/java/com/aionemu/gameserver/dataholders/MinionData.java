package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.minion.MinionTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 守护灵 / 宠物（Minion）模板数据容器，按 ID 索引 {@link MinionTemplate}。
 * Minion template data holder, indexing {@link MinionTemplate} by id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "minions")
public class MinionData {
	@XmlElement(name = "minion")
	private List<MinionTemplate> minionTemplates;
	@XmlTransient
	private IntObjectHashMap<MinionTemplate> minionData = new IntObjectHashMap<>();
	@XmlTransient
	private List<Integer> minionDataList = new ArrayList<Integer>();

	/**
	 * JAXB 反序列化完成后，按 ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by id and clears the list.
	 */
	void afterUnmarshal(final Unmarshaller unmarshaller, final Object o) {
		for (MinionTemplate minionTemplate : minionTemplates) {
			minionData.put(minionTemplate.getId(), minionTemplate);
			minionDataList.add(minionTemplate.getId());
		}
		minionTemplates.clear();
		minionTemplates = null;
	}

	/**
	 * 返回已加载的守护灵模板数量。
	 * Returns the number of loaded minion templates.
	 *
	 * @return 已加载的Minion 模板数量 / Returns the number of loaded minion templates.
	 */
	public int size() {
		return minionData.size();
	}

	/**
	 * 按守护灵 ID 获取模板。
	 * Returns the minion template for the given minion id.
	 *
	 * @param minionId Minion ID / minion id
	 * @return 守护灵模板或 null / minion template or null
	 */
	public MinionTemplate getMinionTemplate(int minionId) {
		return minionData.get(minionId);
	}

	/**
	 * 返回全部守护灵 ID 列表。
	 * Returns the full list of minion ids.
	 *
	 * @return 守护灵 ID 列表 / minion id list
	 */
	public List<Integer> getAll() {
		return minionDataList;
	}
}
