package com.aionemu.gameserver.services.rift;

import com.aionemu.gameserver.model.Race;

/**
 * 裂隙类型枚举，定义天族/魔族各地图裂隙与次元漩涡的入口、等级与费用参数。
 * Rift type enum defining Elyos/Asmodian map rifts and dimensional vortexes with entry, level and cost params.
 *
 * @author Rinzler (Encom)
 */
public enum RiftEnum {
	// 天族裂隙 ---。

	/** 凯斯奈尔学院次元漩涡（3.9）/ Kaisinel Academy dimensional vortex (3.9) */
	KAISINEL_AM(1170, "KAISINEL_AM", "KAISINEL_AS", 24, 10000, 46, 65, Race.ASMODIANS, true),

	/** Eltnen rift A (4.9) / Eltnen rift A (4.9) */
	ELTNEN_AM(2120, "ELTNEN_AM", "MORHEIM_AS", 20, 32, 20, 30, Race.ASMODIANS),
	/** Eltnen rift B (4.9) / Eltnen rift B (4.9) */
	ELTNEN_BM(2121, "ELTNEN_BM", "MORHEIM_BS", 30, 32, 20, 30, Race.ASMODIANS),
	/** Eltnen rift C (4.9) / Eltnen rift C (4.9) */
	ELTNEN_CM(2122, "ELTNEN_CM", "MORHEIM_CS", 40, 32, 20, 32, Race.ASMODIANS),
	/** Eltnen rift D (4.9) / Eltnen rift D (4.9) */
	ELTNEN_DM(2123, "ELTNEN_DM", "MORHEIM_DS", 40, 32, 20, 32, Race.ASMODIANS),
	/** Eltnen rift E (4.9) / Eltnen rift E (4.9) */
	ELTNEN_EM(2124, "ELTNEN_EM", "MORHEIM_ES", 45, 32, 20, 32, Race.ASMODIANS),
	/** Eltnen rift F (4.9) / Eltnen rift F (4.9) */
	ELTNEN_FM(2125, "ELTNEN_FM", "MORHEIM_FS", 50, 32, 20, 32, Race.ASMODIANS),
	/** Eltnen rift G (4.9) / Eltnen rift G (4.9) */
	ELTNEN_GM(2126, "ELTNEN_GM", "MORHEIM_GS", 50, 32, 20, 35, Race.ASMODIANS),

	/** Heiron rift A (4.9) / Heiron rift A (4.9) */
	HEIRON_AM(2140, "HEIRON_AM", "BELUSLAN_AS", 30, 32, 20, 40, Race.ASMODIANS),
	/** Heiron rift B (4.9) / Heiron rift B (4.9) */
	HEIRON_BM(2141, "HEIRON_BM", "BELUSLAN_BS", 40, 32, 20, 40, Race.ASMODIANS),
	/** Heiron rift C (4.9) / Heiron rift C (4.9) */
	HEIRON_CM(2142, "HEIRON_CM", "BELUSLAN_CS", 50, 32, 20, 42, Race.ASMODIANS),
	/** Heiron rift D (4.9) / Heiron rift D (4.9) */
	HEIRON_DM(2143, "HEIRON_DM", "BELUSLAN_DS", 50, 32, 20, 42, Race.ASMODIANS),
	/** Heiron rift E (4.9) / Heiron rift E (4.9) */
	HEIRON_EM(2144, "HEIRON_EM", "BELUSLAN_ES", 60, 32, 20, 42, Race.ASMODIANS),
	/** Heiron rift F (4.9) / Heiron rift F (4.9) */
	HEIRON_FM(2145, "HEIRON_FM", "BELUSLAN_FS", 60, 32, 20, 42, Race.ASMODIANS),
	/** Heiron rift G (4.9) / Heiron rift G (4.9) */
	HEIRON_GM(2146, "HEIRON_GM", "BELUSLAN_GS", 144, 32, 20, 45, Race.ASMODIANS),

