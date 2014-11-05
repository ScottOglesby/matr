package com.kurumi.matr;

import java.awt.Point;
import java.util.Random;

import com.kurumi.matr.EName;
import com.kurumi.matr.Route;
import com.kurumi.matr.HighwayLogViewer;
import com.kurumi.matr.Town;

/**
 * Model the island and its towns and roads.
 * Coordinate tile system starts at (x=0, y=0) at upper left.
 * @author soglesby
 *
 */
@SuppressWarnings("unused")
public class Realm {
   // class constants =========
   private final static int landIndent = 4,
      maxTowns = 128,
      maxStreets = 1023,
      maxRoutes = 1023,
      numRuralRoads = 10;

   // odds for doing stuff, divided by oddsMask
   private final static int oddsMask = 15;   // for checking odds out of 16
   private final static int eat9x9odds = 5,  // convert 9x9 land to water
      change3x3odds = 5,  // change 3x3 land <-> water
      fixSlabOdds = 7,  // add a corner to slab
      persistence = 10,  // chances of road continuing beyond junction
      streetpersistence = 8, // same for street
      doubleup = 12,  // chance of overlap at junction
      changeDirBias = 3,  // chance of changing route's direction bias
      returnToNaturalDirBias = 6;  // chance that change auto goes to natural

   // hwys shorter than this is 100% persistence
   private final static int minLengthPersist = 8,
      minLengthStreetPersist = 6,
      minLengthBeforeRemeetRoute = 12,
      minLengthAtDirBias = 12;

   private final static int rcStreet = 0, rcRoute = 1;

   // instance vars
   // 0 cannot be a town id, street id, or route id
	Square[][] grid;
   EName namer = new EName();
   Town[] towns = new Town[maxTowns];
   String[] streetNames = new String[maxStreets];
   Route[] routes = new Route[maxRoutes];
   // note which towns are along top and left of island
   int[] topTowns = new int[100];
   int[] leftTowns = new int[100];

   private Random dice = new Random();   // source of random numbers
	private int width, height;  // # of squares each dimension
   private int numTowns;       // number of distinct towns
   private int numOpenSquares = 1;  // number of land squares without towns
   private int numRoutes;  // # of numbered routes
   private int numSids = 0;  // # of named streets

   // ctor
	Realm(int width_, int height_, int numRoutes_) {
		width = width_;
		height = height_;
      numRoutes = numRoutes_;
		grid = new Square[width][height];

      // clear grid
		for (int i = width-1; i >= 0; i--) {
         for (int j = height-1; j >= 0; j--) {
            grid[i][j] = new Square();
         }
		}
	}

   // called by "Create" GUI button
   public void create() {
      makeCoastline();
      fillTowns();
      MakePaths();
   }

   // # of squares in realm, wide and high; for map
   public int getWidth() { return width; }
   public int getHeight() { return height; }

   public int getNumTowns() { return numTowns; }
   public int getNumRoutes() { return numRoutes; }

	// clear all marked squares
   // assumes no land in outer rim
   private void clearMarks() {
		for (int i = width-1; i >= 0; i--) {
         for (int j = height-1; j >= 0; j--) {
            grid[i][j].setMarked(false);
         }
		}
   }

	// fill rectangular region with single terrain type
   // System.out.println("fill: " + x0 + ", " + y0 + ", " +
   // w + ", " + h + ", " + terr);
	private void fillTerrain(int x0, int y0, int w, int h, int terr) {
		for (int x = x0; x < x0 + w; x++) {
			for (int y = y0; y < y0 + h; y++) {
            grid[x][y].setTerrain(terr);
			}
		}
	}

   // up/down flip. you give upper left of lower square.
   private void do3x3vert(int x, int y) {
      int up =  grid[x][y-1].getTerrain();
      int down =   grid[x][y].getTerrain();
      if (up != down) {
         int chance = dice.nextInt() & oddsMask;
         if (chance < change3x3odds) { // cut down
            fillTerrain(x, y, 3, 3, up);
         }
         else if (chance < 2*change3x3odds) { // cut up
            fillTerrain(x, y-3, 3, 3, down);
         }
      }
   } 

   // left/right flip. you give upper left of right square.
   private void do3x3horiz(int x, int y) {
      int left =  grid[x-1][y].getTerrain();
      int right =   grid[x][y].getTerrain();
      if (left != right) {
         int chance = dice.nextInt() & oddsMask;
         if (chance < change3x3odds) { // cut right
            fillTerrain(x, y, 3, 3, left);
         }
         else if (chance < 2*change3x3odds) { // cut left
            fillTerrain(x-3, y, 3, 3, right);
         }
      }
   } 

   // randomly indent or outdent a 3x3 block of land on coast
   private void do3x3block(int x, int y) {
      // check top border, bottom border
      do3x3vert(x+3, y);
      do3x3vert(x+3, y+9);
      // check left border, right border
      do3x3horiz(x, y+3);
      do3x3horiz(x+9, y+3);
   }
   
