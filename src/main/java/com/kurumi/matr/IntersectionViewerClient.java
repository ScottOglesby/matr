package com.kurumi.matr;

import java.awt.*;
import java.util.Vector;

import com.kurumi.matr.XyzPoint;
import com.kurumi.matr.VEdge;
import com.kurumi.matr.VPoly;
import com.kurumi.matr.Realm;
import com.kurumi.matr.Signpost;


/**
 * Intersection viewer: show driver's eye view of a Junction in the Realm.
 * Has a location (Realm grid coordinates) and heading (north, northeast, etc.)
 * This is the client area of IntersectionViewerFrame
 * 
 * @author soglesby
 *
 */
class IntersectionViewerClient extends Canvas {
	   public static Color skyColor = new Color(0, 191, 255);
	   public static Color groundColor = new Color(34, 140, 34);

	   private static int fustrum = 300;  // scr dist ahead of you, "feet" (kinda)
	   private static int camHeightFt = 5;  // cam height in ft

	   private static int stripeWidth = 4;  // 4-inch wide stripe on road
	   private static int dashLengthFeet = 4;  // for white dashed lines
	   private static int interLengthFeet = 6;
	   private static int dashLengthInches = dashLengthFeet * 12;
	   private static int interLengthInches = interLengthFeet * 12;

	   private static int singleLaneWidthFeet = 10;
	   private static int medianWidth = 16;

	   private int width, height;  // cached size.width and .height

	   private int xCam = 5;  // cam position to right of x=0
	   private int zCam = 0;  // cam position ahead of zero, in feet

	   // Realm-specific stuff
	   Realm myRealm;
	   Signpost left = new Signpost(0, 0);
	   Signpost ahead = new Signpost(0, 0);
	   Signpost right = new Signpost(0, 0);
	   Streetpost street = new Streetpost(0,0);

	   // length in feet of a square. (currently 1/5 mile)
	   public static int fullSquareFeet = Square.straightLength * 5280 / 100;
	   public static int halfSquareFeet = fullSquareFeet/2;

	   // player location in tile coordinates in Realm
	   // finer player location in (x,z) handled by xCam, zCam
	   Point sqPlayer = new Point(0,0);
	   
	   // player heading
	   int aheadDir = 0;

	   IntersectionViewerClient(Realm r) {
	      myRealm = r;
	   }

	   private void cacheSize() {
	      width = getSize().width;
	      height = getSize().height;
	   }      

	   // project a single 3-d point into a Point. camera is at 0, 0, 0.
	   // y is up, x to right, z ahead.
	   // screen is perpendicular to z axis at z=fustrum
	   // distances in feet
	   // Point is screen pixels
	   private Point project(int x, int y, int z) {
	      // translate y based on camera height, and also make up positive
	      y = -y + camHeightFt;

	      // avoid divide by zero or behind
	      // should  clip before you get here
	      if (z <= 0) {
	         z = 1;
	      }

	      return new Point(x * fustrum / z + width/2, 
	                         y * fustrum / z + height/2);
	      //System.out.println("foot: "+x+", "+y+", "+z+" -> "+p.x+", "+p.y);
	   }

	   // same as above, but for distances in inches
	   private Point projectInch(int x, int y, int z) {
	      y = -y + camHeightFt * 12;
	      if (z <= 0) {
	         z = 1;
	      }
	      int sx = x * fustrum / z + width/2;
	      int sy = y * fustrum / z + height/2;
	      // System.out.println("inch: "+x+", "+y+", "+z+" -> "+sx+", "+sy);
	      return new Point(sx, sy);
	   }

