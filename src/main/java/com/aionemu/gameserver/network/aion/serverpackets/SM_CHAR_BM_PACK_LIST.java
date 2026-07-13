package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Character benefit-pack list. Subtype 3 is the independent China VIP branch.
 */
public class SM_CHAR_BM_PACK_LIST extends AionServerPacket {

	private static final int VIP_SUBTYPE = 3;
	private static final int VIP_ITEM_TYPE = 4;
	private static final int VIP_DURATION_SECONDS = Integer.MAX_VALUE;
	private static final int[] CHINA_VIP_BENEFIT_IDS = { 1, 2, 3 };

	private final int subtype;
	private final boolean vipEnabled;

	public SM_CHAR_BM_PACK_LIST(int subtype) {
		this(subtype, false);
	}

	private SM_CHAR_BM_PACK_LIST(int subtype, boolean vipEnabled) {
		this.subtype = subtype;
		this.vipEnabled = vipEnabled;
	}

	public static SM_CHAR_BM_PACK_LIST vip(int vipLevel) {
		if (vipLevel < 0 || vipLevel > 6) {
			throw new IllegalArgumentException("vipLevel must be between 0 and 6");
		}
		return new SM_CHAR_BM_PACK_LIST(VIP_SUBTYPE, vipLevel > 0);
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
			writeD(VIP_DURATION_SECONDS);
		}
	}
}
