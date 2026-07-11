package com.aionemu.gameserver.model.geometry;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

/**
 * Polyline2D，用于几何相关逻辑。
 * Polyline 2 D for geometry logic.
 *
 * @version $Id: Polyline2D.java 594018 2007-11-12 04:17:41Z cam $
 */
class Polyline2D implements Shape, Cloneable, Serializable {

	private static final long serialVersionUID = 8555427697285636463L;

	private static final float ASSUME_ZERO = 0.001f;

	/**
	 * 点总数。<code>npoints</code> 表示此折线点数。 / The total number of points. The value of <code>npoints</code> represents the number of points in this <code>Polyline2D</code>
	 */
	public int npoints;

	/**
	 * x 坐标数组。 / The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the number of points in this <code>Polyline2D</code>
	 */
	public float[] xpoints;

	/**
	 * x 坐标数组。 / The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the number of points in this <code>Polyline2D</code>
	 */
	public float[] ypoints;

	/**
	 * Bounds of the Polyline2D. @see #getBounds()。 / Bounds of the Polyline2D. @see #getBounds()
	 */
	protected Rectangle2D bounds;

	private GeneralPath path;
	private GeneralPath closedPath;

	/**
	 * Creates an empty Polyline2D
	 */
	public Polyline2D() {
		xpoints = new float[4];
		ypoints = new float[4];
	}

	/**
	 * 由指定参数构造并初始化 Polyline2D。 / Constructs and initializes a <code>Polyline2D</code> from the specified parameters.
	 */
	public Polyline2D(float[] xpoints, float[] ypoints, int npoints) {
		if (npoints > xpoints.length || npoints > ypoints.length) {
			throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
		}
		this.npoints = npoints;
		this.xpoints = new float[npoints + 1]; // make space for one more to close the polyline
		this.ypoints = new float[npoints + 1]; // make space for one more to close the polyline
		System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
		System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
		calculatePath();
	}

	/**
	 * 由指定参数构造并初始化 Polyline2D。 / Constructs and initializes a <code>Polyline2D</code> from the specified parameters.
	 */
	public Polyline2D(int[] xpoints, int[] ypoints, int npoints) {
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

	public Polyline2D(Line2D line) {
		npoints = 2;
		xpoints = new float[2];
		ypoints = new float[2];
		xpoints[0] = (float) line.getX1();
		xpoints[1] = (float) line.getX2();
		ypoints[0] = (float) line.getY1();
		ypoints[1] = (float) line.getY2();
		calculatePath();
	}

	/**
	 * 将此 Polyline2D 重置为空折线。 / Resets this <code>Polyline2D</code> object to an empty polygon. The coordinate arrays and the data in them are left untouched but the number of points is reset to zero to mark the old vertex data as invalid and to start accumulating new vertex data at the beginning. All internally-cached data relating to the old vertices are discarded. Note that since the coordinate arrays from before the reset are reused, creating a new empty <code>Polyline2D</code> might be more memory efficient than resetting the current one if the number of vertices in the new polyline data is significantly smaller than the number of vertices in the data from before the reset
	 */
	public void reset() {
		npoints = 0;
		bounds = null;
		path = new GeneralPath();
		closedPath = null;
	}

	/** 克隆 / clone. */
	public Object clone() {
		Polyline2D pol = new Polyline2D();
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
			if (x < _xmin)
				_xmin = x;
			else if (x > _xmax)
				_xmax = x;
			if (y < _ymin)
				_ymin = y;
			else if (y > _ymax)
				_ymax = y;
			bounds = new Rectangle2D.Float(_xmin, _ymin, _xmax - _xmin, _ymax - _ymin);
		}
	}

	/** 添加点。 / Adds point. */
	public void addPoint(Point2D p) {
		addPoint((float) p.getX(), (float) p.getY());
	}

