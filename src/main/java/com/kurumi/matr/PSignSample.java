package com.kurumi.matr;

import java.awt.*;
/* =========
   Show a small sample sign (route and street) to allow user
   to see the effects of different font sizes.
*/
public class PSignSample extends Canvas {

   private static Dimension mySize = new Dimension(120, 100);
   Signpost sign = new Signpost(0, 0);
   TextSign street = new TextSign(0,0);

   PSignSample() {
      setBackground(IntersectionViewerClient.skyColor);
      sign.setRoutes(Junction.north, 127, -1, 0, SignUtils.leftArrow);
      street.add("Lingwoo Ave", SignUtils.aheadArrow | SignUtils.rightArrow);
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

      sign.setLocation(mySize.width/2, mySize.height + 24);
      street.setLocation(mySize.width/2, mySize.height - 20);
      sign.draw(g);
      street.draw(g);
   }

}
