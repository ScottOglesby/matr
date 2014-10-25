package com.kurumi.matr;

import java.awt.*;
/* ==========
   Basic street/route sign functions:
   draw framed rectangles, arrows, etc.
*/

class SignUtils {
   public static final int postThick = 4;
   public static final int arrowWidth = 16;
   private static final int arrowOffset = 10;
   public static final int clearance = 48;
   public static final int margin = 2;

   // any 2 of these can be OR'ed together, using codes below
   public static final int noArrow = 0;
   public static final int leftArrow = 1;
   public static final int aheadArrow = 2;
   public static final int rightArrow = 4;

   public static final int leftAheadArrow = leftArrow | aheadArrow;
   public static final int leftRightArrow = leftArrow | rightArrow;
   public static final int aheadRightArrow = aheadArrow | rightArrow;
   
   // which sort of sign to draw?
   public static final int route = 1;
   public static final int street = 2;  

   // ======= specify a color
   public static final int bgStreet = 1;
   public static final int fgStreet = 2;
   public static final int bgRoute = 3;
   public static final int fgRoute = 4;
   private static Color bgStreetColor = Color.green.darker();
   private static Color fgStreetColor = Color.white;
   private static Color bgRouteColor = Color.white;
   private static Color fgRouteColor = Color.black;
   private static int colorScheme = street;
   
   public static void setColor(int which, Color c) {
      switch (which) {
         case bgStreet:
            bgStreetColor = c;
            break;
         case fgStreet:
            fgStreetColor = c;
            break;
         case bgRoute:
            bgRouteColor = c;
            break;
         case fgRoute:
            fgRouteColor = c;
            break;
      }
   }

   public static void setColorScheme(int what) {
      colorScheme = what;
   }

   // specify the font
   private static Font textFont = new Font("Helvetica", Font.PLAIN, 12);
   public static void setFont(Font f) {
      textFont = f;
   }

   public static Font getFont() {
      return textFont;
   }

   // replace missing Polygon.translate() from 1.1 to 1.0.2
   public static void translatePoly(Polygon p, int dx, int dy) {
      for (int i = 0; i < p.npoints; i++) {
         p.xpoints[i] += dx;
         p.ypoints[i] += dy;
      }
   }

   // draw an arrow in foreground color centered at xc and yc
   public static void drawArrow(Graphics2D g, int xc, int yc, int arr) {
      Polygon p;
      switch(arr) {
         case leftArrow:
            int x[] =  {6, -2, -2, -8, -2, -2, 6};
            int y[] =  {3,  3,  6,  0, -6, -3, -3};
            p = new Polygon(x, y, x.length);
            break;
         case aheadArrow:
            int xa[] =  {3, 3, 6, 0, -6, -3, -3};
            int ya[] =  {4, -1, -1, -7, -1, -1, 4};
            p = new Polygon(xa, ya, xa.length);
            // skooch downward
            translatePoly(p, 0, 1);
            break;
         case rightArrow:
            int xr[] =  {-6, 2,  2,  8,  2,  2, -6};
            int yr[] =  {3,  3,  6,  0, -6, -3, -3};
            p = new Polygon(xr, yr, xr.length);
            break;
         default:
            return;
      }
      g.setColor(colorScheme == street ? fgStreetColor : fgRouteColor);
      // p.translate(xc, yc);
      translatePoly(p, xc, yc);
      g.fillPolygon(p);
   }

   // draw string and arrow(s) given center x and y
   public static void drawStringWithArrow(Graphics2D g, int cx, int cy, 
                           String str, int arrow_) {
      FontMetrics myFm = g.getFontMetrics(textFont);
      int sw = myFm.stringWidth(str);
      int dh = myFm.getHeight()/2 - 2;
      
      switch(arrow_) {
         case leftArrow:
            cx += arrowWidth/2;
            drawArrow(g, cx - sw/2 - arrowOffset, cy, arrow_);
            break;
         case aheadArrow:
         case rightArrow:
            cx -= arrowWidth/2;
            drawArrow(g, cx + sw/2 + arrowOffset, cy, arrow_);
            break;
         case leftArrow | aheadArrow:
            drawArrow(g, cx - sw/2 - arrowOffset, cy, leftArrow);
            drawArrow(g, cx + sw/2 + arrowOffset, cy, aheadArrow);
            break;
         case leftArrow | rightArrow:
            drawArrow(g, cx - sw/2 - arrowOffset, cy, leftArrow);
            drawArrow(g, cx + sw/2 + arrowOffset, cy, rightArrow);
            break;
         case aheadArrow | rightArrow:
            drawArrow(g, cx - sw/2 - arrowOffset, cy, aheadArrow);
            drawArrow(g, cx + sw/2 + arrowOffset, cy, rightArrow);
            break;
      }

      g.setFont(textFont);
      g.setColor(colorScheme == street ? fgStreetColor : fgRouteColor);
      g.drawString(str, cx - sw/2, cy + dh);
   }

   // how many arrows are OR'ed together?
   // in other words, how many bits are set?
   public static int arrowCount(int arrow_) {
      int count = 0;
      while (arrow_ > 0) {
         if ((arrow_ % 2) != 0) {
            count++;
         }
         arrow_ /= 2;
      }
      return count;
   }

   public static void drawFramedRect(Graphics g, int x, int y, Dimension d)
   {
      g.setColor(colorScheme == street ? bgStreetColor : bgRouteColor);
      g.fillRect(x, y, d.width, d.height);
      g.setColor(colorScheme == street ? fgStreetColor : fgRouteColor);
      g.drawRect(x, y, d.width, d.height);
   }

   public static void drawPost(Graphics g, int centerX, int bottomY) {
      g.setColor(Color.gray);
      g.fillRect(centerX - postThick/2, bottomY - clearance, 
                 postThick, clearance);
   }
      
}
