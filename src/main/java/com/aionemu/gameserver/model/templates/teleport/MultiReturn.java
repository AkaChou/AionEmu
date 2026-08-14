package com.aionemu.gameserver.model.templates.teleport;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 多重返回模板（静态数据/XML）。
 * Multi Return template (static data/XML).
 */

@XmlType(name = "MultiReturn")
public class MultiReturn {

	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "loc")
	private List<MultiReturnLocationList> MultiReturnList;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 按索引返回返回数据 / Returns the return data at the given index */
	public MultiReturnLocationList getReturnDataById(int id) {
		if (MultiReturnList != null) {
			return MultiReturnList.get(id);
		}
		return null;
	}

	/** 返回多个返回列表 / Returns the multi return list */
	public List<MultiReturnLocationList> getMultiReturnList() {
		return MultiReturnList;
	}
}
