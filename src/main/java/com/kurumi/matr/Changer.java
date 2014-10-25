package com.kurumi.matr;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.kurumi.matr.Realm;

/**
 * Tool to change highway numbers, street names and town names
 * TODO Event handlers for buttons
 * @author soglesby
 *
 */

public class Changer {
   JTextField tfOldRoute = new JTextField(6);
   JTextField tfNewRoute = new JTextField(6);
   JButton bcRoute = new JButton("Change");
   JTextField tfOldStreet = new JTextField(18);
   JTextField tfNewStreet = new JTextField(18);
   JButton bcStreet = new JButton("Change");
   JTextField tfOldTown = new JTextField(18);
   JTextField tfNewTown = new JTextField(18);
   JButton bcTown = new JButton("Change");
   JButton bClose = new JButton("Close");
   Label laMessage = new Label();

   Realm myRealm;
   MatrPanel mp;
   JFrame frame;

   /**
    * Build the number/name changer and lay out its UI.
    * @param r
    * @param mp_
    */
   Changer(Realm r, MatrPanel mp_) {
	   
	  frame = new JFrame("Renumber/Rename Tool");
      myRealm = r;
      mp = mp_;

      frame.setLayout(new GridBagLayout());
      GridBagConstraints c1 = new GridBagConstraints();
      int row = 0;

      // headings
      MyTools.setLeftNoFill(c1,1,row);
      frame.add(new Label("Is:"), c1);
      c1.gridx++;
      frame.add(new Label("Should be:"), c1);

      // route buttons
      row++;
      MyTools.setLeftNoFill(c1,0,row);
      frame.add(new Label("Route:"), c1);
      c1.gridx++;
      frame.add(tfOldRoute, c1);
      c1.gridx++;
      frame.add(tfNewRoute, c1);
      c1.gridx++;
      frame.add(bcRoute, c1);

      // street buttons
      row++;
      MyTools.setLeftNoFill(c1,0,row);
      frame.add(new Label("Street:"), c1);
      c1.gridx++;
      frame.add(tfOldStreet, c1);
      c1.gridx++;
      frame.add(tfNewStreet, c1);
      c1.gridx++;
      frame.add(bcStreet, c1);

      // town buttons
      row++;
      MyTools.setLeftNoFill(c1,0,row);
      frame.add(new Label("Town:"), c1);
      c1.gridx++;
      frame.add(tfOldTown, c1);
      c1.gridx++;
      frame.add(tfNewTown, c1);
      c1.gridx++;
      frame.add(bcTown, c1);

      // separator
      MyTools.setAllWide(c1,++row);
      frame.add(new JSeparator(SwingConstants.HORIZONTAL), c1);

      // message
      row++;
      MyTools.setAllWide(c1,row);
      MyTools.setLastRow(c1);
      frame.add(laMessage, c1);

      // separator
      MyTools.setAllWide(c1,++row);
      c1.weightx = 1;
      frame.add(new JSeparator(SwingConstants.HORIZONTAL), c1);

      // close JButton
      row++;
      MyTools.setLeftNoFill(c1,3,row);
      MyTools.setLastRow(c1);
      frame.add(bClose, c1);
      
      // JButton event handlers
      bcRoute.addActionListener(new ActionListener() {
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  changeRoute();
    	  }
      });      
      bcStreet.addActionListener(new ActionListener() {
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  changeStreet();
    	  }
      });      
      bcTown.addActionListener(new ActionListener() {
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  changeTown();
    	  }
      });      
      bClose.addActionListener(new ActionListener() {
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  frame.setVisible(false);
    	  }
      });      

      frame.pack();
   }

   private void changeRoute() {
      int oldRoute = MyTools.atoi(tfOldRoute.getText());
      int newRoute = MyTools.atoi(tfNewRoute.getText());
      if (!myRealm.routeExists(oldRoute)) {
         errorMessage("Route " + oldRoute + " doesn't exist.");
         return;
      }
      if (myRealm.routeExists(newRoute)) {
         errorMessage("There already is a route " + newRoute + ".");
         return;
      }
      myRealm.changeRoute(oldRoute, newRoute);
      clearErrorMessage();
      mp.askRefresh(MatrPanel.ALL);
   }

   private void changeStreet() {
      String oldStreet = tfOldStreet.getText();
      String newStreet = tfNewStreet.getText();
      if (!myRealm.streetExists(oldStreet)) {
         errorMessage("'" + oldStreet + "' doesn't exist.");
         return;
      }
      if (myRealm.streetExists(newStreet)) {
         errorMessage("There already is a '" + newStreet + "'.");
         return;
      }
      myRealm.changeStreet(oldStreet, newStreet);
      clearErrorMessage();
      mp.askRefresh(MatrPanel.ALL);
   }
      
   private void changeTown() {
      String oldTown = tfOldTown.getText();
      String newTown = tfNewTown.getText();
      if (!myRealm.townExists(oldTown)) {
         errorMessage("The town '" + oldTown + "' doesn't exist.");
         return;
      }
      if (myRealm.townExists(newTown)) {
         errorMessage("There already is a town named '" + newTown + "'.");
         return;
      }
      myRealm.changeTown(oldTown, newTown);
      clearErrorMessage();
      mp.askRefresh(MatrPanel.ALL);
   }
      
   private void errorMessage(String msg) {
      laMessage.setText(msg);
      frame.validate();
   }

   private void clearErrorMessage() { 
      errorMessage("");
   }

   /**
    * Support eyedropper function on map: populate old route number clicked on
    * @param number number of route user clicked on
    */
   public void setOldRoute(String number) {
      tfOldRoute.setText(number);
   }
   
   /**
    * Support eyedropper function on map: populate old town name clicked on
    * @param name name of street user clicked on
    */
   public void setOldStreet(String name) {
      tfOldStreet.setText(name);
   }
   
   /**
    * Support eyedropper function on map: populate old town name clicked on
    * @param name name of town user clicked on
    */
   public void setOldTown(String name) {
      tfOldTown.setText(name);
   }
   
   /**
    * Make this window visible
    */
   public void show() {
	   frame.setVisible(true);
   }
   
   /**
    * Hide this window
    */
   public void hide() {
	   frame.setVisible(false);
   }
}
