package com.kurumi.matr;

import java.awt.Point;
import java.util.Random;

/* ===========
   This class handles the layout at an intersection,
   as well as the route index and direction tags.
   Each Square (q.v.) gets zero or one Junction.
   Handles 8 directions.
*/

public class Junction {
   // directions
   public static final int north = 0, northeast = 1, east = 2,
      southeast = 3, south = 4, southwest = 5, west = 6,
      northwest = 7, numDirs = 8;

   // Viewer combo box is populated with these. Don't add extras
   public static String[] dirStrings = {
      "north", "northeast", "east", "southeast",
      "south", "southwest", "west", "northwest"
   };
   
   // Paver combox boxes are populated with these.
   // also see dirFromString90().
   public static String[] dirStrings90 = {
	   "north", "east", "south", "west"
   };   

   public static String[] dirAbbs = {
      "N", "NE", "E", "SE", "S", "SW", "W", "NW"
   };

   // relative directions
   public static final int ahead = 0, veerright = 1, right = 2,
      back = 4, left = 6, veerleft = 7;

   // intersection types
   public static final int empty = 0, deadend = 1, straight = 2,
      curve = 4, twoway = 6, threeway = 8, fourway = 16,
      unknown = 256;

   // pavement types
   public static final int none = 0, dirt = 1, undiv2 = 2, undiv4 = 3,
      div4 = 4, div6 = 5;

   public static String[] paveStrings = {
      "no road", "dirt road", "2 lane", "4 lane", "4 lane div"
   };

   // overpass types
   public static final int brNorth = 1, brNortheast = 2, brEast = 3,
      brSoutheast = 4;
   
   // how many route numbers can overlap?
   public static int maxOverlappingRoutes = 2;

   // class variables
   private static Random dice = new Random();  // for "get a random direction"

   // static functions ---------------

   // this does not handle fuzzy rights or lefts at 45 deg angles.
   public static int getGlobalDirection(int heading, int facing) {
      return (heading + facing + numDirs) % numDirs;
   }

   public static int getReverseDirection(int heading) {
      return getGlobalDirection(heading, back);
   }

   /* ===
      get a random direction; either at 90 or 45 deg angles
      used by Realm's automatic island generator
   */
   public static int getRandomDirection45() {
      int dir = dice.nextInt() & (numDirs - 1);
      return dir;
   }
   public static int getRandomDirection90() {
      return getRandomDirection45() & ~1;
   }

   // move a point one unit in a given direction
   // often used to follow a road, or look ahead
   public static void move(Point p, int heading) {
      switch (heading) {
         case north:            p.y--; break;
         case northeast: p.x++; p.y--; break;
         case east:      p.x++;        break;
         case southeast: p.x++; p.y++; break;
         case south:            p.y++; break;
         case southwest: p.x--; p.y++; break;
         case west:      p.x--;        break;
         case northwest: p.x--; p.y--; break;
      }
   }

   public static void move(Point p, int heading, int facing) {
      move(p, getGlobalDirection(heading, facing));
   }

   // do the inverse; figure out direction from here to next
   public static int getDirection(Point here, Point next) {
      int dx = next.x - here.x;
      int dy = next.y - here.y;
      switch (dy) {
         case -1:
            switch (dx) {
               case -1: return northwest;
               case 0: return north;
               case 1: return northeast;
            }
            break;
         case 1:
            switch (dx) {
               case -1: return southwest;
               case 0: return south;
               case 1: return southeast;
            }
            break;
         default:
           switch (dx) {
               case -1: return west;
               case 1: return east;
            }
      }
      return -1;
   }

   // return true if submitted direction is not n,s,e, or w
   public static boolean isDiagonal(int dir) {
      return (dir % 2 != 0);
   }

   public static int dirFromString(String str) {
      for (int k = 0; k < Junction.numDirs; k++) {
         if (str.equalsIgnoreCase(dirStrings[k]) ||
             str.equalsIgnoreCase(dirAbbs[k])) {
            return k;
         }
      }
      return -1;  // not found
   }

   // same as above, but checks only n,e,s,w (no 45 deg)
   public static int dirFromString90(String str) {
      for (int k = 0; k < Junction.numDirs; k+=2) {
         if (str.equalsIgnoreCase(dirStrings[k]) ||
             str.equalsIgnoreCase(dirAbbs[k])) {
            return k;
         }
      }
      return -1;  // not found
   }
   
