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

import com.aionemu.gameserver.model.templates.springzones.SpringTemplate;

/**
 * 泉水/温泉对象数据容器，加载并持有全部泉水区域模板。
 * Spring-object data holder that loads and retains all spring-zone templates.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "springObject" })
@XmlRootElement(name = "spring_objects")
public class SpringObjectsData {
	@XmlElement(name = "spring_object")
	protected List<SpringTemplate> springObject;

	@XmlTransient
	private List<SpringTemplate> springObjects = new ArrayList<SpringTemplate>();

	/**
	 * JAXB 反序列化完成后，将泉水模板复制到运行时列表。
	 * After JAXB unmarshalling, copies spring templates into the runtime list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (SpringTemplate template : springObject)
			springObjects.add(template);
	}

	/**
	 * 返回已加载的泉水对象数量。
	 * Returns the number of loaded spring objects.
	 *
	 * template count
	 */
	public int size() {
		return springObjects.size();
	}

	/**
	 * 返回全部泉水对象模板列表。
	 * Returns the full list of spring-object templates.
	 *
	 * @return 泉水模板列表 / spring template list
	 */
	public List<SpringTemplate> getSpringObject() {
		return springObjects;
	}
}
