package com.aionemu.gameserver.model.templates.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * Mails 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "sysMailTemplates" })
@XmlRootElement(name = "mails")
public class Mails {

	@XmlElement(name = "mail")
	private List<SysMail> sysMailTemplates;

	@XmlTransient
	private Map<String, SysMail> sysMailByName = new HashMap<String, SysMail>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (SysMail template : sysMailTemplates) {
			String sysMailName = template.getName().toLowerCase();
			sysMailByName.put(sysMailName, template);
		}
		sysMailTemplates.clear();
		sysMailTemplates = null;
	}

	/** 获取邮件模板。 / Returns the mail template. */
	public MailTemplate getMailTemplate(String name, String eventName, Race playerRace) {
		SysMail template = (SysMail) sysMailByName.get(name.toLowerCase());
		if (template == null) {
			return null;
		}
		return template.getTemplate(eventName, playerRace);
	}

	/** 大小 / size. */
	public int size() {
		return sysMailByName.values().size();
	}
}
