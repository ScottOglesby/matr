package com.kurumi.matr;

import java.util.Vector;

import com.kurumi.matr.VEdge;

/**
 * Add radials to a polygon, automatically adding vertices
 * at the intersection points.
 * Handle double radials as well.
 * Used for intersection viewer.
 * @author soglesby
 *
 */
public class VPoly {
   private int maxRadials = 8;

   private VEdge firstEdge;  // inbound of first radial
   private VEdge lastEdge;   // outbound of most recent radial

   // handle smart capping
   private XyzPoint inCorner[]  = new XyzPoint[maxRadials];
   private XyzPoint outCorner[]  = new XyzPoint[maxRadials];
   private VEdge inMeds[] = new VEdge[maxRadials];
   private VEdge outMeds[] = new VEdge[maxRadials];
   private int insertPoint[] = new int[maxRadials];
   private int nDoubles = 0;

   // store polygon of 3-D points
   public Vector<XyzPoint> v = new Vector<XyzPoint>();

   VPoly() { }

   public boolean isEmpty() { return v.isEmpty(); }


   // inbound intersecting common to single and double radials
   private void addCommon(VEdge inbound) {
      // if first one, save firstEdge, else connect to previous
      if (isEmpty()) {
         firstEdge = new VEdge(inbound);
      }
      else {
         connect(inbound);
         // if last double is waiting for an out corner, provide it
         if (nDoubles >= 1 && outCorner[nDoubles-1] == null) {
            outCorner[nDoubles-1] = intersectLast(inbound);
         }
      } 
   }

   // add single radial
   public void add(VEdge inbound, VEdge outbound) {
      addCommon(inbound);
      v.addElement(new XyzPoint(inbound.far));
      v.addElement(new XyzPoint(outbound.far));
      lastEdge = new VEdge(outbound);
   }

   // add double radial
   public void add(VEdge inbound, VEdge inMedian,
                   VEdge outMedian, VEdge outbound) {
      if (!isEmpty()) {
         inCorner[nDoubles] = intersectLast(inbound);
      }
      addCommon(inbound);

      v.addElement(new XyzPoint(inbound.far));
      v.addElement(new XyzPoint(inMedian.far));
      // cap will go here; save information
      inMeds[nDoubles] = new VEdge(inMedian);
      outMeds[nDoubles] = new VEdge(outMedian);
      insertPoint[nDoubles] = v.size();
      nDoubles++;
      // end cap processing
      v.addElement(new XyzPoint(outMedian.far));
      v.addElement(new XyzPoint(outbound.far));      
      lastEdge = new VEdge(outbound);
   }

   // must do this in reverse, otherwise insert points will move
   public void makeCaps() {
      for (int i = nDoubles - 1; i >= 0; i--) {
         // find appropriate corner to use
         // need closest to average of far points
         boolean useInCorner;
         // System.out.println("i = " + i);
         if (inCorner[i] == null && outCorner[i] == null) {
//              System.out.println("both corners NULL");
             XyzPoint inp = inMeds[i].near.midPointTo(outMeds[i].near);
             v.insertElementAt(along(inMeds[i], inMeds[i].near, 8),
                               insertPoint[i]);
             v.insertElementAt(inp, insertPoint[i]+1);
             v.insertElementAt(along(outMeds[i], outMeds[i].near, 8),
                               insertPoint[i]+2);
             continue;
         }
         else if (inCorner[i] == null) {
            useInCorner = false;
//             System.out.println("in corner NULL");
         }
         else if (outCorner[i] == null) {
            useInCorner = true;
//             System.out.println("out corner NULL");
         }
         else {
            XyzPoint middleOut = inMeds[i].far.midPointTo(outMeds[i].far);
            double inDist = middleOut.distanceTo(inCorner[i]);
            double outDist = middleOut.distanceTo(outCorner[i]);
//             System.out.println("middle out: " + middleOut);
//             System.out.println("in corner: " + inCorner[i]);
//             System.out.println("out corner: " + outCorner[i]);
//             System.out.println("in distance: " + inDist);
//             System.out.println("out distance: " + outDist);
            useInCorner = (inDist < outDist);
         }
         
         // find intersect points
         XyzPoint corner = useInCorner ? inCorner[i] : outCorner[i];
         XyzPoint incap = perpendicular(inMeds[i], corner);
         XyzPoint outcap = perpendicular(outMeds[i], corner);

         v.insertElementAt(along(inMeds[i], incap, 8), insertPoint[i]);
         v.insertElementAt(incap.midPointTo(outcap), insertPoint[i]+1);
         v.insertElementAt(along(outMeds[i], outcap, 8), insertPoint[i]+2);
      }
   }


