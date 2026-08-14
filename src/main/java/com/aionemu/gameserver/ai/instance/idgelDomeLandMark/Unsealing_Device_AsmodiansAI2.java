package com.aionemu.gameserver.ai.instance.idgelDomeLandMark;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;

import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Idgel Dome Land Mark 副本 NPC AI：Unsealing Device Asmodians（@AIName "Unsealing_Device_Asmodians"），继承 ActionItemNpcAI2。
 * Idgel Dome Land Mark instance NPC AI: Unsealing Device Asmodians (@AIName "Unsealing_Device_Asmodians"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("Unsealing_Device_Asmodians")
public class Unsealing_Device_AsmodiansAI2 extends ActionItemNpcAI2
{
    private boolean isRewarded; // 是否已发放过奖励 / whether the reward was already granted
	protected int startBarAnimation = 1; // 读条开始动画 ID / start cast bar animation ID
	protected int cancelBarAnimation = 2; // 读条取消动画 ID / cancel cast bar animation ID
	
    @Override
    protected void handleDialogStart(Player player) {
        handleUseItemStart(player);
		InstanceReward<?> instance = getPosition().getWorldMapInstance().getInstanceHandler().getInstanceReward();
        if (instance != null && !instance.isStartProgress()) {
            return;
        }
        super.handleDialogStart(player);
    }
	
	/**
	 * 带读条的物品交互：延迟结束后触发使用完成逻辑，交互中断则取消任务。
	 * Item use with a cast bar: triggers the finish logic after the delay, cancels the task on abort.
	 */
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
	
    @Override
    protected void handleUseItemFinish(Player player) {
        // 仅首次使用生效：删除解封装置并生成对应阶段的周期装置。
        // Applies only on first use: deletes the device and spawns the corresponding cycle devices.
        if (!isRewarded) {
            isRewarded = true;
			AI2Actions.deleteOwner(this);
			AI2Actions.handleUseItemFinish(this, player);
			switch (getNpcId()) {
			    case 806375: //解封装置。 / Unsealing Device.
					spawn(806359, 264.12469f, 273.46167f, 85.796768f, (byte) 0, 154);
					spawn(806280, 264.74783f, 259.22983f, 88.351212f, (byte) 0, 194);
				break;
				case 806376: //解封装置。 / Unsealing Device.
			        spawn(806280, 264.74783f, 259.22983f, 89.924179f, (byte) 0, 195);
					spawn(806360, 265.26395f, 245.08080f, 85.796768f, (byte) 0, 106);
				break;
				case 806377: //解封装置。 / Unsealing Device.
					spawn(806280, 264.74783f, 259.22983f, 91.488808f, (byte) 0, 196);
					spawn(806361, 250.50882f, 258.67883f, 85.796768f, (byte) 0, 153);
				break;
				case 806378: //解封装置。 / Unsealing Device.
			        spawn(806362, 278.85132f, 259.72498f, 85.796768f, (byte) 0, 104);
				break;
			}
        }
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	protected int getTalkDelay() {
		return getObjectTemplate().getTalkDelay() * 1000;
	}
}
