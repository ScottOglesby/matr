package com.kurumi.matr;

import java.awt.*;
/* =========
   Show a small sample map to allow user
   to see the effects of different font sizes.
*/
public class PMapSample extends Canvas {

   private Point xyTown;
   private Point xyRoute1;
   private Point xyRoute2;
   private static Dimension mySize = new Dimension(120, 100);

   PMapSample() {
      setBackground(Color.white);
      xyTown = new Point(mySize.width/2, mySize.height/2);
      xyRoute1 = new Point(3*mySize.width/5, 3*mySize.height/4);
      xyRoute2 = new Point(mySize.width/5, 3*mySize.height/8);
   }

   @Override
public Dimension preferredSize() {
      return mySize;
   }

   @Override
public Dimension minimumSize() {
      return preferredSize();
   }

   @Override
public void paint(Graphics g) {
      // border
      g.setColor(Color.black);
      g.drawRect(0, 0, mySize.width, mySize.height);
      // roads
      g.drawLine(0, xyRoute1.y, mySize.width, xyRoute1.y);
      g.drawLine(xyRoute2.x, 0, xyRoute2.x, mySize.height);
      // markers
      MapUtils.drawMarker(g, xyRoute1.x, xyRoute1.y, 130);
      MapUtils.drawMarker(g, xyRoute2.x, xyRoute2.y, 8, 25);
      // town
      g.setColor(Color.blue);
      g.setFont(Empire.townFont);
      g.drawOval(xyTown.x-7, xyTown.y-2, 4, 4);
      g.drawString("Tokyo", xyTown.x, xyTown.y);
   }

}
