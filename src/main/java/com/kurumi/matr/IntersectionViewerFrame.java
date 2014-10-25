package com.kurumi.matr;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;

import com.kurumi.matr.Realm;

public class IntersectionViewerFrame extends JFrame{
	private JButton bClose = new JButton("Close");
	private JButton bTurnLeft = new JButton ("⤾");
	private JButton bTurnRight = new JButton ("⤿");
	private IntersectionViewerClient viewerClient;
	private JComboBox<String> chDir = new JComboBox<String>(Junction.dirStrings);
	private Realm myRealm;
	private Point player = new Point();
	private int dirFacing = 0;

	IntersectionViewerFrame(Realm r) {
		super("Intersection View");
		myRealm = r;
		viewerClient = new IntersectionViewerClient(r);

		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.RIGHT));

		p.add(new Label("  facing:"));
		p.add(chDir);
		p.add(bTurnLeft);
		p.add(bTurnRight);
		p.add(bClose);
		
		bTurnLeft.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dirFacing++;
				if (dirFacing > 7) {
					dirFacing = 0;
				}
				chDir.setSelectedIndex(dirFacing);
				movePlayer();
			}
		});		

		bTurnRight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dirFacing--;
				if (dirFacing < 0) {
					dirFacing = 7;
				}
				chDir.setSelectedIndex(dirFacing);
				movePlayer();
			}
		});	
		
		chDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dirFacing = chDir.getSelectedIndex();
				movePlayer();
			}
		});	
		
		bClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});	
		
		add("South", p);
		add("Center", viewerClient);
		setBounds(250,250,540,300); // move and place

		// move to first town
		Point here = myRealm.towns[1].getCenter();
		player.setLocation(here);

		chDir.setSelectedIndex(Junction.north);

		movePlayer();
	}


	public void setViewerLocation(int x, int y) {
		player.setLocation(x, y);
		movePlayer();
	}

	public void refresh() { 
		viewerClient.repaint();
	}


	private void movePlayer() {
		viewerClient.movePlayer(player, dirFacing);
	}
}