	/** Inggison rift A (4.8) / Inggison rift A (4.8) */
	INGGISON_AM(2150, "INGGISON_AM", "GELKMAROS_AS", 150, 24, 50, 75, Race.ASMODIANS),
	/** Inggison rift B (4.8) / Inggison rift B (4.8) */
	INGGISON_BM(2151, "INGGISON_BM", "GELKMAROS_BS", 150, 24, 50, 75, Race.ASMODIANS),
	/** Inggison rift C (4.8) / Inggison rift C (4.8) */
	INGGISON_CM(2152, "INGGISON_CM", "GELKMAROS_CS", 150, 24, 50, 75, Race.ASMODIANS),
	/** Inggison rift D (4.8) / Inggison rift D (4.8) */
	INGGISON_DM(2153, "INGGISON_DM", "GELKMAROS_DS", 150, 24, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift E (4.8) / Inggison volatile rift E (4.8) */
	INGGISON_EM(2154, "INGGISON_EM", "GELKMAROS_ES", 6, 36, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift F (4.8) / Inggison volatile rift F (4.8) */
	INGGISON_FM(2155, "INGGISON_FM", "GELKMAROS_FS", 6, 36, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift G (4.8) / Inggison volatile rift G (4.8) */
	INGGISON_GM(2156, "INGGISON_GM", "GELKMAROS_GS", 6, 36, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift H (4.8) / Inggison volatile rift H (4.8) */
	INGGISON_HM(2157, "INGGISON_HM", "GELKMAROS_HS", 6, 36, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift I (4.8) / Inggison volatile rift I (4.8) */
	INGGISON_IM(2158, "INGGISON_IM", "GELKMAROS_IS", 6, 36, 50, 75, Race.ASMODIANS),
	/** Inggison volatile rift J (4.8) / Inggison volatile rift J (4.8) */
	INGGISON_JM(2159, "INGGISON_JM", "GELKMAROS_JS", 6, 36, 50, 75, Race.ASMODIANS),

	/** Cygnea rift A (4.8) / Cygnea rift A (4.8) */
	CYGNEA_AM(2170, "CYGNEA_AM", "ENSHAR_AS", 12, 24, 50, 75, Race.ASMODIANS),
	/** Cygnea rift B (4.8) / Cygnea rift B (4.8) */
	CYGNEA_BM(2171, "CYGNEA_BM", "ENSHAR_BS", 36, 24, 50, 75, Race.ASMODIANS),
	/** Cygnea rift C (4.8) / Cygnea rift C (4.8) */
	CYGNEA_CM(2172, "CYGNEA_CM", "ENSHAR_CS", 48, 24, 55, 75, Race.ASMODIANS),
	/** Cygnea rift D (4.8) / Cygnea rift D (4.8) */
	CYGNEA_DM(2173, "CYGNEA_DM", "ENSHAR_DS", 48, 24, 55, 75, Race.ASMODIANS),
	/** Cygnea rift E (4.8) / Cygnea rift E (4.8) */
	CYGNEA_EM(2174, "CYGNEA_EM", "ENSHAR_ES", 48, 24, 55, 75, Race.ASMODIANS),
	/** Cygnea rift F (4.8) / Cygnea rift F (4.8) */
	CYGNEA_FM(2175, "CYGNEA_FM", "ENSHAR_FS", 48, 24, 55, 75, Race.ASMODIANS),
	/** Cygnea volatile rift G (4.8) / Cygnea volatile rift G (4.8) */
	CYGNEA_GM(2176, "CYGNEA_GM", "ENSHAR_GS", 144, 36, 60, 75, Race.ASMODIANS),
	/** Cygnea volatile rift H (4.8) / Cygnea volatile rift H (4.8) */
	CYGNEA_HM(2177, "CYGNEA_HM", "ENSHAR_HS", 144, 36, 60, 75, Race.ASMODIANS),
	/** Cygnea volatile rift I (4.8) / Cygnea volatile rift I (4.8) */
	CYGNEA_IM(2178, "CYGNEA_IM", "ENSHAR_IS", 144, 36, 60, 75, Race.ASMODIANS),

	/** Iluma rift A (5.0) / Iluma rift A (5.0) */
	ILUMA_AM(2101, "ILUMA_AM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift B (5.0) / Iluma rift B (5.0) */
	ILUMA_BM(2102, "ILUMA_BM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift C (5.0) / Iluma rift C (5.0) */
	ILUMA_CM(2103, "ILUMA_CM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift D (5.0) / Iluma rift D (5.0) */
	ILUMA_DM(2104, "ILUMA_DM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift E (5.0) / Iluma rift E (5.0) */
	ILUMA_EM(2105, "ILUMA_EM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift F (5.0) / Iluma rift F (5.0) */
	ILUMA_FM(2106, "ILUMA_FM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),
	/** Iluma rift G (5.0) / Iluma rift G (5.0) */
	ILUMA_GM(2107, "ILUMA_GM", "NORSVOLD_AS", 24, 84, 66, 83, Race.ASMODIANS),

	// 魔族裂隙 ---。

	/** 马修坦神殿次元漩涡（3.9）/ Marchutan Priory dimensional vortex (3.9) */
	MARCHUTAN_AM(1280, "MARCHUTAN_AM", "MARCHUTAN_AS", 24, 10000, 46, 65, Race.ELYOS, true),

	/** Morheim rift A (4.9) / Morheim rift A (4.9) */
	MORHEIM_AM(2220, "MORHEIM_AM", "ELTNEN_AS", 20, 32, 20, 30, Race.ELYOS),
	/** Morheim rift B (4.9) / Morheim rift B (4.9) */
	MORHEIM_BM(2221, "MORHEIM_BM", "ELTNEN_BS", 30, 32, 20, 30, Race.ELYOS),
	/** Morheim rift C (4.9) / Morheim rift C (4.9) */
	MORHEIM_CM(2222, "MORHEIM_CM", "ELTNEN_CS", 40, 32, 20, 32, Race.ELYOS),
	/** Morheim rift D (4.9) / Morheim rift D (4.9) */
	MORHEIM_DM(2223, "MORHEIM_DM", "ELTNEN_DS", 40, 32, 20, 32, Race.ELYOS),
	/** Morheim rift E (4.9) / Morheim rift E (4.9) */
	MORHEIM_EM(2224, "MORHEIM_EM", "ELTNEN_ES", 45, 32, 20, 32, Race.ELYOS),
	/** Morheim rift F (4.9) / Morheim rift F (4.9) */
	MORHEIM_FM(2225, "MORHEIM_FM", "ELTNEN_FS", 50, 32, 20, 32, Race.ELYOS),
	/** Morheim rift G (4.9) / Morheim rift G (4.9) */
	MORHEIM_GM(2226, "MORHEIM_GM", "ELTNEN_GS", 50, 32, 20, 35, Race.ELYOS),

	/** Beluslan rift A (4.9) / Beluslan rift A (4.9) */
	BELUSLAN_AM(2240, "BELUSLAN_AM", "HEIRON_AS", 30, 32, 20, 40, Race.ELYOS),
	/** Beluslan rift B (4.9) / Beluslan rift B (4.9) */
	BELUSLAN_BM(2241, "BELUSLAN_BM", "HEIRON_BS", 40, 32, 20, 40, Race.ELYOS),
	/** Beluslan rift C (4.9) / Beluslan rift C (4.9) */
	BELUSLAN_CM(2242, "BELUSLAN_CM", "HEIRON_CS", 50, 32, 20, 42, Race.ELYOS),
	/** Beluslan rift D (4.9) / Beluslan rift D (4.9) */
	BELUSLAN_DM(2243, "BELUSLAN_DM", "HEIRON_DS", 50, 32, 20, 42, Race.ELYOS),
	/** Beluslan rift E (4.9) / Beluslan rift E (4.9) */
	BELUSLAN_EM(2244, "BELUSLAN_EM", "HEIRON_ES", 60, 32, 20, 42, Race.ELYOS),
	/** Beluslan rift F (4.9) / Beluslan rift F (4.9) */
	BELUSLAN_FM(2245, "BELUSLAN_FM", "HEIRON_FS", 60, 32, 20, 42, Race.ELYOS),
	/** Beluslan rift G (4.9) / Beluslan rift G (4.9) */
	BELUSLAN_GM(2246, "BELUSLAN_GM", "HEIRON_GS", 144, 32, 20, 45, Race.ELYOS),

	/** Gelkmaros rift A (4.8) / Gelkmaros rift A (4.8) */
	GELKMAROS_AM(2270, "GELKMAROS_AM", "INGGISON_AS", 150, 24, 50, 75, Race.ELYOS),
	/** Gelkmaros rift B (4.8) / Gelkmaros rift B (4.8) */
	GELKMAROS_BM(2271, "GELKMAROS_BM", "INGGISON_BS", 150, 24, 50, 75, Race.ELYOS),
	/** Gelkmaros rift C (4.8) / Gelkmaros rift C (4.8) */
	GELKMAROS_CM(2272, "GELKMAROS_CM", "INGGISON_CS", 150, 24, 50, 75, Race.ELYOS),
	/** Gelkmaros rift D (4.8) / Gelkmaros rift D (4.8) */
	GELKMAROS_DM(2273, "GELKMAROS_DM", "INGGISON_DS", 150, 24, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift E (4.8) / Gelkmaros volatile rift E (4.8) */
	GELKMAROS_EM(2274, "GELKMAROS_EM", "INGGISON_ES", 6, 36, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift F (4.8) / Gelkmaros volatile rift F (4.8) */
	GELKMAROS_FM(2275, "GELKMAROS_FM", "INGGISON_FS", 6, 36, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift G (4.8) / Gelkmaros volatile rift G (4.8) */
	GELKMAROS_GM(2276, "GELKMAROS_GM", "INGGISON_GS", 6, 36, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift H (4.8) / Gelkmaros volatile rift H (4.8) */
	GELKMAROS_HM(2277, "GELKMAROS_HM", "INGGISON_HS", 6, 36, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift I (4.8) / Gelkmaros volatile rift I (4.8) */
	GELKMAROS_IM(2278, "GELKMAROS_IM", "INGGISON_IS", 6, 36, 50, 75, Race.ELYOS),
	/** Gelkmaros volatile rift J (4.8) / Gelkmaros volatile rift J (4.8) */
	GELKMAROS_JM(2279, "GELKMAROS_JM", "INGGISON_JS", 6, 36, 50, 75, Race.ELYOS),

	/** Enshar rift A (4.8) / Enshar rift A (4.8) */
	ENSHAR_AM(2280, "ENSHAR_AM", "CYGNEA_AS", 12, 24, 50, 75, Race.ELYOS),
	/** Enshar rift B (4.8) / Enshar rift B (4.8) */
	ENSHAR_BM(2281, "ENSHAR_BM", "CYGNEA_BS", 36, 24, 50, 75, Race.ELYOS),
	/** Enshar rift C (4.8) / Enshar rift C (4.8) */
	ENSHAR_CM(2282, "ENSHAR_CM", "CYGNEA_CS", 48, 24, 55, 75, Race.ELYOS),
	/** Enshar rift D (4.8) / Enshar rift D (4.8) */
	ENSHAR_DM(2283, "ENSHAR_DM", "CYGNEA_DS", 48, 24, 55, 75, Race.ELYOS),
	/** Enshar rift E (4.8) / Enshar rift E (4.8) */
	ENSHAR_EM(2284, "ENSHAR_EM", "CYGNEA_ES", 48, 24, 55, 75, Race.ELYOS),
	/** Enshar rift F (4.8) / Enshar rift F (4.8) */
	ENSHAR_FM(2285, "ENSHAR_FM", "CYGNEA_FS", 48, 24, 55, 75, Race.ELYOS),
	/** Enshar volatile rift G (4.8) / Enshar volatile rift G (4.8) */
	ENSHAR_GM(2286, "ENSHAR_GM", "CYGNEA_GS", 144, 36, 60, 75, Race.ELYOS),
	/** Enshar volatile rift H (4.8) / Enshar volatile rift H (4.8) */
	ENSHAR_HM(2287, "ENSHAR_HM", "CYGNEA_HS", 144, 36, 60, 75, Race.ELYOS),
	/** Enshar volatile rift I (4.8) / Enshar volatile rift I (4.8) */
	ENSHAR_IM(2288, "ENSHAR_IM", "CYGNEA_IS", 144, 36, 60, 75, Race.ELYOS),

	/** Norsvold rift A (5.0) / Norsvold rift A (5.0) */
	NORSVOLD_AM(2201, "NORSVOLD_AM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift B (5.0) / Norsvold rift B (5.0) */
	NORSVOLD_BM(2202, "NORSVOLD_BM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift C (5.0) / Norsvold rift C (5.0) */
	NORSVOLD_CM(2203, "NORSVOLD_CM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift D (5.0) / Norsvold rift D (5.0) */
	NORSVOLD_DM(2204, "NORSVOLD_DM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift E (5.0) / Norsvold rift E (5.0) */
	NORSVOLD_EM(2205, "NORSVOLD_EM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift F (5.0) / Norsvold rift F (5.0) */
	NORSVOLD_FM(2206, "NORSVOLD_FM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS),
	/** Norsvold rift G (5.0) / Norsvold rift G (5.0) */
	NORSVOLD_GM(2207, "NORSVOLD_GM", "ILUMA_AS", 24, 84, 66, 83, Race.ELYOS);

	private int id;
	private String master;
	private String slave;
	private int entries;
	private int abyssPoint;
	private int minLevel;
	private int maxLevel;
	private Race destination;
	private boolean vortex;

	private RiftEnum(int id, String master, String slave, int entries, int abyssPoint, int minLevel, int maxLevel,
			Race destination) {
		this(id, master, slave, entries, abyssPoint, minLevel, maxLevel, destination, false);
	}

	private RiftEnum(int id, String master, String slave, int entries, int abyssPoint, int minLevel, int maxLevel,
			Race destination, boolean vortex) {
		this.id = id;
		this.master = master;
		this.slave = slave;
		this.entries = entries;
		this.abyssPoint = abyssPoint;
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		this.destination = destination;
		this.vortex = vortex;
	}

	/**
	 * 按裂隙 ID 查找枚举。
	 * Resolve rift enum by id.
	 *
	 * @param id 裂隙 ID / Rift id
	 * @return 匹配的枚举 / Matching enum
	 * Unsupported id
	 */
	public static RiftEnum getRift(int id) throws IllegalArgumentException {
		for (RiftEnum rift : RiftEnum.values()) {
			if (rift.getId() == id) {
				return rift;
			}
		}
		throw new IllegalArgumentException("Unsupported rift id: " + id);
	}

	/**
	 * 按目标种族查找次元漩涡。
	 * Resolve dimensional vortex by destination race.
	 *
	 * @param race 目标种族 / Destination race
	 * @return 匹配的漩涡枚举 / Matching vortex enum
	 * Unsupported race。
	 */
	public static RiftEnum getVortex(Race race) throws IllegalArgumentException {
		for (RiftEnum rift : RiftEnum.values()) {
			if (rift.isVortex() && rift.getDestination().equals(race)) {
				return rift;
			}
		}
		throw new IllegalArgumentException("Unsupported vortex race: " + race);
	}

	/**
	 * 返回裂隙 ID。
	 * Returns the rift id.
	 *
	 * Rift id
	 */
	public int getId() {
		return id;
	}

	/**
	 * 返回主端（入口侧）锚点名。
	 * Returns the master (entry-side) anchor name.
	 *
	 * Master anchor
	 */
	public String getMaster() {
		return master;
	}

	/**
	 * 返回从端（出口侧）锚点名。
	 * Returns the slave (exit-side) anchor name.
	 *
	 * Slave anchor
	 */
	public String getSlave() {
		return slave;
	}

	/**
	 * 返回允许进入次数。
	 * Returns allowed entry count.
	 *
	 * Entry count
	 */
	public int getEntries() {
		return entries;
	}

	/**
	 * 返回欧比斯点消耗。
	 * Returns abyss point cost.
	 *
	 * Abyss points
	 */
	public int getAbyssPoint() {
		return abyssPoint;
	}

	/**
	 * 返回最低进入等级。
	 * Returns minimum enter level.
	 *
	 * Min level
	 */
	public int getMinLevel() {
		return minLevel;
	}

	/**
	 * 返回最高进入等级。
	 * Returns maximum enter level.
	 *
	 * Max level
	 */
	public int getMaxLevel() {
		return maxLevel;
	}

	/**
	 * 返回目标侧种族。
	 * Returns destination race.
	 *
	 * Destination race
	 */
	public Race getDestination() {
		return destination;
	}

	/**
	 * 是否为次元漩涡。
	 * Whether this is a dimensional vortex.
	 *
	 * @return {@code true} if vortex。
	 */
	public boolean isVortex() {
		return vortex;
	}
}