	   // sine and cosine values are * 1024, at pi/8 intervals.
	   // degrees:         0   22   45   67    90   112   135    157    180
	   int[] sines =   {   0, 392, 724, 946, 1024,  946,  724,   392,     0};
	   int[] cosines = {1024, 946, 724, 392,    0, -392, -724,  -946, -1024 };
	   // rotate an array of xyz about the y axis
	   // give an angle in pi/8 units, clockwise from north
	   private void rotate(XyzPoint[] p, int thetaPi8) {
	      thetaPi8 %= 16;
	      int sin, cos;
	      if (thetaPi8 > 8) {
	         sin = -sines[16 - thetaPi8];
	         cos = cosines[16 - thetaPi8];
	      }
	      else {
	         sin = sines[thetaPi8];
	         cos = cosines[thetaPi8];
	      }
	      for (int i = 0; i < p.length; i++) {
	         int ax = p[i].x;
	         int az = p[i].z;
	         p[i].z = (az*cos - ax*sin)/1024;
	         p[i].x = (ax*cos + az*sin)/1024;
	      }
	   }

	   private void rotate(VEdge v, int thetaPi8) {
	      XyzPoint alias[] = {v.near, v.far};
	      rotate(alias, thetaPi8);
	   }

	   // project a polygon based on an xyz vector
	   private Polygon projectPoly(Vector<XyzPoint> v) {
	      Polygon po = new Polygon();
	      for (int i = 0; i < v.size(); i++) {
	         XyzPoint xyz = (v.elementAt(i));
	         Point p = project(xyz.x, xyz.y, xyz.z);
	         po.addPoint(p.x, p.y);
	      }
	      return po;
	   }
	   
	   // clip to z=clipz plane
	   final static int clipZFeet = 10;
	   final static int clipZInches = clipZFeet * 12;

	   private static XyzPoint[] clip(XyzPoint[] orig, int clipz) {
	      XyzPoint working[] = new XyzPoint[orig.length+1];
	      XyzPoint here, next;  // always an alias
	      int ri = 0;
	      for (int i = 0; i < orig.length; i++) {
	         // treat poly as closed; wrap around
	         here = orig[i];
	         next = (i < orig.length-1) ? orig[i+1] : orig[0];
	         // always add the point if it's > clipz
	         if (here.z >= clipz) {
	            working[ri++] = new XyzPoint(here);
	         }
	         // positive crossing?
	         if (next.z >= clipz && here.z < clipz) {
	            int zdiff = next.z - here.z;
	            working[ri] = new XyzPoint(0, 0, clipz);
	            working[ri].x = here.x + (clipz-here.z)*(next.x - here.x)/zdiff;
	            working[ri].y = here.y + (clipz-here.z)*(next.y - here.y)/zdiff;
	            ri++;
	         }
	         // negative crossing?
	         if (next.z < clipz && here.z >= clipz) {
	            int zdiff = here.z - next.z;
	            working[ri] = new XyzPoint(0, 0, clipz);
	            working[ri].x = next.x + (clipz-next.z)*(here.x - next.x)/zdiff;
	            working[ri].y = next.y + (clipz-next.z)*(here.y - next.y)/zdiff;
	            ri++;
	         }
	      }
	      XyzPoint result[] = new XyzPoint[ri];
	      for (int i = 0; i < ri; i++) {
	         result[i] = new XyzPoint(working[i]);
	      }
	      return result;
	   }
	            
	   private static Vector<XyzPoint> clip(Vector<XyzPoint> orig, int clipz) {
	      Vector<XyzPoint> working = new Vector<XyzPoint>();
	      XyzPoint here, next;  // always an alias
	      XyzPoint newone;

	      for (int i = 0; i < orig.size(); i++) {
	         // treat poly as closed; wrap around
	         here = orig.elementAt(i);
	         if (i == orig.size() - 1) {
	            next = orig.elementAt(0);
	         }
	         else {
	            next = orig.elementAt(i+1);
	         }

	         // always add the point if it's > clipz
	         if (here.z >= clipz) {
	            newone = new XyzPoint(here);
	            working.addElement(newone);
	         }
	         // positive crossing?
	         if (next.z >= clipz && here.z < clipz) {
	            int zdiff = next.z - here.z;
	            newone = new XyzPoint(0, 0, clipz);
	            newone.x = here.x + (clipz-here.z)*(next.x - here.x)/zdiff;
	            newone.y = here.y + (clipz-here.z)*(next.y - here.y)/zdiff;
	            working.addElement(newone);
	         }
	         // negative crossing?
	         if (next.z < clipz && here.z >= clipz) {
	            int zdiff = here.z - next.z;
	            newone = new XyzPoint(0, 0, clipz);
	            newone.x = next.x + (clipz-next.z)*(here.x - next.x)/zdiff;
	            newone.y = next.y + (clipz-next.z)*(here.y - next.y)/zdiff;
	            working.addElement(newone);
	         }
	      }
	      return working;
	   }
	            
