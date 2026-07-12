package com.aionemu.gameserver.model.templates.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Component 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Component")
public class Component {
	@XmlElement(name = "component")
	protected ArrayList<ComponentElement> component;

	/** 返回 components / Returns the components */
	public Collection<ComponentElement> getComponents() {
		return component != null ? component : Collections.<ComponentElement>emptyList();
	}
}
