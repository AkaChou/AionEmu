package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.recipe.LunaTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 露娜配方模板数据容器，按 ID 索引并按种族分组。
 * Luna recipe template data holder, indexing by id and grouping by race.
 *
 * Made by Ghostfur (Aion-Unique)
 */
@XmlRootElement(name = "luna_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class LunaData {

	@XmlElement(name = "luna_template")
	protected List<LunaTemplate> list;

	private IntObjectHashMap<LunaTemplate> lunaData;

	private List<LunaTemplate> elyos, asmos, any;

	/**
	 * JAXB 反序列化完成后，按 ID 索引并按种族分组，随后释放原始列表。
	 * After JAXB unmarshalling, indexes by id, groups by race, then clears the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		lunaData = new IntObjectHashMap<LunaTemplate>();
		elyos = new ArrayList<>();
		asmos = new ArrayList<>();
		any = new ArrayList<>();
		for (LunaTemplate lt : list) {
			lunaData.put(lt.getId(), lt);
			switch (lt.getRace()) {
			case ASMODIANS:
				asmos.add(lt);
				break;
			case ELYOS:
				elyos.add(lt);
				break;
			case PC_ALL:
				any.add(lt);
				break;
			default:
				break;
			}
		}
		list = null;
	}

	/**
	 * 返回种族为 PC_ALL 的露娜配方列表。
	 * Returns Luna templates available to all player races.
	 *
	 * @return 通用种族配方列表 / race-agnostic recipe list
	 */
	public List<LunaTemplate> getLunaTemplatesAny() {
		return any;
	}

	/**
	 * 按 ID 获取露娜配方模板。
	 * Returns the Luna template for the given id.
	 *
	 * @param id 配方 ID / template id
	 * @return 配方模板或 null / recipe template or null
	 */
	public LunaTemplate getLunaTemplateById(int id) {
		return lunaData.get(id);
	}

	/**
	 * 返回全部露娜配方映射。
	 * Returns the full Luna template map.
	 *
	 * @return ID 到配方模板的映射 / map of id to recipe template
	 */
	public IntObjectHashMap<LunaTemplate> getLunaTemplates() {
		return lunaData;
	}

	/**
	 * 返回已加载的露娜配方数量。
	 * Returns the number of loaded Luna templates.
	 *
	 * @return 已加载的Luna 模板数量 / Returns the number of loaded Luna templates.
	 */
	public int size() {
		return lunaData.size();
	}
}
