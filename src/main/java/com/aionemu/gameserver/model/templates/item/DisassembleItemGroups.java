package com.aionemu.gameserver.model.templates.item;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Disassemble 物品 Groups 模板（静态数据/XML）。
 * XML template.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "itemGroup")
public class DisassembleItemGroups
{
	@XmlAttribute(name = "gProb")
	private int GroupProb;
	@XmlAttribute(name = "minLevel")
	private int MinLevel;
	@XmlAttribute(name = "maxLevel")
	private int MaxLevel;
	@XmlList
	@XmlAttribute(name = "onlyClass")
	private List<PlayerClass> OnlyClass;
	@XmlAttribute(name = "race")
	private Race PlayerRace = Race.PC_ALL;
	@XmlElement(name = "item")
	private List<DisassembleItems> GroupItems;

	public int getGroupProb()
	{
		return GroupProb;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel()
	{
		return MinLevel;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel()
	{
		return MaxLevel;
	}
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

	/** 获取队伍物品。 / Returns the group items. */
	public List<DisassembleItems> getGroupItems()
	{
		return GroupItems;
	}
}
