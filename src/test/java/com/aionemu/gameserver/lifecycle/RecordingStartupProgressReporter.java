package com.aionemu.gameserver.lifecycle;

import java.util.List;

final class RecordingStartupProgressReporter implements StartupProgressReporter {

	private final List<String> events;

	RecordingStartupProgressReporter(List<String> events) {
		this.events = events;
	}

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
