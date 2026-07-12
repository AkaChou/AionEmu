package com.aionemu.gameserver.model.templates.tribe;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TribeClass;

/**
 * 部落模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tribe")
public class Tribe {
	@XmlList
	protected List<TribeClass> aggressive;

	@XmlList
	protected List<TribeClass> hostile;

	@XmlList
	protected List<TribeClass> friendly;

	@XmlList
	protected List<TribeClass> neutral;

	@XmlList
	protected List<TribeClass> none;

	@XmlList
	protected List<TribeClass> support;

	@XmlAttribute
	protected TribeClass base = TribeClass.NONE;

	@XmlAttribute(required = true)
	protected TribeClass name;

	/** 返回敌对 / Returns the aggressive*/
	public List<TribeClass> getAggressive() {
		if (aggressive == null) {
			aggressive = Collections.emptyList();
		}
		return this.aggressive;
	}

	/** 返回 hostile / Returns the hostile */
	public List<TribeClass> getHostile() {
		if (hostile == null) {
			hostile = Collections.emptyList();
		}
		return this.hostile;
	}

	/** 返回 friendly / Returns the friendly */
	public List<TribeClass> getFriendly() {
		if (friendly == null) {
			friendly = Collections.emptyList();
		}
		return this.friendly;
	}

	/** 返回 neutral / Returns the neutral */
	public List<TribeClass> getNeutral() {
		if (neutral == null) {
			neutral = Collections.emptyList();
		}
		return this.neutral;
	}

	/** 返回无 / Returns the none*/
	public List<TribeClass> getNone() {
		if (none == null) {
			none = Collections.emptyList();
		}
		return this.none;
	}

	/** 返回 support / Returns the support */
	public List<TribeClass> getSupport() {
		if (support == null) {
			support = Collections.emptyList();
		}
		return this.support;
	}

	/** 获取基础。 / Returns the base. */
	public TribeClass getBase() {
		return base;
	}

	/** 获取名称。 / Returns the name. */
	public TribeClass getName() {
		return name;
	}

	/** 是否为守卫。 / Whether guard. */
	public final boolean isGuard() {
		return name.isGuard();
	}

	/** 是否基础 / Whether basic */
	public final boolean isBasic() {
		return name.isBasicClass();
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return name + " (" + base + ")";
	}
}
