package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameEventBootstrapGatewayTest {

	@Test
	void bootstrapReportsProgressForEventStartupSteps() {
		List<String> events = new ArrayList<>();
		GameEventBootstrapGateway gateway = new RecordingGameEventBootstrapGateway(events, new RecordingStartupProgressReporter(events));

		gateway.bootstrap();

		assertEquals(List.of(
			"progress:start:game event systems",
			"progress:started:Luna Shop System",
			"load:Luna Shop System",
			"progress:finished:Luna Shop System",
			"progress:started:Minion System",
			"load:Minion System",
			"progress:finished:Minion System",
			"progress:started:Shugo Sweep System",
			"load:Shugo Sweep System",
			"progress:finished:Shugo Sweep System",
			"progress:started:Atreian Passport System",
			"load:Atreian Passport System",
			"progress:finished:Atreian Passport System",
			"progress:started:Event Window System",
			"load:Event Window System",
			"progress:finished:Event Window System",
			"progress:finish:game event systems"
		), events);
	}

	private static final class RecordingGameEventBootstrapGateway extends GameEventBootstrapGateway {

		private final List<String> events;

		private RecordingGameEventBootstrapGateway(List<String> events, StartupProgressReporter progressReporter) {
			super(progressReporter);
			this.events = events;
		}

		@Override
		protected void initializeLunaShopSystem() {
			events.add("load:Luna Shop System");
		}

		@Override
		protected void initializeMinionSystem() {
			events.add("load:Minion System");
		}

		@Override
		protected void initializeShugoSweepSystem() {
			events.add("load:Shugo Sweep System");
		}

		@Override
		protected void initializeAtreianPassportSystem() {
			events.add("load:Atreian Passport System");
		}

		@Override
		protected void initializeEventWindowSystem() {
			events.add("load:Event Window System");
		}
	}
}
