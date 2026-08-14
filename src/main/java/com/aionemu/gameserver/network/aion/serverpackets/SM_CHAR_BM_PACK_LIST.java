package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 角色福利包列表：子类型 3 为国服独立的 VIP 分支。
 * Character benefit-pack list: subtype 3 is the independent China VIP branch.
 * <p>
 * 选人界面阶段：duration 字段承载 AccumulateGradeScore 供 Game.dll 使用。
 * Char-select stage UI: duration field carries AccumulateGradeScore for Game.dll.
 * 游戏内 VIP 包：duration 为 account_vip.expire_time 计算的剩余秒数。
 * In-world VIP packs: duration is remaining seconds from account_vip.expire_time.
 */
public class SM_CHAR_BM_PACK_LIST extends AionServerPacket {

	private static final int VIP_SUBTYPE = 3;
	private static final int VIP_ITEM_TYPE = 4;
	private static final int[] CHINA_VIP_BENEFIT_IDS = { 1, 2, 3 };

	/** 1–6 档 vip_grade_exp 分数下限。 / vip_grade_exp floors for stages 1–6. */
	private static final int[] LEVEL_TO_SCORE = { 0, 178, 544, 1034, 2069, 3758, 3759 };

	private final int subtype;
	private final boolean vipEnabled;
	private final int duration;

	/**
	 * 按子类型构造福利包列表包。
	 * Creates a benefit-pack list packet for the given subtype.
	 *
	 * @param subtype 福利包子类型 / benefit-pack subtype
	 */
	public SM_CHAR_BM_PACK_LIST(int subtype) {
		this(subtype, false, 0);
	}

	private SM_CHAR_BM_PACK_LIST(int subtype, boolean vipEnabled, int duration) {
		this.subtype = subtype;
		this.vipEnabled = vipEnabled;
		this.duration = duration;
	}

	/**
	 * 游戏内 VIP 福利包。duration 为剩余秒数（必须 &gt; 3759，避免 Game.dll
	 * 档位补丁将其误判为 VIP 分数）。
	 * In-world VIP benefits. Duration is remaining seconds (must be &gt; 3759 so the
	 * Game.dll stage patch does not treat it as a VIP score).
	 *
	 * @param vipLevel VIP 档位（0–6）/ VIP level (0-6)
	 * @param remainingSeconds 剩余有效秒数 / remaining seconds
	 */
	public static SM_CHAR_BM_PACK_LIST vip(int vipLevel, int remainingSeconds) {
		if (vipLevel < 0 || vipLevel > 6) {
			throw new IllegalArgumentException("vipLevel must be between 0 and 6");
		}
		boolean active = vipLevel > 0 && remainingSeconds > 0;
		// 钳制时长，使客户端 BM 补丁的分数守卫（1..3759）在游戏内永不触发。
		// Clamp so client BM patch score-guard (1..3759) never fires in-world.
		int duration = active ? Math.max(remainingSeconds, 3760) : 0;
		return new SM_CHAR_BM_PACK_LIST(VIP_SUBTYPE, active, duration);
	}

	/**
	 * 仅选人界面使用：duration 字段承载 AccumulateGradeScore 供 Game.dll 使用。
	 * Char-select only: duration field carries AccumulateGradeScore for Game.dll.
	 *
	 * @param vipLevel VIP 档位（0–6）/ VIP level (0-6)
	 * @param vipExp VIP 累计经验 / accumulated VIP experience
	 */
	public static SM_CHAR_BM_PACK_LIST vipForCharSelect(int vipLevel, long vipExp) {
		if (vipLevel < 0 || vipLevel > 6) {
			throw new IllegalArgumentException("vipLevel must be between 0 and 6");
		}
		int score = resolveScore(vipLevel, vipExp);
		return new SM_CHAR_BM_PACK_LIST(VIP_SUBTYPE, vipLevel > 0 && score > 0, score);
	}

	/**
	 * 将 VIP 档位与经验解析为分数：经验有效时优先取经验，否则取档位下限。
	 * Resolves VIP level and experience into a score: uses experience when valid, else the level floor.
	 */
	static int resolveScore(int vipLevel, long vipExp) {
		if (vipExp > 0 && vipExp <= Integer.MAX_VALUE) {
			return (int) vipExp;
		}
		if (vipLevel <= 0) {
			return 0;
		}
		if (vipLevel >= LEVEL_TO_SCORE.length) {
			return LEVEL_TO_SCORE[LEVEL_TO_SCORE.length - 1];
		}
		return LEVEL_TO_SCORE[vipLevel];
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(subtype);
		switch (subtype) {
		case 1:
			writeH(0);
			break;
		case 2:
			writeH(1);
			writeC(2);
			writeH(3000);
			writeH(0);
			writeD(388306);
			break;
		case VIP_SUBTYPE:
			writeVipBenefits();
			break;
		}
	}

	private void writeVipBenefits() {
		writeH(vipEnabled ? CHINA_VIP_BENEFIT_IDS.length : 0);
		if (!vipEnabled) {
			return;
		}
		for (int benefitId : CHINA_VIP_BENEFIT_IDS) {
			writeC(VIP_ITEM_TYPE);
			writeD(benefitId);
			writeD(duration);
		}
	}
}
