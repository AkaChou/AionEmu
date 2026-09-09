package com.aionemu.gameserver.model.templates.item;


import com.aionemu.boot.i18n.I18n;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.Rnd;
import lombok.Getter;

/**
 * 随机物品模板：随机数量或等级范围。
 * Random item template: random count or grade range.
 *
 * @author vlog
 */
@XmlType(name = "RandomItem")
@Slf4j
public class RandomItem {

	/** 获取类型。 / Returns the type. */
	@Getter
	@XmlAttribute(name = "type")
	protected RandomType type;
	/** 获取计数。 / Returns the count. */
	@Getter
	@XmlAttribute(name = "count")
	protected int count;

	/** 返回随机下限 / Returns the rnd min */
	@Getter
	@XmlAttribute(name = "rnd_min")
	public int rndMin;

	/** 返回随机上限 / Returns the rnd max */
	@Getter
	@XmlAttribute(name = "rnd_max")
	public int rndMax;

	/** 获取结果计数。 / Returns the result count. */
	public final int getResultCount() {
		if ((count == 0) && (rndMin == 0) && (rndMax == 0)) {
			return 1;
		}
		if ((rndMin > 0) || (rndMax > 0)) {
			if (rndMax < rndMin) {
				log.warn(I18n.get("log.d278b3682428", rndMin, rndMax));
				return 1;
			}
			return Rnd.get(rndMin, rndMax);
		}
		return count;
	}
}
