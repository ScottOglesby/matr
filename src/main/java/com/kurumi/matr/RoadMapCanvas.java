package com.kurumi.matr;

import java.awt.*;
import java.util.Vector;

import com.kurumi.matr.MatrPanel;
import com.kurumi.matr.MapFrame;

public class RoadMapCanvas extends Canvas {

	// min length of road (in pixels) that has room for a marker
	private static int minMarkerClearance = 36;

	// future: min pixel size of tile (square) for which you display street names
	//   private static int minTileSizeForStreetNames = 48;

	// future: min size of tile for displaying hash marks for overpasses
	//   private static int minTileSizeForGradeSep = 24;

	// define what a mouse click should do.
	// future: interchange = 5
	public static final int doNothing = 0,
			doCenter = 1, doCenterZoom = 2,
			doMoveViewer = 3, doPave = 4;
	public static String clickStrings[] = 
		{ "Nothing", "Center", "Zoom In",
		"Move Viewer", "Pave"};

	// ---- instance variables ----

	// width and height of associated realm in tiles
	private int width = 0;
	private int height = 0;

	private Realm myRealm;
	private MapFrame myMapFrame;  // frame enclosing the map
	private MatrPanel myPanel;   // supports move viewer

	private int cvNum = 2, cvDenom = 1; // conversion from tiles to pixels
	private Point upleft = new Point(0, 0);
	private boolean firstPaint = true;
	
	// units below are tiles
	private Rectangle viewport;
	private Rectangle fullViewPort;  // includes all of grid
	private Rectangle landViewPort;  // includes all of grid - 1 on each side

	private Vector<Point> selection = new Vector<Point>();  // selected path for action

	RoadMapCanvas(Realm realm, MatrPanel panel, MapFrame frame) {
		this.myRealm = realm;
		this.myPanel = panel;
		this.myMapFrame = frame;
		width = myRealm.getWidth();
		height = myRealm.getHeight();
		fullViewPort = new Rectangle(0, 0, width, height);
		landViewPort = new Rectangle(0, 0, width, height);
		landViewPort.grow(-1, -1);
		setBackground(Color.white);
		MapUtils.setMarkerColor(MapUtils.foreground, MapUtils.highwayColor);
	}

   /* ====
      conversion functions
      pixels <--> grid coordinates
      can get pixel for either top-left or center of grid square
      don't precalc num/denom; avoid int roundoff lossage
   */
	private int sqToXPixel(int sq) {
		return sq * cvNum / cvDenom - upleft.x;
	}
	private int sqToXMidPixel(int sq) {
		return sq * cvNum / cvDenom + cvNum / (cvDenom*2) - upleft.x;
	}
	private int sqToYPixel(int sq) {
		return sq * cvNum / cvDenom - upleft.y;
	}
	private int sqToYMidPixel(int sq) {
		return sq * cvNum / cvDenom + cvNum / (cvDenom*2) - upleft.y;
	}

	private int pixelToXSq(int pixel) {
		return (pixel + upleft.x) * cvDenom/cvNum;
	}
	private int pixelToYSq(int pixel) {
		return (pixel + upleft.y) * cvDenom/cvNum;
	}

	/**
	 * check if pixel (usually from mouse click) is actually a valid square
	 * @param x  x position in pixels
	 * @param y  y position in pixels
	 * @return true if click is somewhere in the realm (still may be outside viewport)
	 */
	private boolean isPixelOnMap(int x, int y) {
		int xp = pixelToXSq(x);
		int yp = pixelToYSq(y);
		return (xp >= 0 && xp < width && yp >= 0 && yp < height);
	}