	   // project a polygon based on an xyz array
	   // TODO not currently used
	   @SuppressWarnings("unused")
	   private Polygon projectPoly(XyzPoint[] foo, int length) {
	      Polygon po = new Polygon();
	      for (int i = 0; i < length; i++) {
	         Point p = project(foo[i].x, foo[i].y, foo[i].z);
	         po.addPoint(p.x, p.y);
	      }
	      return po;
	   }

	   // project a polygon based on an xyz array
	   private Polygon projectPolyInch(XyzPoint[] foo) {
	      Polygon po = new Polygon();
	      for (int i = 0; i < foo.length; i++) {
	         Point p = projectInch(foo[i].x, foo[i].y, foo[i].z);
	         po.addPoint(p.x, p.y);
	      }
	      return po;
	   }

	   // add a vector (x,y,z) to each point in foo[].
	   void translate(XyzPoint[] foo, int x, int y, int z) {
	      for (int i = 0; i < foo.length; i++) {
	         foo[i].x += x;
	         foo[i].y += y;
	         foo[i].z += z;
	      }
	   }

	   // add a vector (x,y,z) to each point in Vector.
	   void translate(Vector<XyzPoint> v, int x, int y, int z) {
	      for (int i = 0; i < v.size(); i++) {
	         XyzPoint p = v.elementAt(i);
	         p.x += x;
	         p.y += y;
	         p.z += z;
	      }
	   }

	   /* ===
	      draw double solid stripe, len feet long, start at (x, z).
	      in positive z direction.
	      These are rotated around (x=0, z=0) by dir * pi/4.
	      area between stripes is a stripe wide.
	      convert incoming units (feet) to inches.
	   */
	   public void drawDoubleSolidStripe(Graphics g, int x, int z,
	                                     int len, int dir) {
	      int wid = stripeWidth; // inches
	      x *= 12;
	      z *= 12;
	      len *= 12;
	      // define a rectangle for single stripe
	      XyzPoint[] foo;
	      int yp[] = {0, 0, 0, 0};
	      int xp[] = {x+wid/2, x-wid/2, x-wid/2, x+wid/2};
	      int zp[] = {z+len, z+len, z, z};

	      // draw left, then right line, translated for camera x, z
	      foo = XyzPoint.makeXyz(xp, yp, zp, xp.length);
	      rotate(foo, dir*2);
	      translate(foo, -xCam * 12 - wid, 0, (halfSquareFeet-zCam) * 12);
	      g.fillPolygon(projectPolyInch(clip(foo, clipZInches)));

	      foo = XyzPoint.makeXyz(xp, yp, zp, xp.length);
	      rotate(foo, dir*2);
	      translate(foo, -xCam * 12 + wid, 0, (halfSquareFeet-zCam )* 12);
	      g.fillPolygon(projectPolyInch(clip(foo, clipZInches)));

	   }

