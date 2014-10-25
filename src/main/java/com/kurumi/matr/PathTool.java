package com.kurumi.matr;

import java.awt.*;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
   
/* =====
   Path tools.
   no close button because its appearance depends on PMap pave mode
*/

/**
 * This paver/namer tool does a few things:
 * - lays down roadway along a path you draw on the map
 * - set or add route numbers or street name along that path
 * It's still easy to screw things up badly :-(
 * 
 * @author soglesby
 *
 */
public class PathTool extends JFrame {

   // name actions
   private final static int add = 0, subtract = 1, paveNew = 2;

   private Button bAdd = new Button("Add");
   private Button bSub = new Button("Remove");
   private Button bSet = new Button("Set");
   private Button bErase = new Button("Erase");
   private Button bPave = new Button("Pave");
   private Button bDeselect = new Button("Deselect");
   private Choice chPaveType = new Choice();
   private Label laMessage = new Label();

   private TextField tfRoute[] = new TextField[Junction.maxOverlappingRoutes];
   private Choice chDirection[] = new Choice[Junction.maxOverlappingRoutes];
   private TextField tfStreet = new TextField("", 15);

   private Realm myRealm;
   private RoadMapCanvas myMap;
   private MatrPanel myPanel;
   

   // convenience function to add components
   private GridBagLayout layout = new GridBagLayout();
   private void add(Component c, GridBagConstraints gbc) {
      layout.setConstraints(c, gbc);
      this.add(c);
   }

   public PathTool(Realm r, RoadMapCanvas m, MatrPanel mp) {
      super("Pave, Number and Name");
      
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      
      myRealm = r;
      myMap = m;
      myPanel = mp;

      setLayout(layout);

      for (int i = 0; i < Junction.maxOverlappingRoutes; i++) {
    	  tfRoute[i] = new TextField(4);
    	  chDirection[i] = new Choice();
    	  for (String s: Junction.dirStrings90) {
    		  chDirection[i].add(s);
    	  }
      }
      
      // fill out pavement type choices.
      // don't include "none" because Erase button handles this.
      for (int i = 1; i < Junction.paveStrings.length; i++) {
         chPaveType.addItem(Junction.paveStrings[i]);
      }

      GridBagConstraints c1 = new GridBagConstraints();
      int row = 0;

      // path buttons
      MyTools.setLeftNoFill(c1,0,row);
      add(new Label("Path:"), c1);
      MyTools.setLeftNoFill(c1,1,row);
      c1.gridwidth = 2;
      add(bDeselect, c1);
      
      // separator
      MyTools.setAllWide(c1,++row);
      add(new JSeparator(SwingConstants.HORIZONTAL), c1);

      // type
      row++;
      MyTools.setLeft(c1, 0, row);
      add(new Label("Type:"), c1);
      MyTools.setRestOfRow(c1, 1, row);
      add(chPaveType, c1);

      // two buttons
      row++;
      MyTools.setLeftNoFill(c1,1,row);
      add(bPave, c1);
      MyTools.setLeftNoFill(c1,3,row);
      add(bErase, c1);
      
      // separator
      MyTools.setAllWide(c1,++row);
      add(new JSeparator(SwingConstants.HORIZONTAL), c1);

      // first route
      row++;
      MyTools.setLeft(c1, 0, row);
      add(new Label("Route:"), c1);
      MyTools.setLeft(c1, 1, row);
      add(tfRoute[0], c1);
      MyTools.setLeft(c1, 2, row);
      c1.fill = GridBagConstraints.NONE;
      c1.gridwidth = GridBagConstraints.REMAINDER;
      add(chDirection[0], c1);

      // second route
      row++;
      MyTools.setLeft(c1, 0, row);
      add(new Label("Route:"), c1);
      MyTools.setLeft(c1, 1, row);
      add(tfRoute[1], c1);
      MyTools.setLeft(c1, 2, row);
      c1.fill = GridBagConstraints.NONE;
      c1.gridwidth = GridBagConstraints.REMAINDER;
      add(chDirection[1], c1);

      // street name
      row++;
      MyTools.setLeft(c1, 0, row);
      add(new Label("Street:"), c1);
      MyTools.setRestOfRow(c1, 1, row);
      add(tfStreet, c1);

      // three buttons
      row++;
      MyTools.setLeftNoFill(c1,1,row);
      add(bAdd, c1);
      c1.gridx = GridBagConstraints.RELATIVE;
      add(bSet, c1);
      add(bSub, c1);
      
      // separator
      MyTools.setAllWide(c1,++row);
      add(new JSeparator(SwingConstants.HORIZONTAL), c1);

      // message
      row++;
      MyTools.setAllWide(c1,row);
      MyTools.setLastRow(c1);
      add(laMessage, c1);

      pack();
   }

   // buttons
   @Override
public boolean action(Event e, Object o) {
      if (e.target == bDeselect) {
         myMap.deselect();
         return true;
      }
      if (e.target == bPave) {
         handlePave(false);
         return true;
      }
      if (e.target == bErase) {
         handlePave(true);
         return true;
      }
      if (e.target == bAdd) {
         handleName(add);
         return true;
      }
      if (e.target == bSub) {
         handleName(subtract);
         return true;
      }
      if (e.target == bSet) {
         handleName(paveNew);
         return true;
      }
      return super.action(e,o);
   }
      