   // break a square down into octants, to determine which
   // road someone has selected.
   // make sure it's already on map.
   private int pixelToPointAndOctant(Point mouse, Point square) {
      square.x = pixelToXSq(mouse.x);
      square.y = pixelToYSq(mouse.y);
      int d = cvNum / cvDenom;   // width of one square
      int dx = mouse.x - sqToXPixel(square.x);  // offset into square
      int dy = mouse.y - sqToYPixel(square.y);  // offset into square

      boolean above1 = (dy >  -d/2 + 2 * dx);
      boolean above2 = (dy >   d/4 + dx/2);
      boolean above3 = (dy > 3*d/4 - dx/2);
      boolean above4 = (dy > 3*d/2 - 2 * dx);
      if (!above1 && !above4) {
         return Junction.north;
      }
      if (above1 && !above2) {
         return Junction.northwest;
      }
      if (above2 && !above3) {
         return Junction.west;
      }
      if (above3 && !above4) {
         return Junction.southwest;
      }
      if (above4 && above1) {
         return Junction.south;
      }
      if (!above1 && above2) {
         return Junction.southeast;
      }
      if (!above2 && above3) {
         return Junction.east;
      }
      if (!above3 && above4) {
         return Junction.northeast;
      }
      return Junction.north;
   }

   // return rectangle containing all squares seen in window
   // top and left are aligned with squares, but bottom and
   // right are usually partial squares
   // this is cached, since it's compared many times during redraws.
   Rectangle viewableRect() {
      return new Rectangle(pixelToXSq(0), pixelToYSq(0), 
                           pixelToXSq(getSize().width), pixelToYSq(getSize().height)); 
   }

   void setViewableRect() {
      viewport = viewableRect();
   }


   /* =========
      handle paving selection
   */
   public void deselect() {
      selection.setSize(0);
      repaint();
   }

   // client iterates over this to pave/unpave, whatever
   public Vector<Point> getSelection() {
      return selection;
   }

   // provide sign of (left - right): -1, 0, or 1.
   private static int sign(int left, int right) {
      if (left >= right) {
         return (left == right) ? 0: 1;
      }
      return -1;
   }

   // given a new mouse click, extend the selection in that direction
   // takes mx and my in grid coordinates
   void extendSelection(int mx, int my) {

      Point tileClicked = new Point(mx, my);

      // if selection empty, first click sets start
      if (selection.size() == 0) {
         selection.addElement(tileClicked);
         return;
      }
      
      // if point is already in selection, cut it there
      int matchIndex = selection.indexOf(tileClicked);
      if (matchIndex >= 0) {
         selection.setSize(matchIndex + 1);
         return;
      }
            
      // determine which way to go.
      // pathEnd is end of existing path.
      Point pathEnd = selection.lastElement();

      // start of 45 deg path
      int sx = pathEnd.x;
      int sy = pathEnd.y;
      
      /* ===
         if existing path (2 or more elements) is diagonal,
         and tileclicked is within 44.999 deg from extension
         of this diagonal, go diagonal first before going orthogonal
      */
      if (selection.size() >= 2) {
         Point penult = 
            selection.elementAt(selection.size()-2);
         if (penult.x != sx && penult.y != sy) {
            if (sx - penult.x == sign(mx, sx) &&
                sy - penult.y == sign(my, sy)) {
               while (sx != mx && sy != my) {
                  sx += sign(mx, sx);
                  sy += sign(my, sy);
                  selection.addElement(new Point(sx, sy));
               }
            }
         }
      }

      /* ==
         Move in 2 segments (either may be missing):
         - 90 deg to intercept a 45 deg path to target (like a rook)
         - 45 deg path (like a bishop)
      */

      // 90 deg: go in direction of greatest difference
      while (Math.abs(mx - sx) != Math.abs(my - sy)) {
         if (Math.abs(mx - sx) > Math.abs(my - sy)) {
            sx += sign(mx, sx);
         }
         else {
            sy += sign(my, sy);
         }
         //System.out.println("|:(" + sx + ", " + sy + ")");
         selection.addElement(new Point(sx, sy));
      }

      // now |d.s - sx| == |d.y - sy|
      while (sx != mx) {
         sx += sign(mx, sx);
         sy += sign(my, sy);
         //System.out.println("/:(" + sx + ", " + sy + ")");
         selection.addElement(new Point(sx, sy));
      }
      
   }

