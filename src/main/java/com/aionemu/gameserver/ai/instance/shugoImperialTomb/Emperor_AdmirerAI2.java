package com.aionemu.gameserver.ai.instance.shugoImperialTomb;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.ShugoImperialTombInstance;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Shugo Imperial Tomb 副本 NPC AI：Emperor Admirer（@AIName "Emperor_Admirer"），继承 NpcAI2。
 * Shugo Imperial Tomb instance NPC AI: Emperor Admirer (@AIName "Emperor_Admirer"), extends NpcAI2.
 * 波次任务由副本持有，开战删除本 NPC 不会取消波次，销毁副本则会取消。
 * Wave tasks belong to the instance: deleting this NPC starts the battle, while instance teardown cancels its waves.
 * @author Encom
 */
@AIName("Emperor_Admirer")
public class Emperor_AdmirerAI2 extends NpcAI2 {
	/** 防止重复对话重复开战 / Guards against duplicate battle start. */
	private boolean tombBattleStarted;

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	/**
	 * 仅在合法开战请求且参与者均已变身时启动一次战斗；拒绝时保留入口和变身装置。
	 * Starts one battle only for a valid request with transformed participants; rejection preserves the entry and device.
	 */
	@Override
	public synchronized boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId != 10000 || getNpcId() != 831112 || player == null) {
			sendDialogClose(player);
			return true;
		}
		if (!canStartBattle(player)) {
			PacketSendUtility.sendMessage(player, I18n.get("instance.shugo_tomb.transform_required"));
			sendDialogClose(player);
			return true;
		}
		if (!tombBattleStarted) {
			tombBattleStarted = true;
			startTombWaveC1();
			// 掠夺者来袭。守卫皇帝纪念碑！ / Pillagers incoming. Guard the Emperor's Monument!
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401584));
			spawn(831130, 452.43765f, 106.14462f, 212.20023f, (byte) 68);
			spawn(831250, 452.92874f, 85.73192f, 214.3359f, (byte) 75);
			spawn(831251, 435.34854f, 120.64044f, 214.336f, (byte) 62);
		}
		sendDialogClose(player);
		deleteOwner();
		return true;
	}

	protected boolean canStartBattle(Player player) {
		ShugoImperialTombInstance handler =
			(ShugoImperialTombInstance) getPosition().getWorldMapInstance().getInstanceHandler();
		return handler != null && handler.canStartTombBattle(player);
	}

	protected void sendDialogClose(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
	}

	protected void deleteOwner() {
		AI2Actions.deleteOwner(this);
	}

	private void attackEvent(final Npc npc, float x, float y, float z, boolean despawn) {
		if (npc == null) {
			return;
		}
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}

	/**
	 * 将开场波次交由副本调度，使其在本 NPC 删除后继续、在副本销毁时取消。
	 * Schedules opening waves on the instance so they survive this NPC's deletion but not instance teardown.
	 */
	protected void startTombWaveC1() {
		ShugoImperialTombInstance handler =
			(ShugoImperialTombInstance) getPosition().getWorldMapInstance().getInstanceHandler();
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219521, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219521, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219521, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 10000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219522, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219522, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219522, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 30000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219523, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219523, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219523, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 50000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219521, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219521, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219521, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 70000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219522, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219522, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219522, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 90000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219523, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
			attackEvent((Npc)spawn(219523, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
			attackEvent((Npc)spawn(219523, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
		}, 110000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