   // call this when done adding radials. It closes the last vertex.
   public void close() {
      if (v.isEmpty()) {
         return;
      }
      // intersection: works with single node as well,
      // because lines are parallel
      connect(firstEdge);
      if (nDoubles > 0 && inCorner[0] == null) {
         inCorner[0] = intersectLast(firstEdge);
      }
      if (nDoubles > 0 && outCorner[nDoubles-1] == null) {
         outCorner[nDoubles-1] = intersectLast(firstEdge);
      }
      makeCaps();
   }
      
   // connect an edge to lastEdge.
   // do a fillet if cosine is non-negative (angle <= 90).
   private void connect(VEdge edge) {
      XyzPoint p = intersectLast(edge);
      if (p == null) {
         // straight merge. Taper from center to smaller size.
         if (lastEdge.near.mag2() > edge.near.mag2()) {
            v.addElement(new XyzPoint(lastEdge.near));
            v.addElement(along(edge, edge.near, 24));
         }
         else {
            v.addElement(along(lastEdge, lastEdge.near, 24));
            v.addElement(new XyzPoint(edge.near));
         }
      }
      else {
         if (cosine(lastEdge, edge) > .5) {
            v.addElement(along(lastEdge, p, 12));
            v.addElement(along(edge, p, 12));
         }
         else if (cosine(lastEdge, edge) > -0.01) {
            v.addElement(along(lastEdge, p, 4));
            v.addElement(along(edge, p, 4));
         }
         else {
            v.addElement(new XyzPoint(p));
         }
      }
   }


   // dot product of lengths
   private static long dotxz(VEdge left, VEdge right) {
      return (left.far.x - left.near.x) * (right.far.x - right.near.x) +
         (left.far.z - left.near.z) * (right.far.z - right.near.z);
   }

   // cosine of the angle between them
   // a dot b = |a||b| cos theta => cos theta = a dot b/|a||b|
   private static double cosine(VEdge left, VEdge right) {
      return dotxz(left, right)/(left.mag()*right.mag());
   }
      
   // move x feet from near point on this edge
   private static XyzPoint along(VEdge e, XyzPoint isect, int offset) {
      double len = e.mag();
      int xa = (int) (isect.x + (offset/len)*(e.far.x - isect.x));
      int za = (int) (isect.z + (offset/len)*(e.far.z - isect.z));
//       System.out.println("edge: " + e);
//       System.out.println("isect: " + isect);
//       System.out.println("x, z: " + xa + ", " + za);
      return new XyzPoint(xa, 0, za);
   }

   // same as above, but proportional to length
   // TODO: will we ever use this?
   @SuppressWarnings("unused")
   private static XyzPoint alongProp(VEdge e, XyzPoint isect, double prop) {
	   double len = e.mag();
	   return along(e, isect, (int) (prop * len));
   }

   // find a point on 'edge' where a perpendicular line would
   // intersect 'offpoint'
   private static XyzPoint perpendicular(VEdge edge, XyzPoint offPoint) {
      XyzPoint a = edge.near;
      XyzPoint b = edge.far;
      XyzPoint c = offPoint;
      double len2 = edge.mag2();
      double rnum = (a.z - c.z)*(a.z - b.z) - (a.x - c.x)*(b.x - a.x);
      double xi = a.x + (rnum/len2)*(b.x - a.x);
      double zi = a.z + (rnum/len2)*(b.z - a.z);
      return new XyzPoint((int)xi, 0, (int)zi);
   }

   // don't feed it parallel lines
   // works as 2-d only
   private XyzPoint intersectLast(VEdge edge) {
      double xi, zi;
      XyzPoint a = lastEdge.near;
      XyzPoint b = lastEdge.far;
      XyzPoint c = edge.near;
      XyzPoint d = edge.far;

      double denom = (b.x - a.x)*(d.z - c.z) - (b.z - a.z)*(d.x - c.x);
      // if lines are parallel, denom == 0
      if (denom == 0) {
         //System.out.println(" ** no cross");
         return null;
      }

      double rnum = (a.z - c.z)*(d.x - c.x) - (a.x - c.x)*(d.z - c.z);

      double r = rnum/denom;
      xi = a.x + r*(b.x - a.x);
      zi = a.z + r*(b.z - a.z);
      //      System.out.println(" .. cross: " + xi + "," + zi);
      return new XyzPoint((int)xi, 0, (int)zi);
   }
}