   // draw selection path as green 3-pixel line;
   // circle around start point.
   private void drawSelection(Graphics g)
   {
      if (selection.size() < 1) {
         return;
      }

      int x0, y0, x1, y1;
      g.setColor(MapUtils.selectionColor);

      // draw circle around start point
      Point pp = selection.elementAt(0);
      x0 = sqToXMidPixel(pp.x);
      y0 = sqToYMidPixel(pp.y);
      g.drawOval(x0-3, y0-3, 6, 6);

      if (selection.size() == 1) {
         return;
      }
      for (int i = 0; i < selection.size()-1; i++) {
         Point from = selection.elementAt(i);
         Point to = selection.elementAt(i+1);
         x0 = sqToXMidPixel(from.x);
         y0 = sqToYMidPixel(from.y);
         x1 = sqToXMidPixel(to.x);
         y1 = sqToYMidPixel(to.y);

         // draw a line guaranteed to be at least 3 thick in any direction
         g.drawLine(x0+1, y0-1, x1+1, y1-1);
         g.drawLine(x0-1, y0-1, x1-1, y1-1);
         g.drawLine(x0, y0, x1, y1);
         g.drawLine(x0+1, y0+1, x1+1, y1+1);
         g.drawLine(x0-1, y0+1, x1-1, y1+1);
      }
   }

   // draw all the water squares on the map
   // Land is (background) white.
   // using white background chopped redraw in half (100->50 ms)
   private void drawWater(Graphics g)
   {
	   int x0, y0, x1, y1;
	   g.setColor(MapUtils.oceanColor);
	   // determine clipping region
	   Rectangle myRect = viewport.intersection(fullViewPort);

	   for (int xx = myRect.x; xx < myRect.x + myRect.width; xx++) {
		   for (int yy = myRect.y; yy < myRect.y + myRect.height; yy++) {
			   x0 = sqToXPixel(xx);
			   x1 = sqToXPixel(xx+1);
			   y0 = sqToYPixel(yy);
			   y1 = sqToYPixel(yy+1);
			   // debugging: draw circle if you would have drawn water southeast
			   if (myRealm.grid[xx][yy].getDisplayHint() == Square.water_se) {
				   Polygon po = new Polygon();
				   po.addPoint(x0, y1);
				   po.addPoint(x1, y1);
				   po.addPoint(x1, y0);
				   g.fillPolygon(po);
			   }
			   else 
				   if (myRealm.grid[xx][yy].getDisplayHint() == Square.water_sw) {
					   Polygon po = new Polygon();
					   po.addPoint(x0, y0);
					   po.addPoint(x0, y1);
					   po.addPoint(x1, y1);
					   g.fillPolygon(po);
				   }
				   else 
					   if (myRealm.grid[xx][yy].getDisplayHint() == Square.water_nw) {
						   Polygon po = new Polygon();
						   po.addPoint(x0, y0);
						   po.addPoint(x0, y1);
						   po.addPoint(x1, y0);
						   g.fillPolygon(po);
					   }
					   else 
						   if (myRealm.grid[xx][yy].getDisplayHint() == Square.water_ne) {
							   Polygon po = new Polygon();
							   po.addPoint(x0, y0);
							   po.addPoint(x1, y0);
							   po.addPoint(x1, y1);
							   g.fillPolygon(po);
						   }
						   else 
							   if (myRealm.grid[xx][yy].getTerrain() == Square.water) {
								   g.fillRect(x0, y0, x1-x0, y1-y0);
							   }
		   }
	   }
   }

