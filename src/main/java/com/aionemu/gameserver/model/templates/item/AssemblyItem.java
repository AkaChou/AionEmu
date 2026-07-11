package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Assembly 物品模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssemblyItem")
public class AssemblyItem {
	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(name = "parts_num")
	protected int partsNum;

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

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 设置 id / Sets the id */
	public void setId(int value) {
		id = value;
	}

	/** 返回 parts num / Returns the parts num */
	public int getPartsNum() {
		return partsNum;
	}

	/** 设置 parts num / Sets the parts num */
	public void setPartsNum(int value) {
		partsNum = value;
	}

	/** 返回 proc assembly / Returns the proc assembly */
	public int getProcAssembly() {
		return procAssembly;
	}

	/** 设置 proc assembly / Sets the proc assembly */
	public void setProcAssembly(int value) {
		procAssembly = value;
	}
}