   // same as above, but maps a 4-option Choice (n, e, s, w) to proper direction code
   public static int dirFromChoice4(int selectedItem) {
	   if (selectedItem >= 0 && selectedItem <=3) {
		   return selectedItem * 2;
	   }
	   return -1;
   }
      
   // private bit-busting constants
   private static final short forwardMask = 1024;
   private static final int ridMask = forwardMask - 1;
   public static final int maxRouteIndex = forwardMask - 1;

   // mask is 1 bit in each direction (north == 0 to northwest == 7)
   // paveMask is shifted left to extract layout in one direction
   private static final int layoutMask[] =
   {1, 1<<3, 1<<6, 1<<9, 1<<12, 1<<15, 1<<18, 1<<21};
   // private static final int paveMask = 7;  // not used (yet?)


   // instance vars --------------------

   /* =========
      rd code is route index ("rid") with optional forward bit.
      legal rids are 1 thru maxRouteIndex.
      "forward bit" (maxRouteIndex + 1) is set if this leg is
      in the log direction of the route, exiting the square.
   */
   short rd[][] = new short[numDirs][maxOverlappingRoutes];  // road and dir codes
   short sd[] = new short[numDirs];       // street and dir codes
   short bridge = (short) none;                   // bridge code
   int layout;  // packed 8x3-bit pavement codes


   // ctors --------------
   // no need to call clear(), since vars are initted to zero
   Junction() { this(0); }

   Junction(int layout_) {
      layout = layout_;
   }

   public void clear() {
      for (int i = 0; i < numDirs; i++) {
         rd[i][0] = rd[i][1] = 0;
         sd[i] = 0;
      }
      layout = 0;
   }
   // end ctors ----------

   public void clearLeg(int dir) {
      setPavement(dir, 0);
      rd[dir][0] = 0;
      rd[dir][1] = 0;
      sd[dir] = 0;
   }

   // see what else is at this junction
   // show first rid you find
   // return 0 if no rid found
   public int anyRidExcept(int rid) {
      for (int i = 0; i < numDirs; i++) {
         for (int j = 0; j < 2; j++) {
            if (rd[i][j] > 0 && (rd[i][j] & ridMask) != rid) {
               return rd[i][j] & ridMask;
            }
         }
      }
      return 0;
   }

   /* =====
      operations for route indexes (rids).
   */

   // return specific rid (first or second) for a leg.
   public final int ridAt(int direction, int which) {
      return rd[direction][which] & ridMask;
   }
     
   // return array[2] of route Id's at a given direction
   // used only in RoadMapFrame for double marker
   // TODO: assumes maxOverlappingRoutes == 2
   public int[] ridsAt(int direction) {
      int[] rids = {rd[direction][0] & ridMask, 
                    rd[direction][1] & ridMask};
      return rids;
   }
   
   // there might be a road here, but there can't be a numbered route
   public boolean hasNoRoutes(int direction) {
      return (rd[direction][0]) == 0;
   }

   public final boolean isRidFwdAt(int direction, int which) {
      return (rd[direction][which] & forwardMask) > 0;
   }
     
   // find out which direction a route is leaving an intersection.
   // algo is same as next fcn, but turn on forwardMask bit.
   public int getForwardDirection(int rid) {
      return getBackwardDirection(rid | forwardMask);
   }

   // find out which direction a route is entering an intersection.
   public int getBackwardDirection(int rid) {
      for (int dir = 0; dir < rd.length; dir++ ) {
         if (rd[dir][0] == rid || rd[dir][1] == rid) {
            return dir;
         }
      }
      return -1;
   }

   // find out which direction a route is leaving an intersection;
   // return "failed" (-1) if intersection is not well-formed
   // (more than 2 legs for rid)
   // this prevents nextJunc or prevJunc getting in an infinite loop
   public int getForwardDirectionStrict(int rid) {
      return getBackwardDirectionStrict(rid | forwardMask);
   }

   // find out which direction a route is entering an intersection.
   public int getBackwardDirectionStrict(int rid) {
      int answer = -1;
      int numLegs = 0;
      for (int dir = 0; dir < rd.length; dir++ ) {
         if (rd[dir][0] == rid || rd[dir][1] == rid) {
            answer = dir;
            numLegs++;
         }
      }
      if (numLegs < 2) {
         return answer;
      }
	return -1;
   }

