package com.aionemu.gameserver.skillengine.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 真实技能中尚未编译进现有运行模型的无损字段。
 * Lossless fields from retail skills not yet compiled into the current runtime model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetailSkillFields", propOrder = "fields")
public class RetailSkillFields {

	@XmlElement(name = "field")
	private List<Field> fields;

	public List<Field> getFields() {
		if (fields == null) {
			fields = new ArrayList<>();
		}
		return fields;
	}

	public String get(String name) {
		return get(name, 0);
	}

	public String get(String name, int occurrence) {
		// ponytail: linear scan is enough for diagnostic fields; index if runtime consumers become hot.
		for (Field field : getFields()) {
			if (field.name.equals(name) && field.occurrence == occurrence) {
				return field.value;
			}
		}
		return null;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "RetailSkillField")
	public static class Field {

		@XmlAttribute(required = true)
		private String name;

		@XmlAttribute
		private int occurrence;

		@XmlAttribute(required = true)
		private String value;

		public String getName() {
			return name;
		}

		public int getOccurrence() {
			return occurrence;
		}

		public String getValue() {
			return value;
		}
	}
}