   /* ===
      handle the pave button - either clear out a road,
      or add/change the pavement of existing road.
      Doesn't mess with names/numbers, except for clearing.
      stops at first illegal pave attempt.
      works beautifully!
   */
   void handlePave(boolean erase) {
      Vector<Point> v = myMap.getSelection();
      if (v.size() < 2) {
         errorMessage("Can't pave; no path");
         return;
      }
      int paveType = chPaveType.getSelectedIndex() + 1;

      // iterate through map selection
      for (int i = 0; i < v.size() - 1; i++) {
         Point here = v.elementAt(i);
         Point next = v.elementAt(i+1);
         int dir = Junction.getDirection(here, next);
         if (dir < 0) {
            errorMessage("Internal error: J.getDirection() < 0");
            if (i == 0) {
               myPanel.askRefresh(MatrPanel.ALL);
            }
            return;
         }
         if (erase) {
            myRealm.pToJ(here).clearLeg(dir);
            myRealm.pToJ(next).clearLeg(Junction.getReverseDirection(dir));
         }
         else {
            // stop at first illegal pave attempt
            if (myRealm.cantPave(here, dir)) {
               errorMessage("Couldn't pave completely (bad intersection)");
               if (i == 0) {
                  myPanel.askRefresh(MatrPanel.ALL);
               }
               return;
            }
            myRealm.setPaveBoth(here, dir, paveType);
         }
      }
      myPanel.askRefresh(MatrPanel.ALL);
      clearErrorMessage();
   }
   
   private void errorMessage(String msg) {
      laMessage.setText(msg);
      validate();
   }

   private void clearErrorMessage() { 
      errorMessage("");
   }

   /* ===
      take care of naming existing pavement.
   */
   void handleName(int nameAction) {
      Vector<Point> v = myMap.getSelection();
      if (v.size() < 2) {
         errorMessage("Couldn't name or number; no path");
         return;
      }

      // need first square and direction to check route (possibly new route)
      // specification
      Point here = v.elementAt(0);
      Point next = v.elementAt(1);
      int dir = Junction.getDirection(here, next);
      if (dir < 0) {
         errorMessage("Internal error: J.getDirection() < 0");
         return;
      }

      /* check and make note of routes specified in textfields.
         for subtract mode, you don't need a direction...
         but an unrecognized route number must not create a new route.
      */
      int rid[] = new int[tfRoute.length];
      boolean isForward[] = new boolean[tfRoute.length];

      for (int j = 0; j < tfRoute.length; j++) {
         int routeNumber = 0;  // route number asked for
         int specDir = -1;  // which direction specified for route

         routeNumber = MyTools.atoi(tfRoute[j].getText());
 
         if (routeNumber > 0) {
            // subtract: direction not needed; route # must exist
            if (nameAction == subtract) {
               if (myRealm.getRouteId(routeNumber) <= 0) {
                  errorMessage("Can't subtract "+ routeNumber +
                               "; doesn't exist");
                  return;
                  }
            }
            // add/set: find out if you're adding forward or backward
            // ok to create new routes here
            else {
            	specDir = Junction.dirFromChoice4(chDirection[j].getSelectedIndex());
            	if (specDir >= 0) {
                  rid[j] = myRealm.getRouteIdOrCreate(routeNumber,
                                                   here, dir, specDir);
               }
               else {
                 errorMessage("Internal error: bad direction for "
                              + routeNumber);
                 return;
               }
            	// at this point the new route exists.
            	// mainly for existing routes, we validate the direction
            	// e.g. can't set "4 south" if 4 is an east/west route
               int logDir =  myRealm.routes[rid[j]].getLogDirection();
               int revDir =  Junction.getReverseDirection(logDir);
               if (specDir == logDir) {
                  isForward[j] = true;
               }
               else if (specDir == revDir) {
                  isForward[j] = false;
               }
               else {
                  errorMessage("Please specify " + 
                               Junction.dirStrings[logDir] +
                               " or " + Junction.dirStrings[revDir] +
                               " for route " + routeNumber);
                  return;
               }
            }
         }
      }
 
      // iterate through map selection
      for (int i = 0; i < v.size() - 1; i++) {
         here = v.elementAt(i);
         next = v.elementAt(i+1);
         dir = Junction.getDirection(here, next);
         if (dir < 0) {
            errorMessage("Internal error: J.getDirection() < 0");
            if (i == 0) {
               myPanel.askRefresh(MatrPanel.ALL);
            }
            return;
         }

         switch(nameAction){
            case paveNew:
               myRealm.clearRidsBoth(here, dir);
               // $FALL-THROUGH$
            case add:
               for (int j = 0; j < tfRoute.length; j++) {
                  if (rid[j] > 0) {
                     myRealm.addRidBoth(here, dir, rid[j], isForward[j]);
                  }
               }
               break;
            case subtract:
               for (int j = 0; j < tfRoute.length; j++) {
                  if (rid[j] > 0) {
                     myRealm.removeRidBoth(here, dir, rid[j]);
                  }
               }
               break;
         }
      }
      // brute-force find new start of route and new end
      for (int j = 0; j < tfRoute.length; j++) {
         if (rid[j] > 0) {
            myRealm.findRouteStart(rid[j]);
            myRealm.findRouteEnd(rid[j]);
         }
      }
      myPanel.askRefresh(MatrPanel.ALL);
      clearErrorMessage();
   }

   // support eyedropper function in map
   public void reportRoadInfo(String route[], String street, int paveType) {
      tfRoute[0].setText(new String(route[0]));
      tfRoute[1].setText(new String(route[1]));
      tfStreet.setText(new String(street));
      if (paveType > 0) {
         chPaveType.select(paveType - 1);
      }
   }
}

