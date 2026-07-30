package com.aionemu.gameserver.questEngine.graph;

import java.util.LinkedHashSet;
import java.util.Set;

/** 构造由 5.8 客户端静态数据证明存在的任务影片引用。 / Builds quest-movie references proven by the 5.8 client static data. */
public final class QuestGraphMovieReferenceCatalog {

	/** 来自 58Server-new/Map/XML/CutScenes.xml，排除协议不可表示的 CUTSCENE_NULL。 / From CutScenes.xml, excluding CUTSCENE_NULL. */
	private static final int[][] CLIENT_5_8_ID_RANGES = {
		{ 1, 300 },
		{ 351, 1008 },
		{ 10001, 10002 },
		{ 11000, 11012 },
		{ 12000, 12012 },
		{ 13000, 13002 }
	};

	private QuestGraphMovieReferenceCatalog() {
	}

	/** 返回客户端 CutScenes 表的不可变 ID 闭包。 / Returns the immutable id closure of the client CutScenes table. */
	public static Set<Integer> build() {
		Set<Integer> movieIds = new LinkedHashSet<>();
		for (int[] range : CLIENT_5_8_ID_RANGES) {
			for (int movieId = range[0]; movieId <= range[1]; movieId++) {
				movieIds.add(movieId);
			}
		}
		return Set.copyOf(movieIds);
	}
}
