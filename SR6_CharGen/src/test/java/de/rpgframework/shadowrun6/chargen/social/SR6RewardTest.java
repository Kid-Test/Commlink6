package de.rpgframework.shadowrun6.chargen.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.Reward;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;
import de.rpgframework.shadowrun6.proc.EnsureAttributePresence;

/**
 * What a run changes: Karma, nuyen, Heat, Reputation, contacts met
 * and a note. Removing the reward takes all of it back.
 *
 * @author prelle
 */
public class SR6RewardTest {

	private Shadowrun6Character model;

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	private void setup() {
		model = new Shadowrun6Character();
		new EnsureAttributePresence(model).process(new ArrayList<>());
		model.setKarmaFree(10);
		model.setNuyen(1000);
		model.getAttribute(ShadowrunAttribute.HEAT).setDistributed(1);
	}

	//-------------------------------------------------------------------
	private static Reward reward(int karma, int nuyen) {
		Reward ret = new Reward();
		ret.setTitle("Food Fight");
		ret.setExperiencePoints(karma);
		ret.setMoney(nuyen);
		return ret;
	}

	//-------------------------------------------------------------------
	private static void change(Reward reward, ShadowrunAttribute key, int value) {
		reward.addIncomingModification(new ValueModification(ShadowrunReference.ATTRIBUTE, key.name(), value));
	}

	//-------------------------------------------------------------------
	private int value(ShadowrunAttribute key) {
		return model.getAttribute(key).getDistributed();
	}

	//-------------------------------------------------------------------
	@Test
	public void testKarmaNuyenAndHeat() {
		setup();
		Reward reward = reward(5, 3000);
		change(reward, ShadowrunAttribute.HEAT, 2);
		Shadowrun6Tools.applyReward(model, reward);
		assertEquals(15, model.getKarmaFree());
		assertEquals(4000, model.getNuyen());
		assertEquals(3, value(ShadowrunAttribute.HEAT));

		Shadowrun6Tools.removeReward(model, reward);
		assertEquals(10, model.getKarmaFree());
		assertEquals(1000, model.getNuyen());
		assertEquals(1, value(ShadowrunAttribute.HEAT));
		assertTrue(model.getRewards().isEmpty());
	}

	//-------------------------------------------------------------------
	/**
	 * Was broken: Reputation was changed by the Heat of the reward
	 */
	@Test
	public void testReputationWithoutHeat() {
		setup();
		Reward reward = reward(0, 0);
		change(reward, ShadowrunAttribute.REPUTATION, 1);
		Shadowrun6Tools.applyReward(model, reward);
		assertEquals(1, value(ShadowrunAttribute.REPUTATION));
		assertEquals(1, value(ShadowrunAttribute.HEAT));

		Shadowrun6Tools.removeReward(model, reward);
		assertEquals(0, value(ShadowrunAttribute.REPUTATION));
	}

	//-------------------------------------------------------------------
	@Test
	public void testContactsMetLeaveWithTheReward() {
		setup();
		Contact old = new Contact();
		old.setName("Mags");
		model.addContact(old);
		Contact met = new Contact();
		met.setName("Dr. Okafor");

		Reward reward = reward(2, 0);
		Shadowrun6Tools.applyReward(model, reward, List.of(met));
		assertEquals(List.of(old, met), model.getContacts());
		assertEquals(List.of(met.getId()), Shadowrun6Tools.getContactsOfReward(reward));

		Shadowrun6Tools.removeReward(model, reward);
		assertEquals(List.of(old), model.getContacts());
	}

	//-------------------------------------------------------------------
	@Test
	public void testSurvivesSaveAndLoad() throws Exception {
		setup();
		Contact met = new Contact();
		met.setName("Dr. Okafor");
		Reward reward = reward(2, 500);
		reward.setNote("Paid in certified credsticks");
		change(reward, ShadowrunAttribute.HEAT, 1);
		Shadowrun6Tools.applyReward(model, reward, List.of(met));

		Shadowrun6Character loaded = Shadowrun6Core.decode(Shadowrun6Core.encode(model));
		assertEquals(1, loaded.getRewards().size());
		Reward again = loaded.getRewards().get(0);
		assertEquals("Paid in certified credsticks", again.getNote());
		assertEquals(List.of(met.getId()), Shadowrun6Tools.getContactsOfReward(again));
		assertNotNull(loaded.getContact(met.getId()));

		Shadowrun6Tools.removeReward(loaded, again);
		assertNull(loaded.getContact(met.getId()));
		assertEquals(10, loaded.getKarmaFree());
	}

}
