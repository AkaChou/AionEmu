package com.aionemu.gameserver.questEngine.definition;

/** Client-side movie resource family used by {@code SM_PLAY_MOVIE}. */
public enum QuestMovieType {
	/** CutScenes.xml entry (packet type 0). */
	CUTSCENE(0),
	/** CutSceneMovies.xml entry (packet type 1). */
	CUTSCENE_MOVIE(1);

	private final int wireValue;

	QuestMovieType(int wireValue) {
		this.wireValue = wireValue;
	}

	public int wireValue() {
		return wireValue;
	}
}
