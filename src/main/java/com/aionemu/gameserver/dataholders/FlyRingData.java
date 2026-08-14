package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;

/**
 * 飞行环配置数据容器，维护全部飞行环模板列表。
 * Fly ring configuration data holder for all fly ring templates.
 *
 * @author M@xx
 */
@XmlRootElement(name = "fly_rings")
@XmlAccessorType(XmlAccessType.FIELD)
public class FlyRingData {

	@XmlElement(name = "fly_ring")
	private List<FlyRingTemplate> flyRingTemplates;

	/**
	 * 返回飞行环模板数量；若列表尚未初始化则初始化为空列表并返回 0。
	 * Returns the number of fly ring templates; initializes an empty list and returns 0 if null.
	 *
	 * @return fly ring templates; initializes an empty list and returns 0 if null数量 / Returns the number of fly ring templates; initializes an empty list and returns 0 if null.
	 */
	public int size() {
		if (flyRingTemplates == null) {
			flyRingTemplates = new ArrayList<FlyRingTemplate>();
			return 0;
		}
		return flyRingTemplates.size();
	}

	/**
	 * 返回飞行环模板列表；若尚未初始化则返回空列表。
	 * Returns the fly ring template list; returns an empty list if not yet initialized.
	 *
	 * @return 飞行环模板列表 / fly ring template list
	 */
	public List<FlyRingTemplate> getFlyRingTemplates() {
		if (flyRingTemplates == null) {
			return new ArrayList<FlyRingTemplate>();
		}
		return flyRingTemplates;
	}

	/**
	 * 批量追加飞行环模板。
	 * Appends the given fly ring templates.
	 *
	 * @param templates 待添加的模板集合 / templates to add
	 */
	public void addAll(Collection<FlyRingTemplate> templates) {
		if (flyRingTemplates == null) {
			flyRingTemplates = new ArrayList<FlyRingTemplate>();
		}
		flyRingTemplates.addAll(templates);
	}
}
