package com.aionemu.gameserver.model.templates.Guides;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Survey 模板（静态数据/XML）。
 * Survey template (static data/XML).
 * @author xTz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SurveyTemplate")
public class SurveyTemplate {
	@XmlAttribute(name = "itemId")
	private int itemId;
	@XmlAttribute(name = "count")
	private long count;
}
