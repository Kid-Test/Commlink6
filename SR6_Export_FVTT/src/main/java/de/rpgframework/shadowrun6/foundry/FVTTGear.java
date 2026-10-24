package de.rpgframework.shadowrun6.foundry;

import java.util.ArrayList;
import java.util.List;

import de.rpgframework.foundry.ItemData;

public class FVTTGear extends GenericFVTT {
	
	public static class Matrix {
		public int deviceRating;
		public int a;
		public int s;
		public int d;
		public int f;
	}

	public String type;
	public String subtype;
	public String availDef;
	public int    avail;
	public String priceDef;
	public int    price;
	public String accessories;
	public boolean needsRating;
	public int    rating;
	/** Associated skill */
	public String skill;
	/** Associated skill specialization*/
	public String skillSpec;
	public boolean wild;
	public String notes;
	public String customName;
	public boolean countable;
	public int count;
	public boolean usedForPool;
	public float essence;
	public List<ItemData<FVTTGear>> itemsInItem = new ArrayList<>();
	public Matrix matrix;
	public String gearMods;

}