   // >= 0 if exists a direction with route number to the left
   public int ridToLeftDir(int aheadDir) {
      for (int i = -1; i >= -3; i--) {
         int tryDir = getGlobalDirection(aheadDir, i);
         if (ridAt(tryDir, 0) > 0) {
            return tryDir;
         }
      }
      return -1;
   }

   // >= 0 if exists a direction with route number to the right
   public int ridToRightDir(int aheadDir) {
      for (int i = 1; i <= 3; i++) {
         int tryDir = getGlobalDirection(aheadDir, i);
         if (ridAt(tryDir, 0) > 0) {
            return tryDir;
         }
      }
      return -1;
   }

   // true if this route leaves but does not enter junction.
   public boolean isRouteStart(int rid) {
      int answer = -1;
      for (int dir = 0; dir < rd.length; dir++ ) {
         if (rd[dir][0] == (rid | forwardMask) || 
             rd[dir][1] == (rid | forwardMask)) {
            answer = dir;
         }
         // if entering, then you know it's wrong already
         if (rd[dir][0] == rid || rd[dir][1] == rid) {
            return false;
         }
      }
      return (answer >= 0);
   }

   // true if this route enters but does not leave junction.
   public boolean isRouteEnd(int rid) {
      int answer = -1;
      for (int dir = 0; dir < rd.length; dir++ ) {
         if (rd[dir][0] == rid || rd[dir][1] == rid) {
            answer = dir;
         }
         // if leaving, then you know it's wrong already
         if (rd[dir][0] == (rid | forwardMask) || 
             rd[dir][1] == (rid | forwardMask)) {
            return false;
         }
      }
      return (answer >= 0);
   }

   // does the route leave diagonally?
   // used for determining true mileage in this square.
   public boolean ridLeavesDiagonally(int rid) {
      return (getForwardDirection(rid) & 1) > 0;
   }

   // does this junction contains the given rid in any direction?
   public boolean containsRoute(int rid) {
      rid &= ridMask;
      for (int dir = 0; dir < rd.length; dir++ ) {
         if ((rd[dir][0] & ridMask) == rid || 
             (rd[dir][1] & ridMask) == rid) {
            return true;
         }
      }
      return false;
   }