   // draw all route markers where needed.
   private void drawRouteMarkers(Graphics g)
   {
      // route marker font
      int perSquare = cvNum / cvDenom;
      boolean wasVisible = false;

      // for each route
      int numRoutes = myRealm.getNumRoutes();
      for (int i = 1; i <= numRoutes; i++) {
         Point start, half;
         boolean endOfRoute = false;
         start = new Point(myRealm.routes[i].getStart());
         half = new Point(start);
         wasVisible = viewport.contains(start.x, start.y);

         while (!endOfRoute) {
            int length = 0;
            while (true) {

               // at end of this segment?
               if (!myRealm.nextJunc(start, i)) {
                  endOfRoute = true;
                  break;
               }
               // look for a visible starting point
               if (!viewport.contains(start.x, start.y)) {
                  // looking for first one?
                  if (!wasVisible) {
                     continue;
                  }
                  // end of visible stretch... write marker
                  else {
                     break;
                  }
               }
               else {
                  // if you just got out of invisibility, 
                  // set halfway point to this one
                  if (!wasVisible) {
                     half.move(start.x, start.y);
                     length = 0;
                  }
                  // only inc length if this and last was visible
                  else {
                     length++;
                  }
                  wasVisible = true;
               }

               //System.out.println("start: " + start.x + ", " + start.y);
               //r.grid[start.x][start.y].getJunc().dump();
               //System.out.println("half: " + half.x + ", " + half.y);
               // is it still two-way?
               // is it on the screen? if not, don't care
               // wasVisible is known true if you get to this point
               if (wasVisible && 
                   myRealm.grid[start.x][start.y].getJunc().isRouteInflection()) {
                  break;
               }
               // halfway point follows half as often
               if ((length & 1) != 0) {
                   myRealm.nextJunc(half, i);
               }
            }
            if (wasVisible && length * perSquare > minMarkerClearance) {

               // find out routes of interest
               Junction jh = myRealm.grid[half.x][half.y].getJunc();
               int mydir =  jh.getForwardDirection(i);
               int[] rids = jh.ridsAt(mydir);

               // special case if segment is only one square long
               // if double marker, only print for first rid
               if (length > 1) {
                  if (rids[1] > 0) {
                     if (rids[1] != i) {
                        drawMarker(g, half, rids);
                     }
                  }
                  else {
                     drawMarker(g, half, i);
                  }
               }
               if (length == 1) {
                  if (rids[1] > 0) {
                     if (rids[1] != i) {
                        drawMarker(g, half, start, rids);
                     }
                  }
                  else {
                     drawMarker(g, half, start, i);
                  }
               }
            }
            // sync up half and start
            half.move(start.x, start.y);
            wasVisible = viewport.contains(start.x, start.y);
         } // end of route
      }
   }

   // draw single marker
   void drawMarker(Graphics g, Point here, int rid) {
      int xc = sqToXMidPixel(here.x);
      int yc = sqToYMidPixel(here.y);
      MapUtils.drawMarker(g, xc, yc, myRealm.routes[rid].getNumber());
   }

   // draw single marker "between" squares
   void drawMarker(Graphics g, Point here, Point next, int rid) {
      int xc = (sqToXMidPixel(here.x) + sqToXMidPixel(next.x))/2;
      int yc = (sqToYMidPixel(here.y) + sqToYMidPixel(next.y))/2;
      MapUtils.drawMarker(g, xc, yc, myRealm.routes[rid].getNumber());
   }

   // draw double marker
   void drawMarker(Graphics g, Point here, int rid[]) {
      int xc = sqToXMidPixel(here.x);
      int yc = sqToYMidPixel(here.y);
      int num0 = myRealm.routes[rid[0]].getNumber();
      int num1 = myRealm.routes[rid[1]].getNumber();
      MapUtils.drawMarker(g, xc, yc, num0, num1);
   }

   // draw double marker "between" squares
   void drawMarker(Graphics g, Point here, Point next, int rid[]) {
      int xc = (sqToXMidPixel(here.x) + sqToXMidPixel(next.x))/2;
      int yc = (sqToYMidPixel(here.y) + sqToYMidPixel(next.y))/2;
      int num0 = myRealm.routes[rid[0]].getNumber();
      int num1 = myRealm.routes[rid[1]].getNumber();
      MapUtils.drawMarker(g, xc, yc, num0, num1);
   }

