package com.aionemu.gameserver.model.templates.Guides;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Survey 模板（静态数据/XML）。
 * Survey template (static data/XML).
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SurveyTemplate")
public class SurveyTemplate {
	/**
	 * @return the itemId
	 */
	@Getter
	@XmlAttribute(name = "itemId")
	private int itemId;
	/**
	 * @return the count
	 */
	@Getter
	@XmlAttribute(name = "count")
	private long count;
}
