package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
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

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
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

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
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
	void routesSelectionDialogAndQuestStateSyncToDistinctTypedPorts() {
		List<String> calls = new ArrayList<>();
		QuestDialogPort dialogs = new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				calls.add("quest:" + dialogId);
				return true;
			}

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				calls.add("selection:" + dialogId);
				return true;
			}
		};
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(dialogs, null, null, null, null, null,
			(snapshot, plan, mode) -> {
				calls.add("sync:" + plan.nextStatus() + ":" + mode);
				return true;
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.REWARD, 1, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.COMPLETE, 0, List.of(), List.of());

		port.execute(new AfterCommitAction.ShowQuestSelectionDialog(10), snapshot, plan);
		port.execute(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY), snapshot, plan);

		assertEquals(List.of("selection:10", "sync:COMPLETE:PACKET_ONLY"), calls);
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

				@Override
				public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
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

				@Override
				public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
					return true;
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.REWARD, 1, List.of(), List.of());

		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> port.execute(new AfterCommitAction.TeleportPlayer(110010000, 1f, 2f, 3f, (byte) 0), snapshot, plan));
	}

	@Test
	void routesPlayerEmotionAndNpcAggroToTypedPorts() {
		List<String> calls = new ArrayList<>();
		QuestEffectPort effects = new QuestEffectPort() {
			@Override
			public boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId) {
				return true;
			}

			@Override
			public boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId) {
				return true;
			}

			@Override
			public boolean playerEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
					QuestPlayerEmotion emotion) {
				calls.add("emotion:" + emotion + ":" + snapshot.interactionObjectId());
				return true;
			}
		};
		QuestNpcPort npcs = new QuestNpcPort() {
			@Override
			public boolean deleteInteractionNpc(QuestSnapshot snapshot, QuestMutationPlan plan,
					boolean scheduleRespawn) {
				return true;
			}

			@Override
			public boolean addNpcAggro(QuestSnapshot snapshot, QuestMutationPlan plan,
					int npcTemplateId, int damage) {
				calls.add("aggro:" + npcTemplateId + ":" + damage);
				return true;
			}
		};
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(dialogPort(), null, null, null, null,
			null, null, null, effects, npcs);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of())
			.withInteractionObjectId(900009);
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.START, 0, List.of(), List.of());

		port.execute(new AfterCommitAction.PlayerEmotion(QuestPlayerEmotion.STAND), snapshot, plan);
		port.execute(new AfterCommitAction.AddNpcAggro(203175, 50), snapshot, plan);

		assertEquals(List.of("emotion:STAND:900009", "aggro:203175:50"), calls);
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
			}, spawns, ai, timers, (snapshot, plan, mode) -> true,
			(snapshot, plan) -> {
				commands.add("stats");
				return true;
			},
			new QuestEffectPort() {
				@Override
				public boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId) {
					commands.add("morph:" + ascensionId);
					return true;
				}

				@Override
				public boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan,
						int flightTeleportId) {
					commands.add("flight:" + flightTeleportId);
					return true;
				}
			},
			(snapshot, plan, scheduleRespawn) -> {
				commands.add("delete-npc:" + scheduleRespawn);
				return true;
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1001, QuestStatus.START, 0, List.of(), List.of());

		port.execute(new AfterCommitAction.PlayMovie(250), snapshot, plan);
		port.execute(new AfterCommitAction.StartQuestTimer(300), snapshot, plan);
		port.execute(new AfterCommitAction.StartFollow("escort"), snapshot, plan);
		port.execute(new AfterCommitAction.RefreshPlayerStats(), snapshot, plan);

		assertEquals(List.of("movie:250", "timer:300", "follow:escort", "stats"), commands);
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

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}
		};
	}
}