   // true if two or more routes meet here.
   boolean isRouteJunction() {
      int ridSeen = 0;
      for (int i = 0; i < numDirs; i++) {
         for (int j = 0; j <= 1; j++) {
            if (rd[i][j] != 0) {
               if (ridSeen == 0) {
                  ridSeen = rd[i][j] & ridMask;
               }
               else {
                  if (ridSeen != (rd[i][j] & ridMask)) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   // true if junction has anything but a single route
   // that enters and leaves, or paired routes that
   // stay overlapped
   // used for mapping
   boolean isRouteInflection() {
      int ridLegs[] = new int[numDirs];
      int numRidLegs = 0;

      // count and record legs with routes
      for (int i = 0; i < numDirs; i++) {
         if (rd[i][0] > 0 || rd[i][1] > 0) {
            ridLegs[numRidLegs++] = i;
         }
      }

      // 1 or three route legs, etc => must be inflection
      if (numRidLegs != 2) {
         return true;
      }

      // now make sure max of 2 numbers are both seen twice
      for (int i = 0; i < numRidLegs; i++) {
         int rid00 = rd[ridLegs[0]][0] & ridMask;
         int rid01 = rd[ridLegs[0]][1] & ridMask;
         int rid10 = rd[ridLegs[1]][0] & ridMask;
         int rid11 = rd[ridLegs[1]][1] & ridMask;
         //System.out.println("rids: " + rid00 + rid01 + rid10 + rid11);
         
         // rids on 2nd leg must match first, either order
         if (rid10 != 0 && rid10 != rid01 && rid10 != rid00) {
            return true;
         }
         
         if (rid11 != 0 && rid11 != rid01 && rid11 != rid00) {
            return true;
         }
      }
      return false;
   }

   // is there no more room to buddy up another route here?
   boolean isFull(int direction) {
      return ((rd[direction][1] & ridMask )!= none);
   }

   // rid mutators --------

   public void clearRids(int direction) {
      rd[direction][0] = rd[direction][1] = 0;
   }

   // add a rid at a given direction. Leave pavement alone.
   // if leg is full, do not add rid.
   // do not add duplicate to a rid that's there.
   private void addRid(int dir, int rid, boolean isForward) {
      short forwardBit = isForward ? forwardMask : 0;

      if (rd[dir][0] == 0)
      {
         rd[dir][0] = (short) (rid + forwardBit);
         return;
      }
      if (rd[dir][1] == 0 && rd[dir][0] != rid)
      {
         rd[dir][1] = (short) (rid + forwardBit);
      }
      // else do nothing
   }

   // adds a rid at a given dir
   void addRidForward(int dir, int rid) {
      addRid(dir, rid, true);
   }

   // adds a rid at opposite dir
   void addRidBack(int dir, int rid) {
      int backDir = getGlobalDirection(dir, back);
      addRid(backDir, rid, false);
   }

   // remove only route numbering at a given dir.
   // doesn't matter if rid is forward or backward.
   void removeRid(int dir, int rid) {
      if ((rd[dir][1] & ridMask) == rid) {
         rd[dir][1] = 0;
      }
      if ((rd[dir][0] & ridMask) == rid)
      {
         rd[dir][0] = rd[dir][1];
         rd[dir][1] = 0;
      }
   }

   /* =====
      operations for street indexes (sids).
   */
   public final int sidAt(int direction) {
      return sd[direction] & ridMask;
   }
         
   /* ===
      set the street id for a leg.
      Optionally overwrite existing sid; otherwise leave it
      alone.
   */
   void setStreetId(int direction, int sid, boolean forceIt) {
      if (sd[direction] == 0 || forceIt) {
         sd[direction] = (short) sid;
      }
   }

   void setStreetId(int direction, int sid) {
      setStreetId(direction, sid, true);
   }

   // add a street at a given direction.
   // Can widen but not narrow existing street.
   // doesn't check for existing; that's up to you!
   private void addStreet(int dir, int sid, int paveType, boolean isForward) {
      int existingRoad = pavementAt(dir);
      short forwardBit = isForward ? forwardMask : 0;

      sd[dir] = (short) (sid + forwardBit);
      
      if (existingRoad < paveType)
         setPavement(dir, paveType);
   }

   // remove a street at a given dir; take out pavement
   private void removeStreet(int dir) {
      sd[dir] = 0;
      setPavement(dir, none);
   }

   // adds a street at a given dir
   void addStreetForward(int dir, int sid, int paveType) {
      addStreet(dir, sid, paveType, true);
   }

   // adds a street at opposite dir
   void addStreetBack(int dir, int sid, int paveType) {
      int backDir = getGlobalDirection(dir, back);
      addStreet(backDir, sid, paveType, false);
   }

   // removes a street at a given direction
   void removeStreetForward(int dir) {
      removeStreet(dir);
   }

   // removes a street at opposite direction
   void removeStreetBack(int dir) {
      int backDir = getGlobalDirection(dir, back);
      removeStreet(backDir);
   }

   // >= 0 if exists a direction with road to the left
   public int roadToLeftDir(int aheadDir) {
      for (int i = -1; i >= -3; i--) {
         int tryDir = getGlobalDirection(aheadDir, i);
         if (pavementAt(tryDir) > 0) {
            return tryDir;
         }
      }
      return -1;
   }

   // >= 0 if exists a direction with road to the right
   public int roadToRightDir(int aheadDir) {
      for (int i = 1; i <= 3; i++) {
         int tryDir = getGlobalDirection(aheadDir, i);
         if (pavementAt(tryDir) > 0) {
            return tryDir;
         }
      }
      return -1;
   }

   // return the type of pavement at a compass direction
   public int pavementAt(int direction) {
      int mask = layoutMask[direction];
      return (layout & (mask * 7)) / mask;
   }

   // return the type of road given your heading and which way you are facing
   public int pavementAt(int heading, int facing) {
      return pavementAt(getGlobalDirection(heading, facing));
   }

   // faster than (pavementAt(dir) == 0)
   public boolean isEmpty(int direction) {
      int mask = layoutMask[direction];
      return (layout & (mask * 7)) == none;
   }

   public boolean isEmpty(int heading, int facing) {
      return isEmpty(getGlobalDirection(heading, facing));
   }

   // is road a dead end in the current square?  (only way is to turn back)
   // Dead end == only 1 non-empty segment
   boolean isDeadEnd() {
      int nonEmpties = 0;
      for (int i = 0; i < numDirs; i++) {
         if (!isEmpty(i)) {
            nonEmpties++;
            if (nonEmpties > 1) {
               return false;
            }
         }
      }
      return true;
   }

   // is there a road passing thru, any direction
   // not currently used
   boolean hasThruRoad() {
      int nonEmpties = 0;
      for (int i = 0; i < numDirs; i++) {
         if (!isEmpty(i)) {
            nonEmpties++;
            if (nonEmpties >= 2) {
               return true;
            }
         }
      }
      return false;
   }

   // what type of intersection is here?
   int intersectionType() {
      int nonEmpties = 0;
      int numTwoWays = 0;
      for (int i = 0; i < numDirs/2; i++) {
         if (!isEmpty(i) && !isEmpty(i+4)) {
            numTwoWays++;
         }
         if (!isEmpty(i)) {
            nonEmpties++;
         }
         if (!isEmpty(i+4)) {
            nonEmpties++;
         }
      }

      // now make an inference based on data
      switch (nonEmpties) {
         case 0: return empty;
         case 1: return deadend;
         case 2:
            if (numTwoWays > 0)
               return straight;
            else
               return curve;
         case 3: return threeway;
         case 4: return fourway;
      }
      return unknown;
   }

   // used to find good spot for starting a new road.
   boolean isTwoWay() {
      return (intersectionType() & twoway) > 0;
   }

   // special case for rendering (two roadways)
   boolean isTwoWayDivided() {
      if (!isTwoWay()) {
         return false;
      }
      for (int i = 0; i < numDirs; i++) {
         if (!isEmpty(i) && !isDivided(i)) {
            return false;
         }
      }
      return true;
   }

   // used to find good spot for starting a new road.
   boolean isThreeWayOrMore() {
      return intersectionType() >= threeway;
   }

   // is it ok to add outgoing leg in this direction?
   boolean isSuitableForLeg(int dir) {
      int numTwoWays = 0; // number of leg pairs 180 deg apart
      int numLegs = 0; // number of legs including proposed one
      for (int i = 0; i < numDirs/2; i++) {
         if (!isEmpty(i) || !isEmpty(i+4) || dir == i || dir == i+4) {
            numTwoWays++;
         }
         if (!isEmpty(i) || dir == i) {
            numLegs++;
         }
         if (!isEmpty(i+4) || dir == i+4) {
            numLegs++;
         }
      }

      // special case for Y-type intersections;
      // can't have 3 legs within same 179 deg of any circle.
      if (numTwoWays > 2 & numLegs == 3) {
         int legAt[] = new int [3];
         int diffs[] = new int [3];
         int which = 0;
         for (int i = 0; i < numDirs; i++) {
            if (!isEmpty(i) || dir == i) {
               legAt[which++] = i;
            }
         }
         diffs[0] = legAt[1] - legAt[0];
         diffs[1] = legAt[2] - legAt[1];
         diffs[2] = legAt[0] - legAt[2] + numDirs;
         return (diffs[0] + diffs[1] >= 4 &&
             diffs[1] + diffs[2] >= 4 &&
             diffs[2] + diffs[0] >= 4);
      }

      // non-special case
      return (numTwoWays <= 2 || numLegs <= 3);
   }

   /* ===
      set the type of pavement at a compass direction.
      Overwrites any existing pavement type.
   */
   void setPavement(int direction, int paveType) {
      int mask = layoutMask[direction];
      layout = (layout & ~(7*mask)) | (mask * paveType);
   }

   // set the pavement only if existing is narrower.
   void setWiderPavement(int dir, int paveType) {
      if (pavementAt(dir) < paveType) {
         setPavement(dir, paveType);
      }
   }
   
   boolean isDivided(int dir) {
      switch (pavementAt(dir)) {
         case div4: case div6: return true;
      }
      return false;
   }

   static boolean isPaveTypeDivided(int what) {
      switch(what) {
         case div4: case div6: return true;
      }
      return false;
   }

   int countLanes(int dir) {
      switch (pavementAt(dir)) {
         case div6: return 6;
         case div4: case undiv4: return 4;
         case dirt: case undiv2: return 2;
      }
      return 0;
   }

   // is it ok to turn in this direction? todo: needs work
   // handle freeways - might move isdivided() from squaredraw to Sq to make
   boolean accessOK(int heading, int facing) {
      switch (facing) {
         case left: case right: case ahead:
            return (pavementAt(heading,facing) > none);
         case back:
            if (intersectionType() == curve)
               return false;
            return (pavementAt(heading,facing) > none);
      }
      return false;
   }


   // describe an intersection, except route provided.
   // for example, if on rte 7, intersecting with rte 13,
   // say "Jct rte 13".
   String describe(Realm theRealm, int excludeRoute) {
      String desc = "";

      if (intersectionType() == deadend) {
         return "Dead end";
      }

      // look for overlap
      int fwd = getForwardDirection(excludeRoute);
      int rev = getBackwardDirection(excludeRoute);

      // see if forward overlap route
      int fwdOverlap = 0;
      if (fwd >= 0) {
          fwdOverlap = rd[fwd][0] & ridMask;
          if (fwdOverlap <= 0 || fwdOverlap == excludeRoute) {
             fwdOverlap = rd[fwd][1] & ridMask;
             if (fwdOverlap <= 0 || fwdOverlap == excludeRoute) {
                fwdOverlap = 0; // nothing
             }
          }
      }
      // see if reverse overlap route
      int revOverlap = 0;
      if (rev >= 0) {
          revOverlap = rd[rev][0] & ridMask;
          if (revOverlap <= 0 || revOverlap == excludeRoute) {
             revOverlap = rd[rev][1] & ridMask;
             if (revOverlap <= 0 || revOverlap == excludeRoute) {
                revOverlap = 0; // nothing
             }
          }
      }

      if (fwdOverlap > 0 && fwdOverlap != revOverlap) {
         desc += "Begin overlap rte " + theRealm.routes[fwdOverlap].getNumber() + " ";
      }
      if (revOverlap > 0 && fwdOverlap != revOverlap) {
         desc += "End overlap rte " + theRealm.routes[revOverlap].getNumber() + " ";
      }

      int routesSeen[] = new int[numDirs*2];
      int numRoutesSeen = 0; // # routes seen

      // get a list of routes in other legs of junction
      for (int i = 0; i < rd.length; i++) {
         // skip overlap directions
         if (i == fwd || i == rev) {
            continue;
         }
         for (int which = 0; which <= 1; which++) {
            int rid = rd[i][which] & ridMask;
            if (rid > 0 && rid != fwdOverlap && rid != revOverlap) {
               boolean found = false;
               for (int j = 0; j < numRoutesSeen; j++) {
                  if (rid == routesSeen[j]) {
                     found = true;
                     break;
                  }
               }
               if (!found) {
                  routesSeen[numRoutesSeen++] = rid;
               }
            }
         }
      }

      // list any nonoverlapping routes found
      if (numRoutesSeen > 0) {
         desc += "Jct";
         for (int j = 0; j < numRoutesSeen; j++) {
            desc += " rte " + theRealm.routes[routesSeen[j]].getNumber();
         }
      }

      // check for streets if empty desc so far
      if (desc.length() == 0) {
         int seenSid[] = new int[numDirs];
         int nSeen = 0;
         boolean seenIt = false;
         for (int i = 0; i < numDirs; i++) {
            if (i != fwd && i != rev) {
               int sid = sidAt(i);
               if (sid > 0) {
                  seenIt = false;
                  for (int j = 0; j < nSeen; j++) {
                     if (sid == seenSid[j]) {
                        seenIt = true;
                        break;
                     }
                  }
                  if (!seenIt) {
                     desc += theRealm.streetNames[sid] + " ";
                     seenSid[nSeen++] = sid;
                  }
               }
            }
         }
      }

      return desc;
   }

   void dump() {
      System.out.println("junc dump:");
      for (int i = 0; i < numDirs; i++) {
         int rid0 = rd[i][0] & ridMask;
         int rid1 = rd[i][1] & ridMask;
         int sid = sd[i] & ridMask;
         char fwd0 = (rd[i][0] & forwardMask) > 0 ? '>' : '<';
         char fwd1 = (rd[i][1] & forwardMask) > 0 ? '>' : '<';
         char fwds = (sd[i] & forwardMask) > 0 ? '>' : '<';
         System.out.print(dirAbbs[i] + ": ");
         if (pavementAt(i) > 0) {
            System.out.print("p" + pavementAt(i) + " ");
            System.out.print("rd's: " + rid0 + fwd0);
            System.out.print(", " + rid1 + fwd1 + "; ");
            System.out.println("sd: " + sid + fwds);
         }
         else {
            System.out.println("");
         }
      }
   }

}