   /* --
      smooth out "slab" corners, where a 2x2 block could fit.
      change the center square to make it a stairstep.
      slabs occur only if 4 out of 9, including the center,
      are one type, while the other 5 are another.
      You should call this before doing any other single-square
      manipulations.
   */
   private void fixslab(int x, int y) {
      // if you fix every one, you get all 45-deg slants
      if ((dice.nextInt() & oddsMask) >= fixSlabOdds) {
         return;
      }
      int numWater = 0;
      int me =  grid[x][y].getTerrain();
      for (int i = x-1; i <= x+1; i++) {
         for (int j = y-1; j <= y+1; j++) {
            if ( grid[i][j].getTerrain() == Square.water) {
               numWater++;
            }
         }
      }
      if (numWater == 4 && me == Square.water) {
         grid[x][y].setTerrain(Square.land);
      }
      if (numWater == 5 && me == Square.land) {
         grid[x][y].setTerrain(Square.water);
      }
   }

   // use crude fractals to make jagged coastline
   void makeCoastline() {
	   // make indented landmass
	   fillTerrain(landIndent, landIndent,
			   width - landIndent*2, height - landIndent*2, Square.land);

	   // randomly take out 9x9 chunks from shoreline
	   // north and south
	   for (int x = landIndent; x < width - landIndent - 9; x += 9) {
		   if ((dice.nextInt() & oddsMask) < eat9x9odds) {
			   fillTerrain(x, landIndent, 9, 9, Square.water);
		   }
		   if ((dice.nextInt() & oddsMask) < eat9x9odds) {
			   fillTerrain(x, height-landIndent-9, 9, 9, Square.water);
		   }
	   }
	   // east and west. Corners have already been looked at
	   for (int y = landIndent + 9; y < height - landIndent - 18; y += 9) {
		   if ((dice.nextInt() & oddsMask) < eat9x9odds) {
			   fillTerrain(landIndent, y, 9, 9, Square.water);
		   }
		   if ((dice.nextInt() & oddsMask) < eat9x9odds) {
			   fillTerrain(width-landIndent-9, y, 9, 9, Square.water);
		   }
	   }

	   // randomly add or subtract 3x3 chunks
	   // north and south
	   for (int x = landIndent; x < width - landIndent - 9; x += 9) {
		   do3x3block(x, landIndent);
		   do3x3block(x, height-landIndent-9);
	   }
	   // east and west
	   for (int y = landIndent + 9; y < height - landIndent - 18; y += 9) {
		   do3x3block(landIndent, y);
		   do3x3block(width-landIndent-9, y);
	   }

	   // now fix:        .L
	   //        bowties  L.    and slab corners
	   for (int x = 0; x < width-1; x++) {
		   for (int y = 0; y < height-1; y++) {
			   if (x > 0 && y > 0) {
				   fixslab(x, y);
			   }
			   int ul =  grid[x][y].getTerrain();
			   int ur =  grid[x+1][y].getTerrain();
			   int ll =  grid[x][y+1].getTerrain();
			   int lr =  grid[x+1][y+1].getTerrain();
			   if (ul == lr && ur == ll && ul != ur) {
				   fillTerrain(x, y, 2, 2, Square.land);
			   }
		   }
	   }

	   // new feature: smooth out jaggies in shoreline by setting display hints
	   // try northwest first
	   // 7 6 5
	   // 0 . 4
	   // 1 2 3
	   // index by the bit positions above: land is 1, water is 0
	   //           0   1  2  3  4   5   6   7
	   int[] dx = {-1, -1, 0, 1, 1,  1,  0, -1};
	   int[] dy = { 0,  1, 1, 1, 0, -1, -1, -1};
	   // following are bit masks to compare your surroundings with
	   int nw = (1 << 0) + (1 << 7) + (1 << 6);
	   int se = (1 << 2) + (1 << 3) + (1 << 4);
	   int ne = (1 << 6) + (1 << 5) + (1 << 4);
	   int sw = (1 << 0) + (1 << 1) + (1 << 2);
	   for (int x = 1; x < width-1; x++) {
		   for (int y = 1; y < height-1; y++) {
			   // we expand into water -- not shave off land
			   if (grid[x][y].getTerrain() == Square.land) {
				   continue;
			   }
			   int surroundingLands = 0;
			   for (int i = 0; i <= 7; i++) {
				   if (grid[x+dx[i]][y+dy[i]].getTerrain() == Square.land) {
					   surroundingLands += (1 << i);
				   }
			   }
			   // I believe no more than one of the following can be true
			   if ((surroundingLands & nw) == nw && (surroundingLands & se) == 0) {
				   grid[x][y].setDisplayHint(Square.water_se);
			   }
			   if ((surroundingLands & ne) == ne && (surroundingLands & sw) == 0) {
				   grid[x][y].setDisplayHint(Square.water_sw);
			   }
			   if ((surroundingLands & se) == se && (surroundingLands & nw) == 0) {
				   grid[x][y].setDisplayHint(Square.water_nw);
			   }
			   if ((surroundingLands & sw) == sw && (surroundingLands & ne) == 0) {
				   grid[x][y].setDisplayHint(Square.water_ne);
			   }
		   }
	   }
   }

