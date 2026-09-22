package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.Getter;

/**
 * 分解套装列表根模板：容纳多个分解物品组。
 * Root template for a disassemble set list: holds multiple item groups.
 * @author BeckUp.Media
 */
@Getter
@XmlRootElement(name = "disassemble_set_list")
public class DisassembleSetList
{
	/** 返回物品组 / Returns the item groups*/
	@XmlElement(name = "itemGroup")
	private List<DisassembleItemGroups> ItemGroups;
}
