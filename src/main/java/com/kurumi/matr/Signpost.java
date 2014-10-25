package com.kurumi.matr;

import java.awt.*;

import com.kurumi.matr.Junction;
import com.kurumi.matr.Realm;
import com.kurumi.matr.SignUtils;

/**
 * Draw a rural sign assembly.
 * @author soglesby
 *
 */
class Signpost {
   private static final Dimension metaPanelSize = new Dimension(40, 18);
   private static final Dimension numberPanelSize = new Dimension(40, 36);
   private static final int spacing = 2;

   Point stakeLoc;
   int r1, r2;
   int d1, d2;
   int ar = 0;

   // TODO put this in Junction?
   // TODO this list partially matches Junction constants, and partially overloads them
   private static String dirText[] = {
      "NORTH", "JCT", "EAST", "BEGIN", "SOUTH", "END", "WEST" };

   Signpost(int x, int y) {
      stakeLoc = new Point(x, y);
   }

   public void setLocation(int x, int y) {
      stakeLoc.setLocation(x, y);
   }

   // more raw version of setRoutes.
   public void setRoutes(int d1_, int r1_, int d2_, int r2_, int ar_) {
      d1 = d1_;
      r1 = r1_;
      d2 = d2_;
      r2 = r2_;
      ar = ar_;
   }
   

   // given a junction, ahead direction, and facing (left, right, ahead)
   // search for appropriate stuff to display.
   public void setRoutes(Realm r, Point here, int aheadDir, int facing)
   {
      int realDir = -1; // not found
      Junction j = r.pToJ(here);
      d1 = d2 = -1;
      r1 = r2 = 0;
      ar = -1;

      switch (facing) {
         case Junction.left:
            ar = SignUtils.leftArrow;
            realDir = j.ridToLeftDir(aheadDir);  // may be -1
            break;
         case Junction.ahead:
            ar = SignUtils.aheadArrow;
            realDir = aheadDir;
            break;
         case Junction.right:
            ar = SignUtils.rightArrow;
            realDir = j.ridToRightDir(aheadDir);  // may be -1
            break;
      }
      if (realDir >= 0) {
         int tempDir;
         int rid = j.ridAt(realDir, 0);
         if (rid > 0) {
            tempDir = r.routes[rid].getLogDirection();
            r1 = r.routes[rid].getNumber();
            d1 = j.isRidFwdAt(realDir, 0) ? 
               tempDir : Junction.getReverseDirection(tempDir);
         }
         rid = j.ridAt(realDir, 1);
         if (rid > 0) {
            tempDir = r.routes[rid].getLogDirection();
            r2 = r.routes[rid].getNumber();
            d2 = j.isRidFwdAt(realDir, 1) ? 
               tempDir : Junction.getReverseDirection(tempDir);
         }
      }
   }
  
   // draw route # given center x, top y
   private static void drawRouteNum(Graphics2D g2, int xx, int yy, int routeNumber) {
	   FontMetrics numFm = g2.getFontMetrics(Empire.routeTabFont);
	   String s = String.valueOf(routeNumber);
	   int sw = numFm.stringWidth(s);
	   int dh = numFm.getHeight()/2 - 2;

	   g2.setFont(Empire.routeTabFont);
	   g2.setColor(Color.black);
	   g2.drawString(s, xx - sw/2, yy + numberPanelSize.height/2 + dh);
   }

   // draw direction given center x, top y
   private static void drawDirText(Graphics2D g2, int xx, int yy, int dir) {
	   FontMetrics dirFm = g2.getFontMetrics(Empire.dirTabFont);
	   String text = dirText[dir];
	   int sw = dirFm.stringWidth(text);
	   int dh = dirFm.getHeight()/2 - 3;

	   g2.setFont(Empire.dirTabFont);
	   g2.setColor(Color.black);
	   g2.drawString(text, xx - sw/2, yy + metaPanelSize.height/2 + dh);
   }

   public void draw(Graphics g1) {
	   Graphics2D g2 = MyTools.modernize(g1);
	   
       int y = stakeLoc.y - SignUtils.clearance;
       SignUtils.setColorScheme(SignUtils.route);

       // support
       SignUtils.drawPost(g2, stakeLoc.x, stakeLoc.y);
       
       // arrow
       if (ar > 0) {
          y -= metaPanelSize.height;
          SignUtils.drawFramedRect(g2, stakeLoc.x - metaPanelSize.width/2, 
                     y,  metaPanelSize);
          SignUtils.drawArrow(g2, stakeLoc.x, y + metaPanelSize.height/2, ar);
          y -= spacing;
       }

       // r 2
       if (r2 > 0) {
          y -= numberPanelSize.height;
          SignUtils.drawFramedRect(g2, stakeLoc.x - numberPanelSize.width/2, 
                     y,  numberPanelSize);
          drawRouteNum(g2, stakeLoc.x, y, r2);
          y -= spacing;
       }

       // meta 2
       if (d2 > -1 && d2 != d1) {
          y -= metaPanelSize.height;
          SignUtils.drawFramedRect(g2, stakeLoc.x - metaPanelSize.width/2, 
                     y,  metaPanelSize);
          drawDirText(g2, stakeLoc.x, y, d2);
          y -= spacing;
       }

       // r 1
       if (r1 > 0) {
          y -= numberPanelSize.height;
          SignUtils.drawFramedRect(g2, stakeLoc.x - numberPanelSize.width/2, 
                     y,  numberPanelSize);
          drawRouteNum(g2, stakeLoc.x, y, r1);
          y -= spacing;
       }
       
       // meta 1
       if (d1 > -1) {
          y -= metaPanelSize.height;
          SignUtils.drawFramedRect(g2, stakeLoc.x - metaPanelSize.width/2, 
                     y,  metaPanelSize);
          drawDirText(g2, stakeLoc.x, y, d1);
          y -= spacing;
       }
   }
}
