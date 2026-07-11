package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 向客户端同步任务状态、步骤、计时或共享等任务动作。
 * Server packet synchronizing quest status, step, timer, or share actions to the client.
 */
public class SM_QUEST_ACTION extends AionServerPacket {
	protected int questId;
	private int status;
	private int step;
	protected int action;
	private int timer;
	private int sharerId;
	@SuppressWarnings("unused")
	private boolean unk;

	SM_QUEST_ACTION() {

	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 * quest status
	 * @param step 任务步骤 / quest step
	 */
	public SM_QUEST_ACTION(int questId, int status, int step) {
		this.action = 1;
		this.questId = questId;
		this.status = status;
		this.step = step;
	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 * quest status
	 * @param step 任务步骤 / quest step
	 */
	public SM_QUEST_ACTION(int questId, QuestStatus status, int step) {
		this.action = 2;
		this.questId = questId;
		this.status = status.value();
		this.step = step;
	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 */
	public SM_QUEST_ACTION(int questId) {
		this.action = 3;
		this.questId = questId;
	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 * @param timer 计时秒数 / timer seconds
	 */
	public SM_QUEST_ACTION(int questId, int timer) {
		this.action = 4;
		this.questId = questId;
		this.timer = timer;
		this.step = 0;
	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 * sharer id
	 * @param unk 未知字段 / unknown field
	 */
	public SM_QUEST_ACTION(int questId, int sharerId, boolean unk) {
		this.action = 5;
		this.questId = questId;
		this.sharerId = sharerId;
		this.unk = unk;
	}

	/**
	 * 使用给定参数构造 SM_QUEST_ACTION 包。
	 * Creates a SM_QUEST_ACTION packet with the given parameters.
	 *
	 * quest id
	 * @param fake 是否伪造 / whether fake
	 */
	public SM_QUEST_ACTION(int questId, boolean fake) {
		this.action = 6;
		this.questId = questId;
		this.timer = 0;
		this.step = 0;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		writeD(questId);
		switch (action) {
		case 1:
			writeC(status);
			writeC(0x0);
			writeD(step);
			writeH(0);
			writeC(0);
			break;
		case 2:
			writeC(status);
			writeC(0x0);
			writeD(step);
			writeH(0);
			break;
		case 3:
			writeD(0);
			break;
		case 4:
			writeD(timer);
			writeC(0x01);
			writeH(0x0);
			writeC(0x01);
			break;
		case 5:
			writeD(this.sharerId);
			writeD(0);
			break;
		case 6:
			writeH(0x01);
			writeH(0x0);
			break;
		}
	}
}
