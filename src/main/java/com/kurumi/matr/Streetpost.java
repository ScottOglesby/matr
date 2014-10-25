package com.kurumi.matr;

import java.awt.*;
import com.kurumi.matr.Junction;
import com.kurumi.matr.Realm;
import com.kurumi.matr.SignUtils;

// display street sign
class Streetpost {
   Point stakeLoc;
   TextSign mySign = new TextSign(0,0);

   Streetpost(int x, int y) {
      stakeLoc = new Point(x, y);
   }

   public void setLocation(int x, int y) {
      stakeLoc.move(x, y);
   }

   // given a junction, ahead direction, and facing (left, right, ahead)
   // search for appropriate stuff to display.
   // if any two match, submit one name with double arrow
   // if only names are left and right, omit arrows.
   // only submit "ahead" name if it doesn't match behind.
   public void setSids(Realm r, Point here, int aheadDir)
   {
      int realDir;
      Junction j = r.pToJ(here);
      int sidLeft = 0, sidRight = 0, sidAhead = 0, sidBack = 0;
      mySign.clear();
      int numNamesAdded = 0;

      // grab street id's for comparison
      realDir = j.roadToLeftDir(aheadDir);
      if (realDir >= 0) {
         sidLeft = j.sidAt(realDir);
      }
      realDir = j.roadToRightDir(aheadDir);
      if (realDir >= 0) {
         sidRight = j.sidAt(realDir);
      }
      if (!j.isEmpty(aheadDir)) {
         sidAhead = j.sidAt(aheadDir);
      }
      realDir = Junction.getReverseDirection(aheadDir);
      if (!j.isEmpty(realDir)) {
         sidBack = j.sidAt(realDir);
      }
     
      // make new text sign
      // decide whether to submit streets
      // fails if more than 2 sids are alike
      // ahead goes first
      if ((sidAhead > 0) && (sidAhead != sidBack)) {
         if (sidAhead == sidLeft) {
            mySign.add(r.streetNames[sidAhead], SignUtils.leftAheadArrow);
            numNamesAdded++;
         }
         if (sidAhead == sidRight) {
            mySign.add(r.streetNames[sidAhead], SignUtils.aheadRightArrow);
            numNamesAdded++;
         }
         if ((sidAhead != sidLeft) && (sidAhead != sidRight)) {
            mySign.add(r.streetNames[sidAhead], SignUtils.aheadArrow);
            numNamesAdded++;
         }
      }
      // skip left if it's matched with ahead
      if (sidLeft > 0 && sidLeft != sidAhead) {
         if (sidLeft == sidRight) {
            if (numNamesAdded == 0) {
               mySign.add(r.streetNames[sidLeft], SignUtils.noArrow);
            }
            else {
               mySign.add(r.streetNames[sidLeft], SignUtils.leftRightArrow);
            }
         }
         else {
            mySign.add(r.streetNames[sidLeft], SignUtils.leftArrow);
         }
      }
      // unless right is unique, it's already been taken care of
      if (sidRight > 0 && sidLeft != sidRight && sidAhead != sidRight) {
         mySign.add(r.streetNames[sidRight], SignUtils.rightArrow);
      }
   }

   public void draw(Graphics g, boolean includePost) {
      if (mySign.isEmpty()) {
         return;
      }

      // support
      if (includePost) {
         SignUtils.drawPost(g, stakeLoc.x, stakeLoc.y);
      }

      // set top center of panel
      int y = stakeLoc.y - SignUtils.clearance + 4;
      if (includePost) {
         y -= 24;
      }
      mySign.setLocation(stakeLoc.x  - SignUtils.postThick/2, y);

      // draw panel
      mySign.draw(g);
   }
}
