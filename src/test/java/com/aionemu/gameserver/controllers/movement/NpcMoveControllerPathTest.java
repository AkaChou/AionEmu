package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.objenesis.ObjenesisStd;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.geo.GeoService;

class NpcMoveControllerPathTest {

	@Test
	void homeReturnReplacesAnAlreadyStartedPointMove() throws ReflectiveOperationException {
		Npc owner = new ObjenesisStd().newInstance(Npc.class);
		Field objectId = AionObject.class.getDeclaredField("objectId");
		objectId.setAccessible(true);
		objectId.set(owner, 210667);
		owner.setAi2(new NpcAI2());
		SpawnTemplate spawn = new SpawnTemplate(new SpawnGroup2(210010000, 210667),
				1038.970f, 1127.112f, 118.437f, (byte) 0, 2, null, 0, 0);
		owner.setSpawn(spawn);
		NpcMoveController controller = new NpcMoveController(owner);
		Field destination = NpcMoveController.class.getDeclaredField("destination");
		destination.setAccessible(true);
		destination.set(controller, Enum.valueOf(destination.getType().asSubclass(Enum.class), "POINT"));
		Field cachedPath = NpcMoveController.class.getDeclaredField("cachedPath");
		cachedPath.setAccessible(true);
		cachedPath.set(controller, new float[][] {{1019.832f, 1140.955f, 118.042f}});
		Field cachedPathValid = NpcMoveController.class.getDeclaredField("cachedPathValid");
		cachedPathValid.setAccessible(true);
		cachedPathValid.set(controller, true);
		Field pointX = NpcMoveController.class.getDeclaredField("pointX");
		Field pointY = NpcMoveController.class.getDeclaredField("pointY");
		Field pointZ = NpcMoveController.class.getDeclaredField("pointZ");
		pointX.setAccessible(true);
		pointY.setAccessible(true);
		pointZ.setAccessible(true);
		controller.started.set(true);
		MoveTaskManager moveTaskManager = GameMovementLoopServices.moveTaskManager();
		moveTaskManager.removeCreature(owner);

		try {
			controller.moveToHome();

			assertEquals("HOME", destination.get(controller).toString());
			assertEquals(spawn.getX(), pointX.getFloat(controller));
			assertEquals(spawn.getY(), pointY.getFloat(controller));
			assertEquals(spawn.getZ(), pointZ.getFloat(controller));
			assertTrue(controller.started.get());
			assertFalse(cachedPathValid.getBoolean(controller));
			assertNull(cachedPath.get(controller));
		} finally {
			moveTaskManager.removeCreature(owner);
		}
	}

	@Test
	void chaseReplacesAnActivePointMoveAndRestoresMoveRegistration() throws ReflectiveOperationException {
		Npc owner = new ObjenesisStd().newInstance(Npc.class);
		Field objectId = AionObject.class.getDeclaredField("objectId");
		objectId.setAccessible(true);
		objectId.set(owner, 1);
		NpcMoveController controller = new NpcMoveController(owner);
		Field destination = NpcMoveController.class.getDeclaredField("destination");
		destination.setAccessible(true);
		destination.set(controller, Enum.valueOf(destination.getType().asSubclass(Enum.class), "POINT"));
		controller.started.set(true);
		MoveTaskManager moveTaskManager = GameMovementLoopServices.moveTaskManager();
		moveTaskManager.removeCreature(owner);

		controller.moveToTargetObject();

		assertTrue(controller.isFollowingTarget());
		Field movingCreatures = MoveTaskManager.class.getDeclaredField("movingCreatures");
		movingCreatures.setAccessible(true);
		assertTrue(((Map<?, ?>) movingCreatures.get(moveTaskManager)).containsKey(owner.getObjectId()));
		moveTaskManager.removeCreature(owner);
	}

	@Test
	void pathWithIntermediateWaypointRemainsValidWhileTargetMovesSlightly() {
		float[][] path = {{1, 1, 1}, {2, 2, 2}};

		assertTrue(NpcMoveController.hasIntermediateWaypoint(path));
		assertFalse(NpcMoveController.shouldInvalidatePath(path, false, 0.5f));
	}

