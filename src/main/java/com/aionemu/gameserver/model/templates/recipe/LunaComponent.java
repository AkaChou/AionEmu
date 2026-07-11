package com.aionemu.gameserver.model.templates.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华 Component 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaComponent")
public class LunaComponent {
	@XmlElement(name = "luna_component")
	protected ArrayList<LunaComponentElement> luna_component;

	/** 返回 components / Returns the components */
	public Collection<LunaComponentElement> getComponents() {
		return luna_component != null ? luna_component : Collections.<LunaComponentElement>emptyList();
	}
}
