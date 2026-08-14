package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/** 真实 {@link QuestDialogPort}：提交后关闭玩家的任务对话窗口。 / Real {@link QuestDialogPort}: closes the player's quest dialog window after commit. */
public final class PlayerQuestDialogPort implements QuestDialogPort {
	private final QuestPlayerPort players;

	public PlayerQuestDialogPort(QuestPlayerPort players) {
		this.players = Objects.requireNonNull(players, "players");
	}

	@Override
	public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可发送对象，best-effort 关闭。 / Commit succeeded but player logged out: nothing to send to, best-effort close.
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
		return true;
	}

	@Override
	public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId < 0) {
			throw new IllegalArgumentException("dialogId must be non-negative");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可发送对象，best-effort 跳过。 / Commit succeeded but player logged out: nothing to send to, best-effort skip.
			return false;
		}
		int objectId = snapshot.targetlessDialog() ? 0 : snapshot.interactionObjectId();
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			// 缺少权威交互 objectId 时必须 fail closed，禁止用 NPC templateId 或玩家 target 猜测。
			// Missing an authoritative interaction objectId must fail closed; never guess
			// from the NPC templateId or the player's current target.
			throw new IllegalStateException("showDialog requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId, snapshot.questId()));
		return true;
	}

	@Override
	public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId <= 0) {
			throw new IllegalArgumentException("dialogId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int objectId = snapshot.targetlessDialog() ? 0 : snapshot.interactionObjectId();
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			throw new IllegalStateException("showSelectionDialog requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId));
		return true;
	}

	@Override
	public boolean showDialogWindow(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId < 0) {
			throw new IllegalArgumentException("dialogId must be non-negative");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int objectId = snapshot.targetlessDialog() ? 0 : snapshot.interactionObjectId();
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			throw new IllegalStateException("showDialogWindow requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId));
		return true;
	}
}
