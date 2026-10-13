package com.aionemu.gameserver.model.gameobjects.player.f2p;


import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.player.PlayerService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;

/**
 * F2p 游戏对象。
 * F2p game object.
 */

@Getter
@RequiredArgsConstructor
public class F2p {
	private final Player owner;
	/**
	 * 返回 F2p 账号 / Returns the F2p account
	 */
	private F2pAccount f2pAccount;

	/**
	 * 添加。 / Add.
	 */
	public void add(F2pAccount f2pacc, boolean isNew) {
		f2pAccount = f2pacc;
		f2pacc.setActive(true);
		if (isNew) {
			if (f2pacc.getExpireTime() != 0) {
				ExpireTimerTask.getInstance().addTask(f2pacc, owner);
			}
			PlayerService.daos().f2pDAO().storeF2p(owner.getObjectId(), f2pacc.getExpireTime());
		}
	}

	/**
	 * 更新。 / Update.
	 */
	public void update(F2pAccount f2pacc, boolean isNew) {
		f2pAccount = f2pacc;
		f2pacc.setActive(true);
		if (isNew) {
			if (f2pacc.getExpireTime() != 0) {
				ExpireTimerTask.getInstance().addTask(f2pacc, owner);
			}
			PlayerService.daos().f2pDAO().storeF2p(owner.getObjectId(), f2pacc.getExpireTime());
		}
	}

	/**
	 * 移除。 / Remove.
	 */
	public boolean remove() {
		if (f2pAccount != null) {
			f2pAccount.setActive(false);
			PlayerService.daos().f2pDAO().deleteF2p(owner.getObjectId());
			owner.getEquipment().checkRankLimitItems();
			return true;
		}
		return false;
	}
}
