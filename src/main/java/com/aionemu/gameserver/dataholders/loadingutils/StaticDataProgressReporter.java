package com.aionemu.gameserver.dataholders.loadingutils;

interface StaticDataProgressReporter {

	void start(int totalSections);

	void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries);

	void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries);

	void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries);

	void finish(int totalSections, long elapsedMillis);

	void failed();

	static StaticDataProgressReporter noop() {
		return new StaticDataProgressReporter() {
			@Override
			public void start(int totalSections) {
			}

			@Override
			public void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
			}

			@Override
			public void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries) {
			}

			@Override
			public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
			}

			@Override
			public void finish(int totalSections, long elapsedMillis) {
			}

			@Override
			public void failed() {
			}
		};
	}
}
