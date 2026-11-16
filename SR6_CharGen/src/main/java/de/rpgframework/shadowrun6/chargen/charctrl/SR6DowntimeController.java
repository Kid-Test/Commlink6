package de.rpgframework.shadowrun6.chargen.charctrl;

import java.util.List;

import de.rpgframework.genericrpg.modification.ModifiedObjectType;
import de.rpgframework.shadowrun.DowntimeAction;
import de.rpgframework.shadowrun.Lifestyle;
import de.rpgframework.shadowrun.chargen.charctrl.CommonDowntimeController;
import de.rpgframework.shadowrun6.SR6Lifestyle;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * Downtime actions with the rules of the sixth edition (Core p. 236-237,
 * Sixth World Companion): Heat has no upper limit, and a contact needs a
 * Connection of 3 or more to make Heat go away.
 *
 * @author prelle
 */
public class SR6DowntimeController extends CommonDowntimeController<Shadowrun6Character> {

	private final SR6CharacterController parent;
	private final boolean creation;

	// ---
	public SR6DowntimeController(SR6CharacterController parent, boolean creation) {
		this.parent = parent;
		this.creation = creation;
	}

	// ---
	@Override
	protected Shadowrun6Character getModel() { return parent.getModel(); }

	// ---
	@Override
	protected void runProcessors() { parent.runProcessors(); }

	// ---
	@Override
	protected boolean isCreation() { return creation; }

	// ---
	@Override
	protected List<DowntimeAction> getActions() { return Shadowrun6Core.getItemList(DowntimeAction.class); }

	// ---
	@Override
	protected ModifiedObjectType attributeType() { return ShadowrunReference.ATTRIBUTE; }

	// ---
	@Override
	protected ModifiedObjectType contactType() { return ShadowrunReference.CONTACT; }

	// ---
	@Override
	protected ModifiedObjectType sinType() { return ShadowrunReference.SIN; }

	// ---
	@Override
	protected ModifiedObjectType actionType() { return ShadowrunReference.DOWNTIME_ACTION; }

	// ---
	@Override
	protected int getCostPerMonth(Lifestyle lifestyle) { return lifestyle.getCostPerMonth(); }

	// ---
	@Override
	protected void removeLifestyle(Lifestyle lifestyle) { getModel().removeLifestyle((SR6Lifestyle)lifestyle); }

}
