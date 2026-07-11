package com.aionemu.gameserver.skillengine.properties;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 目标关系属性：敌人/友方/队伍/全部等敌友筛选。
 * Target relation attribute: enemy/friend/party/all relation filters.
 *
 * @author ATracer
 */
@XmlType(name = "TargetRelationAttribute")
@XmlEnum
public enum TargetRelationAttribute {

	NONE, ENEMY, MYPARTY, ALL, FRIEND;
}
