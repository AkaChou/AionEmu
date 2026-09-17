package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 组装配方模板：部件列表与组装数量。
 * Assembly recipe template: parts list and assembly count.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssemblyItem")
public class AssemblyItem {
	/** 返回 ID / Returns the id */
	@XmlAttribute(required = true)
	protected int id;

	/** 返回 parts num / Returns the parts num */
	@XmlAttribute(name = "parts_num")
	protected int partsNum;

	/** 返回 proc assembly / Returns the proc assembly */
	@XmlAttribute(name = "proc_assembly")
	protected int procAssembly;

	@XmlAttribute(required = true)
	protected List<Integer> parts;

	/** 返回 parts / Returns the parts */
	public List<Integer> getParts() {
		if (parts == null) {
			parts = new ArrayList<Integer>();
		}
		return parts;
	}
}
