package com.kurumi.matr;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.RenderingHints;

public class MyTools {
   public static void setDefault(GridBagConstraints gbc, int x, int y) {
      gbc.anchor = GridBagConstraints.CENTER;
      gbc.fill = GridBagConstraints.NONE;
      gbc.gridwidth = 1;
      gbc.gridheight = 1;
      gbc.gridx = x;
      gbc.gridy = y;
      gbc.weightx = 0;
      gbc.weighty = 0;
   }

   public static void setCentered(GridBagConstraints gbc, int x, int y) {
      setDefault(gbc, x, y);
      gbc.fill = GridBagConstraints.HORIZONTAL;
   }

   public static void setLeft(GridBagConstraints gbc, int x, int y) {
      setDefault(gbc, x, y);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
   }

   public static void setLeftNoFill(GridBagConstraints gbc, int x, int y) {
      setDefault(gbc, x, y);
      gbc.anchor = GridBagConstraints.WEST;
   }

   public static void setAllWide(GridBagConstraints gbc, int y) {
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridheight = 1;
      gbc.gridx = 0;
      gbc.gridy = y;
   }

   public static void setRestOfRow(GridBagConstraints gbc, int x, int y) {
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridheight = 1;
      gbc.gridx = x;
      gbc.gridy = y;
      gbc.weightx = 1;
   }

   public static void setLastRow(GridBagConstraints gbc) {
      gbc.weighty = 1;
      gbc.anchor = GridBagConstraints.NORTH;
   }

   public static int atoi(String s)
   {
      int num;
      try {
         num = Integer.parseInt(s);
      }
      catch (NumberFormatException e) {
         num = 0;
      }
      return num;
   }
      
   public static Graphics2D modernize(Graphics g1) {
	   Graphics2D g2 = (Graphics2D) g1;
	   RenderingHints hints = new RenderingHints(
			   RenderingHints.KEY_ANTIALIASING,
			   RenderingHints.VALUE_ANTIALIAS_ON);
	   g2.setRenderingHints(hints);
	   return g2;
   }
}
