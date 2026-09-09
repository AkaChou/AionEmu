package com.aionemu.gameserver.model.ai;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 血量百分比触发点：按血量阈值配置召唤物组与技能。
 * HP-percentage trigger: configures summon groups and a skill by HP threshold.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Percentage")
public class Percentage {

	/** 返回 percent / Returns the percent */
	@Getter
	@XmlAttribute(name = "percent")
	protected int percent;
	/** 返回技能 ID / Returns the skill id */
	@Getter
	@XmlAttribute(name = "skillId")
	protected int skillId = 0;
	/**
	 * 是否为独立召唤（按玩家分别触发）。
	 * Whether the summons trigger individually per player.
	 *
	 * @return 是否独立召唤 / Whether individual
	 */
	@Getter
	@XmlAttribute(name = "isIndividual")
	protected boolean isIndividual = false;
	/** 返回 summons / Returns the summons */
	@Getter
	@XmlElement(name = "summonGroup")
	protected List<SummonGroup> summons;
}
