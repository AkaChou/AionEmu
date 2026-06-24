package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameWorldBootstrapGatewayTest {

	@Test
	void bootstrapReportsProgressForWorldStartupSteps() {
		List<String> events = new ArrayList<>();
		GameWorldBootstrapGateway gateway = new RecordingGameWorldBootstrapGateway(events, new RecordingStartupProgressReporter(events));

		gateway.bootstrap();

		assertEquals(List.of(
			"progress:start:game world",
			"progress:started:IDFactory",
			"load:IDFactory",
			"progress:finished:IDFactory",
			"progress:started:Zone",
			"load:Zone",
			"progress:finished:Zone",
			"progress:started:Hotspot Teleport",
			"load:Hotspot Teleport",
			"progress:finished:Hotspot Teleport",
			"progress:started:Road",
			"load:Road",
			"progress:finished:Road",
			"progress:started:World",
			"load:World",
			"progress:finished:World",
			"progress:finish:game world"
		), events);
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