   private void drawTownBorders(Graphics g)
   {
      int x0, y0, x1, y1;
      g.setColor(MapUtils.townLineColor);
      // determine clipping region
      Rectangle myRect = viewport.intersection(landViewPort);

      for (int xx = myRect.x; xx < myRect.x + myRect.width; xx++) {
         for (int yy = myRect.y; yy < myRect.y + myRect.height; yy++) {
            // optional draw border, above
            if (myRealm.grid[xx][yy].getTerrain() != Square.water) {
               if (yy > 0 && myRealm.grid[xx][yy-1].getTerrain() != Square.water)
               {
                  int ch = myRealm.grid[xx][yy].getTown();
                  int c1 = myRealm.grid[xx][yy-1].getTown();
                  if (ch != c1) {
                     x0 = sqToXPixel(xx);
                     y0 = sqToYPixel(yy);
                     x1 = sqToXPixel(xx+1);
                     g.drawLine(x0, y0, x1, y0);
                  }
               }
               // optional draw border, left
               if (xx > 0 && myRealm.grid[xx-1][yy].getTerrain() != Square.water)
               {
                  int ch = myRealm.grid[xx][yy].getTown();
                  int c3 = myRealm.grid[xx-1][yy].getTown();
                  if (ch != c3) {
                     x0 = sqToXPixel(xx);
                     y0 = sqToYPixel(yy);
                     y1 = sqToYPixel(yy+1);
                     g.drawLine(x0, y0, x0, y1);
                  }
               }
            }
         }
      }
   }

   // draw a single segment of road.
   // decide which color to use.
   private static void drawSeg(Graphics g, int paveType, boolean isRoute,
                        int dir, int xc, int yc, int x1, int y1) {
      Color lineColor = MapUtils.streetColor;
      if (isRoute) {
         lineColor = MapUtils.highwayColor;
      }
      if (paveType == Junction.dirt) {
         lineColor = MapUtils.dirtRoadColor;
      }
      boolean isDivided = Junction.isPaveTypeDivided(paveType);
      MapUtils.drawRoadSegment(g, lineColor, isDivided, dir,
                               xc, yc, x1, y1);
   }