	@Test
	void intermediatePathRepathsWhenTargetDriftsFar() {
		float[][] path = {{1, 1, 1}, {2, 2, 2}};

		assertTrue(NpcMoveController.shouldInvalidatePath(path, false,
				NpcMoveController.CHASE_REPATH_DISTANCE + 0.1f));
	}

	@Test
	void chaseRepathCooldownScalesWithTargetDistance() {
		assertEquals(500, NpcMoveController.chaseRepathInterval(30));
		assertEquals(750, NpcMoveController.chaseRepathInterval(31));
		assertEquals(1_000, NpcMoveController.chaseRepathInterval(61));
		assertTrue(NpcMoveController.shouldRepathChase(false, 1_000, 1_001, 61));
		assertFalse(NpcMoveController.shouldRepathChase(true, 1_000, 1_999, 61));
		assertTrue(NpcMoveController.shouldRepathChase(true, 1_000, 2_000, 61));
	}

	@Test
	void finalOrMissingPathRequiresRecalculation() {
		assertFalse(NpcMoveController.hasIntermediateWaypoint(null));
		assertFalse(NpcMoveController.hasIntermediateWaypoint(new float[0][]));
		assertFalse(NpcMoveController.hasIntermediateWaypoint(new float[][] {{1, 1, 1}}));
		assertTrue(NpcMoveController.shouldInvalidatePath(null, false, 0f));
		assertTrue(NpcMoveController.shouldInvalidatePath(new float[][] {{1, 1, 1}}, false, 0f));
	}

	@Test
	void emptyAsyncPathResultIsRetried() {
		assertFalse(NpcMoveController.isEmptyPathResult(null));
		assertTrue(NpcMoveController.isEmptyPathResult(new float[0][]));
		assertFalse(NpcMoveController.isEmptyPathResult(new float[][] {{1, 1, 1}}));
	}

	@Test
	void confirmedNoPathIsKeptUntilTheDestinationChanges() {
		assertTrue(NpcMoveController.shouldKeepPathResult(null, false));
		assertTrue(NpcMoveController.shouldKeepPathResult(new float[][] {{1, 1, 1}}, false));
		assertFalse(NpcMoveController.shouldKeepPathResult(new float[][] {{1, 1, 1}}, true));
	}

	@Test
	void failedPointMoveEndsOnlyAfterTheReactionDelayAndPendingRequest() {
		assertFalse(NpcMoveController.shouldFinishFailedPointMove(1_000, false, false, 11_000));
		assertTrue(NpcMoveController.shouldFinishFailedPointMove(1_000, false, false, 11_001));
		assertFalse(NpcMoveController.shouldFinishFailedPointMove(1_000, true, false, 12_000));
		assertFalse(NpcMoveController.shouldFinishFailedPointMove(1_000, false, true, 12_000));
	}

	@Test
	void pointDestinationUsesTheSelectedPathLayer() {
		float[][] path = {{1, 1, 10}, {2, 2, 25}};

		assertEquals(25, NpcMoveController.resolvedPointZ(100, path));
		assertEquals(100, NpcMoveController.resolvedPointZ(100, null));
	}

	@Test
	void groundChaseUsesTheSurfaceAtTheTargetsNewPosition() {
		assertEquals(27, NpcMoveController.resolvedTargetZ(false, 30, 27));
		assertEquals(27, NpcMoveController.resolvedTargetZ(false, 35, 27));
	}

	@Test
	void spatialChaseStillTracksTheTargetsHeight() {
		assertEquals(30, NpcMoveController.resolvedTargetZ(true, 30, 27));
	}

	@Test
	void reachableMovingTargetKeepsItsDirectPath() {
		float[][] path = {{1, 1, 1}};

		assertTrue(NpcMoveController.shouldRetargetPath(path, true));
		assertFalse(NpcMoveController.shouldRetargetPath(path, false));
	}

	@Test
	void movingTargetKeepsAnInFlightPathRequestUntilItDrifts() {
		assertFalse(NpcMoveController.shouldInvalidatePath(null, true, 0.5f));
		assertTrue(NpcMoveController.shouldInvalidatePath(null, true,
				NpcMoveController.CHASE_REPATH_DISTANCE + 0.1f));
		assertTrue(NpcMoveController.shouldInvalidatePath(null, false, 0f));
	}

