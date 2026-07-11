package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * LBox 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LBox implements Cloneable {

	@XmlElement(required = true)
	protected int id;

	@XmlElement(required = true)
	protected String name;

	@XmlElement(required = true)
	protected String desc;

	@XmlElement(required = true)
	protected String script;

	@XmlElement(required = true)
	protected int icon;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 设置 id / Sets the id */
	public void setId(int position) {
		id = 100 + position;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 desc / Returns the desc */
	public String getDesc() {
		return desc;
	}

	/** 返回脚本 / Returns the script */
	public String getScript() {
		return script;
	}

	/** 返回 icon / Returns the icon */
	public int getIcon() {
		return icon;
	}

	/** 设置 icon / Sets the icon */
	public void setIcon(int id) {
		icon = id;
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
