package com.aionemu.gameserver.skillengine.properties;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 首要目标属性：决定技能首要目标如何选取。
 * First-target attribute: how a skill selects its primary target.
 */
@XmlType(name = "FirstTargetAttribute")
@XmlEnum
public enum FirstTargetAttribute {

	NONE, TARGETORME, ME, MYPET, MYMASTER, TARGET, PASSIVE, TARGET_MYPARTY_NONVISIBLE, POINT;
}
