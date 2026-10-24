package de.rpgframework.shadowrun6.foundry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.rpgframework.foundry.ItemData;

public class FVTTSIN {

	public String quality;
	public String description;
	public UUID id;
	public String name;
	public List<ItemData<GenericFVTT>> itemsInItem = new ArrayList<>();

}
