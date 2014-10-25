package com.kurumi.matr;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
/* =========
   Static class only.
   Knows how to draw various cartographic symbols for maps.
*/
public class MapUtils {

   // default to black on white markers
   private static Font medRouteNumberFont =
      new Font("Helvetica", Font.PLAIN, 12);
   private static Font smallRouteNumberFont =
      new Font("Helvetica", Font.PLAIN, 10);
   private static Color bgMarkerColor = Color.white;
   private static Color fgMarkerColor = Color.black;

   // lightgray.brighter() doesn't show up at all on Unix
   public static Color dirtRoadColor = new Color(153,153,102);
   // would like dirt for new Color(102,64,32);
   public static Color streetColor = Color.gray;
   public static Color highwayColor = Color.red.darker();
   //public static Color freewayColor = Color.red.darker();
   public static Color townLineColor = Color.lightGray;
   public static Color townNameColor = Color.blue;
   public static Color selectionColor = Color.green;
   public static Color oceanColor = Color.cyan.brighter();

   // ======= specify a font
   public static final int medRoute = 1;
   public static final int smallRoute = 2;

   public static void setFont(int which, Font f) {
      switch (which) {
         case medRoute:
            medRouteNumberFont = f;
            break;
         case smallRoute:
            smallRouteNumberFont = f;
            break;
      }
   }

   public static Font getFont(int which) {
      switch (which) {
         case medRoute:
            return medRouteNumberFont;
         case smallRoute:
            return smallRouteNumberFont;
      }
      return null;
   }

   // ======= specify a color
   public static final int background = 1;
   public static final int foreground = 2;  // foreground
   
   public static void setMarkerColor(int which, Color c) {
      switch (which) {
         case background:
            bgMarkerColor = c;
            break;
         case foreground:
            fgMarkerColor = c;
            break;
      }
   }

   // draw single marker at screen x and y
   /**
    * Draws a single route marker centered at (xc, yc)
    * @param g       Graphics object
    * @param xc      X position in frame coordinates
    * @param yc      Y position in frame coordinates
    * @param number  Route number to display
    */
   public static void drawMarker(Graphics g, int xc, int yc, int number) {
	   Graphics2D g2 = (Graphics2D)g;
	   RenderingHints hints = new RenderingHints(
			   RenderingHints.KEY_ANTIALIASING,
			   RenderingHints.VALUE_ANTIALIAS_ON);
	   g2.setRenderingHints(hints);
	   g2.setFont(medRouteNumberFont);
	   
	   // calculate marker size and position based on route number and font
	   // units are frame coordinates
	   FontMetrics fm = g2.getFontMetrics();
	   String s = String.valueOf(number);
	   int rw = fm.stringWidth(s);
	   int rh = fm.getHeight();
	   // determine padding for number inside marker
	   int paddingX = rh / 4;
	   int paddingY = rh / 4;
	   // fudge factors for the way things look now
	   int dyr = -1;  // move numeral up about 3 px
	   int dxm = -1; // move marker left about 3 px
	   // and marker startx, starty
	   int mx = xc - (rw/2) - paddingX + dxm;
	   int my = yc - (rh/2) - paddingY;
	   int mw = rw + 2 * paddingX;
	   int mh = rh + 2 * paddingY;
	   

	   g2.setColor(bgMarkerColor);
	   g2.fillOval(mx, my, mw, mh);
	   g2.setColor(fgMarkerColor);
	   g2.drawOval(mx, my, mw, mh);
	   g2.drawString(s, xc - rw/2, yc+rh/2+dyr);
   }

   // draw double marker at screen x and y
   public static void drawMarker(Graphics g, int xc, int yc, int num0, int num1) {
      g.setFont(smallRouteNumberFont);
      FontMetrics fm = g.getFontMetrics();
      String s0 = String.valueOf(num0);
      String s1 = String.valueOf(num1);
      int sw0 = fm.stringWidth(s0);
      int sw1 = fm.stringWidth(s1);      
      int sw = Math.max(sw0, sw1);
      int dh = fm.getHeight();

      g.setColor(bgMarkerColor);
      g.fillOval(xc-sw/2 - 5, yc-dh-2, sw+8, dh*2+5);
      g.setColor(fgMarkerColor);
      g.drawOval(xc-sw/2 - 5, yc-dh-2, sw+8, dh*2+5);
      g.drawString(s0, xc - sw0/2-1, yc-1);
      g.drawString(s1, xc - sw1/2-1, yc+dh-2);
   }

   // draw 2 parallel lines. works great!
   // however, diag lines are staggered, make them fit smooth.
   // problem hits single lines, too.
   // NE lines ok.
   // diagonal lines are 1.5 diagonal squares apart; 1.5*sqrt(2) ~ 2.
   private static int dx2l[] = {-1, -1,  0,  0, -1,  0,  0,  1};
   private static int dy2l[] = { 0,  0, -1, -1,  0, -1, -1,  0};
   private static int dx2r[] = { 1,  1,  0, -1,  1,  1,  0, -1};
   private static int dy2r[] = { 0,  1,  1,  1,  0,  1,  1,  1};

