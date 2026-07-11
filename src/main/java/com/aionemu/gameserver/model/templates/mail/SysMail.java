package com.aionemu.gameserver.model.templates.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * Sys 邮件模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SysMail", propOrder = { "templates" })
public class SysMail {

	@XmlElement(name = "template", required = true)
	private List<MailTemplate> templates;

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlTransient
	private Map<String, List<MailTemplate>> mailCaseTemplates = new HashMap<String, List<MailTemplate>>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (MailTemplate template : templates) {
			String caseName = template.getName().toLowerCase();
			List<MailTemplate> sysTemplates = mailCaseTemplates.get(caseName);
			if (sysTemplates == null) {
				sysTemplates = new ArrayList<MailTemplate>();
				mailCaseTemplates.put(caseName, sysTemplates);
			}
			sysTemplates.add(template);
		}
		templates.clear();
		templates = null;
	}

	/** 获取模板。 / Returns the template. */
	public MailTemplate getTemplate(String eventName, Race playerRace) {
		List<MailTemplate> sysTemplates = mailCaseTemplates.get(eventName.toLowerCase());
		if (sysTemplates == null) {
			return null;
		}
		for (MailTemplate template : sysTemplates) {
			if (template.getRace() == playerRace || template.getRace() == Race.PC_ALL) {
				return template;
			}
		}
		return null;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}
}
