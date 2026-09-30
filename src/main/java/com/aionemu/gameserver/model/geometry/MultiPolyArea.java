package com.aionemu.gameserver.model.geometry;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.model.templates.zone.Points;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 多胞多边形区域：一个区名由若干互不相连的多边形胞组成（真端感官区常见形）。
 * 进入任意一胞即视为进入该区；每胞保留自己的 top/bottom，不做全局 Z 合并（否则错胞的高度会被放行）。
 * <p>
 * Multi-cell polygon area: one zone name made of several disjoint polygon cells (the usual retail
 * sensory-area shape). Being inside any cell counts as being inside the zone; every cell keeps its own
 * top/bottom instead of being flattened into one Z range, which would wrongly accept other cells' heights.
 */
public class MultiPolyArea extends AbstractArea {

	/** 各胞（每胞一个独立 Z 区间的多边形）。 / The cells, each a polygon with its own Z range. */
	private final List<PolyArea> cells;

	/**
	 * 由若干环（真端 {@code <sensory_area>} 胞）构造多胞区域。
	 * Builds the multi-cell area from the given rings (retail {@code <sensory_area>} cells).
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param rings 环列表 / the ring list
	 */
	public MultiPolyArea(ZoneName zoneName, int worldId, List<Points> rings) {
		super(zoneName, worldId, minBottom(rings), maxTop(rings));
		if (rings.size() < 2) {
			throw new IllegalArgumentException("multi-cell area needs at least 2 cells, got " + rings.size());
		}
		List<PolyArea> built = new ArrayList<>(rings.size());
		for (Points ring : rings) {
			built.add(new PolyArea(zoneName, worldId, ring.getPoint(), ring.getBottom(), ring.getTop()));
		}
		this.cells = List.copyOf(built);
	}

	/** 返回全部胞（只读）。 / Returns all cells (read-only). */
	public List<PolyArea> getCells() {
		return cells;
	}

	@Override
	public boolean isInside2D(float x, float y) {
		for (PolyArea cell : cells) {
			if (cell.isInside2D(x, y)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 逐胞判定：XY 与 Z 必须落在**同一个胞**内（不能把胞 A 的 XY 与胞 B 的 Z 拼起来）。
	 * Per-cell containment: XY and Z must fall inside the same cell (cell A's XY must not combine with
	 * cell B's Z).
	 */
	@Override
	public boolean isInside3D(float x, float y, float z) {
		for (PolyArea cell : cells) {
			if (cell.isInside3D(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInsideZ(float z) {
		for (PolyArea cell : cells) {
			if (cell.isInsideZ(z)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getDistance2D(float x, float y) {
		double closest = Double.MAX_VALUE;
		for (PolyArea cell : cells) {
			closest = Math.min(closest, cell.getDistance2D(x, y));
		}
		return closest;
	}

	@Override
	public double getDistance3D(float x, float y, float z) {
		double closest = Double.MAX_VALUE;
		for (PolyArea cell : cells) {
			closest = Math.min(closest, cell.getDistance3D(x, y, z));
		}
		return closest;
	}

	@Override
	public Point2D getClosestPoint(float x, float y) {
		PolyArea closest = cells.getFirst();
		double distance = closest.getDistance2D(x, y);
		for (PolyArea cell : cells) {
			double candidate = cell.getDistance2D(x, y);
			if (candidate < distance) {
				closest = cell;
				distance = candidate;
			}
		}
		return closest.getClosestPoint(x, y);
	}

	@Override
	public Point3D getClosestPoint(float x, float y, float z) {
		PolyArea closest = cells.getFirst();
		double distance = closest.getDistance3D(x, y, z);
		for (PolyArea cell : cells) {
			double candidate = cell.getDistance3D(x, y, z);
			if (candidate < distance) {
				closest = cell;
				distance = candidate;
			}
		}
		return closest.getClosestPoint(x, y, z);
	}

	@Override
	public boolean intersectsRectangle(RectangleArea area) {
		for (PolyArea cell : cells) {
			if (cell.intersectsRectangle(area)) {
				return true;
			}
		}
		return false;
	}

	private static float minBottom(List<Points> rings) {
		float bottom = Float.MAX_VALUE;
		for (Points ring : rings) {
			bottom = Math.min(bottom, ring.getBottom());
		}
		return bottom;
	}

	private static float maxTop(List<Points> rings) {
		float top = -Float.MAX_VALUE;
		for (Points ring : rings) {
			top = Math.max(top, ring.getTop());
		}
		return top;
	}
}
