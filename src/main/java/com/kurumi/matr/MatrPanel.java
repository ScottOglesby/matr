package com.kurumi.matr;

import java.awt.Point;

interface MatrPanel {
   public static int MAP = 1;
   public static int LOG = 2;
   public static int VIEW = 4;
   public static int MAPLOG = MAP + LOG;
   public static int MAPVIEW = MAP + VIEW;
   public static int LOGVIEW = LOG + VIEW;
   public static int ALL = MAP + LOG + VIEW;

   public void setPlayerLocation(int x, int y);
   public void askRefresh(int whichOnes);
   public void fontChanged();
   public void infoCopiedFromMap(Point square, int octant);
}
