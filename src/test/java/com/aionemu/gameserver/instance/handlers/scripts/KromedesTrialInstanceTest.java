package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class KromedesTrialInstanceTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KromedesTrialInstance.java");
	private static final Path SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300230000_Kromede's_Trial.xml");

	@Test
	void onDieDoesNotDereferenceMissingDamageOwnerForClassTreasure() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		assertTrue(onDie.contains("Player player = getDeathRewardPlayer(npc);"));
		assertFalse(onDie.contains("player.getPlayerClass()"));
		assertTrue(onDie.contains("spawnClassTreasure(player, 740.83966f, 535.38837f, 199.12067f, (byte) 89);"));
		assertTrue(onDie.contains("spawnClassTreasure(player, 512.89886f, 570.039f, 216.89487f, (byte) 31);"));
	}

	@Test
	void finalBossRewardSkipsPlayerActionsWhenDamageOwnerIsMissing() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		int finalBossCase = onDie.indexOf("case 217005:");
		int playerGuard = onDie.indexOf("if (player != null)", finalBossCase);
		int sendMovie = onDie.indexOf("sendMovie(player, 455);", finalBossCase);

		assertTrue(finalBossCase >= 0);
		assertTrue(playerGuard > finalBossCase);
		assertTrue(sendMovie > playerGuard);
	}

	@Test
	void doesNotDuplicateNpcAiWoundedNpcTriggers() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		assertFalse(onDie.contains("spawn(217001"),
				"the NPC AI owns the IDCromede_Invisible_NPC13 trigger");
		assertFalse(onDie.contains("spawn(217003"),
				"the NPC AI owns the IDCromede_Invisible_NPC14 trigger");
		assertFalse(onDie.contains("spawn(217004"),
				"the NPC AI owns the IDCromede_Invisible_NPC12 trigger");
	}

	@Test
	void classTreasureSpawnIgnoresMissingDamageOwner() throws IOException {
		String spawnClassTreasure = methodBody(Files.readString(SOURCE),
				"private void spawnClassTreasure(Player player, float x, float y, float z, byte heading)");

		int nullGuard = spawnClassTreasure.indexOf("if (player == null)");
		int classSwitch = spawnClassTreasure.indexOf("switch (player.getPlayerClass())");

		assertTrue(nullGuard >= 0);
		assertTrue(classSwitch > nullGuard);
	}

	@Test
	void staticFinalBossSpawnsAreHandledByInstanceHandler() throws IOException {
		String spawns = Files.readString(SPAWNS);

		assertFalse(spawns.contains("<spawn npc_id=\"217005\""),
				"217005 is selected by KromedesTrialInstance and must not be statically spawned");
		assertFalse(spawns.contains("<spawn npc_id=\"217006\""),
				"217006 is selected by KromedesTrialInstance and must not be statically spawned");
		assertFalse(spawns.contains("<spawn npc_id=\"217119\""),
				"217119 must not be a repeating static spawn for the final boss");
	}

	@Test
	void staticRobstinSpawnBelongsOnlyToThePureInstance() throws IOException {
		String spawns = Files.readString(SPAWNS);

		assertFalse(spawns.contains("<spawn npc_id=\"700939\""),
				"700939 is selected only for the active quest step");
		assertTrue(spawns.contains("<spawn npc_id=\"700965\" respawn_time=\"60\">"),
				"700965 is the static pure-instance Robstin");
	}

	@Test
	void taskRobstinIsSelectedOnlyAtTheRescueStep() {
		QuestState elyosRescueStep = new QuestState(18602, QuestStatus.START, 2, 0, null, null, null);
		QuestState asmodianRescueStep = new QuestState(28602, QuestStatus.START, 2, 0, null, null, null);
		QuestState beforeRescue = new QuestState(18602, QuestStatus.START, 1, 0, null, null, null);
		QuestState afterRescue = new QuestState(18602, QuestStatus.START, 3, 0, null, null, null);

		assertTrue(KromedesTrialInstance.isRobstinQuestStep(elyosRescueStep));
		assertTrue(KromedesTrialInstance.isRobstinQuestStep(asmodianRescueStep));
		assertFalse(KromedesTrialInstance.isRobstinQuestStep(beforeRescue));
		assertFalse(KromedesTrialInstance.isRobstinQuestStep(afterRescue));
	}

	@Test
	void preservesOnlyAKeyHeldAtTheMagaPotionQuestStep() {
		QuestState stepOne = new QuestState(18602, QuestStatus.START, 1, 0, null, null, null);
		QuestState beforeInstance = new QuestState(18602, QuestStatus.START, 0, 0, null, null, null);
		QuestState afterPotion = new QuestState(18602, QuestStatus.START, 2, 0, null, null, null);

		assertTrue(KromedesTrialInstance.shouldPreserveRelicKeyOnLeave(stepOne, 1));
		assertFalse(KromedesTrialInstance.shouldPreserveRelicKeyOnLeave(stepOne, 0));
		assertFalse(KromedesTrialInstance.shouldPreserveRelicKeyOnLeave(beforeInstance, 1));
		assertFalse(KromedesTrialInstance.shouldPreserveRelicKeyOnLeave(afterPotion, 1));
	}

	@Test
	void recoversTheKeyOnlyForAnActiveStepWithAnUnavailableSourceOrLeaveMarker() {
		QuestState stepOne = new QuestState(18602, QuestStatus.START, 1, 0, null, null, null);
		QuestState beforeInstance = new QuestState(18602, QuestStatus.START, 0, 0, null, null, null);
		QuestState afterPotion = new QuestState(18602, QuestStatus.START, 2, 0, null, null, null);

		assertTrue(KromedesTrialInstance.shouldRecoverRelicKey(stepOne, false, true, false));
		assertTrue(KromedesTrialInstance.shouldRecoverRelicKey(stepOne, false, false, true));
		assertFalse(KromedesTrialInstance.shouldRecoverRelicKey(stepOne, false, false, false));
		assertFalse(KromedesTrialInstance.shouldRecoverRelicKey(stepOne, true, true, true));
		assertFalse(KromedesTrialInstance.shouldRecoverRelicKey(beforeInstance, false, true, false));
		assertFalse(KromedesTrialInstance.shouldRecoverRelicKey(afterPotion, false, true, false));
	}

	@Test
	void remembersBeforeCleanupAndRestoresOnInstanceReentryOrLogin() throws IOException {
		String source = Files.readString(SOURCE);
		String onEnter = methodBody(source, "public void onEnterInstance(Player player)");
		String onLogin = methodBody(source, "public void onPlayerLogin(Player player)");
		String onLeave = methodBody(source, "public void onLeaveInstance(Player player)");
		String onLogout = methodBody(source, "public void onPlayerLogOut(Player player)");
		String onDestroy = methodBody(source, "public void onInstanceDestroy()");

		assertTrue(onEnter.contains("restoreRelicKey(player);"));
		assertTrue(onLogin.contains("restoreRelicKey(player);"));
		assertTrue(onLeave.indexOf("rememberRelicKey(player);") >= 0);
		assertTrue(onLeave.indexOf("removeItems(player);") > onLeave.indexOf("rememberRelicKey(player);"));
		assertTrue(onLogout.indexOf("rememberRelicKey(player);") >= 0);
		assertTrue(onLogout.indexOf("removeItems(player);") > onLogout.indexOf("rememberRelicKey(player);"));
		assertTrue(source.contains("case 216968: //Divine Hisen."));
		assertTrue(source.contains("relicKeySourceConsumed = true;"));
		assertTrue(onDestroy.contains("relicKeyRecoveryPlayers.clear();"));
		assertTrue(onDestroy.contains("relicKeySourceConsumed = false;"));
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}
}
