package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.character.CharacterIOException;
import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.AttributeValue;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.Priority;
import de.rpgframework.shadowrun.PriorityType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.chargen.charctrl.IMetatypeController;
import de.rpgframework.shadowrun.chargen.gen.PriorityAttributeGenerator;
import de.rpgframework.shadowrun.chargen.gen.PriorityTableController;
import de.rpgframework.shadowrun6.SR6MetaType;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.free.FreeCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.pointbuy.PointBuyCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.PriorityCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.SR6PrioritySettings;
import de.rpgframework.shadowrun6.chargen.gen.priority.SumToTenCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-1029: Minor Awakened qualities in all generators, including
 * saving and loading the character
 */
public class MinorAwakenedPrioTest {

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init();
	}

	//-------------------------------------------------------------------
	@Test
	public void testPriority() throws CharacterIOException {
		runScenario(PriorityCharacterGenerator::new, m -> new PriorityCharacterGenerator(m, null),
				MinorAwakenedPrioTest::preparePriority, MinorAwakenedPrioTest::increaseAdjust);
	}

	//-------------------------------------------------------------------
	@Test
	public void testSumToTen() throws CharacterIOException {
		runScenario(SumToTenCharacterGenerator::new, m -> new SumToTenCharacterGenerator(m, null),
				MinorAwakenedPrioTest::preparePriority, MinorAwakenedPrioTest::increaseAdjust);
	}

	//-------------------------------------------------------------------
	@Test
	public void testKarma() throws CharacterIOException {
		runScenario(KarmaCharacterGenerator::new, m -> new KarmaCharacterGenerator(m, null),
				MinorAwakenedPrioTest::prepareMundane, MinorAwakenedPrioTest::increase);
	}

	//-------------------------------------------------------------------
	@Test
	public void testPointBuy() throws CharacterIOException {
		runScenario(PointBuyCharacterGenerator::new, m -> new PointBuyCharacterGenerator(m, null),
				MinorAwakenedPrioTest::prepareMundane, MinorAwakenedPrioTest::increase);
	}

	//-------------------------------------------------------------------
	private static void preparePriority(SR6CharacterGenerator charGen) {
		@SuppressWarnings("unchecked")
		PriorityTableController<Shadowrun6Character,SR6PrioritySettings> prio = ((PriorityCharacterGenerator)charGen).getPriorityController();
		prio.setPriority(PriorityType.ATTRIBUTE, Priority.A);
		prio.setPriority(PriorityType.SKILLS, Priority.B);
		prio.setPriority(PriorityType.METATYPE, Priority.C);
		prio.setPriority(PriorityType.RESOURCES, Priority.D);
		prio.setPriority(PriorityType.MAGIC, Priority.E);
	}

	//-------------------------------------------------------------------
	private static void prepareMundane(SR6CharacterGenerator charGen) {
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mundane"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testKarmaNotSelectable() {
		assertNotSelectable(new KarmaCharacterGenerator());
	}

	//-------------------------------------------------------------------
	@Test
	public void testPointBuyNotSelectable() {
		assertNotSelectable(new PointBuyCharacterGenerator());
	}

	//-------------------------------------------------------------------
	@Test
	public void testFreeNotSelectable() {
		assertNotSelectable(new FreeCharacterGenerator());
	}

	//-------------------------------------------------------------------
	/** COM6-1035: modOnly types must not show up in selection lists */
	private void assertNotSelectable(SR6CharacterGenerator charGen) {
		charGen.setModel(new Shadowrun6Character(), null);
		charGen.runProcessors();
		MagicOrResonanceType minor = Shadowrun6Core.getItem(MagicOrResonanceType.class, "minor_awakened");
		assertFalse("minor_awakened must only be available via qualities",
				charGen.getMagicOrResonanceController().getAvailable().contains(minor));
	}

	//-------------------------------------------------------------------
	/** Raise with adjustment points */
	private static void increaseAdjust(SR6CharacterGenerator charGen) {
		PriorityAttributeGenerator attribs = (PriorityAttributeGenerator) charGen.getAttributeController();
		AttributeValue<ShadowrunAttribute> magic = charGen.getModel().getAttribute(ShadowrunAttribute.MAGIC);
		Possible poss = attribs.canBeIncreasedPoints(magic);
		assertTrue("Cannot invest adjustment point in MAGIC: "+poss, poss.get());
		assertTrue(attribs.increasePoints(magic).wasSuccessful());
	}

	//-------------------------------------------------------------------
	/** Raise with the generators default currency (Karma, CP) */
	private static void increase(SR6CharacterGenerator charGen) {
		AttributeValue<ShadowrunAttribute> magic = charGen.getModel().getAttribute(ShadowrunAttribute.MAGIC);
		Possible poss = charGen.getAttributeController().canBeIncreased(magic);
		assertTrue("Cannot increase MAGIC: "+poss, poss.get());
		assertTrue(charGen.getAttributeController().increase(magic).wasSuccessful());
	}

	//-------------------------------------------------------------------
	/**
	 * @param newGenerator     Generator for a new character
	 * @param reopenGenerator  Generator for a loaded character - like CharacterGeneratorRegistry.getGenerator()
	 * @param prepare          Make the character mundane
	 * @param increaseMagic    Raise MAGIC by one
	 */
	private void runScenario(Supplier<? extends SR6CharacterGenerator> newGenerator,
			Function<Shadowrun6Character, ? extends SR6CharacterGenerator> reopenGenerator,
			Consumer<SR6CharacterGenerator> prepare,
			Consumer<SR6CharacterGenerator> increaseMagic) throws CharacterIOException {
		SR6CharacterGenerator charGen = newGenerator.get();
		Shadowrun6Character model = new Shadowrun6Character();
		charGen.setModel(model, null);

		prepare.accept(charGen);
		IMetatypeController<SR6MetaType> meta = charGen.getMetatypeController();
		meta.select(Shadowrun6Core.getItem(SR6MetaType.class, "human"));
		// Settings may equal the defaults, so nothing triggered processing yet
		charGen.runProcessors();
		assertNotNull(model.getMagicOrResonanceType());
		assertEquals("mundane", model.getMagicOrResonanceType().getId());

		// Select quality
		SR6Quality sensitive = Shadowrun6Core.getItem(SR6Quality.class, "astral_sensitive");
		assertNotNull(sensitive);
		OperationResult<QualityValue> res = charGen.getQualityController().select(sensitive);
		assertTrue("Selecting failed: "+res, res.wasSuccessful());
		assertEquals("minor_awakened", model.getMagicOrResonanceType().getId());

		// Raise MAGIC
		for (int i=0; i<3; i++) {
			increaseMagic.accept(charGen);
		}
		assertEquals(3, model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue());

		// Process again
		charGen.runProcessors();
		assertEquals("minor_awakened", model.getMagicOrResonanceType().getId());
		assertEquals(3, model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue());

		byte[] raw = Shadowrun6Core.encode(model);

		// Plain loading, e.g. for read-only view or printing
		Shadowrun6Character loaded = Shadowrun6Core.decode(raw);
		Shadowrun6Tools.resolveChar(loaded);
		Shadowrun6Tools.runProcessors(loaded, Locale.getDefault());
		assertEquals("Plain loading", "minor_awakened", loaded.getMagicOrResonanceType().getId());
		assertEquals("Plain loading", 3, loaded.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue());

		// Reopen in generator - like GeneratorWrapper.setWrapped()
		model = Shadowrun6Core.decode(raw);
		Shadowrun6Tools.resolveChar(model);
		charGen = reopenGenerator.apply(model);
		charGen.setModel(model, null);
		charGen.runProcessors();
		assertEquals("Reopened", "minor_awakened", model.getMagicOrResonanceType().getId());
		assertEquals("Reopened", 3, model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue());

		// Deselect quality
		QualityValue qVal = model.getQuality("astral_sensitive");
		assertNotNull(qVal);
		assertTrue(charGen.getQualityController().deselect(qVal));
		assertEquals("mundane", model.getMagicOrResonanceType().getId());
		assertEquals(0, model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue());
	}

}
