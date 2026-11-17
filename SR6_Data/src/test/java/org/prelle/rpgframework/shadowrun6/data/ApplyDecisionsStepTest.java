package org.prelle.rpgframework.shadowrun6.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.items.ApplyDecisionsStep;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;
import de.rpgframework.shadowrun6.items.ItemTemplate;
import de.rpgframework.shadowrun6.items.SR6GearTool;
import de.rpgframework.shadowrun6.items.SR6ItemAttribute;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * Tests for {@link ApplyDecisionsStep} with choices whose 'ref' does not
 * name an element of the chosen type itself (COM6-1050).
 */
public class ApplyDecisionsStepTest {

	/** soft_engineering: choice of RATING */
	private final static UUID CHOICE_RATING = UUID.fromString("c2d17c87-1cfe-4355-9877-a20fe09c170d");
	/** soft_engineering: &lt;choice type="SKILLSPECIALIZATION" ref="engineering"/&gt; */
	private final static UUID CHOICE_SPEC   = UUID.fromString("712a1bb4-b761-48a7-b2ba-ae5d36f47166");

	//-------------------------------------------------------------------
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("logdir", "/tmp");
		Locale.setDefault(Locale.ENGLISH);
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init( );
	}

	//-------------------------------------------------------------------
	private static CarriedItem<ItemTemplate> createEngineeringAutosoft() {
		ItemTemplate soft = Shadowrun6Core.getItem(ItemTemplate.class, "soft_engineering");
		assertNotNull("soft_engineering not found", soft);
		Choice specChoice = soft.getChoice(CHOICE_SPEC);
		assertNotNull("Specialization choice not found", specChoice);
		assertEquals(ShadowrunReference.SKILLSPECIALIZATION, specChoice.getChooseFrom());
		assertEquals("engineering", specChoice.getTypeReference());

		CarriedItem<ItemTemplate> item = new CarriedItem<ItemTemplate>(soft, null, CarryMode.EMBEDDED);
		item.addDecision(new Decision(CHOICE_RATING, "6"));
		item.addDecision(new Decision(CHOICE_SPEC, "engineering/automotive_mechanic"));
		return item;
	}

	//-------------------------------------------------------------------
	/**
	 * The 'ref' of a SKILLSPECIALIZATION choice names the skill whose
	 * specializations are selectable. It must not be resolved as a
	 * specialization.
	 */
	@Test
	public void testSkillSpecializationChoiceWithSkillRef() {
		Shadowrun6Character model = new Shadowrun6Character();
		CarriedItem<ItemTemplate> item = createEngineeringAutosoft();

		ApplyDecisionsStep step = new ApplyDecisionsStep();
		OperationResult<List<Modification>> result = step.process(true, ShadowrunReference.ITEM_ATTRIBUTE, model, item, new ArrayList<>());

		assertNotNull(result);
		assertTrue("Unexpected messages: "+result.getMessages(), result.getMessages().isEmpty());
		// The other decision of the same item must still be applied
		assertNotNull(item.getAsValue(SR6ItemAttribute.RATING));
		assertEquals(6, item.getAsValue(SR6ItemAttribute.RATING).getDistributed());
	}

	//-------------------------------------------------------------------
	/**
	 * The full recalculation of the item must not be aborted by the
	 * specialization choice - otherwise the price is missing.
	 */
	@Test
	public void testRecalculateWithSkillSpecializationChoice() {
		Shadowrun6Character model = new Shadowrun6Character();
		CarriedItem<ItemTemplate> item = createEngineeringAutosoft();

		SR6GearTool.recalculate("", model, item);

		assertNotNull("PRICE missing - recalculation aborted", item.getAsValue(SR6ItemAttribute.PRICE));
		assertEquals(3000, item.getAsValue(SR6ItemAttribute.PRICE).getModifiedValue());
	}

}
