package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 激怒触发目标：自身或对手。
 * Provoke target: self or opponent.
 *
 * @author ATracer
 */
@XmlType(name = "ProvokeTarget")
@XmlEnum
public enum ProvokeTarget {

	/** 自身 / Self */
	ME,
	/** 对手 / Opponent */
	OPPONENT;
}
