package com.aionemu.gameserver.model.templates.mail;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Tail 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tail")
@XmlSeeAlso({ MailPart.class })
public class Tail extends MailPart {

	@XmlAttribute(name = "type")
	protected MailPartType type;

	/** 获取类型。 / Returns the type. */
	@Override
	public MailPartType getType() {
		if (type == null) {
			return MailPartType.TAIL;
		}
		return type;
	}

	/** 返回参数值 / Returns the param value*/
	@Override
	public String getParamValue(String name) {
		return "";
	}
}
