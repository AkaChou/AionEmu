package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 对可交互 NPC 播放使用动画，延迟结束后执行完成操作。
 * Plays the use animation on an interactable NPC and runs finish operations after a delay.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionItemUseOperation", propOrder = { "finish" })
public class ActionItemUseOperation extends QuestOperation {

	/** 使用完成后执行的操作 / Operations run after the use finishes */
	@XmlElement(required = true)
	protected QuestOperations finish;

	/**
	 * 向玩家发送使用 / 表情包，并在默认 3000ms 后执行 finish。
	 * emotion packets to the player and runs finish after the default 3000ms. / emotion packets to the player and runs finish after the default 3000ms.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(final QuestEnv env) {
		final Player player = env.getPlayer();
		final Npc npc;
		if (env.getVisibleObject() instanceof Npc) {
			npc = (Npc) env.getVisibleObject();
		} else {
			return;
		}
		final int defaultUseTime = 3000;
		PacketSendUtility.sendPacket(player,
				new SM_USE_OBJECT(player.getObjectId(), npc.getObjectId(), defaultUseTime, 1));
		PacketSendUtility.broadcastPacket(player,
				new SM_EMOTION(player, EmotionType.START_QUESTLOOT, 0, npc.getObjectId()), true);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				PacketSendUtility.sendPacket(player,
						new SM_USE_OBJECT(player.getObjectId(), npc.getObjectId(), defaultUseTime, 0));
				finish.operate(env);
			}
		}, defaultUseTime);
	}
}
