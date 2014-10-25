package com.kurumi.matr;

import java.awt.Point;
// todo fix logDirection enum

/**
 * Route represents a numbered highway.
 * To get the route's path, we walk along legs of Junction tiles.
 * 
 * @author soglesby
 *
 */
public class Route {
	private final int id;  // index into Realm junctions and other structs
	private int number; // displayed route number
	private Point start;  // start of route in Realm coordinates
	private Point end;  // end of route in Realm coordinates
	private StringBuffer log = new StringBuffer();
	private final int logDirection;  // forward direction on signs; invariant once set

	Route(int id, int routeNumber, int dir, Point start, Point end) {
		this.id = id;
		this.number = routeNumber;
		logDirection = dir;
		this.start = new Point(start);
		this.end = new Point(end);
	}

	/**
	 * @return the Route's id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the Route's displayed number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @param number the Route number to set
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * @return the Route's start in Realm coordinates
	 */
	public Point getStart() {
		return start;
	}

	/**
	 * @param start the Route's start in Realm coordinates
	 * Note: don't use "this.start = start"; instead, make defensive copy
	 */
	public void setStart(Point start) {
		this.start.setLocation(start);
	}

	/**
	 * @param x the Route's starting x position in Realm coordinates
	 * @param y the Route's starting y position in Realm coordinates
	 */
	public void setStart(int x, int y) {
		this.start.move(x, y);
	}
	
	/**
	 * @return  the Route's end in Realm coordinates
	 */
	public Point getEnd() {
		return end;
	}

	/**
	 * @param end the Route's end in Realm coordinates
	 * Note: don't use "this.end = end"; instead, make defensive copy
	 */
	public void setEnd(Point end) {
		this.end.setLocation(end);
	}

	/**
	 * @param x the Route's ending x position in Realm coordinates
	 * @param y the Route's ending y position in Realm coordinates
	 */
	public void setEnd(int x, int y) {
		this.end.move(x, y);
	}

	/**
	 * @return the Route's highway log as text
	 */
	public StringBuffer getLog() {
		return log;
	}

	/**
	 * @param log the Route's highway log as text
	 */
	public void setLog(StringBuffer log) {
		this.log = new StringBuffer(log);
	}
	
	/**
	 * Clear the highway's route log (since you'll rebuild it)
	 */
	public void clearLog() {
		this.log = new StringBuffer();
	}
		

	/**
	 * Append a new line (or more) to the route's highway log
	 * @param newText
	 */
	public void appendToLog(String newText){
		this.log.append(newText);
	}
	
	/**
	 * @return the Route's log direction code (Junction.north, etc.)
	 * todo: use an enum
	 */
	public int getLogDirection() {
		return logDirection;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Route " + number + ": start=(" +
				start.x + ',' + start.y + "), end=(" +
				end.x + "," + end.y +
				"), dir=" + logDirection;
	}


}