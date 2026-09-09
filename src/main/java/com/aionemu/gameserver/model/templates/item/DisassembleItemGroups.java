package com.aionemu.gameserver.model.templates.item;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.Getter;

/**
 * 分解物品组模板：按等级、职业与种族过滤产出条目。
 * Disassemble item group template: filters entries by level, class and race.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "itemGroup")
public class DisassembleItemGroups
{
	@Getter
	@XmlAttribute(name = "gProb")
	private int GroupProb;
	/** 获取最小等级。 / Returns the min level. */
	@Getter
	@XmlAttribute(name = "minLevel")
	private int MinLevel;
	/** 获取最大等级。 / Returns the max level. */
	@Getter
	@XmlAttribute(name = "maxLevel")
	private int MaxLevel;
	@XmlList
	@XmlAttribute(name = "onlyClass")
	private List<PlayerClass> OnlyClass;
	@XmlAttribute(name = "race")
	private Race PlayerRace = Race.PC_ALL;
	/** 获取队伍物品。 / Returns the group items. */
	@Getter
	@XmlElement(name = "item")
	private List<DisassembleItems> GroupItems;

	/** 获取种族。 / Returns the race. */
	public Race getRace()
	{
		return PlayerRace;
	}
	/** 获取玩家职业列表。 / Returns the player class list. */
	public List<PlayerClass> getPlayerClassList()
	{
		return OnlyClass;
	}
}
