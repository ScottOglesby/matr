package com.kurumi.matr;

import java.awt.*;

class TextSign {
   private static final int maxNames = 10;

   // instance vars and methods
   private int numNames = 0;
   private String name[] = new String[maxNames];
   private int arrow[] = new int[maxNames]; // SignUtils arrow codes
   private Point topCenter = new Point(0,0);

   TextSign(int x, int y) {
      topCenter.setLocation(x, y);
   }

   public void setLocation(int x, int y) {
       topCenter.setLocation(x, y);
   }

   // no limit to the number of names you can add
   public void add(String name_, int arrow_) {
      if (numNames < maxNames) {
         name[numNames] = new String(name_);
         arrow[numNames] = arrow_;
         numNames++;
      }
   }

   public void clear() {
      numNames = 0;
   }

   public boolean isEmpty() {
      return (numNames == 0);
   }

   // figure out the size of the sign based on text inside
   private Dimension calcSize(Graphics g) {
      int widest = 0, h = 0;
      FontMetrics fm = g.getFontMetrics(SignUtils.getFont());

      for (int i = 0; i < numNames; i++) {
         if (name[i].length() > 0) {
            int wThis = fm.stringWidth(name[i]);
            wThis += SignUtils.arrowCount(arrow[i]) * SignUtils.arrowWidth;
            widest = Math.max(widest, wThis);
         }
      }

      h = fm.getHeight() * numNames + (numNames + 1) * SignUtils.margin;
      widest += 4 * SignUtils.margin;
      return new Dimension(widest, h);
   }
      
   public void draw(Graphics g1) {
      if (numNames == 0) {
         return;
      }
      Graphics2D g2 = MyTools.modernize(g1);
      SignUtils.setColorScheme(SignUtils.street);

      Dimension d = calcSize(g2);
      SignUtils.drawFramedRect(g2, topCenter.x - d.width/2, topCenter.y, d);

      for (int i = 0; i < numNames; i++) {
         int yc = topCenter.y + (2 * i + 1) * d.height / (2 * numNames);
         SignUtils.drawStringWithArrow(g2, topCenter.x, yc, name[i], arrow[i]);
      }
   }
}
