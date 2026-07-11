package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.Getter;

/**
 * 需先交互生成物再击杀的怪物目标配置（扩展 {@link Monster}）。
 * Monster target that must be spawned via an object interaction before kill (extends {@link Monster}).
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpawnedMonster")
public class SpawnedMonster extends Monster {

	/**
	 * 用于召唤该怪物的场景物体 / 生成器 object ID。
	 * spawner object id used to summon this monster. / spawner object id used to summon this monster.
	 */
	@XmlAttribute(name = "spawner_object", required = true)
	protected int spawnerObject;
}
