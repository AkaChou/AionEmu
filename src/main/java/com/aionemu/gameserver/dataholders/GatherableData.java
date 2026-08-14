package com.aionemu.gameserver.dataholders;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 可采集物配置数据容器，按模板 ID 索引采集物模板。
 * Gatherable configuration data holder, indexed by template id.
 *
 * @author ATracer
 */
@XmlRootElement(name = "gatherable_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class GatherableData {

	@XmlElement(name = "gatherable_template")
	private List<GatherableTemplate> gatherables;

	/** 全部采集物模板的 ID 索引 / ID index of all gatherable templates */
	private IntObjectHashMap<GatherableTemplate> gatherableData = new IntObjectHashMap<GatherableTemplate>();

	/**
	 * JAXB 反序列化完成后，排序材料列表并按模板 ID 建立索引。
	 * After JAXB unmarshalling, sorts material lists and indexes templates by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (GatherableTemplate gatherable : gatherables) {
			if (gatherable.getMaterials() != null) {
				Collections.sort(gatherable.getMaterials().getMaterial());
			}
			if (gatherable.getExtraMaterials() != null) {
				Collections.sort(gatherable.getExtraMaterials().getMaterial());
			}
			gatherableData.put(gatherable.getTemplateId(), gatherable);
		}
		gatherables = null;
	}

	/**
	 * 返回采集物模板数量。
	 * Returns the number of gatherable templates.
	 *
	 * @return 采集物模板数量 / Returns the number of gatherable templates.
	 */
	public int size() {
		return gatherableData.size();
	}

	/**
	 * 按 ID 获取采集物模板。
	 * Returns the gatherable template for the given id.
	 *
	 * @param id 采集物模板 ID / gatherable template id
	 * @return 采集物模板，不存在则为 null / gatherable template, or null if absent
	 */
	public GatherableTemplate getGatherableTemplate(int id) {
		return gatherableData.get(id);
	}
}
