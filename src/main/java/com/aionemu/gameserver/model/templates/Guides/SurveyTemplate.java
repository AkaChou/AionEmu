package com.aionemu.gameserver.model.templates.Guides;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Survey 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SurveyTemplate")
public class SurveyTemplate {
	@XmlAttribute(name = "itemId")
	private int itemId;
	@XmlAttribute(name = "count")
	private long count;

	/**
	 * @return the count
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * @return the itemId
	 */
	public int getItemId() {
		return this.itemId;
	}
}
