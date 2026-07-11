package com.aionemu.gameserver.model.gameobjects.player.title;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.listeners.TitleChangeListener;
import com.aionemu.gameserver.model.templates.TitleTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 称号列表。
 * Title List game object.
 */

public class TitleList {

	private final Map<Integer, Title> titles;
	private Player owner;

	public TitleList() {
		this.titles = new HashMap<>();
		this.owner = null;
	}

	/** 设置所有者 / Sets the owner*/
	public void setOwner(Player owner) {
		this.owner = owner;
	}

	/** 返回所有者 / Returns the owner*/
	public Player getOwner() {
		return owner;
	}

	/** 是否包含。 / Contains. */
	public boolean contains(int titleId) {
		return titles.containsKey(titleId);
	}

	/** 添加条目。 / Adds entry. */
	public void addEntry(int titleId, int remaining) {
		TitleTemplate tt = DataManager.TITLE_DATA.getTitleTemplate(titleId);
		if (tt == null) {
			throw new IllegalArgumentException("Invalid title id " + titleId);
		}
		titles.put(titleId, new Title(tt, titleId, remaining));
	}

	/** 添加称号。 / Adds title. */
	public boolean addTitle(int titleId, boolean questReward, int time) {
		TitleTemplate tt = DataManager.TITLE_DATA.getTitleTemplate(titleId);
		if (tt == null) {
			throw new IllegalArgumentException("Invalid title id " + titleId);
		}
		if (owner != null) {
			if (owner.getRace() != tt.getRace() && tt.getRace() != Race.PC_ALL) {
				PacketSendUtility.sendMessage(owner, "This title is not available for your race.");
				return false;
			}
			Title entry = new Title(tt, titleId, time);
			if (!titles.containsKey(titleId)) {
				titles.put(titleId, entry);
				if (time != 0)
					GameTaskManagerServices.expireTimerTask().addTask(entry, owner);
				DAOManager.getDAO(PlayerTitleListDAO.class).storeTitles(owner, entry);
			} else {
				PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_TOOLTIP_LEARNED_TITLE);
				return false;
			}
			if (questReward) {
				PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_QUEST_GET_REWARD_TITLE(tt.getNameId()));
			} else {
				PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_MSG_GET_CASH_TITLE(tt.getNameId()));
			}
			PacketSendUtility.sendPacket(owner, new SM_TITLE_INFO(owner));
			return true;
		}
		return false;
	}

	/** 设置 display title / Sets the display title */
	public void setDisplayTitle(int titleId) {
		PacketSendUtility.sendPacket(owner, new SM_TITLE_INFO(titleId));
		PacketSendUtility.broadcastPacketAndReceive(owner, new SM_TITLE_INFO(owner, titleId));
		owner.getCommonData().setTitleId(titleId);
	}

	/** 设置加成称号。 / Sets the bonus title. */
	public void setBonusTitle(int bonusTitleId) {
		PacketSendUtility.sendPacket(owner, new SM_TITLE_INFO(6, bonusTitleId));
		if (owner.getCommonData().getBonusTitleId() > 0) {
			if (owner.getGameStats() != null) {
				TitleChangeListener.onBonusTitleChange(owner.getGameStats(), owner.getCommonData().getBonusTitleId(),
						false);
			}
		}
		owner.getCommonData().setBonusTitleId(bonusTitleId);
		if (bonusTitleId > 0 && owner.getGameStats() != null) {
			TitleChangeListener.onBonusTitleChange(owner.getGameStats(), bonusTitleId, true);
		}
	}

	/** 移除称号。 / Removes title. */
	public void removeTitle(int titleId) {
		if (!titles.containsKey(titleId)) {
			return;
		}
		if (owner.getCommonData().getTitleId() == titleId) {
			setDisplayTitle(-1);
		}
		if (owner.getCommonData().getBonusTitleId() == titleId) {
			setBonusTitle(-1);
		}
		titles.remove(titleId);
		PacketSendUtility.sendPacket(owner, new SM_TITLE_INFO(owner));
		DAOManager.getDAO(PlayerTitleListDAO.class).removeTitle(owner.getObjectId(), titleId);
	}

	/** 大小 / size. */
	public int size() {
		return titles.size();
	}

	/** 返回 titles / Returns the titles */
	public Collection<Title> getTitles() {
		return titles.values();
	}
}
