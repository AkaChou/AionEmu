package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 真端 Familiar 包（opcode 0x2EA，真端 FamiliarSystem.cpp::User::SendFamiliarAdd，
 * 组包函数 FUN_140a50d10 逐字段对齐）。
 * Retail Familiar packet (opcode 0x2EA, retail FamiliarSystem.cpp::User::SendFamiliarAdd,
 * field-by-field aligned with the retail packet writer FUN_140a50d10).
 * <p>
 * type 0-3 语义同 SM_MINIONS addType（0=新随从/1=升级/2=合成成功/3=合成失败），携带随从记录；
 * type≥4 只有 2 字节无数据通知（4=契约失败补偿，6=另一流程收尾）。
 * 记录布局（FUN_140a50d10 + 构造器 FUN_140a37530 交叉定案，名字为变长 wchar 数组、含 2 字节终止符）：
 * H type; Q 0（真端契约场景 local_4f8=0）; D familiar+0x08（IdFactory 分配 ID，FUN_1404bced0）;
 * Q familiar+0x10（DB 持久化 ID，SetDBIdAndSendClient 回填，%I64u 日志证实）——本仓单层 objectId
 * 两处同值; D masterId; D minionId（attr[0]，+0x00 是模板属性指针）; S 名字（+0x24 起，上限 25 wchar）;
 * Q birthday（+0x60 创建时间）; D 0（+0x74 全库无写点恒 0）; B 0（+0x78 恒 0）; 6×D doping（+0xD8）;
 * B 锁定（+0x7C）。等级与成长值不在包内——客户端按 minionId 查本地模板。
 * type 0-3 share SM_MINIONS addType semantics and carry the minion record; type≥4 is a bare
 * 2-byte notice. Record layout (FUN_140a50d10 crossed with constructor FUN_140a37530; variable
 * -length wchar name with terminator): H type; Q 0 (retail contract passes local_4f8=0);
 * D familiar+0x08 (IdFactory-assigned id, FUN_1404bced0); Q familiar+0x10 (DB-persisted id,
 * backfilled by SetDBIdAndSendClient) — our single-layer objectId fills both; D masterId;
 * D minionId (attr[0]; +0x00 is the template-attr pointer); S name; Q birthday (+0x60);
 * D 0 (+0x74 has no writer anywhere — always 0); B 0 (+0x78); 6×D doping; B locked (+0x7C).
 * Level and growth are not in the packet — the client resolves them from the minion template.
 */
public class SM_FAMILIAR extends AionServerPacket {
	private final int type;
	private final MinionCommonData minionCommonData;

	public SM_FAMILIAR(int type) {
		this.type = type;
		this.minionCommonData = null;
	}

	public SM_FAMILIAR(int type, MinionCommonData minionCommonData) {
		this.type = type;
		this.minionCommonData = minionCommonData;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(type);
		if (minionCommonData == null) {
			return;
		}
		writeQ(0);
		writeD(minionCommonData.getObjectId());
		writeQ(minionCommonData.getObjectId() & 0xFFFFFFFFL);
		writeD(minionCommonData.getMasterObjectId());
		writeD(minionCommonData.getMinionId());
		writeS(minionCommonData.getName());
		writeQ(minionCommonData.getBirthday() & 0xFFFFFFFFL);
		writeD(0);
		writeC(0);
		for (int slot = 0; slot < 6; slot++) {
			writeD(minionCommonData.getDopingBag() == null ? 0 : minionCommonData.getDopingBag().getItem(slot));
		}
		writeC(minionCommonData.isLock() ? 1 : 0);
	}
}
