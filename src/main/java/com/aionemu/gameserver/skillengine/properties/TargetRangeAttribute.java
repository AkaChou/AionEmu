package com.aionemu.gameserver.skillengine.properties;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 目标范围属性：单体/队伍/区域/坐标点等范围类型。
 * Target range attribute: only-one/party/area/point and related range types.
 *
 * @author ATracer
 */
@XmlType(name = "TargetRangeAttribute")
@XmlEnum
public enum TargetRangeAttribute {

	NONE, ONLYONE, PARTY, AREA, PARTY_WITHPET, POINT;
}