	   /* ===
	      draw dashed white stripe, len feet long, start at (x, z).
	      in positive z direction.
	      These are rotated around (x=0, z=0) by dir * pi/4.
	      convert incoming units (feet) to inches.
	   */
	   public void drawDashedStripe(Graphics g, int x, int z, int len, int dir) {
	      int wid = stripeWidth; // inches
	      x *= 12;
	      z *= 12;
	      len *= 12;
	      XyzPoint[] foo;
	      int yp[] = {0, 0, 0, 0};
	      int xp[] = {x+wid/2, x-wid/2, x-wid/2, x+wid/2};
	      int zp[] = {z+dashLengthInches, z+dashLengthInches, z, z};
	      int zoffset = 0;  // in inches

	      for (zoffset = 0; zoffset < len; 
	           zoffset += (dashLengthInches + interLengthInches)) {

	         foo = XyzPoint.makeXyz(xp, yp, zp, xp.length);
	         translate(foo, 0, 0, zoffset); // move stripe in z only
	         rotate(foo, dir*2);
	         translate(foo, -xCam * 12, 0, (halfSquareFeet-zCam) * 12);
	         g.fillPolygon(projectPolyInch(clip(foo, clipZInches)));
	      }
	   }

	   // fill out a section of road, 4-lane or wider,
	   // with matching dashed stripes for lanes.
	   private void drawLaneStripes(Graphics g, int nStripes,
	                                int curb, int startz, int endz,
	                                int facing)
	   {
	      for (int i = 0; i < nStripes; i++) {
	         int sx = curb + singleLaneWidthFeet * (2*i + 1);
	         drawDashedStripe(g, sx, startz, endz, facing);
	         drawDashedStripe(g, -sx, startz, endz, facing);
	      }
	   }

	   // if something goes straight across, draw stripes all the way
	   private void drawStripesAcross(Graphics g, Junction j) {
	      int facing1 = -1, facing2 = -1;  // facing of 1st and 2nd leg
	      int dir1 = -1, dir2 = -1;    // heading of 1st and 2nd leg

	      // find out heading & facing of the two legs
	      for (int facing = 0; facing < Junction.numDirs; facing++) {
	         int dir = Junction.getGlobalDirection(aheadDir, facing);
	         if (!j.isEmpty(dir)) {
	            facing1 = facing;
	            facing2 = Junction.getReverseDirection(facing);
	            dir1 = dir;
	            dir2 = Junction.getReverseDirection(dir);
	            break;
	         }
	      }

	      // find out if common yellow line
	      g.setColor(Color.yellow);
	      if (!j.isDivided(dir1) && !j.isDivided(dir2)) {
	         drawDoubleSolidStripe(g, 0, -halfSquareFeet, fullSquareFeet, facing1);
	      }
	      else if (!j.isDivided(dir1)) {
	         drawDoubleSolidStripe(g, 0, 0, halfSquareFeet, facing1);
	      }
	      else if (!j.isDivided(dir2)) {
	         drawDoubleSolidStripe(g, 0, 0, halfSquareFeet, facing2);
	      }

	      // find out if common dashed line(s)
	      // then draw the "leftover" dashes for wider segment
	      g.setColor(Color.white);
	      int stripes1 =  j.countLanes(dir1) / 2 - 1;
	      int stripes2 =  j.countLanes(dir2) / 2 - 1;
	      int curb = 0; // distance from center to pavement start.
	      if (j.isDivided(dir1) == j.isDivided(dir2)) {
	         curb = (j.isDivided(dir1)) ?  medianWidth/2 : 0;
	         int minStripes = Math.min(stripes1, stripes2);

	         // draw common stripes
	         drawLaneStripes(g, minStripes, curb, 
	                         -halfSquareFeet, fullSquareFeet, facing1);
	         if (stripes1 > stripes2) {
	            drawLaneStripes(g, stripes1, curb, 0, halfSquareFeet, facing1);
	         }
	         if (stripes2 > stripes1) {
	            drawLaneStripes(g, stripes2, curb, 0, halfSquareFeet, facing2);
	         }
	      }
	      else {
	         curb = (j.isDivided(dir1)) ?  medianWidth/2 : 0;
	         drawLaneStripes(g, stripes1, curb, 0, halfSquareFeet, facing1);
	         curb = (j.isDivided(dir2)) ?  medianWidth/2 : 0;
	         drawLaneStripes(g, stripes2, curb, 0, halfSquareFeet, facing2);
	      }
	   }

	   // distance along centerline from polygon center to
	   // get out of way of other radii
	   // indexed by real direction
	   int setBacks[] = new int[Junction.numDirs];
	   
