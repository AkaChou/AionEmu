package com.aionemu.gameserver.model.templates.item;


import com.aionemu.boot.i18n.I18n;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.Rnd;

/**
 * Random 物品模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author vlog
 */
@XmlType(name = "RandomItem")
@Slf4j
public class RandomItem {

	@XmlAttribute(name = "type")
	protected RandomType type;
	@XmlAttribute(name = "count")
	protected int count;

	@XmlAttribute(name = "rnd_min")
	public int rndMin;

	@XmlAttribute(name = "rnd_max")
	public int rndMax;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 获取类型。 / Returns the type. */
	public RandomType getType() {
		return type;
	}

	/** 返回 rnd min / Returns the rnd min */
	public int getRndMin() {
		return rndMin;
	}

	/** 返回 rnd max / Returns the rnd max */
	public int getRndMax() {
		return rndMax;
	}

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
