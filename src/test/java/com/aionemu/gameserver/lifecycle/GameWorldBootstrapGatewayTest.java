package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameWorldBootstrapGatewayTest {

	@Test
	void bootstrapReportsProgressForWorldStartupSteps() {
		List<String> events = Collections.synchronizedList(new ArrayList<>());
		GameWorldBootstrapGateway gateway = new RecordingGameWorldBootstrapGateway(events, new RecordingStartupProgressReporter(events));

		gateway.bootstrap();

		assertEquals("progress:start:game world", events.getFirst());
		assertEquals("progress:finish:game world", events.getLast());
		assertEquals(17, events.size());
		assertStepReported(events, "IDFactory");
		assertStepReported(events, "Zone");
		assertStepReported(events, "Hotspot Teleport");
		assertStepReported(events, "Road");
		assertStepReported(events, "World");
	}

	private void assertStepReported(List<String> events, String stepName) {
		int started = events.indexOf("progress:started:" + stepName);
		int loaded = events.indexOf("load:" + stepName);
		int finished = events.indexOf("progress:finished:" + stepName);

		assertTrue(started > 0, stepName);
		assertTrue(loaded > started, stepName);
		assertTrue(finished > loaded, stepName);
	}

	private static final class RecordingGameWorldBootstrapGateway extends GameWorldBootstrapGateway {

		private final List<String> events;

		private RecordingGameWorldBootstrapGateway(List<String> events, StartupProgressReporter progressReporter) {
			super(progressReporter);
			this.events = events;
		}

		@Override
		protected void initializeIDFactory() {
			events.add("load:IDFactory");
		}

		@Override
		protected void loadZoneService() {
			events.add("load:Zone");
		}

		@Override
		protected void initializeHotspotTeleportService() {
			events.add("load:Hotspot Teleport");
		}

		@Override
		protected void initializeRoadService() {
			events.add("load:Road");
		}

		@Override
		protected void initializeWorld() {
			events.add("load:World");
		}
	}
}
