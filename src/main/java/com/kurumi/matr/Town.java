package com.kurumi.matr;

import java.awt.Point;

/**
 * Represents a town (name and set of tiles) in the Realm.
 * @author soglesby
 *
 */
public class Town {

   public final static int typicalArea = 300;  // in squares
 
   // # squares before you start accretion
   public final static int coreArea = 14;

   private String name;
   private Point center = new Point(0,0);
   private int area;

   public Point getCenter() {
	   return center;
   }

   public void setCenter(Point center) {
	   this.center = center;
   }

   public int getArea() {
	   return area;
   }

   public void setArea(int area) {
	   this.area = area;
   }
   
   /**
    * Adding a tile to a town? Increment its area
    */
   public void incrementArea() {
	   this.area++;
   }

   Town(String name) {
	   this.name = name;
   }

   void setCenter(int x, int y) {
	   center.setLocation(x, y);
   }

   public String getName() {
	   return name;
   }

   public void setName(String name) {
	   this.name = name;
   }
}