	   private void calcSetBacks(Junction j) {
	      int widths[] = new int[8]; // width of half of entire road
	      for (int dir = 0; dir < Junction.numDirs; dir++) {
	         if (!j.isEmpty(dir)) {
	            widths[dir] = singleLaneWidthFeet * j.countLanes(dir);
	            if (j.isDivided(dir)) {
	               widths[dir] += medianWidth;
	            }
	            widths[dir] /= 2;
	         }
	         //System.out.println("width for dir " + dir + " is " + widths[dir]);
	      }
	      for (int dir = 0; dir < Junction.numDirs; dir++) {
	         int sb = 20;
	         int dirp1 = Junction.getGlobalDirection(dir, 1);
	         int dirp2 = Junction.getGlobalDirection(dir, 2);
	         int dirp3 = Junction.getGlobalDirection(dir, 3);
	         int dirn1 = Junction.getGlobalDirection(dir, -1);
	         int dirn2 = Junction.getGlobalDirection(dir, -2);
	         int dirn3 = Junction.getGlobalDirection(dir, -3);

	         if (widths[dirp3] > 0) {
	            sb = Math.max(sb, widths[dirp3] * 1414/1000 - widths[dir]);
	         }
	         if (widths[dirn3] > 0) {
	            sb = Math.max(sb, widths[dirn3] * 1414/1000 - widths[dir]);
	         }
	         if (widths[dirp2] > 0) {
	            sb = Math.max(sb, widths[dirp2] + widths[dir]);
	         }
	         if (widths[dirn2] > 0) {
	            sb = Math.max(sb, widths[dirn2] + widths[dir]);
	         }
	         if (widths[dirp1] > 0) {
	            sb = Math.max(sb, widths[dirp1] * 1414/1000 + widths[dir]);
	         }
	         if (widths[dirn1] > 0) {
	            sb = Math.max(sb, widths[dirn1] * 1414/1000 + widths[dir]);
	         }
	         //System.out.println("setback for dir " + dir + " is " + sb);
	         setBacks[dir] = sb;
	      }
	   }

	   // need to figure out how far out to start
	   private void drawIntersectionStripes(Graphics g, Junction j) {
	      for (int facing = 0; facing < Junction.numDirs; facing++) {
	         int dir = Junction.getGlobalDirection(aheadDir, facing);
	         if (!j.isEmpty(dir)) {
	            int sb = setBacks[dir];
	            //int paveWidth = singleLaneWidthFeet * j.countLanes(dir);
	            int stripesEachSide = j.countLanes(dir) / 2 - 1;
	            int curb = 0; // distance from center to pavement start.
	            // this is zero for undivided roads.

	            // yellow line for undivided and not dirt
	            if (!j.isDivided(dir) && 
	                j.pavementAt(dir) != Junction.dirt) {
	               g.setColor(Color.yellow);
	               drawDoubleSolidStripe(g, 0, sb, halfSquareFeet-sb, facing);
	            }
	            // align dashed lines with pavement
	            if (j.isDivided(dir)) {
	               curb = medianWidth/2;
	            }
	            // passing lanes - left side, then right side
	            g.setColor(Color.white);
	            drawLaneStripes(g, stripesEachSide, curb, 
	                            sb, halfSquareFeet-sb, facing);
	         }
	      }
	   }

	   // make an edge from x = startx, z = 0 to x = startx, z = 1/2 square
	   // mainly syntactic sugar to make the draw code more concise.
	   private static VEdge makeHsVEdge(int startX) {
	      return new VEdge (new XyzPoint(startX, 0, 0), 
	                        new XyzPoint(startX, 0, halfSquareFeet));
	   }