   private void drawRoads(Graphics g) {
      int x0, y0, x1, y1, xc, yc, paveType;

      // determine clipping region
      Rectangle myRect = viewport.intersection(landViewPort);

      for (int xx = myRect.x; xx < myRect.x + myRect.width; xx++) {
         for (int yy = myRect.y; yy < myRect.y + myRect.height; yy++) {
            if (myRealm.grid[xx][yy].junc == null) {
               continue;
            }

            // don't precalculate deltas; want roundoff as close as possible
            x0 = sqToXPixel(xx);
            y0 = sqToYPixel(yy);
            x1 = sqToXPixel(xx+1);
            y1 = sqToYPixel(yy+1);
            xc = (x0 + x1) / 2;
            yc = (y0 + y1) / 2;

            // destination x and y for north..northwest clockwise
            int dx[] = {xc, x1, x1, x1, xc, x0, x0, x0};
            int dy[] = {y0, y0, yc, y1, y1, y1, yc, y0};

            Junction j = myRealm.grid[xx][yy].junc; // alias
            if (j.bridge == Junction.none) {
               // do undivided first, so any divided can overprint
               for (int dir = 0; dir < Junction.numDirs; dir++) {
                  paveType = j.pavementAt(dir);
                  if (paveType != Junction.none && 
                      !Junction.isPaveTypeDivided(paveType)) {
                     drawSeg(g, paveType, !j.hasNoRoutes(dir),
                             dir, xc, yc, dx[dir], dy[dir]);
                  }
               }

               // now do divided
               for (int dir = 0; dir < Junction.numDirs; dir++) {
                  paveType = j.pavementAt(dir);
                  if (paveType != Junction.none && 
                      Junction.isPaveTypeDivided(paveType)) {
                     drawSeg(g, paveType, !j.hasNoRoutes(dir),
                             dir, xc, yc, dx[dir], dy[dir]);
                  }
               }
            }
            else {
               int bdir = j.bridge - 1;
               // do lower road first
               for (int dir = 0; dir < Junction.numDirs; dir++) {
                  if ((dir % 4) != bdir) {
                     paveType = j.pavementAt(dir);
                     if (paveType != Junction.none) {
                        drawSeg(g, paveType, !j.hasNoRoutes(dir),
                                dir, xc, yc, dx[dir], dy[dir]);
                     }
                  }
               }
               // draw grade separation
               boolean divided = j.isDivided(bdir) ||
                  j.isDivided(Junction.getReverseDirection(bdir));
               MapUtils.drawGradeSeps(g, bdir, xc, yc, divided);
               // do upper road
               for (int dir = 0; dir < Junction.numDirs; dir++) {
                  if ((dir % 4) == bdir) {
                     paveType = j.pavementAt(dir);
                     if (paveType != Junction.none) {
                        drawSeg(g, paveType, !j.hasNoRoutes(dir),
                                dir, xc, yc, dx[dir], dy[dir]);
                     }
                  }
               }
               // draw interchange box if appropriate
               //MapUtils.drawInterchangeBox(g, xc, yc, (bdir & 1) != 0);
            }
         }
      }
   }


   // draw names of all towns, offset from town center to the right.
   private void drawTownNames(Graphics g) {
      g.setFont(Empire.townFont);
      g.setColor(MapUtils.townNameColor);
      for(int i = 1; i <= myRealm.getNumTowns(); i++) {
         int xt = sqToXMidPixel(myRealm.towns[i].getCenter().x);
         int yt = sqToYMidPixel(myRealm.towns[i].getCenter().y);
         g.drawOval(xt-2, yt-2, 4, 4);
         g.drawString(myRealm.towns[i].getName(), xt+5, yt);
      }
   }

   /* =====
      These 2 are called when the user moves the scrollbars
      on the enclosing window. The arg is a pixel value,
      usually directly from the scrollbar.
   */
   public void updatex(int x) {
      upleft.move(x, upleft.y);
      repaint();
      setViewableRect();
   }
      
   public void updatey(int y) {
      upleft.move(upleft.x, y);
      repaint();
      setViewableRect();
   }
      
   public void zoomToFit() {
      int viewWidth = getSize().width;
      int viewHeight = getSize().height;

      // get square size numerator and denominator
      if (viewWidth * height < viewHeight * width) {
         cvNum = viewWidth;
         cvDenom = width;
      }
      else {
         cvNum = viewHeight;
         cvDenom = height;
      }
      upleft.x = upleft.y = 0;
      adjustScrollbars();
      setViewableRect();
   }


   // max width and height in pixels at current zoom state.
   public int pixWidth() {
      return width * cvNum/cvDenom;
   }
   public int pixHeight() {
      return height * cvNum/cvDenom;
   }

   // keep offset at center, if possible
   public void zoomIn() {
      if (cvDenom % 2 == 0) {
         cvDenom /= 2;
      }
      else {
         cvNum *= 2;
      }
      // keep same proportional center
      int cx = upleft.x + getSize().width/2;
      upleft.x = 2 * cx - getSize().width/2;
      int cy = upleft.y + getSize().height/2;
      upleft.y = 2 * cy - getSize().height/2;
      //System.out.println("Center is " + cx + "," + cy);
      adjustScrollbars();
      setViewableRect();
   }

