package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypedQuestAfterCommitPortTest {
	@Test
	void routesCloseDialogToTypedPort() {
		List<Integer> questIds = new ArrayList<>();
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				questIds.add(snapshot.questId());
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				questIds.add(dialogId);
				return true;
			}
		});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.REWARD, 1, List.of(), List.of());

		port.execute(new AfterCommitAction.CloseDialog(), snapshot, plan);

		assertEquals(List.of(1001), questIds);
	}

	@Test
	void routesShowQuestDialogToTypedPort() {
		List<Integer> dialogs = new ArrayList<>();
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				dialogs.add(-1);
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				dialogs.add(dialogId);
				return true;
			}
		});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.REWARD, 1, List.of(), List.of());

		port.execute(new AfterCommitAction.ShowQuestDialog(1011), snapshot, plan);

		assertEquals(List.of(1011), dialogs);
	}

	@Test
	void routesTeleportPlayerToTypedPort() {
		List<String> teleports = new ArrayList<>();
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(
			new QuestDialogPort() {
				@Override
				public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
					return true;
				}

				@Override
				public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
					return true;
				}
			},
			(snapshot, plan, worldId, x, y, z, heading) -> {
				teleports.add(worldId + ":" + x + ":" + y + ":" + z + ":" + heading);
				return true;
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.REWARD, 1, List.of(), List.of());

		port.execute(new AfterCommitAction.TeleportPlayer(110010000, 1474f, 1352f, 564f, (byte) 21), snapshot, plan);

		assertEquals(List.of("110010000:1474.0:1352.0:564.0:21"), teleports);
	}

	@Test
	void teleportPlayerFailsClosedWithoutPort() {
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(
			new QuestDialogPort() {
				@Override
				public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
					return true;
				}

				@Override
				public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
					return true;
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.REWARD, 1, List.of(), List.of());

		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> port.execute(new AfterCommitAction.TeleportPlayer(110010000, 1f, 2f, 3f, (byte) 0), snapshot, plan));
	}

	@Test
	void routesEmotionAndFollowZoneToTypedAiPort() {
		List<String> commands = new ArrayList<>();
		QuestAiPort ai = new QuestAiPort() {
			@Override
			public boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return true;
			}

			@Override
			public boolean stopFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return true;
			}

			@Override
			public boolean attackTarget(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return true;
			}

			@Override
			public boolean startWalking(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return true;
			}

			@Override
			public boolean broadcastEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
					QuestNpcEmotion emotion) {
				commands.add("emotion:" + slot + ":" + emotion);
				return true;
			}

			@Override
			public boolean watchFollowZone(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, String zone) {
				commands.add("zone:" + slot + ":" + zone);
				return true;
			}
		};
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(dialogPort(), null, null, null, ai, null);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.START, 0, List.of(), List.of());

		port.execute(new AfterCommitAction.BroadcastNpcEmotion("escort", QuestNpcEmotion.START_EMOTE2), snapshot, plan);
		port.execute(new AfterCommitAction.WatchFollowZone("escort", "DF2_ITEMUSEAREA_Q2333"), snapshot, plan);

		assertEquals(List.of("emotion:escort:START_EMOTE2", "zone:escort:DF2_ITEMUSEAREA_Q2333"), commands);
	}

	@Test
	void fullyComposedPortRoutesMovieTimerAndEscortCapabilities() {
		List<String> commands = new ArrayList<>();
		QuestAiPort ai = new RecordingAiPort(commands);
		QuestTimerPort timers = new QuestTimerPort() {
			@Override
			public boolean startQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
					QuestTimerPolicy policy) {
				commands.add("timer:" + seconds);
				return true;
			}

			@Override
			public boolean startInvisibleTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
					QuestTimerPolicy policy) {
				return true;
			}

			@Override
			public boolean cancelQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan,
					QuestTimerPolicy.Identity identity) {
				return true;
			}
		};
		QuestSpawnPort spawns = new QuestSpawnPort() {
			@Override
			public boolean spawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int templateId,
					QuestSpawnLocation location) {
				return true;
			}

			@Override
			public boolean despawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return true;
			}
		};
		TypedQuestAfterCommitPort port = TypedQuestAfterCommitPort.fullyComposed(
			dialogPort(), (snapshot, plan, worldId, x, y, z, heading) -> true,
			(snapshot, plan, movieId) -> {
				commands.add("movie:" + movieId);
				return true;
			}, spawns, ai, timers);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.START, 0, List.of(), List.of());

		port.execute(new AfterCommitAction.PlayMovie(250), snapshot, plan);
		port.execute(new AfterCommitAction.StartQuestTimer(300), snapshot, plan);
		port.execute(new AfterCommitAction.StartFollow("escort"), snapshot, plan);

		assertEquals(List.of("movie:250", "timer:300", "follow:escort"), commands);
	}

	private static final class RecordingAiPort implements QuestAiPort {
		private final List<String> commands;

		private RecordingAiPort(List<String> commands) {
			this.commands = commands;
		}

		@Override
		public boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
			commands.add("follow:" + slot);
			return true;
		}

		@Override
		public boolean stopFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
			return true;
		}

		@Override
		public boolean attackTarget(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
			return true;
		}

		@Override
		public boolean startWalking(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
			return true;
		}

		@Override
		public boolean broadcastEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
				QuestNpcEmotion emotion) {
			return true;
		}

		@Override
		public boolean watchFollowZone(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, String zone) {
			return true;
		}
	}

	private static QuestDialogPort dialogPort() {
		return new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}
		};
	}
}
