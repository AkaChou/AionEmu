package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;

/**
 * 治疗物体数据容器，持有全部治疗区域/物体模板列表。
 * Curing object data holder, retaining all curing zone/object templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "curingObject" })
@XmlRootElement(name = "curing_objects")
public class CuringObjectsData {

	@XmlElement(name = "curing_object")
	protected List<CuringTemplate> curingObject;

	@XmlTransient
	private List<CuringTemplate> curingObjects = new ArrayList<CuringTemplate>();

	/**
	 * JAXB 反序列化完成后，将列表拷贝到运行时集合。
	 * After JAXB unmarshalling, copies templates into the runtime list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (CuringTemplate template : curingObject) {
			curingObjects.add(template);
		}
	}

	/**
	 * 返回已加载的治疗物体数量。
	 * Returns the number of loaded curing objects.
	 *
	 * template count
	 */
	public int size() {
		return curingObjects.size();
	}

	/**
	 * 返回全部治疗物体模板列表。
	 * Returns all curing object templates.
	 *
	 * template list
	 */
	public List<CuringTemplate> getCuringObject() {
		return curingObjects;
	}
}
