package com.kurumi.matr;

import com.kurumi.matr.XyzPoint;

/**
 * Component of a polygon. Used in intersection viewer.
 * @author soglesby
 *
 */
public class VEdge {
   public XyzPoint near, far;

   VEdge(XyzPoint near_, XyzPoint far_) {
      near = new XyzPoint(near_);
      far = new XyzPoint(far_);
   }
   
   VEdge(VEdge you) {
      near = new XyzPoint(you.near);
      far = new XyzPoint(you.far);
   }

   public double mag() {
      return Math.sqrt(mag2());
   }

   public double mag2() {
      double x = (far.x - near.x);
      double z = (far.z - near.z);
      return x*x + z*z;
   }

   @Override
   public String toString() {
	   return "near: " + near + "   far: " + far;
   }
}

