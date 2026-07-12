package com.aionemu.gameserver.model.geometry;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

/**
 * 多边形2D，用于几何相关逻辑。
 * Polygon 2 D for geometry logic.
 *
 * @version $Id: Polygon2D.java 594018 2007-11-12 04:17:41Z cam $
 */
public class Polygon2D implements Shape, Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 点总数。<code>npoints</code> 表示此多边形有效点数。 / The total number of points. The value of <code>npoints</code> represents the number of valid points in this <code>Polygon</code>
	 */
	public int npoints;

	/**
	 * x 坐标数组。 / The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the number of points in this <code>Polygon2D</code>
	 */
	public float[] xpoints;

	/**
	 * x 坐标数组。 / The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the number of points in this <code>Polygon2D</code>
	 */
	public float[] ypoints;

	/**
	 * Bounds of the Polygon2D. @see #getBounds()。
	 */
	protected Rectangle2D bounds;

	private GeneralPath path;
	private GeneralPath closedPath;

	/**
	 * Creates an empty Polygon2D
	 */
	public Polygon2D() {
		xpoints = new float[4];
		ypoints = new float[4];
	}

	/**
	 * 由指定 Rectangle2D 构造并初始化 Polygon2D。 / Constructs and initializes a <code>Polygon2D</code> from the specified Rectangle2D.
	 */
	public Polygon2D(Rectangle2D rec) {
		if (rec == null) {
			throw new IndexOutOfBoundsException("null Rectangle");
		}
		npoints = 4;
		xpoints = new float[4];
		ypoints = new float[4];
		xpoints[0] = (float) rec.getMinX();
		ypoints[0] = (float) rec.getMinY();
		xpoints[1] = (float) rec.getMaxX();
		ypoints[1] = (float) rec.getMinY();
		xpoints[2] = (float) rec.getMaxX();
		ypoints[2] = (float) rec.getMaxY();
		xpoints[3] = (float) rec.getMinX();
		ypoints[3] = (float) rec.getMaxY();
		calculatePath();
	}

	/**
	 * 由指定 Polygon 构造并初始化 Polygon2D。 / Constructs and initializes a <code>Polygon2D</code> from the specified Polygon.
	 */
	public Polygon2D(Polygon pol) {
		if (pol == null) {
			throw new IndexOutOfBoundsException("null Polygon");
		}
		this.npoints = pol.npoints;
		this.xpoints = new float[pol.npoints];
		this.ypoints = new float[pol.npoints];
		for (int i = 0; i < pol.npoints; i++) {
			xpoints[i] = pol.xpoints[i];
			ypoints[i] = pol.ypoints[i];
		}
		calculatePath();
	}

	/**
	 * 由指定参数构造并初始化 Polygon2D。 / Constructs and initializes a <code>Polygon2D</code> from the specified parameters.
	 */
	public Polygon2D(float[] xpoints, float[] ypoints, int npoints) {
		if (npoints > xpoints.length || npoints > ypoints.length) {
			throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
		}
		this.npoints = npoints;
		this.xpoints = new float[npoints];
		this.ypoints = new float[npoints];
		System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
		System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
		calculatePath();
	}

	/**
	 * 由指定参数构造并初始化 Polygon2D。 / Constructs and initializes a <code>Polygon2D</code> from the specified parameters.
	 */
	public Polygon2D(int[] xpoints, int[] ypoints, int npoints) {
		if (npoints > xpoints.length || npoints > ypoints.length) {
			throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
		}
		this.npoints = npoints;
		this.xpoints = new float[npoints];
		this.ypoints = new float[npoints];
		for (int i = 0; i < npoints; i++) {
			this.xpoints[i] = xpoints[i];
			this.ypoints[i] = ypoints[i];
		}
		calculatePath();
	}

	/**
	 * 将此 Polygon 重置为空多边形。 / Resets this <code>Polygon</code> object to an empty polygon
	 */
	public void reset() {
		npoints = 0;
		bounds = null;
		path = new GeneralPath();
		closedPath = null;
	}

	/** 克隆 / clone. */
	public Object clone() {
		Polygon2D pol = new Polygon2D();
		for (int i = 0; i < npoints; i++) {
			pol.addPoint(xpoints[i], ypoints[i]);
		}
		return pol;
	}

	private void calculatePath() {
		path = new GeneralPath();
		path.moveTo(xpoints[0], ypoints[0]);
		for (int i = 1; i < npoints; i++) {
			path.lineTo(xpoints[i], ypoints[i]);
		}
		bounds = path.getBounds2D();
		closedPath = null;
	}

	private void updatePath(float x, float y) {
		closedPath = null;
		if (path == null) {
			path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
			path.moveTo(x, y);
			bounds = new Rectangle2D.Float(x, y, 0, 0);
		} else {
			path.lineTo(x, y);
			float _xmax = (float) bounds.getMaxX();
			float _ymax = (float) bounds.getMaxY();
			float _xmin = (float) bounds.getMinX();
			float _ymin = (float) bounds.getMinY();
			if (x < _xmin) {
				_xmin = x;
			} else if (x > _xmax) {
				_xmax = x;
			}
			if (y < _ymin) {
				_ymin = y;
			} else if (y > _ymax) {
				_ymax = y;
			}
			bounds = new Rectangle2D.Float(_xmin, _ymin, _xmax - _xmin, _ymax - _ymin);
		}
	}

	/*
	 * get the associated {@link Polyline2D}.
	 */
	public Polyline2D getPolyline2D() {

		Polyline2D pol = new Polyline2D(xpoints, ypoints, npoints);

		pol.addPoint(xpoints[0], ypoints[0]);

		return pol;
	}

	/** 获取多边形。 / Returns the polygon. */
	public Polygon getPolygon() {
		int[] _xpoints = new int[npoints];
		int[] _ypoints = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			_xpoints[i] = (int) xpoints[i];
			_ypoints[i] = (int) ypoints[i];
		}
		return new Polygon(_xpoints, _ypoints, npoints);
	}

	/** 添加点。 / Adds point. */
	public void addPoint(Point2D p) {
		addPoint((float) p.getX(), (float) p.getY());
	}

	/**
	 * 向此 Polygon2D 追加指定坐标。 / Appends the specified coordinates to this <code>Polygon2D</code>.
	 */
	public void addPoint(float x, float y) {
		if (npoints == xpoints.length) {
			float[] tmp;

			tmp = new float[npoints * 2];
			System.arraycopy(xpoints, 0, tmp, 0, npoints);
			xpoints = tmp;

			tmp = new float[npoints * 2];
			System.arraycopy(ypoints, 0, tmp, 0, npoints);
			ypoints = tmp;
		}
		xpoints[npoints] = x;
		ypoints[npoints] = y;
		npoints++;
		updatePath(x, y);
	}

	/**
	 * 判断指定点/坐标是否在此几何内。 / Determines whether the specified {@link Point} is inside this <code>Polygon</code>.
	 */
	public boolean contains(Point p) {
		return contains(p.x, p.y);
	}

	/**
	 * 判断指定点/坐标是否在此几何内。 / Determines whether the specified coordinates are inside this <code>Polygon</code>. <p>.
	 */
	public boolean contains(int x, int y) {
		return contains((double) x, (double) y);
	}

	/**
	 * Returns the high precision bounding box of the {@link Shape}。
	 *
	 * @return a {@link Rectangle2D} that precisely bounds the <code>Shape< / code>.
	 */
	public Rectangle2D getBounds2D() {
		return bounds;
	}

	/** 返回 bounds / Returns the bounds */
	public Rectangle getBounds() {
		if (bounds == null) {
			return null;
		} else {
			return bounds.getBounds();
		}
	}

	/**
	 * 判断指定坐标是否在此几何内。 / Determines if the specified coordinates are inside this <code>Polygon</code>. For the definition of <i>insideness</i>, see the class comments of {@link Shape}.
	 */
	public boolean contains(double x, double y) {
		if (npoints <= 2 || !bounds.contains(x, y)) {
			return false;
		}
		updateComputingPath();
		return closedPath.contains(x, y);
	}

	private void updateComputingPath() {
		if (npoints >= 1) {
			if (closedPath == null) {
				closedPath = (GeneralPath) path.clone();
				closedPath.closePath();
			}
		}
	}

	/**
	 * 测试指定 Point2D 是否在此几何内。 / Tests if a specified {@link Point2D} is inside the boundary of this <code>Polygon</code>.
	 */
	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}

	/**
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polygon</code> intersects the interior of a specified set of rectangular coordinates.
	 */
	public boolean intersects(double x, double y, double w, double h) {
		if (npoints <= 0 || !bounds.intersects(x, y, w, h)) {
			return false;
		}
		updateComputingPath();
		return closedPath.intersects(x, y, w, h);
	}

	/**
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polygon</code> intersects the interior of a specified <code>Rectangle2D</code>.
	 */
	public boolean intersects(Rectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polygon</code> entirely contains the specified set of rectangular coordinates.
	 */
	public boolean contains(double x, double y, double w, double h) {
		if (npoints <= 0 || !bounds.intersects(x, y, w, h)) {
			return false;
		}
		updateComputingPath();
		return closedPath.contains(x, y, w, h);
	}

	/**
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polygon</code> entirely contains the specified <code>Rectangle2D</code>.
	 */
	public boolean contains(Rectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * 返回遍历路径几何的迭代器。 / Returns an iterator object that iterates along the boundary of this <code>Polygon</code> and provides access to the geometry of the outline of this <code>Polygon</code>. An optional {@link AffineTransform} can be specified so that the coordinates returned in the iteration are transformed accordingly.
	 */
	public PathIterator getPathIterator(AffineTransform at) {
		updateComputingPath();
		if (closedPath == null) {
			return null;
		} else {
			return closedPath.getPathIterator(at);
		}
	}

	/**
	 * 返回遍历路径几何的迭代器。 / Returns an iterator object that iterates along the boundary of the <code>Polygon2D</code> and provides access to the geometry of the outline of the <code>Shape</code>. Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are returned by the iterator. Since polygons are already flat, the <code>flatness</code> parameter is ignored.
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPathIterator(at);
	}
}
