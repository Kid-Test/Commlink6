package de.rpgframework.shadowrun6.chargen.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.DowntimeAction;
import de.rpgframework.shadowrun.LifestyleQuality;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.chargen.charctrl.IRejectReasons;
import de.rpgframework.shadowrun6.SR6Lifestyle;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6DowntimeController;
import de.rpgframework.shadowrun6.chargen.gen.SR6TestGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;
import de.rpgframework.shadowrun6.proc.EnsureAttributePresence;

/**
 * Downtime actions of the sixth edition. The actions and their effects
 * come from the data (core and companion downtimeactions.xml):
 * <ul>
 * <li>Work a Contact: Loyalty -1, Heat -2. The contact needs Connection 3+.</li>
 * <li>Pay a Bribe: 1000 nuyen, Heat -1</li>
 * <li>Lie Low: a month, Heat -1 - no yearly limit</li>
 * <li>Find a Fall Guy: any amount of Heat - no minimum</li>
 * <li>Companion: Working for the Man / the People (Karma &lt;-&gt; nuyen),
 *     Improve Reputation (1000 nuyen x new Reputation and 1 Karma),
 *     Buy a Favor (1 Karma or 1000 nuyen x Connection)</li>
 * </ul>
 * Every executed action is an entry in the history, with the nuyen paid.
 *
 * @author prelle
 */
public class SR6DowntimeControllerTest {

	private final static int NUYEN = 10000;
	private final static int KARMA = 10;

	private Shadowrun6Character model;
	private SR6DowntimeController ctrl;

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	private void setup(boolean creation, int heat) {
		model = new Shadowrun6Character();
		new EnsureAttributePresence(model).process(new ArrayList<>());
		model.setNuyen(NUYEN);
		model.setKarmaFree(KARMA);
		model.getAttribute(ShadowrunAttribute.HEAT).setDistributed(heat);
		ctrl = new SR6DowntimeController(new SR6TestGenerator(model), creation);
	}

	//-------------------------------------------------------------------
	private static DowntimeAction action(String id) {
		DowntimeAction ret = Shadowrun6Core.getItem(DowntimeAction.class, id);
		assertNotNull("Missing downtime action "+id, ret);
		return ret;
	}

	//-------------------------------------------------------------------
	private static Decision choose(DowntimeAction action, UUID value) {
		return new Decision(action.getChoices().get(0).getUUID(), value);
	}

	//-------------------------------------------------------------------
	private static Decision amount(DowntimeAction action, int value) {
		return new Decision(action.getChoices().get(0).getUUID(), String.valueOf(value));
	}

	//-------------------------------------------------------------------
	private Contact contact(String name, int connection, int loyalty, int favors) {
		Contact con = new Contact();
		con.setName(name);
		con.setRating(connection);
		con.setLoyalty(loyalty);
		con.setFavors(favors);
		model.addContact(con);
		return con;
	}

	//-------------------------------------------------------------------
	private int value(ShadowrunAttribute key) {
		return model.getAttribute(key).getDistributed();
	}

	//-------------------------------------------------------------------
	private int heat() {
		return value(ShadowrunAttribute.HEAT);
	}

	//-------------------------------------------------------------------
	private static void assertRejected(String key, Possible poss) {
		assertFalse("Should not be possible", poss.get());
		assertNotNull("No reason given", poss.getMostSevere());
		assertEquals(key, poss.getMostSevere().getKey());
	}

