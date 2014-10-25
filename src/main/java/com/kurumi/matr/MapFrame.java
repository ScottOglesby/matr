package com.kurumi.matr;

import java.awt.Dimension;
import java.awt.Point;

/**
 * MapFrame needs to be able to handle the following from a PMap:
 * - adjust scrollbars
 * - send data to a PathTool
 * @author soglesby
 *
 */

interface MapFrame {
	public void adjustScrollbars(Point upleft, Dimension size,
			Point min, Point max);
	public void reportRoadInfo(Point square, int octant);
}