	@Test
	void pathDestinationDriftUsesTheLastWaypoint() {
		float[][] path = {{0, 0, 0}, {10, 0, 0}};

		assertEquals(0f, NpcMoveController.pathDestinationDrift(path, 10, 0, 0), 0.001f);
		assertEquals(5f, NpcMoveController.pathDestinationDrift(path, 15, 0, 0), 0.001f);
		assertTrue(Float.isInfinite(NpcMoveController.pathDestinationDrift(null, 1, 1, 1)));
	}

	@Test
	void changingTargetRejectsCachedAndInFlightRoutesForTheOldObject() {
		assertTrue(NpcMoveController.targetsAnotherObject(true, 10, false, 0, 20));
		assertTrue(NpcMoveController.targetsAnotherObject(false, 0, true, 10, 20));
		assertFalse(NpcMoveController.targetsAnotherObject(true, 20, true, 20, 20));
	}

	@Test
	void changedDestinationRequiresMovePacketEvenWhenMaskStaysTheSame() {
		assertTrue(NpcMoveController.shouldBroadcastMovement((byte) -32, (byte) -32, true));
		assertFalse(NpcMoveController.shouldBroadcastMovement((byte) -32, (byte) -32, false));
	}

	@Test
	void movingChaseTargetDoesNotRestartTheMoveAnimation() {
		assertTrue(NpcMoveController.shouldRestartMovement(false, true, MovementMask.IMMEDIATE));
		assertFalse(NpcMoveController.shouldRestartMovement(false, true, MovementMask.NPC_STARTMOVE));
		assertFalse(NpcMoveController.shouldRestartMovement(false, true, MovementMask.NPC_RUN_SLOW));
		assertTrue(NpcMoveController.shouldRestartMovement(false, false, MovementMask.NPC_RUN_SLOW));
		assertFalse(NpcMoveController.shouldRestartMovement(true, false, MovementMask.NPC_RUN_SLOW));
	}

	@Test
	void movingChaseTargetPacketsAreThrottled() {
		assertFalse(NpcMoveController.shouldBroadcastDestination(true, false, true, 1_199, 1_000));
		assertTrue(NpcMoveController.shouldBroadcastDestination(true, false, true, 1_200, 1_000));
		assertTrue(NpcMoveController.shouldBroadcastDestination(false, false, true, 1_001, 1_000));
		assertFalse(NpcMoveController.shouldBroadcastDestination(true, false, false, 2_000, 1_000));
	}

	@Test
	void intermediateChaseWaypointsAreBroadcastImmediately() {
		assertTrue(NpcMoveController.shouldBroadcastDestination(true, true, true, 1_001, 1_000));
	}

	@Test
	void localAvoidanceProjectionKeepsTheCurrentSegmentHeightAndRejectsLayerJumps() {
		float[] projected = {1, 2, 3.2f};

		assertSame(projected, NpcMoveController.stableAvoidanceProjection(3, projected));
		assertEquals(3, projected[2]);
		assertEquals(null, NpcMoveController.stableAvoidanceProjection(3, new float[] {1, 2, 3.3f}));
	}

	@Test
	void pathMovePacketStartsAtThePreviousServerStep() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java"));
		String method = source.substring(source.indexOf("private void moveToLocation"),
				source.indexOf("void sampleStuckShadow"));
		String compact = method.replaceAll("\\s+", " ");