	/**
	 * 向此 Polyline2D 追加指定坐标。 / Appends the specified coordinates to this <code>Polyline2D</code>. <p> If an operation that calculates the bounding box of this <code>Polyline2D</code> has already been performed, such as <code>getBounds</code> or <code>contains</code>, then this method updates the bounding box.
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
	 * 获取此 Polyline2D 的包围盒。 / Gets the bounding box of this <code>Polyline2D</code>. The bounding box is the smallest {@link Rectangle} whose sides are parallel to the x and y axes of the coordinate space, and can completely contain the <code>Polyline2D</code>.
	 */
	public Rectangle getBounds() {
		if (bounds == null) {
			return null;
		} else {
			return bounds.getBounds();
		}
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
	 * 判断指定点/坐标是否在此几何内。 / Determines whether the specified {@link Point} is inside this <code>Polyline2D</code>. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(Point p) {
		return false;
	}

	/**
	 * 判断指定坐标是否在此几何内。 / Determines if the specified coordinates are inside this <code>Polyline2D</code>. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(double x, double y) {
		return false;
	}

	/**
	 * 判断指定点/坐标是否在此几何内。 / Determines whether the specified coordinates are inside this <code>Polyline2D</code>. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(int x, int y) {
		return false;
	}

	/**
	 * Returns the high precision bounding box of the {@link Shape}。 / Returns the high precision bounding box of the {@link Shape}
	 *
	 * @return a {@link Rectangle2D} that precisely bounds the <code>Shape< / code>.
	 */
	public Rectangle2D getBounds2D() {
		return bounds;
	}

	/**
	 * 测试指定 Point2D 是否在此几何内。 / Tests if a specified {@link Point2D} is inside the boundary of this <code>Polyline2D</code>. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(Point2D p) {
		return false;
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
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polyline2D</code> entirely contains the specified set of rectangular coordinates. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(double x, double y, double w, double h) {
		return false;
	}

	/**
	 * 测试此多边形内部是否与给定形状相交。 / Tests if the interior of this <code>Polyline2D</code> entirely contains the specified <code>Rectangle2D</code>. This method is required to implement the Shape interface, but in the case of Line2D objects it always returns false since a line contains no area
	 */
	public boolean contains(Rectangle2D r) {
		return false;
	}

	/**
	 * 返回遍历路径几何的迭代器。 / Returns an iterator object that iterates along the boundary of this <code>Polygon</code> and provides access to the geometry of the outline of this <code>Polygon</code>. An optional {@link AffineTransform} can be specified so that the coordinates returned in the iteration are transformed accordingly.
	 */
	public PathIterator getPathIterator(AffineTransform at) {
		if (path == null) {
			return null;
		} else {
			return path.getPathIterator(at);
		}
	}

	/*
	 * get the associated {@link Polygon2D}. This method take care that may be the
	 * last point can be equal to the first. In that case it must not be included in
	 * the Polygon, as polygons declare their first point only once.
	 */
	public Polygon2D getPolygon2D() {
		Polygon2D pol = new Polygon2D();
		for (int i = 0; i < npoints - 1; i++) {
			pol.addPoint(xpoints[i], ypoints[i]);
		}
		Point2D.Double p0 = new Point2D.Double(xpoints[0], ypoints[0]);
		Point2D.Double p1 = new Point2D.Double(xpoints[npoints - 1], ypoints[npoints - 1]);

		if (p0.distance(p1) > ASSUME_ZERO) {
			pol.addPoint(xpoints[npoints - 1], ypoints[npoints - 1]);
		}
		return pol;
	}

	/**
	 * 返回遍历路径几何的迭代器。 / Returns an iterator object that iterates along the boundary of the <code>Shape</code> and provides access to the geometry of the outline of the <code>Shape</code>. Only SEG_MOVETO and SEG_LINETO, point types are returned by the iterator. Since polylines are already flat, the <code>flatness</code> parameter is ignored.
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return path.getPathIterator(at);
	}
}
