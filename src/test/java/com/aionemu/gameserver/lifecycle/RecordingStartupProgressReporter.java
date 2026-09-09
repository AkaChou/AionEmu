package com.aionemu.gameserver.lifecycle;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class RecordingStartupProgressReporter implements StartupProgressReporter {

	private final List<String> events;

	@Override
	public void start(String groupName) {
		events.add("progress:start:" + groupName);
	}

	@Override
	public void stepStarted(String stepName) {
		events.add("progress:started:" + stepName);
	}

	@Override
	public void stepFinished(String stepName) {
		events.add("progress:finished:" + stepName);
	}

	@Override
	public void finish(String groupName, long elapsedMillis) {
		events.add("progress:finish:" + groupName);
	}

	@Override
	public void failed() {
		events.add("progress:failed");
	}
}
