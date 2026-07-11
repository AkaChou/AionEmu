package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 称号相关信息的服务端包（列表、自身/广播设置、导师标记、奖励称号等）。
 * Server packet for title-related info (list, self/broadcast set, mentor flag, bonus title, etc.).
 * <p>
 * 通过 {@code action} 区分用途：0 列表、1 自身设置、3 广播设置、4/5 导师标记、6 奖励称号。
 * Uses {@code action} to distinguish purpose: 0 list, 1 self set, 3 broadcast set, 4/5 mentor flag, 6 bonus title.
 *
 * @author cura, xTz
 */
public class SM_TITLE_INFO extends AionServerPacket {

	private TitleList titleList;
	private int bonusTitleId;
	private int action; // 0: list, 1: self set, 3: broad set
	private int titleId;
	private int playerObjId;

	/**
	 * 称号列表。
	 * Title list.
	 *
	 * 玩家 / player
	 */
	public SM_TITLE_INFO(Player player) {
		this.action = 0;
		this.titleList = player.getTitleList();
	}

	/**
	 * 自身称号设置。
	 * Self title set.
	 *
	 * title id
	 */
	public SM_TITLE_INFO(int titleId) {
		this.action = 1;
		this.titleId = titleId;
	}

	/**
	 * 广播称号设置。
	 * Broadcast title set.
	 *
	 * 玩家 / player
	 * title id
	 */
	public SM_TITLE_INFO(Player player, int titleId) {
		this.action = 3;
		this.playerObjId = player.getObjectId();
		this.titleId = titleId;
	}

	/**
	 * 自身导师标记。
	 * Self mentor flag.
	 *
	 * @param flag 是否开启 / whether enabled
	 */
	public SM_TITLE_INFO(boolean flag) {
		this.action = 4;
		this.titleId = flag ? 1 : 0;
	}

	/**
	 * 广播导师标记。
	 * Broadcast mentor flag.
	 *
	 * 玩家 / player
	 * @param flag   是否开启 / whether enabled
	 */
	public SM_TITLE_INFO(Player player, boolean flag) {
		this.action = 5;
		this.playerObjId = player.getObjectId();
		this.titleId = flag ? 1 : 0;
	}

	/**
	 * 奖励称号等其它 action。
	 * Bonus title or other action.
	 *
	 * action type
	 * bonus title id
	 */
	public SM_TITLE_INFO(int action, int bonusTitleId) {
		this.action = action;
		this.bonusTitleId = bonusTitleId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 0:
			writeC(0x00);
			writeC(0x01);// 5.0
			writeH(titleList.size());
			for (Title title : titleList.getTitles()) {
				writeD(title.getId());
				writeD(title.getRemainingTime());
			}
			break;
		case 1: // self set
			writeH(titleId);
			break;
		case 3: // broad set
			writeD(playerObjId);
			writeH(titleId);
			break;
		case 4: // Mentor flag self
			writeH(titleId);
			break;
		case 5: // broad set mentor fleg
			writeD(playerObjId);
			writeH(titleId);
			break;
		case 6: // bonus title
			writeH(bonusTitleId);
			break;
		}
	}
}
