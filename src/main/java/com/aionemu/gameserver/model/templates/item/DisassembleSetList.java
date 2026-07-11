package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DisassembleSet 列表模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "disassemble_set_list")
public class DisassembleSetList
{
	@XmlElement(name = "itemGroup")
	private List<DisassembleItemGroups> ItemGroups;

	/** 返回物品组 / Returns the item groups*/
	public List<DisassembleItemGroups> getItemGroups()
	{
		return ItemGroups;
	}
}
