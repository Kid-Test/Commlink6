package de.rpgframework.shadowrun6.items;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.ApplyTo;
import de.rpgframework.genericrpg.data.Lifeform;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarriedItemProcessor;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModifiedObjectType;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * Applies the item attribute modifications of user-installed accessories
 * (e.g. vehicle modifications like Improved Handling) to the item they are
 * installed in.
 * <p>
 * Stock accessories (&lt;embed&gt;) are handled in
 * {@link ApplyStockModificationsStep}. User-installed accessories are only
 * calculated in phase 2 ({@code RecurseEmbeddedItems}), after the parent
 * item's attributes have been set up - so their modifications have to be
 * applied here. Must run directly after {@code RecurseEmbeddedItems}.
 * <p>
 * Only modifications without an explicit target or with
 * {@code apply="PARENT"} are forwarded - {@code apply="DATA_ITEM"} keeps the
 * modification on the accessory itself.
 *
 * @see <a href="https://rpgframework.atlassian.net/browse/COM6-1051">COM6-1051</a>
 */
public class ApplyAccessoryItemAttributesStep implements CarriedItemProcessor {

	final static Logger logger = SR6GearTool.logger;

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.items.CarriedItemProcessor#process(boolean, de.rpgframework.genericrpg.modification.ModifiedObjectType, de.rpgframework.genericrpg.data.Lifeform, de.rpgframework.genericrpg.items.CarriedItem, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public OperationResult<List<Modification>> process(boolean strict, ModifiedObjectType ref, Lifeform charac, CarriedItem<?> model,
			List<Modification> unprocessed) {
		CarriedItem<ItemTemplate> parent = (CarriedItem<ItemTemplate>) model;

		for (CarriedItem<?> accessory : model.getAccessories()) {
			// Modifications of active operation modes (e.g. a switched on laser sight) are
			// already collected by the parent itself (GetModificationsStep) - don't apply twice
			List<Modification> fromModes = new ArrayList<>();
			accessory.getActiveOperationModes(false).forEach(m -> fromModes.addAll(m.getModifications()));
			for (Modification mod : accessory.getIncomingModifications()) {
				if (fromModes.remove(mod))
					continue;
				if (!(mod instanceof ValueModification valMod))
					continue;
				if (valMod.getReferenceType()!=ShadowrunReference.ITEM_ATTRIBUTE)
					continue;
				if (valMod.getApplyTo()!=null && valMod.getApplyTo()!=ApplyTo.PARENT)
					continue;
				logger.log(Level.DEBUG, "Apply {0} from accessory {1} to {2}", valMod, accessory.getKey(), model.getKey());
				ItemUtil.addOrSetItemAttribute(parent, valMod);
			}
		}

		return new OperationResult<List<Modification>>(unprocessed);
	}


}
