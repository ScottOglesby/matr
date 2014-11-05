package com.kurumi.matr;

/**
 * Represent a tile of land or water in the realm.
 * @author soglesby
 *
 */
public class Square {

	public static final int water = 0;
	public static final int land = 1;
	
	public static final int hint_none = 0;  // just water or land as i
	public static final int water_nw = 1; // water to NW, land to SE
	public static final int water_ne = 2;
	public static final int water_se = 3;
	public static final int water_sw = 4;
	
	// how wide is square?
	public static final double miles = 0.2;

	// segment lengths, in miles/100
	// diagonal length is approx. straight * sqrt(2)
	public static final int straightLength = 20; // should == "miles"
	public static final int diagLength = 28;

	// initial values are applied when object is created
	private byte terrain; // water or land
	private byte town = 0;  // town ID; index into Realm.towns[]
	private boolean marked = false;
	
	private int displayHint = 0;
	
	Junction junc = null;

	Square() {
		init(water);
	}

	Square(int terrain_) {
		init(terrain_);
	}

	public void init(int terrain_) {
		setTerrain(terrain_);
	}

	public void setTerrain(int terrain_) {
		terrain = (byte) terrain_;
	}
	
	public int getTerrain() {
		return terrain;
	}

	public void setTown(int town_) {
		town = (byte) town_;
	}
	public void setTownAndMark(int town_) {
		town = (byte) town_;
		marked = true;
	}
	public int getTown() { return terrain == water ? 0 : (int) town; }

	public void setMarked(boolean marked_) {
		marked = marked_;
	}
	public boolean getMarked() { return marked; }

	// smartness functions
	// true if ok to put new town here.
	public boolean okForTown() {
		if ((terrain == water) || marked || (town != 0)) {
			return false;
		}
		return true;
	}

	public Junction getJunc() {
		if (junc == null) {
			junc = new Junction();
		}
		return junc;
	}

	// true if town here existed before this fill pass.
	public boolean hasRealTown() {
		if (terrain == water || marked || town == 0) {
			return false;
		}
		return true;
	}

	/**
	 * @return the displayHint
	 */
	public int getDisplayHint() {
		return displayHint;
	}

	/**
	 * @param displayHint the displayHint to set
	 */
	public void setDisplayHint(int displayHint) {
		this.displayHint = displayHint;
	}
}
