package org.prelle.rpgframework.shadowrun6.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.items.OperationMode;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;
import de.rpgframework.shadowrun6.items.ItemHook;
import de.rpgframework.shadowrun6.items.ItemTemplate;
import de.rpgframework.shadowrun6.items.SR6GearTool;
import de.rpgframework.shadowrun6.items.SR6ItemAttribute;

/**
 * Item attribute modifications of accessories: user-installed accessories
 * modify their parent (COM6-1051), included stock accessories do not
 * (COM6-462).
 */
public class AccessoryItemAttributeTest {

	private final static UUID UUID_RATING = UUID.fromString("c2d17c87-1cfe-4355-9877-a20fe09c170d");

	//-------------------------------------------------------------------
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("logdir", "/tmp");
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init( );
	}

	//-------------------------------------------------------------------
	private static CarriedItem<ItemTemplate> create(String id, CarryMode mode) {
		ItemTemplate templ = Shadowrun6Core.getItem(ItemTemplate.class, id);
		assertNotNull(id+" not found", templ);
		CarriedItem<ItemTemplate> item = new CarriedItem<ItemTemplate>(templ, null, mode);
		SR6GearTool.recalculate("", null, item);
		return item;
	}

	//-------------------------------------------------------------------
	private static int body(CarriedItem<ItemTemplate> vehicle) {
		return vehicle.getAsValue(SR6ItemAttribute.BODY).getModifiedValue();
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-1051: GMC Bulldog BOD 16 + increased structural integrity 3 = 19
	 */
	@Test
	public void testUserInstalledModChangesVehicle() {
		CarriedItem<ItemTemplate> vehicle = create("gmc_bulldog", CarryMode.CARRIED);
		assertEquals(16, body(vehicle));

		CarriedItem<ItemTemplate> mod = create("increased_integrity", CarryMode.EMBEDDED);
		mod.addDecision(new Decision(UUID_RATING, "3"));
		vehicle.addAccessory(mod, ItemHook.VEHICLE_CHASSIS);
		SR6GearTool.recalculate("", null, vehicle);
		assertEquals(19, body(vehicle));

		// Recalculating again must not add it a second time
		SR6GearTool.recalculate("", null, vehicle);
		assertEquals(19, body(vehicle));
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-462: Silverback has BOD 14 including its standard increased
	 * structural integrity 4 (Double Clutch)
	 */
	@Test
	public void testIncludedStockModDoesNotChangeVehicle() {
		CarriedItem<ItemTemplate> vehicle = create("shiawase_BD_silverback", CarryMode.CARRIED);
		SR6GearTool.recalculate("", null, vehicle);
		assertEquals(14, body(vehicle));
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-462: Older saves store the included stock accessory in the
	 * accessory list - it must not be applied either.
	 */
	@Test
	public void testStoredIncludedStockModDoesNotChangeVehicle() {
		CarriedItem<ItemTemplate> vehicle = create("shiawase_BD_silverback", CarryMode.CARRIED);
		CarriedItem<ItemTemplate> mod = create("increased_integrity", CarryMode.EMBEDDED);
		mod.addDecision(new Decision(UUID_RATING, "4"));
		vehicle.addAccessory(mod, ItemHook.VEHICLE_CHASSIS);
		SR6GearTool.recalculate("", null, vehicle);
		assertEquals(14, body(vehicle));
	}

	//-------------------------------------------------------------------
	/**
	 * A switchable accessory (laser sight) only gives its bonus while it is
	 * switched on - and only once (COM6-1051 must not apply mode
	 * modifications a second time).
	 * Colt America L36 has Attack Ratings 8/8/6 (SR6 Core, Gear).
	 */
	@Test
	public void testSwitchableAccessoryOnlyWhenActive() {
		CarriedItem<ItemTemplate> weapon = create("colt_america_l36", CarryMode.CARRIED);
		CarriedItem<ItemTemplate> laser  = create("laser_sight", CarryMode.EMBEDDED);
		weapon.addAccessory(laser, ItemHook.TOP);
		SR6GearTool.recalculate("", null, weapon);
		int[] ar = (int[]) weapon.getAsObject(SR6ItemAttribute.ATTACK_RATING).getModifiedValue();
		assertEquals("Laser sight switched off must not change AR", 8, ar[0]);

		OperationMode offline = laser.getResolved().getOperationModes().stream()
				.filter(m -> m.getId().equals("laser_sight.active.offline")).findFirst().orElseThrow();
		laser.setMode(offline, true);
		SR6GearTool.recalculate("", null, weapon);
		ar = (int[]) weapon.getAsObject(SR6ItemAttribute.ATTACK_RATING).getModifiedValue();
		assertEquals("Laser sight switched on: +1", 9, ar[0]);
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-462: Fichetti Security 600 comes with a laser sight and has
	 * Attack Ratings 10/9/6 (SR6 Core, Gear)
	 */
	@Test
	public void testIncludedWeaponAccessory() {
		CarriedItem<ItemTemplate> weapon = create("fichetti_security_600", CarryMode.CARRIED);
		SR6GearTool.recalculate("", null, weapon);
		int[] ar = (int[]) weapon.getAsObject(SR6ItemAttribute.ATTACK_RATING).getModifiedValue();
		assertArrayEquals(new int[] {10,9,6,0,0}, ar);
	}

}
