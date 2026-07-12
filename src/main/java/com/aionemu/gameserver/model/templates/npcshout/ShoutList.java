package com.aionemu.gameserver.model.templates.npcshout;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * NPC 喊话列表模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShoutList", propOrder = { "npcShouts" })
public class ShoutList {

	@XmlElement(name = "shout", required = true)
	protected List<NpcShout> npcShouts;

	@XmlAttribute(name = "npc_ids", required = true)
	protected List<Integer> npcIds;

	@XmlAttribute(name = "restrict_world")
	protected Integer restrictWorld;

	/**
	 * 获取 npcShouts 属性值。 / Gets the value of the npcShouts property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the npcShouts property. <p> For example, to add a new item, do as follows: <pre> getNpcShouts().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link NpcShout }
	 */
	public List<NpcShout> getNpcShouts() {
		if (npcShouts == null) {
			npcShouts = new ArrayList<NpcShout>();
		}
		return this.npcShouts;
	}

	/**
	 * 获取 npcIds 属性值。 / Gets the value of the npcIds property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the npcIds property. <p> For example, to add a new item, do as follows: <pre> getNpcIds().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link Integer }
	 */
	public List<Integer> getNpcIds() {
		if (npcIds == null) {
			npcIds = new ArrayList<Integer>();
		}
		return this.npcIds;
	}

	 /**
	  * 获取 restrictWorld 属性值。
	  * Gets the value of the restrictWorld property
	  * @return possible object is {@link Integer }
	  */
	public int getRestrictWorld() {
		if (restrictWorld == null) {
			return 0;
		}
		return restrictWorld;
	}

	/** 置空 / make Null. */
	public void makeNull() {
		this.npcIds = null;
		this.npcShouts = null;
		this.restrictWorld = null;
	}
}