   //---------------------- town stuff
	// clear all marked squares
   // also caches # of "okForTown" squares
   // assumes no land in outer rim
   void clearMarksAndCountOpen() {
      numOpenSquares = 0;
 		for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            grid[x][y].setMarked(false);
            if (grid[x][y].okForTown()) {
               numOpenSquares++;
            }
         }
      }
   }

   // if this space adjoins a town, return which one; or 0 if none
   // use randomly from all available adjacent ones to prevent
   // bias to one direction
   int adjRegion(int x, int y) {
      int cr[] = {0, 0, 0, 0};
      int numFound = 0;  // will be 0 thru 4
      if (grid[x][y-1].hasRealTown()) {
         cr[numFound++] =  grid[x][y-1].getTown();
      }
      if (grid[x+1][y].hasRealTown()) {
         cr[numFound++] =  grid[x+1][y].getTown();
      }
      if (grid[x][y+1].hasRealTown()) {
         cr[numFound++] =  grid[x][y+1].getTown();
      }
      if (grid[x-1][y].hasRealTown()) {
         cr[numFound++] =  grid[x-1][y].getTown();
      }
      if (numFound > 0) {
         return cr[(dice.nextInt() & 3) % numFound];
      }
	return 0;  // no town
   }

   // only call this one after you've filled in towns
   // if a square is surrounded by 3 others of a different town,
   // let the other town take this "notch"
   void checkNotch(int x, int y) {
      if (!grid[x][y-1].hasRealTown() || ! grid[x+1][y].hasRealTown() ||
          !grid[x][y+1].hasRealTown() || ! grid[x-1][y].hasRealTown()) {
         return;
      }
      int tNorth =  grid[x][y-1].getTown();
      int tEast =  grid[x+1][y].getTown();
      int tSouth =  grid[x][y+1].getTown();
      int tWest =  grid[x-1][y].getTown();
      if (tNorth == tEast && (tEast == tSouth || tEast == tWest)) {
         grid[x][y].setTown(tNorth);
      }
      if (tNorth == tSouth && tSouth == tWest) {
         grid[x][y].setTown(tNorth);
      }
      if (tEast == tSouth && tSouth == tWest) {
         grid[x][y].setTown(tEast);
      }
   }
            
   // count total land area in squares
   int surfaceArea() {
      int total = 0;
 		for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            if (grid[x][y].getTerrain() != Square.water) {
               total++;
            }
         }
      }
      return total;
   }

   // figure town "centers of gravity" and land areas
   void calculateTownCenters() {

      // reset vars
      Point sums[] = new Point[numTowns+1];
      for (int i = 1; i <= numTowns; i++) {
         sums[i] = new Point(0,0);
         towns[i].setArea(0);
      }

      for (int xx = 0; xx < width; xx++) {
         for (int yy = 0; yy < height; yy++) {
            int myTown =  grid[xx][yy].getTown();
            if (myTown > 0) {
               towns[myTown].incrementArea();
               sums[myTown].x += xx;
               sums[myTown].y += yy;
            }
         }
      }

      for (int i = 1; i <= numTowns; i++) {
         int xt = 0, yt = 0;
         if (towns[i].getArea() > 0) {
            xt = sums[i].x/towns[i].getArea();
            yt = sums[i].y/towns[i].getArea();
         }
         // make sure center is on land; if not, move toward land center
         while (grid[xt][yt].getTerrain() == Square.water) {
            if (xt > width/2) {
               xt--;
            }
            else {
               xt++;
            }
            if (yt > height/2) {
               yt--;
            }
            else {
               yt++;
            }
         }
         towns[i].setCenter(xt, yt);
      }
         
   }

   // return number of town center (if any) at point p.
   // return 0 if not at any town center.
   int townCenterAt(Point p) {
      for (int i = 1; i <= numTowns; i++) {
         if (p.equals(towns[i].getCenter())) {
            return i;
         }
      }
      return 0;
   }
      

   void fillTowns() {
      // figure out town spacing for region cores
      int ntowns = surfaceArea()/Town.typicalArea;
      int myWidth = width - 8;  // exclude shoreline
      int myHeight = height - 8;  // exclude shoreline
      int nrows = (int)Math.round(Math.sqrt(ntowns * myHeight/myWidth));
      if (nrows < 1) {
         nrows = 1;
      }
      int thisId = 0;  // num town id's used
      int ncused = 0;  // num town slots used
      int nrused = 0;  // num of rows used

      for (int r = 0; r < nrows; r++) {
         // starting y
         int ty =  myHeight/nrows * r + myHeight/nrows/2 + 4;
         int ncols = Math.round((ntowns-ncused)/(nrows-nrused));
         for (int c = 0; c < ncols; c++) {
            boolean createNewTown = false;
            // start x
            int tx =  myWidth/ncols * c + myWidth/ncols/2 + 4;
            // mark town with a digit if square is suitable
            for (int i = 0; i < Town.coreArea; i++) {
               if ( grid[tx][ty].okForTown()) {
                  grid[tx][ty].setTown(thisId+1);  // yes, +1
                  createNewTown = true;
               }
               switch(dice.nextInt() & 3) {
                  case 0: ty--; break;
                  case 1: tx++; break;
                  case 2: ty++; break;
                  case 3: tx--; break;
               }
               if (tx < 1) tx = 1;
               if (tx > myWidth-2) tx = myWidth-2;
               if (ty < 1) ty = 1;
               if (ty > myHeight-2) ty = myHeight-2;
            }
            if (createNewTown) {
               thisId++;
               towns[thisId] = new Town(namer.pickNewTownName());
               if (nrused == 0) {
                  topTowns[ncused] = thisId;
               }
               if (c == 0) {
                  leftTowns[nrused] = thisId;
               }
            }
            ncused++;
         }
         nrused++;
      }
      numTowns = thisId;

      // now randomly fill out regions.
      // first check, then fill, to avoid chaining.
      int npasses = 15;
      for (int i = 0; i < npasses; i++) {
         for (int y = 1; y < height-1; y++) {
            for (int x = 1; x < width-1; x++) {
               if ((dice.nextInt() & oddsMask) < 12) {
                  if ( grid[x][y].okForTown()) {
                     grid[x][y].setTownAndMark(adjRegion(x, y));
                  }
               }
            }
         }
         clearMarksAndCountOpen();
      }

      // now fill in rest
      int oldNumOpenSquares = 0;
      while (numOpenSquares > 0) {
         for (int y = 1; y < height-1; y++) {
            for (int x = 1; x < width-1; x++) {
               if ( grid[x][y].okForTown()) {
                  grid[x][y].setTownAndMark(adjRegion(x, y));
               }
            }
         }
         clearMarksAndCountOpen();
         // sometimes there are little "sandbars" in ocean with no town
         // don't get stuck in an infinite loop
         if (numOpenSquares == oldNumOpenSquares) {
            break;
         }
         oldNumOpenSquares = numOpenSquares;
      }

      // now fix any single-square "notches"
      for (int y = 1; y < height-1; y++) {
         for (int x = 1; x < width-1; x++) {
            checkNotch(x,y);
         }
      }

      // now figure out town centers and areas
      calculateTownCenters();
   }

   // ========= r stuff ==============================================

   // utility function: return the Junction for a Point.
   public final Junction pToJ(Point p) {
      return grid[p.x][p.y].getJunc();
   }

   // given a point (square) and route id, follow route backward
   // to next square; returns false if at start
   boolean prevJunc(Point here, int routeId) {
      int dir = pToJ(here).getBackwardDirectionStrict(routeId);
      if (dir >= 0) {
         Junction.move(here, dir);
         return true;
      }
      return false;
   }
      
   // given a point (square) and route id, follow route forward
   // to next square; returns false if at end
   boolean nextJunc(Point here, int routeId) {
      int dir = pToJ(here).getForwardDirectionStrict(routeId);
      if (dir >= 0) {
         Junction.move(here, dir);
         return true;
      }
      return false;
   }
      
   // find start of this route from scratch by scanning grid.
   void findRouteStart(int rid) {
      for (int y = 1; y < height-1; y++) {
         for (int x = 1; x < width-1; x++) {
            if (grid[x][y].junc != null) {
               if (grid[x][y].junc.isRouteStart(rid)) {
                  routes[rid].setStart(x, y);
                  return;
               }
            }
         }
      }
   }
               
   // find end of this route from scratch by scanning grid.
   void findRouteEnd(int rid) {
      for (int y = 1; y < height-1; y++) {
         for (int x = 1; x < width-1; x++) {
            if (grid[x][y].junc != null) {
               if (grid[x][y].junc.isRouteEnd(rid)) {
                  routes[rid].setEnd(x, y);
                  return;
               }
            }
         }
      }
   }
               

   // how many squares long is route.
   // does not know about diagonals, so it's inappropriate
   // for calculating mileage.
   int routeLength(Point start, int rid) {
      int length = 0;
      Point here = new Point(start);
      while (nextJunc(here, rid)) {
         length++;
      }
      return length;
   }

   // return true if you can't pave any road out of x,y in direction
   boolean cantPave(Junction j, Point p, int heading) {
      if (grid[p.x][p.y].getTerrain() == Square.water) {
         return true;
      }

      // don't make a bad intersection
      if (!j.isSuitableForLeg(heading)) {
         return true;
      }

      Point nextp = new Point(p);
      Junction.move(nextp, heading);

      // bounds check for following water check
      if (nextp.x < 0 || nextp.y < 0 || 
          nextp.x >= width || nextp.y >= height) {
         //System.out.println("out of bounds");
         return true;
      }

      if (grid[nextp.x][nextp.y].getTerrain() == Square.water) {
         //System.out.println("off land");
         return true;
      }

      // is an incoming leg in this dir bad for next sq?
      if (!pToJ(nextp).isSuitableForLeg(Junction.getReverseDirection(heading))) {
         //System.out.println("congested at " + nextp);
         return true;
      }

      // would this create a corner intersection?
      if (Junction.isDiagonal(heading)) {
         nextp.setLocation(p);
         Junction.move(nextp, heading, Junction.veerleft);
         if (!pToJ(nextp).isEmpty(heading, Junction.right)) {
            return true;
         }
         nextp.setLocation(p);
         Junction.move(nextp, heading, Junction.veerright);
         if (!pToJ(nextp).isEmpty(heading, Junction.left)) {
            return true;
         }
      }
      return false;
   }

   // convenience form of cantPave
   boolean cantPave(Point p, int heading) {
      return cantPave(pToJ(p), p, heading);
   }   

   // return true if you can't add a route out of (x,y) in 'direction'
   boolean cantAddRoute(Junction j, Point p, int direction) {
      if (cantPave(j, p, direction)) {
         return true;
      }
         
      // no room left to add route number
      if (j.isFull(direction)) {
         return true;
      }
      return false;
   }

   // return true if you can't add a street out of (x,y) in 'direction'
   boolean cantAddStreet(Junction j, Point p, int direction) {
      if (cantPave(j, p, direction)) {
         return true;
      }
         
      // leg is already paved
      if (!j.isEmpty(direction)) {
         return true;
      }
      return false;
   }

   /* choose an appropriate start for street or route.
      Return true if it's OK to start paving there;
      false if not.
      (Clients will manage things like max # of tries
      and other recovery techniques.
   */
   boolean pickPavementStart(Point here, int heading)
   {
      int delta;

      // pick x start, guaranteed in bounds
      delta = dice.nextInt(Integer.MAX_VALUE) % (2*width / 3);
      switch(heading) {
         case Junction.north: case Junction.east: 
         case Junction.northeast: case Junction.southeast: 
            here.x = delta; break;
         default:
            here.x = width - 1 - delta; break;
      }
      
      // pick y start, guaranteed in bounds
      delta = dice.nextInt(Integer.MAX_VALUE) % (2*height / 3);
      switch(heading) {
         case Junction.east: case Junction.south:
         case Junction.southwest: case Junction.southeast: 
            here.y = delta; break;
         default:
            here.y = height - 1 - delta; break;
      }

      return !cantPave(pToJ(here), here, heading);
   }

   // choose an appropriate start for a new route
   // it can start up overlapped with a street, but not another route
   void pickRouteStart(Point here, int heading, boolean findExisting)
   {
      int maxTries = 200;

      while (true) {

         if (pickPavementStart(here, heading)) {
            Junction j = pToJ(here);

            // need a 2-way stretch of road to start from...
            if (findExisting || maxTries <= 0) {
               if (j.isTwoWay() && j.isEmpty(heading)) {
                  return;
               }
            }
            // ... or any leg not taken by another route
            else {
               if (j.hasNoRoutes(heading) && 
                   j.isSuitableForLeg(heading)) {
                  return;
               }
            }
         }
         maxTries--;
      }
   }

   // choose an appropriate start for a new street.
   // streets must have their own pavement.
   void pickStreetStart(Point here, int heading, boolean findExisting)
   {
      int maxTries = 200;

      while (true) {

         if (pickPavementStart(here, heading)) {
            Junction j = pToJ(here);

            if (j.isEmpty(heading)) {
               // need a 2-way stretch of road to start from...
               if (findExisting || maxTries <= 0) {
                  if (j.isTwoWay()) {
                     return;
                  }
               }
               // ... or any leg that makes a valid intersection
               else {
                  if (j.isSuitableForLeg(heading)) {
                     return;
                  }
               }
            }
         }
         maxTries--;
      }
   }

   /* =============
      convenience functions to set things in this square and next.
      heading: compass heading of this segment. Not log direction.
      forward: whether route is going forward or backward.
   */
   void addRouteBoth(Point here, int heading, 
                     int rid, int paveType, boolean forward) {
      Point next = new Point(here);
      Junction.move(next, heading);
      int reverse = Junction.getReverseDirection(heading);
      pToJ(here).setWiderPavement(heading, paveType);
      pToJ(next).setWiderPavement(reverse, paveType);
      addRidBoth(here, heading, rid, forward);
   }

   // add route number; don't touch pavement
   void addRidBoth(Point here, int heading, 
                     int rid, boolean forward) {
      Point next = new Point(here);
      Junction.move(next, heading);
      if (forward) {
         pToJ(here).addRidForward(heading, rid);
         pToJ(next).addRidBack(heading, rid);
      }
      else {
         // must flip because you're adding backwards out of sq
         int flipheading = Junction.getReverseDirection(heading);
         pToJ(here).addRidBack(flipheading, rid);
         pToJ(next).addRidForward(flipheading, rid);
      }
   }

   // delete route number; don't touch pavement
   void removeRidBoth(Point here, int heading, 
                     int rid) {
      Point next = new Point(here);
      Junction.move(next, heading);
      pToJ(here).removeRid(heading, rid);
      pToJ(next).removeRid(Junction.getReverseDirection(heading), rid);
   }

   // clear all route id's from this leg and next
   void clearRidsBoth(Point here, int heading) {
      Point next = new Point(here);
      Junction.move(next, heading);
      pToJ(here).clearRids(heading);
      pToJ(next).clearRids(Junction.getReverseDirection(heading));
   }

   void addStreetBoth(Point here, int heading, 
                     int sid, int paveType, boolean forward) {
      Point next = new Point(here);
      Junction.move(next, heading);
      if (forward) {
         pToJ(here).addStreetForward(heading, sid, paveType);
         pToJ(next).addStreetBack(heading, sid, paveType);
      }
      else {
         // must flip because you're adding backwards out of sq
         int flipheading = Junction.getReverseDirection(heading);
         pToJ(here).addStreetBack(flipheading, sid, paveType);
         pToJ(next).addStreetForward(flipheading, sid, paveType);
      }
   }

   void setSidBoth(Point here, int heading, int sid) {
      Point next = new Point(here);
      Junction.move(next, heading);
      pToJ(here).setStreetId(heading, sid, true);
      pToJ(next).setStreetId(Junction.getReverseDirection(heading), sid, true);
   }

   void setPaveBoth(Point here, int heading, int paveType) {
      Point next = new Point(here);
      Junction.move(next, heading);
      pToJ(here).setPavement(heading, paveType);
      pToJ(next).setPavement(Junction.getReverseDirection(heading), paveType);
   }


   // bias toward current bias
   // log heading + bias = temporary favored heading
   // call init, then call check every square
   // let road curve, but discourage constant curviness
   // tends to return to straight
   // member vars: lengthAtDirBias, currentDirBias, naturalDirBias
   int lengthAtDirBias, currentDirBias, naturalDirBias;
   void initDirBias() {
      currentDirBias = 0;
      lengthAtDirBias = 0;
      int bc = dice.nextInt() & oddsMask;
      if (bc < 3) {
         naturalDirBias = -1;
      }
      else if (bc > 12) {
         naturalDirBias = 1;
      }
      else {
         naturalDirBias = 0;
      }
   }

   void checkDirBias() {
      lengthAtDirBias++;
      if (lengthAtDirBias > minLengthAtDirBias) {
         int chance = dice.nextInt() & oddsMask;
         if (chance < changeDirBias) {
            chance = dice.nextInt() & oddsMask;
            if (chance < returnToNaturalDirBias) {
               currentDirBias = naturalDirBias;
            }
            else {
               currentDirBias = (chance % 3) - 1;  // -1, 0, or 1
            }
            lengthAtDirBias = 0;
            //System.out.println("New Nat bias ind: " + naturalDirBias);
         }
      }
   }

   // Forbid pavement turns greater than 90 deg.
   // Discourage 90 deg turns.
   void checkTurns(int dir[], int dirChances[], 
                   int oldHeading, boolean keepStraight) {
      for (int i = 0; i < dir.length; i++) {
         // if needs to be straight, don't waste your time
         if (keepStraight && (dir[i] != oldHeading)) {
            dirChances[i] = 0;
            continue;
         }
         // no turn > 90 deg
         int degTurn = Math.abs(oldHeading - dir[i]) * 45;
         if (degTurn > 90) {
            dirChances[i] = 0;
            //System.out.println("too sharp " + i + "(" + degTurn + ")");
            continue;
         }
         // discourage 90 deg turns
         if (degTurn == 90) {
            dirChances[i] /= 2;
         }
      }
   }

   // lay out one route or street, whose id, start, heading, and pavement
   // are already chosen.
   // returns end of route or street.
   // - routes can overlap, streets can't.
   Point layout(int id, int roadClass, Point start, int heading, 
                  int paveType, int maxLength, boolean forward,
                  boolean dieSoon, boolean keepStraight) {
      boolean done = false;
      boolean doubling = false;
      int numForcedDirections = 0;
      int length = 0;      // how many squares has road gone
      Point here = new Point(start);
      Point next = new Point();
      int lastIdSeen = 0;      // last rid/sid intersected
      int sqSinceLastId = 0;   // how many sqs ago
      boolean enc2;             // encouraged to double (debug)
     
      // if you're backward, pretend you're going the other way
      if (!forward) {
         heading = Junction.getReverseDirection(heading);
      }

      // get primary (dir[2]) and alternate dirs (2 to each side)
      int dir[] = {0, 0, 0, 0, 0};
      for (int i = 0; i < dir.length; i++) {
         dir[i] = Junction.getGlobalDirection(heading, i-2);
      }
      initDirBias();

      while (!done) {
         //System.out.println("here: " + here);
         Junction j = pToJ(here);  // alias

         // remember old heading - prevent too sharp a turn
         int oldHeading = heading;

         // options when you're at an intersection
         // NB: a "3-way" includes you entering the square.
         // none of these apply when you're doubling
         boolean isIntersection = 
            (roadClass == rcRoute && !doubling && j.isRouteJunction()) ||
            (roadClass == rcStreet && j.isThreeWayOrMore());

         int minPersist = (roadClass == rcRoute) ?
            minLengthPersist : minLengthStreetPersist;

         if (isIntersection) {
            // dieSoon => die at first non-dead-end
            if (dieSoon) {
               break;
            }

            // do you feel like ending at this intersection?
            if (length > minPersist && 
                (dice.nextInt() & oddsMask) > persistence) {
               //System.out.println("id " + id + " tired");
               break;
            }

            // avoid bumping into the same route time after time
            if (roadClass == rcRoute) {
               int anyOtherRid = j.anyRidExcept(id);
               if (anyOtherRid != 0) {
                  lastIdSeen = anyOtherRid;
                  //System.out.println("rid " + rid + " tired of rid " +
                  //                   anyOtherRid + " at " + length);
                  sqSinceLastId = 0;
               }
            }
         }

         // init decision matrix; slight bias toward original heading
         int dirChances[] = {5, 10, 30, 10, 5};  // chance to go in each dir

         // favor biased direction; see if it should change
         dirChances[2 + currentDirBias] += 20;
         checkDirBias();
               
         // first, determine where it is possible to go.
         // don't double back on yourself or go to unsuitable space.
         int sumChances = 0;
         enc2 = false;

         // curves are ok, but discourage sharp ones
         checkTurns(dir, dirChances, oldHeading, keepStraight);
         for (int i = 0; i < dir.length; i++) {
            if (dirChances[i] == 0) {
               continue;
            }

            if (roadClass == rcRoute && cantAddRoute(j, here, dir[i])) {
               dirChances[i] = 0;
               continue;
            }
            if (roadClass == rcStreet && cantAddStreet(j, here, dir[i])) {
               dirChances[i] = 0;
               continue;
            }

            // if leg leaving this square in that direction
            // is in use, consider doubling.
            // stay doubled if already doubling.
            if (roadClass == rcRoute && !j.hasNoRoutes(dir[i])) {
               // don't start off doubling
               if (length == 0) {
                  dirChances[i] = 0;
                  continue;
               }
               
               // do you really want to start doubling?
               if (!doubling) {
                  if ((dice.nextInt() & oddsMask) >= doubleup) {
                     dirChances[i] = 0;
                     continue;
                  }
                  else {
                     dirChances[i] += 20;
                     //System.out.println("encouraged rid " +
                     //                 rid + " to double along " + i);
                     enc2 = true;
                  }
               }
            }
            // segment should go in primary direction for first 3 squares
            if (length < 3 && i != 2) {
               dirChances[i] /= 2;
            }
            // this chance is nonzero
            sumChances++;
         }

         // are you forced in a direction? Usually by water
         if (sumChances == 1) {
            if (++numForcedDirections > 5 && length > 15) {
               //System.out.println("too forced");
               done = true;
               break;
            }
            if (dieSoon) {
               done = true;
               break;
            }
         }

         // avoid remeeting a route you've just seen
         if (roadClass == rcRoute) {
            for (int i = 0; i < dir.length; i++) {
               // don't bother if you can't go there
               if (dirChances[i] == 0) {
                  continue;
               }
               next.setLocation(here); // lookahead
               Junction.move(next, dir[i]);
               Junction jn = pToJ(next);

               // will you meet a road you've already seen?
               if (!doubling && !enc2 && lastIdSeen != 0 && 
                   jn.containsRoute(lastIdSeen) &&
                   sqSinceLastId < minLengthBeforeRemeetRoute) {
                  //System.out.println("rid " + rid + " seeing rid " +
                  //                   lastRidSeen + " again at " + 
                  //                   length + " - " + next);
                  // jn.dump();
                  dirChances[i] = 0;
               }
            }
         }

         // count up total chance bins
         int totalChances = 0;
         for (int i = 0; i < dir.length; i++) {
            totalChances += dirChances[i];
         }
         
         //System.out.println("2nd chances: " + dirChances[0] + ',' +
         //  dirChances[1] + ',' + dirChances[2] + "," +
         //  dirChances[3] + ',' + dirChances[4]);
         
         // is there nowhere to go?
         if (totalChances == 0) {
            //System.out.println("nowhere to go");
            done = true;
            break;
         }

         // now decide where to go
         int sample = dice.nextInt(Integer.MAX_VALUE) % totalChances;
         int binTotal = 0; // now shows sum of bins visited so far
         for (int i = 0; i < dir.length; i++) {
            // don't bother if you can't go there
            if (dirChances[i] == 0) {
               continue;
            }
            binTotal += dirChances[i];
            if (sample < binTotal) {
               heading = dir[i];
               break;
            }
         }
            
         // track doubling up.
         // you've already decided whether or not you can
         boolean wasDoubling = doubling;
         if (roadClass == rcRoute) {
            doubling = !j.hasNoRoutes(heading);
         }

         // determine next square
         next.setLocation(here);
         Junction.move(next, heading);
         sqSinceLastId++;

         // pave outgoing (this sq) and incoming (next)
         if (roadClass == rcRoute) {
            addRouteBoth(here, heading, id, paveType, forward);
         }
         if (roadClass == rcStreet) {
            addStreetBoth(here, heading, id, paveType, forward);
         }
         here.setLocation(next);

         length++;
         if (length >= maxLength) {
            break;
         }
      } // while !done
      return here;
   }

   // convenience functions
   // layout straight city streets
   private Point layoutCityStreet(int sid, Point here, int dir, int len) {
      return layout(sid, rcStreet, here, dir, Junction.undiv2, len, 
                    true, false, true);
   }

   // layout curvy rural roads
   private Point layoutRuralRoad(int sid, Point here, int dir, int len) {
      return layout(sid, rcStreet, here, dir, Junction.undiv2, len, 
                    true, false, false);
   }

   // lay out highways
   void MakePaths() {
      int heading;   // current global direction
      Point here = new Point();
      Point next = new Point();
      Point start = new Point();
      Point end = new Point();
      int routeCount;   // how many routes generated
      int pavementType;  // undiv2, etc.
      int currentSid = 0;
      int topTownId = 0;
      int leftTownId = 0;

      // keep track of which town has been served by a route start
      boolean townUsed[] = new boolean[numTowns+1];
      for (int i = 1; i <= numTowns; i++) {
         townUsed[i] = false;
      }

      // choose pavement type - undiv2 for now
      pavementType = Junction.undiv2;

      // give each town a 5x5 block of streets
      for (int i = 1; i <= numTowns; i++) {
         here.setLocation(towns[i].getCenter());
         for (int j = 0; j <= 4; j++) {
            // vertical
            next.x = here.x + j - 2;
            next.y = here.y + 2;
            layoutCityStreet(++currentSid, next, Junction.north, 4);
            streetNames[currentSid] = namer.pickNewStreetName();
            // horizontal
            next.x = here.x - 2;
            next.y = here.y + j - 2;
            layoutCityStreet(++currentSid, next, Junction.east, 4);
            streetNames[currentSid] = namer.pickNewStreetName();
         }
      }

      Point notSet = new Point(0, 0);
      for (routeCount=1; routeCount <= numRoutes;) {

         // starting points: alternate n/s routes from top towns,
         // then e/w routes from left towns,m
         // then routes from inner towns
         // then random; pick a starting location based on direction

         // need a n,s,e, or w heading; no diagonal
         // this will be the heading unless reassigned
         heading = Junction.getRandomDirection90();
         here.setLocation(notSet);  // not assigned

         // start routes at towns at top and left edges
         // alternate north-south and east-west routes
         if (routeCount % 2 != 0) {
            if (topTowns[topTownId] > 0) {
               townUsed[topTowns[topTownId]] = true;
               here.setLocation(towns[topTowns[topTownId++]].getCenter());
               heading = Junction.south;
            }
         }
         else {
            if (leftTowns[leftTownId] > 0) {
               townUsed[leftTowns[leftTownId]] = true;
               here.setLocation(towns[leftTowns[leftTownId++]].getCenter());
               heading = Junction.east;
            }
         }
        
         // if not assigned, choose an unused city
         if (here.equals(notSet)) {
            for (int i = numTowns; i >= 1; i--) {
               if (!townUsed[i]) {
                  here.setLocation(towns[i].getCenter());
                  townUsed[i] = true;
                  break;
               }
            }
         }

         // if all cities used up, choose a random spot
         if (here.equals(notSet)) {
            pickRouteStart(here, heading, true);
         }

         // lay out the route; save start and end
         start.setLocation(here);
         end.setLocation(layout(routeCount, rcRoute, here, heading, 
                                pavementType, 300, true, false, false));

         // if route has nonzero length, give it a number
         // and go to next one
         if (routeLength(start, routeCount) > 0) {
            int pref;
            switch (heading) {
               case Junction.north:
               case Junction.south:
                  pref = EName.prefOdd;
                  break;
               default:
                  pref = EName.prefEven;
                  break;
            }
            int routeNum = namer.pickNewNumber(pref);
            routes[routeCount] = 
               new Route(routeCount, routeNum, heading, start, end);
            routeCount++;
         }
      } // for all routes

      // extend dead ends backwards, if feasible
      // go backwards and die at first road you meet
      for (int i = 1; i <= numRoutes; i++) {
         here.setLocation(routes[i].getStart());
         heading = routes[i].getLogDirection();
         here = layout(i, rcRoute, here, heading, pavementType, 
                            300, false, true, false);
         routes[i].setStart(here);
      }

      // do some rural streets
      for (int i = 1; i <= numRuralRoads; i++) {
         heading = Junction.getRandomDirection90();
         pickStreetStart(here, heading, true);
         layoutRuralRoad(++currentSid, here, heading, 96);
         streetNames[currentSid] = namer.pickNewRuralStreetName();
      }
      numSids = currentSid;

   }

   // changing things
   void changeRoute(int oldNumber, int newNumber) {
      for (int i = 1; i <= numRoutes; i++) {
         if (routes[i].getNumber() == oldNumber) {
            routes[i].setNumber(newNumber);
            return;
         }
      }
   }

   void changeStreet(String old, String anew) {
      for (int i = 1; i <= numSids; i++) {
         if (streetNames[i].equalsIgnoreCase(old)) {
            streetNames[i] = new String(anew);
            return;
         }
      }
   }

   void changeTown(String old, String anew) { 
      for (int i = 1; i <= numTowns; i++) {
         if (towns[i].getName().equalsIgnoreCase(old)) {
            towns[i].setName(anew);
            return;
         }
      }
   }

   // find out route Id
   public int getRouteId(int rNumber) {
      for (int i = 1; i <= getNumRoutes(); i++) {
         if (routes[i].getNumber() == rNumber) {
            return i;
         }
      }
      return 0; // not found
   }

   // used for paver. creates new route if you give it a new number.
   public int getRouteIdOrCreate(int rNumber, Point here, int heading,
                                 int logHeading) {
      int rid = getRouteId(rNumber);
      if (rid == 0) {
         // not found; make new one
         Point next = new Point(here);
         Junction.move(next, heading);
         routes[++numRoutes] = new Route(numRoutes, rNumber, logHeading, here, next);
         return numRoutes;
      }
      return rid;
   }

   // find out street Id. Don't give it an empty street.
   public int getSId(String sName) {
      for (int i = 1; i <= numSids; i++) {
         if (streetNames[i].equalsIgnoreCase(sName)) {
            return i;
         }
      }
      return 0;
   }

   // used for paver. creates new street if you give it a new number.
   public int getSIdOrCreate(String sName) {
      if (sName.length() == 0) {
         return 0;
      }
      int sid = getSId(sName);
      if (sid == 0) {
         streetNames[++numSids] = new String(sName);
         return numSids;
      }
      return sid;
   }

   public boolean routeExists(int rnum) {
      return (getRouteId(rnum) > 0);
   }

   public boolean streetExists(String name) {
      return (getSId(name) > 0);
   }

   public boolean townExists(String name) {
	   for (Town town: towns) {
		   if (town.getName().equalsIgnoreCase(name)) {
			   return true;
		   }   
	   }
	   return false;
   }

	// print towns to stdout
	void dump() {
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
				switch ( grid[x][y].getTerrain()) {
					case Square.water: System.out.print('.'); break;
					case Square.land:
                  System.out.print((char)('0' +  grid[x][y].getTown())); break;
				}
			}
			System.out.println("");
		}
		System.out.println("---------------");
	}

}

