package com.aionemu.gameserver.model.templates.npc;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Talk 信息模板（静态数据/XML）。
 * XML template. / XML template.
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
	 * @return the talkDistance
	 */
	public int getDistance() {
		return talkDistance;
	}

	/**
	 * @return the talk_delay
	 */
	public int getDelay() {
		return talkDelay;
	}

	/**
	 * @return the hasDialog
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