	   // special case: 2-way boulevard makes 2 unconnected pavements
	   // origin is center of intersection; translate accordingly.
	   private void drawTwoWayDivided(Graphics g, Junction j) {
	      int facing1 = -1, facing2 = -1;  // facing of 1st and 2nd leg
	      int dir1 = -1, dir2 = -1;    // heading of 1st and 2nd leg

	      // find out heading & facing of the two legs
	      for (int facing = 0; facing < Junction.numDirs; facing++) {
	         int dir = Junction.getGlobalDirection(aheadDir, facing);
	         if (!j.isEmpty(dir)) {
	            if (facing1 < 0) {
	               facing1 = facing;
	               dir1 = dir;
	            }
	            else {
	               facing2 = facing;
	               dir2 = dir;
	            }
	         }
	      }

	      // get pavement widths
	      int pw1 = singleLaneWidthFeet * j.countLanes(dir1)/2;
	      int pw2 = singleLaneWidthFeet * j.countLanes(dir2)/2;
	         
	      // do in first and out second
	      int lx = -pw1 - medianWidth/2;
	      int mlx = -medianWidth/2;
	      int mrx = medianWidth/2;
	      int rx = pw2 + medianWidth/2;

	      VEdge inbound = makeHsVEdge(lx);
	      VEdge inMedian = makeHsVEdge(mlx);
	      VEdge outMedian = makeHsVEdge(mrx);
	      VEdge outbound = makeHsVEdge(rx);

	      rotate(inbound, facing1*2);
	      rotate(inMedian, facing1*2);
	      rotate(outMedian, facing2*2);
	      rotate(outbound, facing2*2);
	         
	      VPoly vp = new VPoly();
	      vp.add(inbound, inMedian);
	      vp.add(outMedian, outbound);
	      vp.close();
	      translate(vp.v, -xCam, 0, halfSquareFeet-zCam);
	      g.fillPolygon(projectPoly(clip(vp.v, clipZFeet)));

	      // out first and in second
	      lx = -pw2 - medianWidth/2;
	      rx = pw1 + medianWidth/2;

	      outMedian = makeHsVEdge(mrx);
	      outbound = makeHsVEdge(rx);
	      inbound =  makeHsVEdge(lx);
	      inMedian = makeHsVEdge(mlx);

	      rotate(inbound, facing2*2);
	      rotate(inMedian, facing2*2);
	      rotate(outMedian, facing1*2);
	      rotate(outbound, facing1*2);
	         
	      vp = new VPoly();
	      vp.add(outMedian, outbound);
	      vp.add(inbound, inMedian);
	      vp.close();
	      translate(vp.v, -xCam, 0, halfSquareFeet-zCam);
	      g.fillPolygon(projectPoly(clip(vp.v, clipZFeet)));
	   }

	   // general surface intersection. Handles all except 2-way divided.
	   // origin is center of intersection; translate accordingly.
	   private void drawSurfaceIntersection(Graphics g, Junction j) {
	      VPoly vp = new VPoly();
	      for (int facing = 0; facing < Junction.numDirs; facing++) {
	         int dir = Junction.getGlobalDirection(aheadDir, facing);
	         if (!j.isEmpty(dir)) {
	            // figure out width of one carriageway
	            int paveWidth = singleLaneWidthFeet * j.countLanes(dir);
	            if (j.isDivided(dir)) {
	               paveWidth /= 2;
	            }
	            // print two or one carriageways
	            if (j.isDivided(dir)) {
	               int lx = -paveWidth - medianWidth/2;
	               int mlx = -medianWidth/2;
	               int mrx = medianWidth/2;
	               int rx = paveWidth + medianWidth/2;

	               VEdge inbound = makeHsVEdge(lx);
	               VEdge inMedian = makeHsVEdge(mlx);
	               VEdge outMedian = makeHsVEdge(mrx);
	               VEdge outbound = makeHsVEdge(rx);

	               rotate(inbound, facing*2);
	               rotate(inMedian, facing*2);
	               rotate(outMedian, facing*2);
	               rotate(outbound, facing*2);

	               vp.add(inbound, inMedian, outMedian, outbound);
	            }
	            else {
	               int lx = -paveWidth/2;
	               int rx = paveWidth/2;
	               VEdge inbound = makeHsVEdge(lx);
	               VEdge outbound = makeHsVEdge(rx);
	               rotate(inbound, facing*2);
	               rotate(outbound, facing*2);
	               vp.add(inbound, outbound);
	            }
	         }
	      }
	      if (!vp.isEmpty()) {
	         vp.close();
	         translate(vp.v, -xCam, 0, halfSquareFeet-zCam);
	         g.fillPolygon(projectPoly(clip(vp.v, clipZFeet)));
	      }
	   }

