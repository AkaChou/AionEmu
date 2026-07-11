package com.aionemu.gameserver.model.templates.mail;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

/**
 * 邮件 Part 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MailPart")
@XmlSeeAlso({ Sender.class, Header.class, Body.class, Tail.class, Title.class })
public abstract class MailPart extends StringParamList implements IMailFormatter {

	@XmlAttribute(name = "id")
	protected Integer id;

	/** 获取类型。 / Returns the type. */
	public MailPartType getType() {
		return MailPartType.CUSTOM;
	}

	/** 返回 ID / Returns the id */
	public Integer getId() {
		return id;
	}

	/** 返回 formatted string / Returns the formatted string */
	public String getFormattedString(IMailFormatter customFormatter) {
		String result = "";
		IMailFormatter formatter = this;
		if (customFormatter != null) {
			formatter = customFormatter;
		}
		result = getFormattedString(getType());

		String[] paramValues = new String[getParam().size()];
		for (int i = 0; i < getParam().size(); i++) {
			StringParamList.Param param = (StringParamList.Param) getParam().get(i);
			paramValues[i] = formatter.getParamValue(param.getId());
		}
		String joinedParams = StringUtils.join(paramValues, ',');
		if (StringUtils.isEmpty(result)) {
			return joinedParams;
		}
		if (!StringUtils.isEmpty(joinedParams)) {
			result = result + "," + joinedParams;
		}
		return result;
	}

	/** 返回 formatted string / Returns the formatted string */
	@Override
	public String getFormattedString(MailPartType partType) {
		String result = "";
		if (id > 0) {
			result = result + id.toString();
		}
		return result;
	}
}
