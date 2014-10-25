package com.kurumi.matr;

/**
 * Utility class for a 3-D point. Used mainly in intersection viewer
 * @author soglesby
 *
 */
class XyzPoint {
   int x,y,z;

   XyzPoint(XyzPoint you) {
      x = you.x;
      y = you.y;
      z = you.z;
   }

   XyzPoint(int x_, int y_, int z_) {
      x = x_;
      y = y_;
      z = z_;
   }

   void set(XyzPoint you) {
      x = you.x;
      y = you.y;
      z = you.z;
   }
     
   /**
    * Find the squared distance of this point from (0,0,0)
    * It's only used for comparisons, so we can avoid calling sqrt()
    * @return squared distance of this point from (0,0,0)
    */
   double mag2() {
      return (x*x + y*y + z*z);
   }

   double distanceTo(XyzPoint other) {
      int dx = other.x - x;
      int dy = other.y - y;
      int dz = other.z - z;
      return Math.sqrt(dx*dx + dy*dy + dz*dz);
   }

   XyzPoint midPointTo(XyzPoint other) {
      return new XyzPoint((other.x + x)/2, (other.y + y)/2, (other.z + z)/2);
   }

   static XyzPoint[] makeXyz(int ax[], int ay[], int az[], int length) {
      XyzPoint[] result = new XyzPoint[length];
      for (int i = 0; i < length; i++) {
         result[i] = new XyzPoint(ax[i], ay[i], az[i]);
      }
      return result;
   }

   @Override
   public String toString() {
	   return "x: " + x + ", y: " + y + ", z: " + z;
   }

   static void dump(XyzPoint[] foo, int length) {
      for (int i = 0; i < length; i++) {
         System.out.println("Point " + i + ": " + foo[i]);
      }
      System.out.println("");
   }
}
