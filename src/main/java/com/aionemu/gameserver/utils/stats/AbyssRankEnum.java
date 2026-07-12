package com.aionemu.gameserver.utils.stats;

import jakarta.xml.bind.annotation.XmlEnum;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 欧比斯军衔枚举，定义士兵/军官/将军等军衔的 AP/GP 门槛与配额
 * Abyss rank enum defining AP/GP thresholds and quotas for soldier/officer/general ranks
 */
@XmlEnum
@Getter
@Slf4j
public enum AbyssRankEnum {
	// AP 军阶 5.3 / Ap Rank 5.3
	/** 9 级士兵 / Grade 9 Soldier */
	GRADE9_SOLDIER(1, 300, 90, 0, 0, 0, 1802431),
	/** 8 级士兵 / Grade 8 Soldier */
	GRADE8_SOLDIER(2, 414, 103, 1200, 0, 0, 1802433),
	/** 7 级士兵 / Grade 7 Soldier */
	GRADE7_SOLDIER(3, 475, 118, 4220, 0, 0, 1802435),
	/** 6 级士兵 / Grade 6 Soldier */
	GRADE6_SOLDIER(4, 546, 136, 10990, 0, 0, 1802437),
	/** 5 级士兵 / Grade 5 Soldier */
	GRADE5_SOLDIER(5, 627, 156, 23500, 0, 0, 1802439),
	/** 4 级士兵 / Grade 4 Soldier */
	GRADE4_SOLDIER(6, 721, 180, 42780, 0, 0, 1802441),
	/** 3 级士兵 / Grade 3 Soldier */
	GRADE3_SOLDIER(7, 865, 216, 69700, 0, 0, 1802443),
	/** 2 级士兵 / Grade 2 Soldier */
	GRADE2_SOLDIER(8, 1038, 259, 105600, 0, 0, 1802445),
	/** 1 级士兵 / Grade 1 Soldier */
	GRADE1_SOLDIER(9, 1245, 311, 150800, 0, 0, 1802447),

	// 荣耀军阶 5.3 / Glory Rank 5.3
	/** 1 星军官 / 1-Star Officer */
	STAR1_OFFICER(10, 1868, 467, 0, 1244, 1000, 1802449),
	/** 2 星军官 / 2-Star Officer */
	STAR2_OFFICER(11, 2241, 560, 0, 1368, 700, 1802451),
	/** 3 星军官 / 3-Star Officer */
	STAR3_OFFICER(12, 2577, 644, 0, 1915, 500, 1802453),
	/** 4 星军官 / 4-Star Officer */
	STAR4_OFFICER(13, 2964, 741, 0, 3064, 300, 1802455),
	/** 5 星军官 / 5-Star Officer */
	STAR5_OFFICER(14, 4446, 1511, 0, 5210, 100, 1802457),
	/** 将军 / General */
	GENERAL(15, 4890, 1662, 0, 8335, 30, 1802459),
	/** 大将军 / Great General */
	GREAT_GENERAL(16, 5378, 1828, 0, 10002, 10, 1802461),
	/** 司令 / Commander */
	COMMANDER(17, 5916, 2011, 0, 11503, 3, 1802463),
	/** 最高司令 / Supreme Commander */
	SUPREME_COMMANDER(18, 7099, 2413, 0, 12437, 1, 1802465);

	/** 军衔 ID / Rank id */
	private int id;
	/** 击杀该军衔获得的点数 / Points gained when killing this rank */
	private int pointsGained;
	/** 被击杀时损失的点数 / Points lost when defeated */
	private int pointsLost;
	/** 升至该军衔所需 AP / AP required for this rank */
	private int apRequired;
	/** 升至该军衔所需 GP / GP required for this rank */
	private int gpRequired;
	/** 该军衔最大人数配额 / Maximum player quota for this rank */
	private int quota;
	/** 描述字符串 ID / Description string id */
	private int descriptionId;

	/**
	 * 构造欧比斯军衔
	 * Construct an abyss rank
	 *
	 * @param id 军衔 ID / Rank id
	 * @param pointsGained 击杀获得点数 / Points gained on kill
	 * @param pointsLost 死亡损失点数 / Points lost on death
	 * Required AP
	 * Required GP
	 * @param quota 人数配额 / Player quota
	 * Description id
	 */
	private AbyssRankEnum(int id, int pointsGained, int pointsLost, int apRequired, int gpRequired, int quota,
			int descriptionId) {
		this.id = id;
		this.pointsGained = pointsGained;
		this.pointsLost = pointsLost;
		this.apRequired = apRequired;
		this.gpRequired = gpRequired;
		this.quota = quota;
		this.descriptionId = descriptionId;
	}

	/**
	 * 按玩家种族返回军衔描述 ID
	 * Return rank description id adjusted for the player's race
	 *
	 * 玩家 / Player
	 * Description id
	 */
	public static DescriptionId getRankDescriptionId(Player player) {
		int pRankId = player.getAbyssRank().getRank().getId();
		for (AbyssRankEnum rank : values()) {
			if (rank.getId() == pRankId) {
				int descId = rank.getDescriptionId();
				return (player.getRace() == Race.ELYOS) ? new DescriptionId(descId) : new DescriptionId(descId + 36);
			}
		}
		throw new IllegalArgumentException("No rank Description Id found for player: " + player);
	}

	/**
	 * 按军衔 ID 查找枚举
	 * Look up rank enum by id
	 *
	 * @param id 军衔 ID / Rank id
	 * Matching rank
	 */
	public static AbyssRankEnum getRankById(int id) {
		for (AbyssRankEnum rank : values()) {
			if (rank.getId() == id) {
				return rank;
			}
		}
		throw new IllegalArgumentException("Invalid abyss rank provided " + id);
	}

	/**
	 * 按当前 AP 计算可达军衔
	 * Resolve the highest rank available for the given AP
	 *
	 * @param ap 欧比斯点数 / Abyss points
	 * Matching rank
	 */
	public static AbyssRankEnum getRankForAp(int ap) {
		AbyssRankEnum r = AbyssRankEnum.GRADE9_SOLDIER;
		for (AbyssRankEnum rank : values()) {
			if (rank.getApRequired() <= ap) {
				r = rank;
			} else {
				break;
			}
		}
		return r;
	}

	/**
	 * 按当前 GP 计算可达军衔
	 * Resolve the highest rank available for the given GP
	 *
	 * @param gp 荣耀点数 / Glory points
	 * Matching rank
	 */
	public static AbyssRankEnum getRankForGp(int gp) {
		AbyssRankEnum rgp = AbyssRankEnum.STAR1_OFFICER;
		for (AbyssRankEnum rank : values()) {
			if (rank.getGpRequired() <= gp) {
				rgp = rank;
			} else {
				break;
			}
		}
		return rgp;
	}
}
