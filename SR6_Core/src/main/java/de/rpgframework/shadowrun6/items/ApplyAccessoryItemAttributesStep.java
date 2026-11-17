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
import de.rpgframework.genericrpg.modification.EmbedModification;
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
			// COM6-462: A stored accessory may be the counterpart of an included stock accessory
			if (isIncludedStockAccessory(model, accessory)) {
				logger.log(Level.DEBUG, "Don't apply modifications of included stock accessory {0} to {1}", accessory.getKey(), model.getKey());
				continue;
			}
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

	//-------------------------------------------------------------------
	/**
	 * Does the item (or its variant) define an &lt;embed&gt; for the accessory's
	 * template whose effects are already included in the item's stats (see
	 * {@link ApplyStockModificationsStep#isIncludedInStats}, COM6-462)? This matches what
	 * {@link ApplyStockModificationsStep} does for such accessories (COM6-971:
	 * a stored accessory of the same template replaces the stock one).
	 */
	private static boolean isIncludedStockAccessory(CarriedItem<?> model, CarriedItem<?> accessory) {
		List<Modification> stock = new ArrayList<>(model.getModifyable().getOutgoingModifications());
		if (model.getVariant()!=null)
			stock.addAll(model.getVariant().getOutgoingModifications());
		for (Modification mod : stock) {
			if (mod instanceof EmbedModification embed && accessory.getKey().equals(embed.getKey())
					&& ApplyStockModificationsStep.isIncludedInStats(model, embed))
				return true;
		}
		return false;
	}

}
