package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

/**
 * LBox 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LBox implements Cloneable {

	/** 返回 ID / Returns the id */
	@Getter
	@XmlElement(required = true)
	protected int id;

	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlElement(required = true)
	protected String name;

	/** 返回 desc / Returns the desc */
	@Getter
	@XmlElement(required = true)
	protected String desc;

	/** 返回脚本 / Returns the script */
	@Getter
	@XmlElement(required = true)
	protected String script;

	/** 返回 icon / Returns the icon */
	@Getter
	@Setter
	@XmlElement(required = true)
	protected int icon;

	/** 设置 id / Sets the id */
	public void setId(int position) {
		id = 100 + position;
	}

	/** 克隆 / clone. */
	@Override
	public Object clone() {
		LBox result = new LBox();
		result.id = this.id;
		result.name = this.name;
		result.desc = this.desc;
		result.script = this.script;
		result.icon = this.icon;
		return result;
	}
}
