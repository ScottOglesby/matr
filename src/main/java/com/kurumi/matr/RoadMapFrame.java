package com.kurumi.matr;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.kurumi.matr.RoadMapCanvas;
import com.kurumi.matr.PathTool;

/**
   Frame for RoadMapCanvas.
   Also handles paving on map.
*/
public class RoadMapFrame extends JFrame implements MapFrame, AdjustmentListener {
   // general map controls
   private Choice chClick = new Choice();
   private Button bZoomIn = new Button("+");
   private Button bZoomOut = new Button("-");
   private Button bFit = new Button("Fit");
   private Button bClose = new Button("Close");

   // model and view data
   private Realm myRealm;
   
   RoadMapCanvas map;

   // Paver tool window
   private PathTool myPathTool;

   private Scrollbar hbar = new Scrollbar(Scrollbar.HORIZONTAL);
   private Scrollbar vbar = new Scrollbar(Scrollbar.VERTICAL);

   RoadMapFrame(Realm r, MatrPanel mp) {
      super("Map");
      setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      hbar.setUnitIncrement(16);
      vbar.setUnitIncrement(16);
      map = new RoadMapCanvas(r, mp, this);
      myPathTool = new PathTool(r, map, mp);
      myRealm = r;
      
      vbar.addAdjustmentListener(this);
      hbar.addAdjustmentListener(this);

      // fill out click action choices
      for (int i = 0; i < RoadMapCanvas.clickStrings.length; i++) {
         chClick.addItem(RoadMapCanvas.clickStrings[i]);
      }
      setLayout(new BorderLayout());

      Panel p = new Panel();
      p.setLayout(new BorderLayout());
      p.add("Center", map);
      p.add("South", hbar);
      p.add("East", vbar);
      add("Center", p);

      p = new Panel();
      p.setLayout(new FlowLayout(FlowLayout.RIGHT));
      p.add(new Label("Mouse click:"));
      p.add(chClick);
      p.add(bZoomIn);
      p.add(bZoomOut);
      p.add(bFit);
      p.add(bClose);

      add("South", p);
      
      setBounds(50,50,760,600);
      map.setClickAction(RoadMapCanvas.doMoveViewer);
   }
   
   // handle manual scrolling of map
   @Override
   public void adjustmentValueChanged(AdjustmentEvent e) {
	   if (e.getAdjustable() == vbar) {
		   map.updatey(e.getValue());
	   }
	   if (e.getAdjustable() == hbar) {
		   map.updatex(e.getValue());
	   }
   }

   // buttons
   @Override
   public boolean action(Event e, Object o) {
      if (e.target == bClose) {
    	  setVisible(false);
         return true;
      }
      if (e.target == bZoomIn) {
         map.zoomIn();
         map.repaint();
         return true;
      }
      if (e.target == bZoomOut) {
         map.zoomOut();
         map.repaint();
         return true;
      }
      if (e.target == bFit) {
         map.zoomToFit();
         map.repaint();
         return true;
      }
      if (e.target == chClick) {
         int clickAction = chClick.getSelectedIndex();
         map.setClickAction(clickAction);
         if (clickAction == RoadMapCanvas.doPave) {
            myPathTool.setVisible(true);
         }
         else {
            myPathTool.setVisible(false);
         }

         return true;
      }
      return super.action(e,o);
   }

   // called by main program (e.g. if user changed something)
   public void refresh() { map.repaint(); }
   

   // from MapFrame interface - let map (which has zoomed or moved)
   // tell us to readjust our scrollbars
   @Override
public void adjustScrollbars(Point upleft, Dimension size,
                                Point min, Point max) {
      hbar.setValues(upleft.x, size.width, min.x, max.x);
      vbar.setValues(upleft.y, size.height, min.y, max.y);
   }

   // from MapFrame interface - user has done an eyedropper on
   // a segment of road
   // todo: also report to changer window
   @Override
public void reportRoadInfo(Point square, int octant) {
      Junction j = myRealm.pToJ(square);
      String route[] = new String[2];
      String street = "";
      int rid;
      for (int i = 0; i < 2; i++) {
         rid = j.ridAt(octant,i);
         if (rid > 0) {
            route[i] = "" + myRealm.routes[rid].getNumber();
         }
         else {
            route[i] = "";
         }
      }
      int sid = j.sidAt(octant);
      if (sid > 1) {
         street = myRealm.streetNames[sid];
      }
      myPathTool.reportRoadInfo(route, street, j.pavementAt(octant));
   }
}

