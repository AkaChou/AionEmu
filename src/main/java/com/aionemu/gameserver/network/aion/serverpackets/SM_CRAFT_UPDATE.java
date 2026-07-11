package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 制作进度/结果更新包：同步成功/失败值、执行时序与物品名。
 * Server packet for craft progress/result: success/failure values, timing and item name.
 */
public class SM_CRAFT_UPDATE extends AionServerPacket {
	private int skillId;
	private int itemId;
	private int action;
	private int success;
	private int failure;
	private int nameId;
	private int executionDelay = 700;
	private int executionPeriod = 1200;

	/**
	 * craft skill id
	 * @param item    产出物品模板 / result item template
	 * success progress
	 * failure progress
	 * @param action  阶段动作码 / phase action code
	 */
	public SM_CRAFT_UPDATE(int skillId, ItemTemplate item, int success, int failure, int action) {
		this.action = action;
		this.skillId = skillId;
		this.itemId = item.getTemplateId();
		this.success = success;
		this.failure = failure;
		this.nameId = item.getNameId();
		// 以太变形。 / Aether Morphing.
		if (skillId == 40009) {
			this.executionPeriod = 1500;
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(skillId);
		writeC(action);
		writeD(itemId);
		switch (action) {
		case 0:
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(1200);
			writeD(1330048);
			writeH(0x24);
			writeD(nameId);
			writeH(0);
			break;
		case 1:
		case 2:
		case 5:
			writeD(success);
			writeD(failure);
			writeD(executionDelay);
			writeD(executionPeriod);
			writeD(0);
			writeH(0);
			break;
		case 3:
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(executionPeriod);
			writeD(1330048);
			writeH(0x24);
			writeD(nameId);
			writeH(0);
			break;
		case 4:
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(0);
			writeD(1330051);
			writeH(0);
			break;
		case 6:
			writeD(success);
			writeD(failure);
			writeD(executionDelay);
			writeD(executionPeriod);
			writeD(1330050);
			writeH(0x24);
			writeD(nameId);
			writeH(0);
			break;
		case 7:
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(executionPeriod);
			writeD(1330050);
			writeH(0x24);
			writeD(nameId);
			writeH(0);
			break;
		}
	}
}
