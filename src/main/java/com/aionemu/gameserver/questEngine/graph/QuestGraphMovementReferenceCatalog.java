package com.aionemu.gameserver.questEngine.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.dataholders.FlyRingData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamRoute;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.FlyingRingReference;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.WindstreamRouteReference;

/**
 * 从正式 windstream 与 fly-ring 静态数据构造 movement 引用闭包。
 * Builds movement reference closure from formal windstream and fly-ring static data.
 */
public final class QuestGraphMovementReferenceCatalog {

	/** 禁止实例化纯静态目录构造器。 / Prevents instantiation of this static catalog builder. */
	private QuestGraphMovementReferenceCatalog() {
	}

	/**
	 * 构造复合 movement 引用并拒绝重复或无效静态键。
	 * Builds composite movement references and rejects duplicate or invalid static keys.
	 */
	public static MovementReferences build(WindstreamData windstreams, FlyRingData flyingRings) {
		Objects.requireNonNull(windstreams, "windstreams");
		Objects.requireNonNull(flyingRings, "flyingRings");
		Set<WindstreamRouteReference> routes = new LinkedHashSet<>();
		for (WindstreamRoute route : windstreams.getRoutes()) {
			WindstreamRouteReference reference = new WindstreamRouteReference(route.getMapId(), route.getId());
			if (!routes.add(reference)) {
				throw new IllegalArgumentException("Duplicate windstream route reference " + reference);
			}
		}
		Set<FlyingRingReference> rings = new LinkedHashSet<>();
		for (FlyRingTemplate ring : flyingRings.getFlyRingTemplates()) {
			FlyingRingReference reference = new FlyingRingReference(ring.getMap(), ring.getName());
			if (!rings.add(reference)) {
				throw new IllegalArgumentException("Duplicate flying-ring reference " + reference);
			}
		}
		return new MovementReferences(routes, rings);
	}

	/** 保存从正式静态数据生成的两类复合 movement 引用。 / Holds both composite movement reference sets built from formal static data. */
	public record MovementReferences(Set<WindstreamRouteReference> windstreamRoutes, Set<FlyingRingReference> flyingRings) {
		/** 复制集合以冻结一次 compiler generation 的引用闭包。 / Copies sets to freeze reference closure for one compiler generation. */
		public MovementReferences {
			windstreamRoutes = Set.copyOf(windstreamRoutes);
			flyingRings = Set.copyOf(flyingRings);
		}
	}
}
