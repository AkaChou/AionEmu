package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RvR 相关 NPC AI：Field Gun（@AIName "field_gun"），继承 NpcAI2。
 * RvR-related NPC AI: Field Gun (@AIName "field_gun"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("field_gun")
public class Field_GunAI2 extends NpcAI2
{
	protected int startBarAnimation = 1;
	protected int cancelBarAnimation = 2;
	
	private AtomicBoolean canUse = new AtomicBoolean(true);
	
	@Override
	protected void handleDialogStart(Player player) {
		handleUseItemStart(player);
	}
	
	protected void handleUseItemStart(final Player player) {
		final int delay = getTalkDelay();
		if (delay != 0) {
			final ItemUseObserver observer = new ItemUseObserver() {
				@Override
				public void abort() {
					player.getController().cancelTask(TaskId.ACTION_ITEM_NPC);
					PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
					PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), 0, cancelBarAnimation));
					player.getObserveController().removeObserver(this);
				}
			};
			player.getObserveController().attach(observer);
			PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), getTalkDelay(), startBarAnimation));
			PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_QUESTLOOT, 0, getObjectId()), true);
			player.getController().addTask(TaskId.ACTION_ITEM_NPC, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
					PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), getTalkDelay(), cancelBarAnimation));
					player.getObserveController().removeObserver(observer);
					handleUseItemFinish(player);
				}
			}, delay));
		} else {
			handleUseItemFinish(player);
		}
	}
	
	protected void handleUseItemFinish(Player player) {
		if (canUse.compareAndSet(true, false)) {
			int morphSkill = getMorphSkill();
			GameEngineServices.skillEngine().getSkill(getOwner(), morphSkill >> 8, morphSkill & 0xFF, player).useWithoutPropSkill();
			AI2Actions.deleteOwner(this);
			AI2Actions.scheduleRespawn(this);
		}
	}
	
	private int getMorphSkill() {
		switch (getNpcId()) {
			case 209472: //Baltasar Hill Field Gun.
			case 831339: //Asmodians Field Gun.
				return 0x4F8D3C;
			case 209471: //Jamanok Inn Field Gun.
			case 831338: //Elyos Field Gun.
				return 0x4F8C3C;
		}
		return 0;
	}
	
	protected int getTalkDelay() {
		return getObjectTemplate().getTalkDelay() * 1000;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