   public void zoomOut() {
      if (cvNum % 2 == 0) {
         cvNum /= 2;
      }
      else {
         cvDenom *= 2;
      }
      // keep same proportional center
      int cx = upleft.x + getSize().width/2;
      upleft.x = cx/2 - getSize().width/2;
      int cy = upleft.y + getSize().height/2;
      upleft.y = cy/2 - getSize().height/2;
      adjustScrollbars();
      setViewableRect();
   }

   public void centerAt(int cx, int cy) {
      upleft.x = cx - getSize().width/2;
      upleft.y = cy - getSize().height/2;
      adjustScrollbars();
      setViewableRect();
   }

   public void centerAndZoomInAt(int cx, int cy) {
      // shift center; first find out real
      int rcx = getSize().width/2;
      int rcy = getSize().height/2;
      upleft.x += cx - rcx;
      upleft.y += cy - rcy;
      zoomIn();
   }

   // eyedropper function: pick up info at mouse click location.
   private void getRoadInfo(int x, int y) {
      Point square = new Point();
      int octant = pixelToPointAndOctant(new Point(x,y), square);
      if (myRealm.grid[square.x][square.y].getTerrain() != Square.water) {
         myMapFrame.reportRoadInfo(square, octant);
         myPanel.infoCopiedFromMap(square, octant);
      }
   }
   // this does get called.
   @Override
public void reshape(int x, int y, int w, int h) {
	   // don't use setBounds() here; recursive loop
      super.reshape(x, y, w, h);
      adjustScrollbars();
      setViewableRect();
   }

   // tell enclosing frame to update its scroll bars.
   private static Point zeroPoint = new Point(0,0);
   public void adjustScrollbars() {
      // Update our scrollbar pos, page size, min, max
      myMapFrame.adjustScrollbars(upleft, getSize(), 
                                  zeroPoint, 
                                  new Point(pixWidth(), pixHeight()));
   }
   
   /**
    * Hmmm... forgot what this does :-/
    * @param x
    * @param y
    */
   private void tempSetBridge(int x, int y) {
      int xx = pixelToXSq(x);
      int yy = pixelToYSq(y);
      Junction j = myRealm.grid[xx][yy].junc;
      if (j == null) {
         return;
      }
      j.bridge++;
      if (j.bridge > 4) {
         j.bridge = 0;
      }
      repaint();
   }

   /* ====
      handle mouse click directly on the map.
      All off-grid (not over a square in the Realm) clicks
      are illegal, except for deselect shortcut.
   */
   private int clickAction = doCenterZoom;
   @Override
public boolean mouseUp(Event e, int x, int y) {
      // only legal off-grid action is deselection
      if (clickAction == doPave && 
          ((e.modifiers & Event.SHIFT_MASK) != 0)) {
         //deselect();
         tempSetBridge(x, y);
         return true;
      }
      // all other off-grid clicks discarded
      if (!isPixelOnMap(x, y)) {
         return true;
      }
      // right-click is eyedropper function
      if ((e.modifiers & Event.META_MASK) != 0) {
         getRoadInfo(x, y);
         return true;
      }

      switch (clickAction) {
         case doNothing:
            break;
         case doCenter:
            centerAt(x, y);
            repaint();
            break;
         case doCenterZoom:
            centerAndZoomInAt(x, y);
            repaint();
            break;
         case doMoveViewer:
            myPanel.setPlayerLocation(pixelToXSq(x), pixelToYSq(y));
            break;
         case doPave:
            // SHIFT_MASK and right-click cases already handled
            extendSelection(pixelToXSq(x), pixelToYSq(y));
            repaint();
            break;
      }
      return true;
   }

   public void setClickAction(int what) {
      clickAction = what;
   }

   @Override
public void paint(Graphics g) {
      // zoom to fit when the window is first painted
      if (firstPaint) {
         firstPaint = false;
         zoomToFit();
      }
      drawSelection(g);
      drawWater(g);
      drawTownBorders(g);
      drawRoads(g);
      drawRouteMarkers(g);
      drawTownNames(g);
   }
}
