package com.aionemu.gameserver.model.templates.npcshout;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * NPC 喊话分组模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShoutGroup", propOrder = { "shoutNpcs" })
public class ShoutGroup {

	@XmlElement(name = "shout_npcs", required = true)
	protected List<ShoutList> shoutNpcs;

	@XmlAttribute(name = "client_ai")
	protected String clientAi;

	/**
	 * 获取 shoutNpcs 属性值。 / Gets the value of the shoutNpcs property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the shoutNpcs property. <p> For example, to add a new item, do as follows: <pre> getShoutNpcs().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link ShoutList }
	 */
	public List<ShoutList> getShoutNpcs() {
		if (shoutNpcs == null) {
			shoutNpcs = new ArrayList<ShoutList>();
		}
		return this.shoutNpcs;
	}

	 /**
	  * 获取 clientAi 属性值。
	  * Gets the value of the clientAi property
	  * @return possible object is {@link String }
	  */
	public String getClientAi() {
		return clientAi;
	}

	/** 置空 / make Null. */
	public void makeNull() {
		this.shoutNpcs = null;
		this.clientAi = null;
	}
}
