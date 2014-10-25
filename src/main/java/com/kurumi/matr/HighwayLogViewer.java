package com.kurumi.matr;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import com.kurumi.matr.Realm;
import com.kurumi.matr.Route;
import com.kurumi.matr.Junction;
import com.kurumi.matr.Square;

/**
 * Display a distance-annotated list of points of interest along a highway
 * Do this for one highway or all highways.
 * 
 * To avoid floating point issues, we track route lengths as integers:
 * a "centimile" is 1/100 of a mile.
 * 
 * TODO we actually store the log text in the Route object -- might be a better way.
 * It's convenient for sorting in a TreeMap
 * 
 * TODO consider change to contains a JFrame instead of extends a JFrame
 * 
 * @author soglesby
 *
 */
public class HighwayLogViewer extends JFrame {

   private JTextArea myText = new JTextArea("", 40, 30);
   private JButton bShow = new JButton("Show Route:");
   private JButton bShowAll = new JButton("Show All");
   private Realm myRealm;
   private boolean showAll = true;
   private TextField tfRoute = new TextField(4);
   private int routeToShow = 0;

   /**
    * Format a length in centimiles as miles, to 2 decimal places
    * For example, f(370) returns "3.70"
    * @param length in centimiles (int)
    * @return length in miles (String)
    */
   private static String mile100String(int centimiles) {
	   return String.format("%d.%02d", centimiles/100, centimiles%100);
   }

   /**
    * Respond to changes to the highway network, e.g. from paving
    * Remembers "show all" or "show one route" setting from UI
    */
   public void refresh() {
	   buildLog();
   }

   /**
    * Construct a Highway Log window
    * @param realm Realm object todo
    */
   HighwayLogViewer(Realm realm)
   {
	   super("Highway Log");
	   myRealm = realm;

	   // don't dispose and later recreate on close ... just hide
	   setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	   setLayout(new BorderLayout());
	   Container pane = getContentPane();
	   
	   // "show one route" button
	   // looking forward to Java 8 lambdas here
	   bShow.addActionListener(new ActionListener()
	   {
		   @Override
		   public void actionPerformed(ActionEvent e)
		   {
			   showAll = false;
			   routeToShow = MyTools.atoi(tfRoute.getText());
			   buildLog();
		   }
	   });

	   // "show one route" button
	   bShowAll.addActionListener(new ActionListener()
	   {
		   @Override
		   public void actionPerformed(ActionEvent e)
		   {
			   showAll = true;
			   buildLog();
		   }
	   });

	   // layout text area
	   JPanel textAreaPanel = new JPanel();
	   textAreaPanel.add(new JScrollPane(myText));
	   myText.setEditable(false);
	   pane.add(textAreaPanel, BorderLayout.CENTER);

	   JPanel buttonsPanel = new JPanel();
	   buttonsPanel.add(bShow);
	   buttonsPanel.add(tfRoute);
	   buttonsPanel.add(bShowAll);
	   pane.add(buttonsPanel, BorderLayout.PAGE_END);

	   buildLog();
	   pack();
   }

   /**
    * Build a highway log for a single route -- add to StringBuffer.
    * @param rid Index to myRealm.routes[] array. Todo: change to Route object.
    * @param log Current highway log buffer (may contain other routes)
    * @return length of this route in centimiles.
    */
   private int logOneRoute(Route route) {
      Point here = new Point();
      Point end = new Point();

      int oldTownId, newTownId, endTownId;  // town codes
      int my100s = 0;  // cumulative mileage in 1/100ths for this route
      
      route.clearLog();
      route.appendToLog("Route " + route.getNumber() + "\n");
      
      here.setLocation(route.getStart());
      end.setLocation(route.getEnd());
      oldTownId = newTownId = myRealm.grid[here.x][here.y].getTown();
      endTownId = myRealm.grid[end.x][end.y].getTown();
      route.appendToLog("From: " + 
                 myRealm.pToJ(here).describe(myRealm, route.getId()) +
                 " " + myRealm.towns[newTownId].getName() + "\n");
      route.appendToLog("To: " + 
                 myRealm.pToJ(end).describe(myRealm, route.getId()) +
                 " " + myRealm.towns[endTownId].getName() + "\n");
      route.appendToLog("Log dir: " + 
                 Junction.dirStrings[route.getLogDirection()] +
                 "\nMileposts:\n");

      // follow a route from start to end
      while (true) {
         
         // describe any junctions
         String s = myRealm.pToJ(here).describe(myRealm, route.getId());
         if (s.length() > 0) {
        	 route.appendToLog(mile100String(my100s) + ": " + s + "\n");
         }
         
         // does the route end here? If so, break
         if (!myRealm.nextJunc(here, route.getId())) {
        	 break;
         }
         
         // route continues... determine length increment
         int lengthIncrement = Square.straightLength;
         if (myRealm.pToJ(here).ridLeavesDiagonally(route.getId())) {
        	 lengthIncrement = Square.straightLength;
         }
         int halfIncrement = lengthIncrement / 2;
         
         // describe any town line crossing
         // this is halfway between this tile center and the next
         newTownId = myRealm.grid[here.x][here.y].getTown();
         if (newTownId != oldTownId) {
        	 route.appendToLog(mile100String(my100s + halfIncrement) + ": ");
        	 route.appendToLog(myRealm.towns[oldTownId].getName() + " - " +
                       myRealm.towns[newTownId].getName() + " TL\n");
         }
         oldTownId = newTownId;
         
         // increment route's mileage
         my100s += lengthIncrement;
      }
      return my100s;
   }

   /* ===
      build the route log for all routes.
      MUCH quicker to build up a stringBuffer, then append once
      to textArea, than to append to textArea bit by bit.
   */
   private void logAllRoutes() {

      int total100s = 0; // total mileage, all routes, 1/100ths

      // sort the highway log by route number
      Map<Integer, Route> treeMap = new TreeMap<>();
 
      for (int i = 1; i <= myRealm.getNumRoutes(); i++) {
         total100s += logOneRoute(myRealm.routes[i]);
         treeMap.put(myRealm.routes[i].getNumber(), myRealm.routes[i]);
      }

      // now build the log as a stringbuffer, with all pieces in order
      StringBuffer completeHighwayLog = new StringBuffer();
      SortedSet<Integer> keys = new TreeSet<Integer>(treeMap.keySet());
      for (Integer key: keys) {
    	  completeHighwayLog.append(treeMap.get(key).getLog());
    	  completeHighwayLog.append("\n\n");
      }
      
      completeHighwayLog.append("\nNumbered routes: " + myRealm.getNumRoutes() + "\n");
      completeHighwayLog.append("\nTotal mileage: " + mile100String(total100s) + "\n");
      myText.setText("");
      myText.setFont(Empire.routeLogFont);
      myText.setText(completeHighwayLog.toString());
   }

   private void buildLog() {
	   if (showAll) {
		   logAllRoutes();
		   return;
	   }
	   // log single route
	   int rid = myRealm.getRouteId(routeToShow);
	   if (rid <= 0) {
		   myText.setText("Route " + routeToShow + " doesn't exist.");
	   }
	   else {
		   logOneRoute(myRealm.routes[rid]);
		   myText.setText(myRealm.routes[rid].getLog().toString());
	   }
   }
}

