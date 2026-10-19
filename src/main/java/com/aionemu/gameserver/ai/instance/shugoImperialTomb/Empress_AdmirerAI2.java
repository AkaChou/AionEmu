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
 * Shugo Imperial Tomb 副本 NPC AI：Empress Admirer（@AIName "Empress_Admirer"），继承 NpcAI2。
 * Shugo Imperial Tomb instance NPC AI: Empress Admirer (@AIName "Empress_Admirer"), extends NpcAI2.
 * 波次任务由副本持有，开战删除本 NPC 不会取消波次，销毁副本则会取消。
 * Wave tasks belong to the instance: deleting this NPC starts the battle, while instance teardown cancels its waves.
 * @author Encom
 */
@AIName("Empress_Admirer")
public class Empress_AdmirerAI2 extends NpcAI2 {
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
		if (dialogId != 10000 || getNpcId() != 831111 || player == null) {
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
			startTombWaveB1();
			// 掠夺者来袭。守卫女皇纪念碑！ / Pillagers incoming. Guard the Empress' Monument!
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401583));
			spawn(831130, 340.27893f, 426.2435f, 294.7574f, (byte) 56);
			spawn(831304, 337.75626f, 415.45035f, 294.76086f, (byte) 56);
			spawn(831305, 342.7412f, 436.7674f, 294.75598f, (byte) 56);
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
	protected void startTombWaveB1() {
		ShugoImperialTombInstance handler =
			(ShugoImperialTombInstance) getPosition().getWorldMapInstance().getInstanceHandler();
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219514, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219514, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219514, 307.97067f, 422.196f, 296.40808f, (byte) 78), 322.31113f, 405.27344f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
		}, 10000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219515, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219515, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219515, 307.97067f, 422.196f, 296.40808f, (byte) 78), 322.31113f, 405.27344f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
		}, 30000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219514, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 313.3913f, 443.98557f, 296.40808f, (byte) 13), 333.79936f, 453.9313f, 296.40808f, false);
			attackEvent((Npc)spawn(219514, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
			attackEvent((Npc)spawn(219514, 307.97067f, 422.196f, 296.40808f, (byte) 78), 322.31113f, 405.27344f, 296.40808f, false);
			attackEvent((Npc)spawn(219519, 316.16348f, 431.76547f, 294.58875f, (byte) 78), 335.83118f, 427.14017f, 294.7588f, false);
		}, 50000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
