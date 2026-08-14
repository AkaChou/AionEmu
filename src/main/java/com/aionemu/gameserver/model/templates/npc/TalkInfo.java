package com.aionemu.gameserver.model.templates.npc;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Talk 信息模板（静态数据/XML）。
 * XML template.
 *
 * @author Ghostfur (Aion-Unique)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TalkInfo")
public class TalkInfo {

	@XmlAttribute(name = "distance")
	private int talkDistance = 2;
	@XmlAttribute(name = "delay")
	private int talkDelay;
	@XmlAttribute(name = "is_dialog")
	private boolean hasDialog;
	@XmlAttribute(name = "func_dialogs")
	private List<Integer> funcDialogIds;
	@XmlAttribute(name = "subdialog_type")
	private String subDialogType;

	/**
	 * 返回对话距离。
	 * Returns the talk distance.
	 *
	 * @return 对话距离 / the talk distance
	 */
	public int getDistance() {
		return talkDistance;
	}

	/**
	 * 返回对话延迟。
	 * Returns the talk delay.
	 *
	 * @return 对话延迟 / the talk delay
	 */
	public int getDelay() {
		return talkDelay;
	}

	/**
	 * 是否为对话型 NPC。
	 * Whether this is a dialog NPC.
	 *
	 * @return 是否有对话 / the hasDialog flag
	 */
	public boolean isDialogNpc() {
		return hasDialog;
	}

	/** 返回 func dialog ids / Returns the func dialog ids */
	public List<Integer> getFuncDialogIds() {
		return funcDialogIds;
	}

	/** 返回 sub dialog type / Returns the sub dialog type */
	public String getSubDialogType() {
		return subDialogType;
	}
}
