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

import com.aionemu.gameserver.model.templates.panel_cp.PanelCp;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 面板 CP（能力点）数据容器，按 ID 双索引 PanelCp 模板。
 * Panel CP data holder, dual-indexing PanelCp templates by id.
 *
 * @author Ghostfur (Aion-Unique)
 */
@XmlRootElement(name = "panel_cps")
@XmlAccessorType(XmlAccessType.FIELD)
public class PanelCpData {

	@XmlElement(name = "panel_cp")
	private List<PanelCp> pclist;

	@XmlTransient
	private IntObjectHashMap<PanelCp> cpData = new IntObjectHashMap<PanelCp>();

	@XmlTransient
	private Map<Integer, PanelCp> cpDataMap = new HashMap<Integer, PanelCp>(1);

	/**
	 * JAXB 反序列化完成后，将列表写入双索引映射。
	 * After JAXB unmarshalling, populates both index maps from the list.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (PanelCp panelCp : pclist) {
			cpData.put(panelCp.getId(), panelCp);
			cpDataMap.put(panelCp.getId(), panelCp);
		}
	}

	/**
	 * 返回已加载的面板 CP 数量。
	 * Returns the number of loaded panel CP entries.
	 *
	 * @return 已加载的面板 CP 条目数量 / Returns the number of loaded panel CP entries.
	 */
	public int size() {
		return cpData.size();
	}

	/**
	 * 按 ID 获取面板 CP 模板。
	 * Returns the panel CP template for the given id.
	 *
	 * @param id CP 条目 ID / CP entry id
	 * @return 模板，不存在则为 null / template or null
	 */
	public PanelCp getPanelCpId(int id) {
		return cpData.get(id);
	}

	/**
	 * 返回全部面板 CP 映射。
	 * Returns the full panel CP map.
	 *
	 * @return ID 到模板的映射 / map of id to template
	 */
	public Map<Integer, PanelCp> getAll() {
		return cpDataMap;
	}
}
