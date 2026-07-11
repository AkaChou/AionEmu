package com.aionemu.gameserver.model.templates.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.Race;

/**
 * 邮件模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MailTemplate")
public class MailTemplate {

	@XmlElements({ @XmlElement(name = "sender", type = Sender.class), @XmlElement(name = "title", type = Title.class),
			@XmlElement(name = "header", type = Header.class), @XmlElement(name = "body", type = Body.class),
			@XmlElement(name = "tail", type = Tail.class) })
	private List<MailPart> mailParts;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "race", required = true)
	protected Race race;

	@XmlTransient
	private Map<MailPartType, MailPart> mailPartsMap = new HashMap<MailPartType, MailPart>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (MailPart part : mailParts) {
			mailPartsMap.put(part.getType(), part);
		}
		mailParts.clear();
		mailParts = null;
	}

	/** 返回 sender / Returns the sender */
	public MailPart getSender() {
		return mailPartsMap.get(MailPartType.SENDER);
	}

	/** 获取称号。 / Returns the title. */
	public MailPart getTitle() {
		return mailPartsMap.get(MailPartType.TITLE);
	}

	/** 返回头 / Returns the header*/
	public MailPart getHeader() {
		return mailPartsMap.get(MailPartType.HEADER);
	}

	/** 返回 body / Returns the body */
	public MailPart getBody() {
		return mailPartsMap.get(MailPartType.BODY);
	}

	/** 返回 tail / Returns the tail */
	public MailPart getTail() {
		return mailPartsMap.get(MailPartType.TAIL);
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回 formatted title / Returns the formatted title */
	public String getFormattedTitle(IMailFormatter customFormatter) {
		return getTitle().getFormattedString(customFormatter);
	}

	/** 返回 formatted message / Returns the formatted message */
	public String getFormattedMessage(IMailFormatter customFormatter) {
		String headerStr = getHeader().getFormattedString(customFormatter);
		String bodyStr = getBody().getFormattedString(customFormatter);
		String tailStr = getTail().getFormattedString(customFormatter);
		String message = headerStr;
		if (StringUtils.isEmpty(message)) {
			message = bodyStr;
		} else if (!StringUtils.isEmpty(bodyStr)) {
			message = message + "," + bodyStr;
		}
		if (StringUtils.isEmpty(message)) {
			message = tailStr;
		} else if (!StringUtils.isEmpty(tailStr)) {
			message = message + "," + tailStr;
		}
		return message;
	}
}