   public static void drawDoubleLine(Graphics g, int dir, 
                               int x0, int y0, int x1, int y1) {
      int x2, y2, x3, y3;
      x2 = x0 + dx2l[dir];
      y2 = y0 + dy2l[dir];
      x3 = x1 + dx2l[(dir + 4)%8];
      y3 = y1 + dy2l[(dir + 4)%8];
      g.drawLine(x2, y2, x3, y3);
      x2 = x0 + dx2r[dir];
      y2 = y0 + dy2r[dir];
      x3 = x1 + dx2r[(dir + 4)%8];
      y3 = y1 + dy2r[(dir + 4)%8];
      g.drawLine(x2, y2, x3, y3);
   }

   /* ======
      draw a single segment of road.
      Specify color and dividedness.
   */
   public static void drawRoadSegment(Graphics g, 
                                Color lineColor, boolean isDivided,
                                int dir, int xc, int yc, int x1, int y1) {
	   Graphics2D g2 = (Graphics2D)g;
	   RenderingHints hints = new RenderingHints(
			   RenderingHints.KEY_ANTIALIASING,
			   RenderingHints.VALUE_ANTIALIAS_ON);
	   g2.setRenderingHints(hints);
	   
	   if (isDivided) {
            g2.setColor(Color.white);
            g2.drawLine(xc, yc, x1, y1);
            g2.setColor(lineColor);
            drawDoubleLine(g2, dir, xc, yc, x1, y1);
      }
      else {
            g2.setColor(lineColor);
            g2.drawLine(xc, yc, x1, y1);
      }
   }

   // length of hash based on N..NW direction, how x and y are affected
   private static int gdx[] = { 0,  4, 5, 4, 0, -4, -5, -4};
   private static int gdy[] = {-5, -4, 0, 4, 5,  4,  0, -4};
   // hash offset from line denoting road
   private static int gxl[] = {-2, -2,  0,  2,  2,  2,  0, -2};
   private static int gyl[] = { 0, -2, -2, -2,  0,  2,  2,  2};
   // note that right values = -left values


   public static void drawGradeSeps(Graphics g, int dir, int xc, int yc,
                             boolean divided) {
      // these are x and y for left and right of upper rdway
      // you are facing direction "dir"
      int xl = xc;
      int xr = xc;
      int yl = yc;
      int yr = yc;
      // values shift outward if upper rdway is divided
      if (divided) {
         xl += dx2l[dir];
         yl += dy2l[dir];
         xr += dx2r[dir];
         yr += dy2r[dir];
      }
      int rev = (dir+4)%8; // opposite direction
      // now set 8 points of grade sep: front 'f' and back 'b'
      int xlb = xl + gdx[dir] + gxl[dir];
      int ylb = yl + gdy[dir] + gyl[dir];
      int xlf = xl + gdx[rev] - gxl[rev];
      int ylf = yl + gdy[rev] - gyl[rev];
      int xrf = xr + gdx[rev] - gxl[dir];
      int yrf = yr + gdy[rev] - gyl[dir];
      int xrb = xr + gdx[dir] + gxl[rev];
      int yrb = yr + gdy[dir] + gyl[rev];

      // first, white out the overpass area
      // make sure left and right are set properly!
      // works on bridges going 0 -> 1 is N, NE, E, or SE
      Polygon po = new Polygon();
      po.addPoint(xlb, ylb);
      po.addPoint(xlf, ylf);
      po.addPoint(xrf, yrf);
      po.addPoint(xrb, yrb);
      g.setColor(Color.white);
      g.fillPolygon(po);

      // draw the hash marks
      g.setColor(Color.gray);
      g.drawLine(xlb, ylb, xlf, ylf);
      g.drawLine(xrb, yrb, xrf, yrf);
   }
   
   // draw a box representing an interchange
   static int boxHalfLength = 3;
   static int boxHalfDiag = 4;
   public static void drawInterchangeBox(Graphics g, int xc, int yc,
                                         boolean diagonal)
   {
      if (diagonal) {
         Polygon po = new Polygon();
         po.addPoint(xc, yc - boxHalfDiag);
         po.addPoint(xc + boxHalfDiag, yc);
         po.addPoint(xc, yc + boxHalfDiag);
         po.addPoint(xc - boxHalfDiag, yc);
         g.setColor(Color.white);
         g.fillPolygon(po);
         g.setColor(Color.black);
         g.drawPolygon(po);
      }
      else {
         g.setColor(Color.white);
         g.fillRect(xc - boxHalfLength, yc - boxHalfLength,
                    2 * boxHalfLength, 2 * boxHalfLength);
         g.setColor(Color.black);
         g.drawRect(xc - boxHalfLength, yc - boxHalfLength,
                    2 * boxHalfLength, 2 * boxHalfLength);
      }
   }
}