		assertTrue(compact.contains("new SM_MOVE(owner.getObjectId(), ownerX, ownerY, ownerZ, "
				+ "targetDestX, targetDestY, targetDestZ, heading, movementMask)"));
		assertFalse(method.contains("new SM_MOVE((Creature)this.owner)"));
	}

	@Test
	void pathMovementKeepsItsPathHeight() {
		assertFalse(NpcMoveController.shouldAdjustGeoHeight(true, true, true, new float[][] {{1, 2, 3}}));
		assertTrue(NpcMoveController.shouldAdjustGeoHeight(true, true, true, null));
		assertFalse(NpcMoveController.shouldAdjustGeoHeight(false, true, true, null));
		assertTrue(NpcMoveController.shouldAdjustGeoHeight(false, false, false, null));
		assertFalse(NpcMoveController.shouldAdjustGeoHeight(false, false, true, null));
	}

	@Test
	void directGroundMovementChecksGeoPassabilityBeforeUpdatingPosition() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java"));
		String method = source.substring(source.indexOf("private void moveToLocation"),
				source.indexOf("void sampleStuckShadow"));
		int passabilityCheck = method.indexOf("canMoveStraight(owner, newX, newY, newZ)");

		assertTrue(passabilityCheck >= 0);
		assertTrue(method.indexOf("stopForPath();", passabilityCheck) > passabilityCheck);
		assertTrue(passabilityCheck < method.indexOf("world().updatePosition"));
	}

	@Test
	void waitingForPathOnlySendsOneStopPerWait() {
		assertTrue(NpcMoveController.shouldSendPathStop(false));
		assertFalse(NpcMoveController.shouldSendPathStop(true));
	}

	@Test
	void consumesWaypointOnlyAfterTheFinalPositionReachesIt() {
		float[][] path = {{1, 1, 1}, {2, 2, 2}};

		assertSame(path, NpcMoveController.remainingPath(path, false));
		assertArrayEquals(new float[][] {{2, 2, 2}}, NpcMoveController.remainingPath(path, true));
		assertTrue(NpcMoveController.reachedWaypoint(1, 1, 1, 1, 1, 1));
		assertFalse(NpcMoveController.reachedWaypoint(1, 1, 1, 1.1f, 1, 1));
	}

	@Test
	void oldMovementSnapshotCannotConsumeANewerPath() {
		float[][] oldPath = {{1, 1, 1}};
		float[][] newPath = {{2, 2, 2}};

		assertSame(newPath, NpcMoveController.consumePath(newPath, oldPath));
	}

	@Test
	void oldRequestOrObstacleVersionCannotInstallAPath() {
		assertTrue(NpcMoveController.shouldAcceptPathResult(7, 7, 3, 3));
		assertFalse(NpcMoveController.shouldAcceptPathResult(6, 7, 3, 3));
		assertFalse(NpcMoveController.shouldAcceptPathResult(7, 7, 2, 3));
	}

	@Test
	void stuckProgressUsesRouteDirectionAndRemainingDistance() {
		assertTrue(NpcMoveController.hasMeaningfulProgress(0, 0, 0, 0.2f, 0, 0, 10, 0, 0, 10, 2, 500));
		assertFalse(NpcMoveController.hasMeaningfulProgress(0, 0, 0, 0, 0.2f, 0, 10, 0, 0, 10, 2, 500));
		assertTrue(NpcMoveController.hasMeaningfulProgress(0, 0, 0, 0, 0, 0.2f, 0, 0, 1, 1, 2, 500));
	}

	@Test
	void stuckShadowOnlyCountsAndCanSelfRecover() {
		NpcMoveController controller = new NpcMoveController(null);
		float[][] path = {{10, 0, 0}, {20, 0, 0}};
		NpcMoveController.RecoveryMetrics before = NpcMoveController.recoveryMetrics();

		controller.sampleStuckShadow(0, 0, 0, 10, 0, 0, 1, 1_000, path, 10, 0);
		controller.sampleStuckShadow(0, 0, 0, 10, 0, 0, 1, 1_500, path, 10, 0);
		controller.sampleStuckShadow(0, 0, 0, 10, 0, 0, 1, 2_000, path, 10, 0);
		controller.sampleStuckShadow(0, 0, 0, 10, 0, 0, 1, 2_500, path, 10, 0);
		controller.sampleStuckShadow(1, 0, 0, 10, 0, 0, 1, 3_000, path, 9, 0);

		NpcMoveController.RecoveryMetrics after = NpcMoveController.recoveryMetrics();
		assertEquals(before.stuckSuspected() + 1, after.stuckSuspected());
		assertEquals(before.stuckConfirmed() + 1, after.stuckConfirmed());
		assertEquals(before.stuckSelfRecovered() + 1, after.stuckSelfRecovered());
	}

	@Test
	void stuckRecoveryIsBoundedAndKeepsTheOldPathOnFailure() {
		assertTrue(NpcMoveController.shouldRequestStuckRecovery(true, false, 0, 0, 1_000));
		assertFalse(NpcMoveController.shouldRequestStuckRecovery(true, true, 0, 0, 1_000));
		assertFalse(NpcMoveController.shouldRequestStuckRecovery(true, false, 1, 1_000, 1_749));
		assertTrue(NpcMoveController.shouldRequestStuckRecovery(true, false, 1, 1_000, 1_750));
		assertFalse(NpcMoveController.shouldRequestStuckRecovery(true, false, 2, 0, 2_000));
		assertEquals(0.35f, NpcMoveController.blockedSegmentRadius(0));
		assertEquals(0.75f, NpcMoveController.blockedSegmentRadius(1));

		float[][] oldPath = {{1, 1, 1}};
		assertSame(oldPath, NpcMoveController.installPathResult(oldPath, null, true));
		assertSame(oldPath, NpcMoveController.installPathResult(oldPath, new float[0][], true));
	}

	@Test
	void waypointSkipIsBoundedByItsCooldownAndKeepsTheSelectedCandidate() {
		float[][] path = {{1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {4, 0, 0}};

		assertTrue(NpcMoveController.shouldTryWaypointSkip(path, 3, 0, 1_000));
		assertFalse(NpcMoveController.shouldTryWaypointSkip(path, 3, 1_000, 1_249));
		assertTrue(NpcMoveController.shouldTryWaypointSkip(path, 3, 1_000, 1_250));
		assertFalse(NpcMoveController.shouldTryWaypointSkip(path, 0, 0, 1_000));
		assertArrayEquals(new float[][] {{3, 0, 0}, {4, 0, 0}}, NpcMoveController.remainingPath(path, 2));
	}

	@Test
	void groundMeleeTargetsUseStableDistributedAttackSlots() {
		assertTrue(NpcMoveController.shouldUseAttackSlot(false, 2));
		assertFalse(NpcMoveController.shouldUseAttackSlot(true, 2));
		assertFalse(NpcMoveController.shouldUseAttackSlot(false, 8));
		assertEquals(NpcMoveController.attackSlotOffsetDegrees(10, 20),
				NpcMoveController.attackSlotOffsetDegrees(10, 20));
		assertFalse(NpcMoveController.attackSlotOffsetDegrees(10, 20)
				== NpcMoveController.attackSlotOffsetDegrees(11, 20));

		float[] slot = NpcMoveController.attackSlotCandidate(10, 0, 0, 0, 5, 1.5f, 0);
		assertEquals(1.5f, Math.hypot(slot[0], slot[1]), 0.001f);
		assertEquals(5, slot[2]);
	}

	@Test
	void retriesAConfirmedFailureOnlyAfterTheTargetMovesFarEnough() {
		assertFalse(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 2.5f, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 2.51f, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(Float.NaN, 0, 0, 1, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 1, 1, 1, 3, 4));
	}

	@Test
	void reactsOnceOnlyAfterTenSecondsOfContinuousFailure() {
		assertFalse(NpcMoveController.shouldReactToPathFailure(1_000, false, 11_000));
		assertTrue(NpcMoveController.shouldReactToPathFailure(1_000, false, 11_001));
		assertFalse(NpcMoveController.shouldReactToPathFailure(1_000, true, 12_000));
		long definitive = NpcMoveController.pathFailureStartedAt(0, 6_000, true);
		assertFalse(NpcMoveController.shouldReactToPathFailure(definitive, false, 16_000));
		assertTrue(NpcMoveController.shouldReactToPathFailure(definitive, false, 16_001));
	}

	@Test
	void failedPathTriesLocalAvoidanceOncePerSecond() {
		assertFalse(NpcMoveController.shouldTryPathAvoidance(1_000, 0, 0, 1_999));
		assertTrue(NpcMoveController.shouldTryPathAvoidance(1_000, 0, 0, 2_000));
		assertFalse(NpcMoveController.shouldTryPathAvoidance(1_000, 1_500, 1, 2_000));
		assertFalse(NpcMoveController.shouldTryPathAvoidance(1_000, 0, 4, 5_000));
	}

	@Test
	void localAvoidanceStepKeepsTheTargetSlope() {
		assertArrayEquals(new float[] {0.9f, 1.2f, 3},
				NpcMoveController.localAvoidanceTarget(0, 0, 0, 3, 4, 10, 1.5f), 0.001f);
		assertEquals(null, NpcMoveController.localAvoidanceTarget(1, 1, 1, 1, 1, 3, 1.5f));
	}

	@Test
	void localAvoidanceRequiresStraightMovement() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java"));
		String method = source.substring(source.indexOf("private boolean tryPathAvoidance"),
				source.indexOf("void blockedPathStep"));

		assertEquals(1, method.split("canMoveStraight", -1).length - 1);
		assertFalse(method.contains("canReachWaypoint"));
		assertFalse(method.contains("NpcCrowdManager"));
	}

	@Test
	void blockedChaseStepInvalidatesThePathAndStartsRecovery() throws ReflectiveOperationException {
		NpcMoveController controller = new NpcMoveController(null);
		Field cachedPathValid = NpcMoveController.class.getDeclaredField("cachedPathValid");
		cachedPathValid.setAccessible(true);
		cachedPathValid.set(controller, true);
		Field firstPathFailureAt = NpcMoveController.class.getDeclaredField("firstPathFailureAt");
		firstPathFailureAt.setAccessible(true);

		controller.blockedPathStep();

		assertFalse(cachedPathValid.getBoolean(controller));
		assertTrue(firstPathFailureAt.getLong(controller) > 0);
	}

	@Test
	void homeReturnUsesElapsedStateTime() {
		assertFalse(NpcMoveController.hasHomeReturnTimedOut(1_000, 30_000, 30_999));
		assertTrue(NpcMoveController.hasHomeReturnTimedOut(1_000, 30_000, 31_000));
		assertFalse(NpcMoveController.hasHomeReturnTimedOut(0, 30_000, 60_000));
		assertFalse(NpcMoveController.shouldTeleportFailedHomeReturn(false, 1_000, 61_000));
		assertFalse(NpcMoveController.shouldTeleportFailedHomeReturn(true, 1_000, 30_999));
		assertTrue(NpcMoveController.shouldTeleportFailedHomeReturn(true, 1_000, 31_000));
	}

	@Test
	void partialPathEndpointDoesNotCompleteHomeReturn() {
		assertFalse(NpcMoveController.shouldCompleteHomeReturn(true, false));
		assertFalse(NpcMoveController.shouldCompleteHomeReturn(false, true));
		assertTrue(NpcMoveController.shouldCompleteHomeReturn(true, true));
	}

	@Test
	void homeReturnKeepsItsInFlightPathRequest() {
		assertFalse(NpcMoveController.shouldRequestHomePath(false, true, true, 1_000, 500));
		assertTrue(NpcMoveController.shouldRequestHomePath(false, true, false, 1_000, 500));
		assertFalse(NpcMoveController.shouldRequestHomePath(true, false, false, 1_000, 500));
	}

	@Test
	void enhancedHomeReturnWaitsForTheTimeoutAfterPathFailure() {
		assertTrue(NpcMoveController.shouldFinishFailedHomeReturn(false, 1_000, false, false));
		assertFalse(NpcMoveController.shouldFinishFailedHomeReturn(true, 1_000, false, false));
		assertFalse(NpcMoveController.shouldFinishFailedHomeReturn(false, 0, false, false));
		assertFalse(NpcMoveController.shouldFinishFailedHomeReturn(false, 1_000, true, false));
		assertFalse(NpcMoveController.shouldFinishFailedHomeReturn(false, 1_000, false, true));
	}

	@Test
	void geodataHeightCorrectionStillCompletesSpawnReturn() {
		assertTrue(NpcMoveController.isReturnDestinationReached(false, 0.01, 1));
		assertFalse(NpcMoveController.isReturnDestinationReached(false, 1, 0));
		assertFalse(NpcMoveController.isReturnDestinationReached(true, 0.01, 1));
		assertTrue(NpcMoveController.isReturnDestinationReached(true, 1, 0.01));
	}

	@Test
	void waypointReturnKeepsPatrolSpeedAndSpawnReturnAppliesPercentage() {
		assertEquals(2f, NpcMoveController.returnSpeed(2f, 150, true));
		assertEquals(3f, NpcMoveController.returnSpeed(2f, 150, false));
		assertEquals(12f, NpcMoveController.returnSpeed(6f, 200, false));
	}

	@Test
	void failureReturnConsumesItsFullHealOnlyOnce() {
		NpcMoveController controller = new NpcMoveController(null);

		assertFalse(controller.consumeFullHealOnHomeReturn());
		controller.requestFullHealOnHomeReturn();
		assertTrue(controller.consumeFullHealOnHomeReturn());
		assertFalse(controller.consumeFullHealOnHomeReturn());
	}

	@Test
	void cancelledHomeReturnDoesNotLeakItsFullHealIntoTheNextReturn() {
		NpcMoveController controller = new NpcMoveController(null);

		controller.requestFullHealOnHomeReturn();
		controller.clearHomeReturn();

		assertFalse(controller.consumeFullHealOnHomeReturn());
	}

	@Test
	void pullTargetFallsBackAfterFiveAttemptsAndResetsForAnotherTarget() {
		NpcMoveController controller = new NpcMoveController(null);

		for (int i = 0; i < 5; i++) {
			assertTrue(controller.tryPathPull(10));
		}
		assertFalse(controller.tryPathPull(10));
		assertTrue(controller.tryPathPull(20));
		controller.clearPathPullAttempts();
		assertTrue(controller.tryPathPull(20));
	}

	@Test
	void numericChaseTimeoutReturnsToCurrentWaypointWhenAvailable() {
		NpcMoveController controller = new NpcMoveController(null);
		controller.currentRoute = List.of(new RouteStep(1, 2, 3, 0), new RouteStep(4, 5, 6, 0));
		controller.currentPoint = 1;

		controller.requestReturnToCurrentWaypoint();
		Point3D target = controller.getHomeReturnDestination();

		assertTrue(controller.isReturningToWaypoint());
		assertEquals(4, target.getX());
		assertEquals(5, target.getY());
		assertEquals(6, target.getZ());
		controller.clearHomeReturn();
		assertFalse(controller.isReturningToWaypoint());
	}

	@Test
	void waypointReturnUsesTheSameGroundHeightCorrectionAsPatrol() throws ReflectiveOperationException {
		boolean oldGeoEnable = GeoDataConfig.GEO_ENABLE;
		boolean oldNpcMove = GeoDataConfig.GEO_NPC_MOVE;
		Field resolvedGeoService = GameWorldServices.class.getDeclaredField("resolvedGeoService");
		resolvedGeoService.setAccessible(true);
		Object oldGeoService = resolvedGeoService.get(null);
		Npc owner = new ObjenesisStd().newInstance(Npc.class);
		owner.setPosition(new WorldPosition(210030000));
		NpcMoveController controller = new NpcMoveController(owner);
		controller.currentRoute = List.of(new RouteStep(1540, 1442, 125, 0));

		try {
			GeoDataConfig.GEO_ENABLE = true;
			GeoDataConfig.GEO_NPC_MOVE = true;
			resolvedGeoService.set(null, new GeoService() {
				@Override
				public float getZ(int worldId, float x, float y, float z, float defaultUp, int instanceId) {
					assertEquals(210030000, worldId);
					assertEquals(124, z);
					return 102.5f;
				}
			});

			controller.requestReturnToCurrentWaypoint();

			assertEquals(102.5f, controller.getHomeReturnDestination().getZ());
		} finally {
			GeoDataConfig.GEO_ENABLE = oldGeoEnable;
			GeoDataConfig.GEO_NPC_MOVE = oldNpcMove;
			resolvedGeoService.set(null, oldGeoService);
		}
	}

	@Test
	void numericChaseWithoutARouteFallsBackToSpawn() {
		NpcMoveController controller = new NpcMoveController(null);

		controller.requestReturnToCurrentWaypoint();

		assertFalse(controller.isReturningToWaypoint());
	}


	@Test
	void reusesReachChecksForTheSameEndpoints() {
		assertTrue(NpcMoveController.canReuseReachCheck(1_100, 1_000, 1, 2, 3, 1, 2, 3, 4, 5, 6, 4, 5, 6));
		assertFalse(NpcMoveController.canReuseReachCheck(1_101, 1_000, 1, 2, 3, 1, 2, 3, 4, 5, 6, 4, 5, 6));
		assertFalse(NpcMoveController.canReuseReachCheck(1_050, 1_000, 1.2f, 2, 3, 1, 2, 3, 4, 5, 6, 4, 5, 6));
		assertFalse(NpcMoveController.canReuseReachCheck(1_050, 1_000, 1, 2, 3, 1, 2, 3, 4.2f, 5, 6, 4, 5, 6));
	}

}
