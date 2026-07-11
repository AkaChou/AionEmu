package com.aionemu.gameserver.skillengine.change;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 属性变更函数：加值、百分比或直接替换。
 * Stat change function: additive, percent-based, or replace.
 *
 * @author ATracer
 */
@XmlType(name = "Func")
@XmlEnum
public enum Func {

	ADD, PERCENT, REPLACE;
}
