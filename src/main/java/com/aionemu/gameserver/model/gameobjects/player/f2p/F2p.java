package com.aionemu.gameserver.model.gameobjects.player.f2p;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.F2pDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;

/**
 * F2p 游戏对象。
 * F2p game object.
 */

public class F2p {
	private Player owner;
	private F2pAccount f2pAccount;

	public F2p(Player owner) {
		this.owner = owner;
	}

	/** 添加。 / Add. */
	public void add(F2pAccount f2pacc, boolean isNew) {
		f2pAccount = f2pacc;
		f2pacc.setActive(true);
		if (isNew) {
			if (f2pacc.getExpireTime() != 0) {
				GameTaskManagerServices.expireTimerTask().addTask(f2pacc, owner);
			}
			DAOManager.getDAO(F2pDAO.class).storeF2p(owner.getObjectId().intValue(), f2pacc.getExpireTime());
		}
	}

	/** 更新。 / Update. */
	public void update(F2pAccount f2pacc, boolean isNew) {
		f2pAccount = f2pacc;
		f2pacc.setActive(true);
		if (isNew) {
			if (f2pacc.getExpireTime() != 0) {
				GameTaskManagerServices.expireTimerTask().addTask(f2pacc, owner);
			}
			DAOManager.getDAO(F2pDAO.class).storeF2p(owner.getObjectId().intValue(), f2pacc.getExpireTime());
		}
	}

	/** 返回 F2p 账号 / Returns the F2p account */
	public F2pAccount getF2pAccount() {
		return f2pAccount;
	}

	/** 移除。 / Remove. */
	public boolean remove() {
		if (f2pAccount != null) {
			f2pAccount.setActive(false);
			DAOManager.getDAO(F2pDAO.class).deleteF2p(owner.getObjectId().intValue());
			owner.getEquipment().checkRankLimitItems();
			return true;
		}
		return false;
	}
}
