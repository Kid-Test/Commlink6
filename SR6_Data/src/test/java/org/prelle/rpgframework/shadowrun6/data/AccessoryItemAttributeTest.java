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
import de.rpgframework.shadowrun6.items.OnRoadOffRoadValue;
import de.rpgframework.shadowrun6.items.SR6PieceOfGearVariant;
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
	private static OnRoadOffRoadValue onOff(CarriedItem<ItemTemplate> vehicle, SR6ItemAttribute attr) {
		return vehicle.getAsObject(attr).getModifiedValue();
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-1052: GMC Bulldog Handling 5/7, Acceleration 10, Speed Interval 10
	 * + Improved Handling 1 (Double Clutch p.125: -1 on- and off-road)
	 * + Smart Tires (Double Clutch p.133: +5 Acceleration, +10 Speed Interval)
	 */
	@Test
	public void testOnRoadOffRoadModifications() {
		CarriedItem<ItemTemplate> vehicle = create("gmc_bulldog", CarryMode.CARRIED);
		assertEquals("5/7", onOff(vehicle, SR6ItemAttribute.HANDLING).toString());

		CarriedItem<ItemTemplate> handling = create("improved_handling", CarryMode.EMBEDDED);
		handling.addDecision(new Decision(UUID_RATING, "1"));
		vehicle.addAccessory(handling, ItemHook.VEHICLE_CHASSIS);
		ItemTemplate tires = Shadowrun6Core.getItem(ItemTemplate.class, "tires_large");
		CarriedItem<ItemTemplate> smart = new CarriedItem<ItemTemplate>(tires, (SR6PieceOfGearVariant)tires.getVariant("smart"), CarryMode.EMBEDDED);
		SR6GearTool.recalculate("", null, smart);
		vehicle.addAccessory(smart, ItemHook.VEHICLE_TIRES);
		SR6GearTool.recalculate("", null, vehicle);
		SR6GearTool.recalculate("", null, vehicle);

		assertEquals("4/6", onOff(vehicle, SR6ItemAttribute.HANDLING).toString());
		assertEquals("15" , onOff(vehicle, SR6ItemAttribute.ACCELERATION).toString());
		assertEquals("20" , onOff(vehicle, SR6ItemAttribute.SPEED_INTERVAL).toString());
	}

	//-------------------------------------------------------------------
	/**
	 * COM6-1052: Double Clutch p.126 - the final modified Handling may never
	 * drop below one. GMC Bulldog 5/7 with Improved Handling 2 (= 3/5) and
	 * a second one (= 1/3) and a third (would be -1/1)
	 */
	@Test
	public void testHandlingNotBelowOne() {
		CarriedItem<ItemTemplate> vehicle = create("gmc_bulldog", CarryMode.CARRIED);
		for (int i=0; i<3; i++) {
			CarriedItem<ItemTemplate> handling = create("improved_handling", CarryMode.EMBEDDED);
			handling.addDecision(new Decision(UUID_RATING, "2"));
			vehicle.addAccessory(handling, ItemHook.VEHICLE_CHASSIS);
		}
		SR6GearTool.recalculate("", null, vehicle);
		assertEquals("1", onOff(vehicle, SR6ItemAttribute.HANDLING).toString());
	}

	//-------------------------------------------------------------------
	/**
	 * The stat blocks of vehicles and drones already contain their standard
	 * equipment - stock accessories must not change them again (Double Clutch):
	 * Dassault Peregrine (enhanced sensors 1, improved handling 1): HAND 2, SENS 5;
	 * Saeder-Krupp Blitz Mk II (increased top speed 3): TOP SPD 310;
	 * Vulcan Hollenfaust (milspec plate 3): SPD INT 15
	 */
	@Test
	public void testVehicleStatBlocksIncludeStockAccessories() {
		CarriedItem<ItemTemplate> peregrine = create("dassault_peregrine", CarryMode.CARRIED);
		SR6GearTool.recalculate("", null, peregrine);
		assertEquals("2", onOff(peregrine, SR6ItemAttribute.HANDLING).toString());
		assertEquals(5, (int)peregrine.getAsValue(SR6ItemAttribute.SENSORS).getModifiedValue());

		CarriedItem<ItemTemplate> blitz = create("saeder_blitz", CarryMode.CARRIED);
		SR6GearTool.recalculate("", null, blitz);
		assertEquals(310, (int)blitz.getAsValue(SR6ItemAttribute.TOPSPEED).getModifiedValue());

		CarriedItem<ItemTemplate> hollenfaust = create("vulcan_hollenfaust", CarryMode.CARRIED);
		SR6GearTool.recalculate("", null, hollenfaust);
		assertEquals("15", onOff(hollenfaust, SR6ItemAttribute.SPEED_INTERVAL).toString());
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