	   @Override
	   public void paint(Graphics g1) {
		   Graphics2D g2 = MyTools.modernize(g1);
	      cacheSize();
	      int postX;  // starting point of signpost
	      int postY = getSize().height/2 + 80;
	      int streetX;
	      int nRoutePosts = 0;
	      int offset[] = { 0, 0, 24, 48 };

	      Junction j = myRealm.pToJ(sqPlayer);

	      // draw sky and ground
	      g2.setColor(skyColor);
	      g2.fillRect(0, 0, width, height/2);
	      g2.setColor(groundColor);
	      g2.fillRect(0, height/2, width, height/2);

	      // determine your x offset based on road type
	      // put you centered in right lane
	      int dirToViewer = Junction.getReverseDirection(aheadDir);
	      xCam = singleLaneWidthFeet * (j.countLanes(dirToViewer) - 1)/2;
	      if (j.isDivided(dirToViewer)) {
	         xCam += medianWidth/2;
	      }
	      if (xCam <= 0) {
	         xCam = singleLaneWidthFeet/2;
	      }
	      postX = getSize().width/2 + 100;
	      streetX = postX;

	      // determine how far back you and stripes should be from
	      // center of intersection
	      calcSetBacks(j);
	      zCam = IntersectionViewerClient.halfSquareFeet - 40 - setBacks[dirToViewer];

	      // show roads, rotated to your POV
	      g2.setColor(Color.black);
	      if (j.isTwoWayDivided()) {
	         drawTwoWayDivided(g2, j);
	      }
	      else {
	         drawSurfaceIntersection(g2, j);
	      }

	      // put stripes on
	      if (j.intersectionType() == Junction.straight) {
	         drawStripesAcross(g2, j);
	      }
	      else {
	         drawIntersectionStripes(g2, j);
	      }

	      // auto-set signs, if you're on a road segment.
	      // assumes you're in rightmost lane, 120 ft ahead
	      // of intersection.
	      if (!j.isEmpty(Junction.getReverseDirection(aheadDir))) {
	         if (j.ridToLeftDir(aheadDir) >= 0) {
	            left.setLocation(postX, postY);
	            left.draw(g2);
	            postX += 48;
	            nRoutePosts++;
	         }
	         if (j.ridAt(aheadDir,0) > 0) {
	            ahead.setLocation(postX, postY);
	            ahead.draw(g2);
	            postX += 48;
	            nRoutePosts++;
	         }
	         if (j.ridToRightDir(aheadDir) >= 0) {
	            right.setLocation(postX, postY);
	            right.draw(g2);
	            nRoutePosts++;
	         }
	         // street sign
	         streetX += offset[nRoutePosts];
	         street.setLocation(streetX, postY);
	         street.draw(g2, (nRoutePosts == 0));
	      }
	   }
	   /**
	    * Move the camera 
	    * @param where player's location in grid coordinates
	    * @param dirFacing direction player is facing; see Junction codes 
	    */
	   public void movePlayer(Point where, int dirFacing) {
	      sqPlayer.setLocation(where);
	      aheadDir = dirFacing;

	      zCam = halfSquareFeet - 80; // todo: never changes

	      // set route info on signs
	      left.setRoutes(myRealm, sqPlayer, aheadDir, Junction.left);
	      ahead.setRoutes(myRealm, sqPlayer, aheadDir, Junction.ahead);
	      right.setRoutes(myRealm, sqPlayer, aheadDir, Junction.right);
	      street.setSids(myRealm, sqPlayer, aheadDir);
	      
	      repaint();
	   }
	   
	}

