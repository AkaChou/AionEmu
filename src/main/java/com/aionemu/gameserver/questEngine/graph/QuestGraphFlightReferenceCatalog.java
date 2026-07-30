package com.aionemu.gameserver.questEngine.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.dataholders.FlyPathData;
import com.aionemu.gameserver.model.templates.flypath.FlyPathEntry;

/** 从同一代已加载 FlyPathData 构造完整路径引用闭包。 / Builds the complete path-reference closure from loaded FlyPathData of one generation. */
public final class QuestGraphFlightReferenceCatalog {

	private QuestGraphFlightReferenceCatalog() {
	}

	/** 返回显式静态数据输入中的不可变路径 ID 集。 / Returns immutable path ids from the explicit static-data input. */
	public static Set<Integer> build(FlyPathData flyPaths) {
		Objects.requireNonNull(flyPaths, "flyPaths");
		Set<Integer> pathIds = new LinkedHashSet<>();
		for (FlyPathEntry path : flyPaths.getPathTemplates()) {
			int pathId = path.getId();
			if (pathId <= 0 || !pathIds.add(pathId)) {
				throw new IllegalStateException("Formal fly-path id is invalid or duplicated: " + pathId);
			}
		}
		if (pathIds.isEmpty()) {
			throw new IllegalStateException("Formal fly-path reference catalog is empty");
		}
		return Set.copyOf(pathIds);
	}
}
