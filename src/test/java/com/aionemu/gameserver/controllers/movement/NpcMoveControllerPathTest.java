package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.objenesis.ObjenesisStd;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;

class NpcMoveControllerPathTest {

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
	void directCrowdStepDoesNotRestartTheMoveAnimation() {
		assertFalse(NpcMoveController.avoidanceChangedStep(1, 2, 3, new float[] {1, 2, 3}));
		assertTrue(NpcMoveController.avoidanceChangedStep(1, 2, 3, new float[] {1, 2.1f, 3}));
	}

	@Test
	void pathMovementKeepsItsPathHeight() {
		assertFalse(NpcMoveController.shouldAdjustGeoHeight(new float[][] {{1, 2, 3}}));
		assertTrue(NpcMoveController.shouldAdjustGeoHeight(null));
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
	void retriesAConfirmedFailureOnlyAfterTheTargetMovesFarEnough() {
		assertFalse(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 2.5f, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 2.51f, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(Float.NaN, 0, 0, 1, 1, 1, 3, 3));
		assertTrue(NpcMoveController.shouldRetryFailedPath(1, 1, 1, 1, 1, 1, 3, 4));
	}

	@Test
	void reactsOnceOnlyAfterFiveSecondsOfContinuousFailure() {
		assertFalse(NpcMoveController.shouldReactToPathFailure(1_000, false, 6_000));
		assertTrue(NpcMoveController.shouldReactToPathFailure(1_000, false, 6_001));
		assertFalse(NpcMoveController.shouldReactToPathFailure(1_000, true, 7_000));
		long definitive = NpcMoveController.pathFailureStartedAt(0, 6_000, true);
		assertTrue(NpcMoveController.shouldReactToPathFailure(definitive, false, 6_000));
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
	void geodataHeightCorrectionStillCompletesSpawnReturn() {
		assertTrue(NpcMoveController.isReturnDestinationReached(false, 0.01, 1));
		assertFalse(NpcMoveController.isReturnDestinationReached(false, 1, 0));
		assertFalse(NpcMoveController.isReturnDestinationReached(true, 0.01, 1));
		assertTrue(NpcMoveController.isReturnDestinationReached(true, 1, 0.01));
	}

	@Test
	void returnSpeedKeepsCurrentStatModifiersAndAppliesPercentage() {
		assertEquals(3f, NpcMoveController.returnSpeed(2f, 150));
		assertEquals(12f, NpcMoveController.returnSpeed(6f, 200));
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
