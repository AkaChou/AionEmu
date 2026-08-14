package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.road.RoadTemplate;

/**
 * 道路模板数据容器，持有全部 RoadTemplate 列表。
 * Road template data holder storing the full RoadTemplate list.
 *
 * @author SheppeR
 */
@XmlRootElement(name = "roads")
@XmlAccessorType(XmlAccessType.FIELD)
public class RoadData {

	@XmlElement(name = "road")
	private List<RoadTemplate> roadTemplates;

	/**
	 * 返回道路模板数量；列表为空时初始化为空列表并返回 0。
	 * Returns the number of road templates; initializes an empty list and returns 0 when null.
	 *
	 * @return road templates; initializes an empty list and returns 0 when null数量 / Returns the number of road templates; initializes an empty list and returns 0 when null.
	 */
	public int size() {
		if (roadTemplates == null) {
			roadTemplates = new ArrayList<RoadTemplate>();
			return 0;
		}
		return roadTemplates.size();
	}

	/**
	 * 返回道路模板列表；为空时返回新空列表。
	 * Returns the road template list; returns a new empty list when null.
	 *
	 * @return 道路模板列表 / road template list
	 */
	public List<RoadTemplate> getRoadTemplates() {
		if (roadTemplates == null) {
			return new ArrayList<RoadTemplate>();
		}
		return roadTemplates;
	}

	/**
	 * 批量追加道路模板。
	 * Appends all given road templates.
	 *
	 * @param templates 待追加的模板集合 / templates to append
	 */
	public void addAll(Collection<RoadTemplate> templates) {
		if (roadTemplates == null) {
			roadTemplates = new ArrayList<RoadTemplate>();
		}
		roadTemplates.addAll(templates);
	}
}
