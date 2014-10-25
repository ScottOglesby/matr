
package com.kurumi.matr;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import com.kurumi.matr.RoadMapFrame;
import com.kurumi.matr.Junction;
import com.kurumi.matr.Changer;
import com.kurumi.matr.IntersectionViewerFrame;

/**
 * Launcher panel for MATR.
 * @author soglesby
 *
 */
public class Empire implements MatrPanel {
	static final boolean expertMode = true;
	static final int defaultHeight = 64;
	static final int defaultWidth = 100;
	static final int defaultNumRoutes = 10;
	static final String appTitle = "Me and the Roads";

	public static Font routeLogFont = new Font("Helvetica", Font.PLAIN, 10);
	public static Font townFont = new Font("Helvetica", Font.PLAIN, 12);
	public static Font dirTabFont = new Font("Arial Narrow", Font.BOLD, 12);
	public static Font routeTabFont = new Font("Helvetica", Font.BOLD, 18);

	Realm myRealm;
	TextField tw, th, tr;  // expert mode width, height, # routes
	Button bCreate = new Button("Create");
	Button bMap = new Button("Map");
	Button bLog = new Button("Log");
	Button bView = new Button("View");
	Button bChange = new Button("Rename");
	Button bFontPreview = new Button("Fonts");

	JFrame myFrame;
	RoadMapFrame map;
	IntersectionViewerFrame viewer;
	HighwayLogViewer routeLog;
	Changer renamer;

	public static void main (String args[]) {
		new Empire().init();
	}

	public void init() {
		myFrame = new JFrame(appTitle);
		myFrame.setBounds(50,50,480,80);
		
		setHandlers();
		
		Panel p;
		myFrame.setLayout(new GridLayout(0,1));
		if (expertMode) {
			tw = new TextField(""+defaultWidth, 5);
			th = new TextField(""+defaultHeight, 5);
			tr = new TextField(""+defaultNumRoutes, 5);
			p = new Panel();
			p.setLayout(new FlowLayout(FlowLayout.LEFT));
			p.add(new Label("width:"));
			p.add(tw);
			p.add(new Label("height:"));
			p.add(th);
			p.add(new Label("routes:"));
			p.add(tr);
			myFrame.add(p);
		}
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p.add(bCreate);
		p.add(bMap);
		p.add(bLog);
		p.add(bView);
		p.add(bChange);
		//p.add(bFontPreview);
		disableToolButtons();
		myFrame.add(p);
		myFrame.pack();
		myFrame.setVisible(true);
	}
	
	/**
	 * Create new map, and hide its frame if it was already visible
	 */
	private void newMap() {
		if (map != null) {
			map.setVisible(false);
		}
		map = new RoadMapFrame(myRealm, this);
		bMap.setEnabled(true);		
	}
	
	/**
	 * Create new highway log, and hide its frame if it was already visible
	 */
	private void newHighwayLog() {
		if (routeLog != null) {
			routeLog.setVisible(false);
		}
		routeLog = new HighwayLogViewer(myRealm);
	}
	
	private void newViewer() {
		if (viewer != null) {
			viewer.setVisible(false);
		}
		viewer = new IntersectionViewerFrame(myRealm);		
	}
	
	private void newRenamer() {
		if (renamer != null) {
			renamer.hide();
		}
		renamer = new Changer(myRealm, this);		
	}
	
	private void newFontPreview() {
		new FontPreview(this).show();		
	}
	
	private void setHandlers() {
		// "Create realm" button
		bCreate.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int nw, nh, nr;
				if (expertMode) {
					nw = MyTools.atoi(tw.getText()); 
					nh = MyTools.atoi(th.getText());
					nr = MyTools.atoi(tr.getText());
				}
				else {
					nw = defaultWidth; 
					nh = defaultHeight;
					nr = defaultNumRoutes;
				}
				disableToolButtons();
				myRealm = new Realm(nw, nh, nr);
				myRealm.create();
				// can only create a realm once
				bCreate.setEnabled(false);

				// hide/delete all old displays
				newMap();
				bMap.setEnabled(true);		
				
				newHighwayLog();
				bLog.setEnabled(true);

				newViewer();
				bView.setEnabled(true);

				newRenamer();
				bChange.setEnabled(true);
			}
		});
		bMap.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				map.setVisible(true);
			}
		});
		bLog.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				routeLog.setVisible(true);
			}
		});
		bView.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewer.setVisible(true);
			}
		});
		bChange.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renamer.show();
			}
		});
		bFontPreview.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newFontPreview();
			}
		});
	}

	private void disableToolButtons() {
		bMap.setEnabled(false);
		bLog.setEnabled(false);
		bView.setEnabled(false);
		bChange.setEnabled(false);
	}

	// matrpanel functions
	// click from map: move viewer
	@Override
	public void setPlayerLocation(int x, int y) {
		viewer.setViewerLocation(x, y);
	}


	// auto refresh done as result of paving, e.g.
	@Override
	public void askRefresh(int whichOnes) {
		if (map != null && (whichOnes & MatrPanel.MAP) != 0) {
			map.refresh();
		}
		if (routeLog != null && (whichOnes & MatrPanel.LOG) != 0) {
			routeLog.refresh();
		}
		if (viewer != null && (whichOnes & MatrPanel.VIEW) != 0) {
			viewer.refresh();
		}
	}

	// TODO assumes the various windows all extend JFrame :-/
   private static void updateFont(Component c) {
      if (c != null) {
         if (c.isVisible()) {
            c.repaint();
         }
      }
   }
         
   // font change message from font preview
   @Override
   public void fontChanged() {
	   updateFont(map);
	   updateFont(routeLog);
	   updateFont(viewer);
	   askRefresh(MatrPanel.ALL);
   }

   // user did eyedropper on map
   @Override
   public void infoCopiedFromMap(Point square, int octant) {
      Junction j = myRealm.pToJ(square);
      String route = "";
      int rid = j.ridAt(octant,0);
      if (rid > 0) {
         route = "" +  myRealm.routes[rid].getNumber();
      }
      String street = "";
      int sid = j.sidAt(octant);
      if (sid > 1) {
         street = myRealm.streetNames[sid];
      }
      String town = myRealm.towns[myRealm.grid[square.x][square.y].getTown()].getName();
      if (renamer != null) {
         renamer.setOldRoute(route);
         renamer.setOldStreet(street);
         renamer.setOldTown(town);
      }
   }     

}

