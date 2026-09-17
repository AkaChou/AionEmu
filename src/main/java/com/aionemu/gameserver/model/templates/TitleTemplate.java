package com.aionemu.gameserver.model.templates;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import lombok.Getter;

/**
 * 称号模板（静态数据/XML）。
 * Title template (static data / XML).
 *
 * @author xavier
 */
@Getter
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "title_templates")
public class TitleTemplate implements StatOwner {

	@XmlAttribute(name = "id", required = true)
	@XmlID
	private String id;

	@XmlElement(name = "modifiers", required = false)
	protected ModifiersTemplate modifiers;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race", required = true)
	private Race race;

	/** 返回标题 ID / Returns the title id */
	private int titleId;

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "nameId")
	private int nameId;

	@XmlAttribute(name = "desc")
	private String description;

	/** 返回 desc / Returns the desc */
	public String getDesc() {
		return description;
	}

	/** 获取修正器。 / Returns the modifiers. */
	public List<StatFunction> getModifiers() {
		if (modifiers != null) {
			return modifiers.getModifiers();
		}
		return null;
	}

	void afterUnmarshal(Unmarshaller u, Object parent) {
		this.titleId = Integer.parseInt(id);
	}
}
