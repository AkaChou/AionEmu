package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestLegacyObservationRecorderTest {
	@Test
	void freezesTypedOwnerObservationAndRestoresNestedContext() {
		QuestLegacyObservationRecorder recorder = new QuestLegacyObservationRecorder();
		recorder.beginOwner(1001);
		recorder.conditionMatched(1001, true);
		recorder.requiredAction(1001, new QuestAction.RemoveItem(182400001, 2));
		recorder.afterCommitAction(1001, new AfterCommitAction.CloseDialog());
		recorder.state(1001, QuestStatus.REWARD, 1);
		recorder.result(1001, QuestRouteResult.HANDLED);
		recorder.completeOwner(1001);

		assertFalse(QuestLegacyObservationContext.current().isPresent());
		try (QuestLegacyObservationContext.Scope ignored = QuestLegacyObservationContext.open(recorder)) {
			assertEquals(Optional.of(recorder), QuestLegacyObservationContext.current());
			QuestLegacyObservationRecorder nested = new QuestLegacyObservationRecorder();
			try (QuestLegacyObservationContext.Scope nestedScope = QuestLegacyObservationContext.open(nested)) {
				assertEquals(Optional.of(nested), QuestLegacyObservationContext.current());
			}
			assertEquals(Optional.of(recorder), QuestLegacyObservationContext.current());
		}
		assertFalse(QuestLegacyObservationContext.current().isPresent());

		QuestShadowObservation observation = recorder.snapshot();
		QuestShadowObservation.Owner owner = observation.owners().get(1001);
		assertEquals(QuestStatus.REWARD, owner.nextStatus());
		assertEquals(1, owner.nextPackedVariables());
		assertEquals(1, owner.requiredActions().size());
		assertEquals(1, owner.afterCommit().size());
		assertEquals(QuestRouteResult.HANDLED, owner.result());
	}

	@Test
	void incompleteOrUnknownOwnerCannotBecomeCleanObservation() {
		QuestLegacyObservationRecorder recorder = new QuestLegacyObservationRecorder();
		recorder.beginOwner(1002);
		recorder.conditionMatched(1002, true);
		recorder.result(1002, QuestRouteResult.HANDLED);
		assertThrows(IllegalStateException.class, () -> recorder.snapshot());
	}
}
