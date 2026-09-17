package com.aionemu.gameserver.model.templates.item;


import com.aionemu.boot.i18n.I18n;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 合成/制作产物物品模板：数量与随机范围。
 * Resulted item template: count and random range.
 *
 * @author antness
 */
@Getter
@XmlType(name = "ResultedItem")
@Slf4j
public class ResultedItem {

	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "id")
	public int itemId;
	/** 获取计数。 / Returns the count. */
	@XmlAttribute(name = "count")
	public int count;
	/** 返回随机下限 / Returns the rnd min */
	@XmlAttribute(name = "rnd_min")
	public int rndMin;
	/** 返回随机上限 / Returns the rnd max */
	@XmlAttribute(name = "rnd_max")
	public int rndMax;
	@XmlAttribute(name = "race")
	public Race race = Race.PC_ALL;

	/** 获取玩家职业。 / Returns the player class. */
	@XmlAttribute(name = "player_class")
	public PlayerClass playerClass = PlayerClass.ALL;

	/** 获取种族。 / Returns the race. */
	public final Race getRace() {
		return race;
	}

	/** 获取结果计数。 / Returns the result count. */
	public final int getResultCount() {
		if (count == 0 && rndMin == 0 && rndMax == 0) {
			return 1;
		} else if (rndMin > 0 || rndMax > 0) {
			if (rndMax < rndMin) {
				log.warn(I18n.get("log.8cf0fd3acbc3", rndMin, rndMax));
				return 1;
			} else {
				return Rnd.get(rndMin, rndMax);
			}
		} else {
			return count;
		}
	}
}
