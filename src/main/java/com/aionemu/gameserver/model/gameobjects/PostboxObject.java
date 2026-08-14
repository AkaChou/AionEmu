package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingPostbox;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_OBJECT_USE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Postbox 对象。
 * Postbox Object game object.
 */

public class PostboxObject extends HouseObject<HousingPostbox> {

	private AtomicReference<Player> usingPlayer = new AtomicReference<Player>();

	public PostboxObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 使用时 / On use. */
	@Override
	public void onUse(final Player player) {
		if (!usingPlayer.compareAndSet(null, player)) {
			if (usingPlayer.compareAndSet(player, player)) {
				return;
			}
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_OCCUPIED_BY_OTHER);
			return;
		}

		final ItemUseObserver observer = new ItemUseObserver() {

			/** 中止 / Abort. */
			@Override
			public void abort() {
				player.getObserveController().removeObserver(this);
				usingPlayer.set(null);
			}
		};
		player.getObserveController().attach(observer);

		PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_USE(getObjectTemplate().getNameId()));
		player.getController().addTask(TaskId.HOUSE_OBJECT_USE,
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

					/** 运行 / Run. */
					@Override
					public void run() {
						try {
							PacketSendUtility.sendPacket(player,
									new SM_DIALOG_WINDOW(getObjectId(), DialogPage.MAIL.id()));
							player.getMailbox().sendMailList(false);
							PacketSendUtility.sendPacket(player,
									new SM_OBJECT_USE_UPDATE(player.getObjectId(), 0, 0, PostboxObject.this));
						} finally {
							player.getObserveController().removeObserver(observer);
							usingPlayer.set(null);
						}
					}
				}, 0));
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return usingPlayer.get() == null;
	}
}
