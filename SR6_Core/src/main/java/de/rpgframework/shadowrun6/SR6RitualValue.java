package de.rpgframework.shadowrun6;

import de.rpgframework.shadowrun.Ritual;
import de.rpgframework.shadowrun.RitualValue;

/**
 * @author prelle
 */
public class SR6RitualValue extends RitualValue<Ritual> {

	//-------------------------------------------------------------------
	public SR6RitualValue() {
	}

	//-------------------------------------------------------------------
	public SR6RitualValue(Ritual data) {
		super(data);
	}

	//-------------------------------------------------------------------
	/**
	 * Drain of the ritual with all modifications applied
	 */
	public int getDrain() {
		if (getResolved()==null)
			return 0;
		return resolved.getDrain();
	}

}
