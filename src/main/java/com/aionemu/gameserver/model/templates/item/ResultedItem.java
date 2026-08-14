package com.aionemu.gameserver.model.templates.item;


import com.aionemu.boot.i18n.I18n;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

/**
 * 合成/制作产物物品模板：数量与随机范围。
 * Resulted item template: count and random range.
 *
 * @author antness
 */
@XmlType(name = "ResultedItem")
@Slf4j
public class ResultedItem {

	@XmlAttribute(name = "id")
	public int itemId;
	@XmlAttribute(name = "count")
	public int count;
	@XmlAttribute(name = "rnd_min")
	public int rndMin;
	@XmlAttribute(name = "rnd_max")
	public int rndMax;
	@XmlAttribute(name = "race")
	public Race race = Race.PC_ALL;

	@XmlAttribute(name = "player_class")
	public PlayerClass playerClass = PlayerClass.ALL;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 返回随机下限 / Returns the rnd min */
	public int getRndMin() {
		return rndMin;
	}

	/** 返回随机上限 / Returns the rnd max */
	public int getRndMax() {
		return rndMax;
	}

	/** 获取种族。 / Returns the race. */
	public final Race getRace() {
		return race;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return playerClass;
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