	//-------------------------------------------------------------------
	private SR6Lifestyle lifestyle(String id, int monthsPaid) {
		SR6Lifestyle ret = new SR6Lifestyle(Shadowrun6Core.getItem(LifestyleQuality.class, id));
		ret.setDistributed(monthsPaid);
		model.addLifestyle(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	@Test
	public void testActionsComeFromData() {
		setup(false, 0);
		List<String> ids = ctrl.getAvailable().stream().map(DowntimeAction::getId).toList();
		for (String id : List.of("work_contact","pay_bribe","lie_low","fall_guy",
				"work_for_the_man","work_for_the_people","improve_reputation","buy_favor_karma","buy_favor_nuyen"))
			assertTrue("Missing "+id, ids.contains(id));
		// English falls back to the default locale of whoever loaded the bundle first
		assertEquals("Bestechungsgeld zahlen", action("pay_bribe").getName(Locale.GERMAN));
	}

	//-------------------------------------------------------------------
	@Test
	public void testNotDuringCreation() {
		setup(true, 3);
		assertRejected(IRejectReasons.IMPOSS_ONLY_IN_CAREER, ctrl.canExecute(action("pay_bribe"), 1000));
	}

	//-------------------------------------------------------------------
	@Test
	public void testBribe() {
		setup(false, 3);
		DowntimeAction bribe = action("pay_bribe");
		assertEquals(1000, ctrl.getUsualCost(bribe));
		assertTrue(ctrl.canExecute(bribe, 1000).get());

		OperationResult<DataItemModification> result = ctrl.execute(bribe, 1000);
		assertTrue(result.wasSuccessful());
		assertEquals(2, heat());
		assertEquals(NUYEN-1000, model.getNuyen());

		assertEquals(1, model.getHistory().size());
		DataItemModification entry = model.getHistory().get(0);
		assertEquals(ShadowrunReference.DOWNTIME_ACTION, entry.getReferenceType());
		assertEquals("pay_bribe", entry.getKey());
		assertEquals(1000, entry.getMoneyCost());
		assertNotNull(entry.getDate());
		assertEquals(List.of(entry), ctrl.getExecuted());
	}

	//-------------------------------------------------------------------
	@Test
	public void testBribeNeedsHeatAndNuyen() {
		setup(false, 0);
		assertRejected(IRejectReasons.IMPOSS_NO_HEAT, ctrl.canExecute(action("pay_bribe"), 1000));
		assertFalse(ctrl.execute(action("pay_bribe"), 1000).wasSuccessful());
		assertTrue(model.getHistory().isEmpty());

		setup(false, 3);
		model.setNuyen(999);
		assertRejected(IRejectReasons.IMPOSS_NOT_ENOUGH_NUYEN, ctrl.canExecute(action("pay_bribe"), 1000));
		assertRejected(IRejectReasons.IMPOSS_INVALID_COST, ctrl.canExecute(action("pay_bribe"), -1));
	}

	//-------------------------------------------------------------------
	/**
	 * SR6 has no upper limit for Heat - a run can add as much as it wants,
	 * and a downtime action only takes away
	 */
	@Test
	public void testNoHeatCap() {
		setup(false, 20);
		assertTrue(ctrl.execute(action("pay_bribe"), 1000).wasSuccessful());
		assertEquals(19, heat());
	}

	//-------------------------------------------------------------------
	@Test
	public void testUndoBribe() {
		setup(false, 3);
		ctrl.execute(action("pay_bribe"), 1000);
		DataItemModification entry = ctrl.getExecuted().get(0);
		assertTrue(ctrl.canUndo(entry).get());
		assertTrue(ctrl.undo(entry));
		assertEquals(3, heat());
		assertEquals(NUYEN, model.getNuyen());
		assertTrue(model.getHistory().isEmpty());
		assertFalse("Cannot undo twice", ctrl.undo(entry));
	}

	//-------------------------------------------------------------------
	@Test
	public void testWorkAContactNeedsConnection3() {
		setup(false, 5);
		DowntimeAction work = action("work_contact");
		Contact cop    = contact("Sgt. Rivera", 3, 2, 0);
		Contact barman = contact("Barkeeper"  , 2, 4, 0);
		Contact loose  = contact("Acquaintance", 5, 1, 0);

		assertEquals(List.of(cop), ctrl.getSelectableContacts(work));
		assertRejected(IRejectReasons.IMPOSS_CHOICE_MISSING, ctrl.canExecute(work, 0));
		assertRejected(IRejectReasons.IMPOSS_CONTACT_NOT_SUITABLE, ctrl.canExecute(work, 0, choose(work, barman.getId())));
		assertRejected(IRejectReasons.IMPOSS_MIN_LEVEL_REACHED, ctrl.canExecute(work, 0, choose(work, loose.getId())));

		assertTrue(ctrl.execute(work, 0, choose(work, cop.getId())).wasSuccessful());
		assertEquals(1, cop.getLoyalty());
		assertEquals(3, heat());
		DataItemModification entry = ctrl.getExecuted().get(0);
		assertEquals("Sgt. Rivera", entry.getDecision(DowntimeAction.DECISION_NAME).getValue());

		assertTrue(ctrl.undo(entry));
		assertEquals(2, cop.getLoyalty());
		assertEquals(5, heat());
	}

	//-------------------------------------------------------------------
	@Test
	public void testLieLowUsesPrepaidMonthsFirst() {
		setup(false, 2);
		DowntimeAction lieLow = action("lie_low");
		SR6Lifestyle low    = lifestyle("low", 2);
		SR6Lifestyle middle = lifestyle("middle", 0);
		int month = middle.getCostPerMonth();
		assertTrue(month>0);
		assertEquals("Only the unpaid lifestyle costs nuyen", month, ctrl.getUsualCost(lieLow));

		assertTrue(ctrl.execute(lieLow, month).wasSuccessful());
		assertEquals(1, heat());
		assertEquals(1, low.getDistributed());
		assertEquals(NUYEN-month, model.getNuyen());

		assertTrue(ctrl.undo(ctrl.getExecuted().get(0)));
		assertEquals(2, heat());
		assertEquals(2, low.getDistributed());
		assertEquals(NUYEN, model.getNuyen());
	}

	//-------------------------------------------------------------------
	@Test
	public void testLieLowWithoutMoneyFails() {
		setup(false, 2);
		DowntimeAction lieLow = action("lie_low");
		SR6Lifestyle middle = lifestyle("middle", 0);
		model.setNuyen(middle.getCostPerMonth()-1);

		assertTrue(ctrl.getAvailable().contains(lieLow));
		int cost = ctrl.getUsualCost(lieLow);
		assertRejected(IRejectReasons.IMPOSS_NOT_ENOUGH_NUYEN, ctrl.canExecute(lieLow, cost));
		assertFalse(ctrl.execute(lieLow, cost).wasSuccessful());
		assertEquals(2, heat());
		assertTrue(model.getHistory().isEmpty());
	}

	//-------------------------------------------------------------------
	/**
	 * Any amount up to the current Heat - SR6 has no minimum
	 */
	@Test
	public void testFallGuyAnyAmount() {
		setup(false, 7);
		DowntimeAction fallGuy = action("fall_guy");
		assertEquals(7, ctrl.getMaxHeatReduction(fallGuy));
		assertRejected(IRejectReasons.IMPOSS_CHOICE_MISSING, ctrl.canExecute(fallGuy, 0));
		assertRejected(IRejectReasons.IMPOSS_HEAT_AMOUNT, ctrl.canExecute(fallGuy, 0, amount(fallGuy, 8)));

		assertTrue(ctrl.execute(fallGuy, 0, amount(fallGuy, 1)).wasSuccessful());
		assertEquals(6, heat());
		assertTrue(ctrl.execute(fallGuy, 0, amount(fallGuy, 6)).wasSuccessful());
		assertEquals(0, heat());

		assertTrue(ctrl.undo(ctrl.getExecuted().get(1)));
		assertEquals(6, heat());
	}

	//-------------------------------------------------------------------
	@Test
	public void testWorkingForTheManAndThePeople() {
		setup(false, 0);
		assertTrue(ctrl.execute(action("work_for_the_man"), 0).wasSuccessful());
		assertEquals(KARMA-1, model.getKarmaFree());
		assertEquals(NUYEN+2000, model.getNuyen());

		DowntimeAction people = action("work_for_the_people");
		assertEquals(2000, ctrl.getUsualCost(people));
		assertTrue(ctrl.execute(people, 2000).wasSuccessful());
		assertEquals(KARMA, model.getKarmaFree());
		assertEquals(NUYEN, model.getNuyen());

		// Undo gives back what was paid and takes what was earned
		assertTrue(ctrl.undo(ctrl.getExecuted().get(1)));
		assertEquals(KARMA-1, model.getKarmaFree());
		assertEquals(NUYEN+2000, model.getNuyen());

		// Earned Karma that has been spent cannot be given back
		DataItemModification man = ctrl.getExecuted().get(0);
		model.setNuyen(0);
		assertRejected(IRejectReasons.IMPOSS_NOT_ENOUGH_NUYEN, ctrl.canUndo(man));

		model.setKarmaFree(0);
		assertRejected(IRejectReasons.IMPOSS_NOT_ENOUGH_KARMA, ctrl.canExecute(action("work_for_the_man"), 0));
	}

	//-------------------------------------------------------------------
	@Test
	public void testImproveReputation() {
		setup(false, 0);
		DowntimeAction improve = action("improve_reputation");
		model.getAttribute(ShadowrunAttribute.REPUTATION).setDistributed(2);
		assertEquals("1000 x new Reputation", 3000, ctrl.getUsualCost(improve));
		assertTrue(ctrl.execute(improve, 3000).wasSuccessful());
		assertEquals(3, value(ShadowrunAttribute.REPUTATION));
		assertEquals(NUYEN-3000, model.getNuyen());
		assertEquals(KARMA-1, model.getKarmaFree());

		assertTrue(ctrl.undo(ctrl.getExecuted().get(0)));
		assertEquals(2, value(ShadowrunAttribute.REPUTATION));
		assertEquals(NUYEN, model.getNuyen());
		assertEquals(KARMA, model.getKarmaFree());
	}

	//-------------------------------------------------------------------
	@Test
	public void testImproveNegativeReputation() {
		setup(false, 0);
		model.getAttribute(ShadowrunAttribute.REPUTATION).setDistributed(-3);
		DowntimeAction improve = action("improve_reputation");
		assertEquals("Absolute value of a negative Reputation", 3000, ctrl.getUsualCost(improve));
		assertTrue(ctrl.execute(improve, 3000).wasSuccessful());
		assertEquals(-2, value(ShadowrunAttribute.REPUTATION));
	}

	//-------------------------------------------------------------------
	@Test
	public void testBuyFavor() {
		setup(false, 0);
		Contact fixer = contact("Mags", 4, 2, 0);

		DowntimeAction byKarma = action("buy_favor_karma");
		assertTrue(ctrl.execute(byKarma, 0, choose(byKarma, fixer.getId())).wasSuccessful());
		assertEquals(1, fixer.getFavors());
		assertEquals(KARMA-1, model.getKarmaFree());

		DowntimeAction byNuyen = action("buy_favor_nuyen");
		Decision which = choose(byNuyen, fixer.getId());
		assertEquals("1000 x Connection", 4000, ctrl.getUsualCost(byNuyen, which));
		assertTrue(ctrl.execute(byNuyen, 4000, which).wasSuccessful());
		assertEquals(2, fixer.getFavors());
		assertEquals(NUYEN-4000, model.getNuyen());

		assertTrue(ctrl.undo(ctrl.getExecuted().get(1)));
		assertEquals(1, fixer.getFavors());
		assertEquals(NUYEN, model.getNuyen());
	}

	//-------------------------------------------------------------------
	@Test
	public void testHistorySurvivesSaveAndLoad() throws Exception {
		setup(false, 5);
		DowntimeAction work = action("work_contact");
		Contact cop = contact("Sgt. Rivera", 3, 2, 0);
		ctrl.execute(work, 0, choose(work, cop.getId()));
		ctrl.execute(action("pay_bribe"), 800);

		Shadowrun6Character loaded = Shadowrun6Core.decode(Shadowrun6Core.encode(model));
		assertEquals(2, loaded.getHistory().size());
		DataItemModification first = loaded.getHistory().get(0);
		assertEquals(ShadowrunReference.DOWNTIME_ACTION, first.getReferenceType());
		assertEquals("work_contact", first.getKey());
		assertEquals("-2", first.getDecision(DowntimeAction.DECISION_HEAT).getValue());
		assertEquals(800, loaded.getHistory().get(1).getMoneyCost());
		assertNull(loaded.getHistory().get(1).getDecision(work.getChoices().get(0).getUUID()));

		// Undo still works on a loaded character
		SR6DowntimeController again = new SR6DowntimeController(new SR6TestGenerator(loaded), false);
		assertTrue(again.undo(again.getExecuted().get(1)));
		assertEquals("5 -2 (contact) -1 (bribe) +1 (undo)", 3, loaded.getAttribute(ShadowrunAttribute.HEAT).getDistributed());
		assertEquals(NUYEN, loaded.getNuyen());
	}

}
