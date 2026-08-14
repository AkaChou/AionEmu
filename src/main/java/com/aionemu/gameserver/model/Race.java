package com.aionemu.gameserver.model;

import jakarta.xml.bind.annotation.XmlEnum;

import org.apache.commons.lang3.NotImplementedException;

/**
 * 种族枚举。
 * Race enumeration.
 */

@XmlEnum
public enum Race {
	/** 天族 / Elyos. */
	ELYOS(0, new DescriptionId(480480)),
	/** 魔族 / Asmodians. */
	ASMODIANS(1, new DescriptionId(480481)),

	/** 狼人 / Lycan. */
	LYCAN(2),
	/** 构造体 / Construct */
	CONSTRUCT(3),
	/** 载具 / Carrier */
	CARRIER(4),
	/** 德拉坎 / Drakan */
	DRAKAN(5),
	/** 蜥蜴人 / Lizardman */
	LIZARDMAN(6),
	/** 传送者 / Teleporter */
	TELEPORTER(7),
	/** 那迦 / Naga */
	NAGA(8),
	/** 棕精 / Brownie */
	BROWNIE(9),
	/** 克拉尔 / Krall */
	KRALL(10),
	/** 舒拉克 / Shulack. */
	SHULACK(11),
	/** 屏障 / Barrier */
	BARRIER(12),
	/** 天族城堡门 / Elyos Castle Door */
	PC_LIGHT_CASTLE_DOOR(13),
	/** 魔族城堡门 / Asmodian Castle Door */
	PC_DARK_CASTLE_DOOR(14),
	/** 龙族城堡门 / Dragon Castle Door */
	DRAGON_CASTLE_DOOR(15),
	/** 天族要塞指挥官 / Elyos Fortress Chief */
	GCHIEF_LIGHT(16),
	/** 魔族要塞指挥官 / Asmodian Fortress Chief */
	GCHIEF_DARK(17),
	/** 龙族 / Dragon */
	DRAGON(18),
	/** 异界者 / Outsider */
	OUTSIDER(19),
	/** 鼠人 / Ratman */
	RATMAN(20),
	/** 半人形 / Demihumanoid */
	DEMIHUMANOID(21),
	/** 不死族 / Undead */
	UNDEAD(22),
	/** 野兽 / Beast */
	BEAST(23),
	/** 魔法怪物 / Magicalmonster. */
	MAGICALMONSTER(24),
	/** 元素 / Elemental */
	ELEMENTAL(25),
	/** 活水 / Living Water */
	LIVINGWATER(28),
	/** 无 / None */
	NONE(26),
	/** 全部玩家 / All Players */
	PC_ALL(27),
	/** 变形 / Deform */
	DEFORM(28),
	/** 中立 / Neutral */
	NEUT(29),
	/** 天族护卫 / Elyos Guard */
	GHENCHMAN_LIGHT(30),
	/** 魔族护卫 / Asmodian Guard */
	GHENCHMAN_DARK(31),
	/** 活动塔（魔） / Event Tower Dark */
	EVENT_TOWER_DARK(32),
	/** 活动塔（天） / Event Tower Light */
	EVENT_TOWER_LIGHT(33),
	/** 哥布林 / Goblin */
	GOBLIN(34),
	/** 三角黑暗 / Tricodark */
	TRICODARK(35),
	/** NPC */
	NPC(36),
	/** 光 / Light */
	LIGHT(37),
	/** 黑暗 / Dark. */
	DARK(38),
	/** 世界事件防御塔 / World Event Def Tower */
	WORLD_EVENT_DEFTOWER(39),
	/** 兽人 / Orc */
	ORC(40),
	/** 小龙 / Dragonet */
	DRAGONET(41),
	/** 攻城德拉坎 / Siege Drakan */
	SIEGEDRAKAN(42),
	/** 龙族要塞指挥官 / Dragon Fortress Chief */
	GCHIEF_DRAGON(43),
	/** 世界事件篝火 / World Event Bonfire*/
	WORLD_EVENT_BONFIRE(44),
	/** 战场（天） / Battleground Elyos */
	BATTLEGROUND_LI(45),
	/** 战场（魔） / Battleground Asmodian */
	BATTLEGROUND_DA(46),
	/** 类型 A / Type A */
	TYPE_A(47),
	/** 类型 B / Type B */
	TYPE_B(48),
	/** 类型 C / Type C */
	TYPE_C(49),
	/** 类型 D / Type D */
	TYPE_D(50),
	/** 人参 / Ginsengs. */
	GINSENGS(51),
	/** 活动年 / Event Year */
	EVENT_YEAR(52),
	/** F6 突袭首领 / F6 Raid Boss */
	F6_RAID_BOSS(53),
	/** 人类 / Human */
	HUMAN(54),
	/** 类型 E / Type E */
	TYPE_E(55),
	/** 活动五 01 / Event Fifth 01 */
	EVENT_FIFTH_01(56),
	/** 活动五 02 / Event Fifth 02 */
	EVENT_FIFTH_02(57),
	/** 事件万圣节 / Event Halloween*/
	EVENT_HALLOWEEN(58);

	private int raceId;
	private DescriptionId descriptionId;

	private Race(int raceId) {
		this(raceId, null);
	}

	private Race(int raceId, DescriptionId descriptionId) {
		this.raceId = raceId;
		this.descriptionId = descriptionId;
	}

	/** 返回种族 ID / Returns the race id */
	public int getRaceId() {
		return raceId;
	}

	/** 是否为玩家种族。 / Whether player race. */
	public boolean isPlayerRace() {
		return raceId < 2 || raceId == 27;
	}

	/** 返回种族描述 ID / Returns the race description id */
	public DescriptionId getRaceDescriptionId() {
		if (descriptionId == null) {
			throw new NotImplementedException("Race name DescriptionId is unknown for race" + this);
		}
		return descriptionId;
	}

	/** 按字符串返回种族 / Returns the race by string*/
	public static Race getRaceByString(String fieldName) {
		for (Race r : values()) {
			if (r.toString().equals(fieldName)) {
				return r;
			}
		}
		return null;
	}
}
