package com.pikadudeno1;
import java.util.EnumSet;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

/**
 * @author Pikadude No. 1
 */
public class RailStations extends JavaPlugin implements Listener {
	// Only supporting rideable minecarts for now
	public static EnumSet<EntityType> minecartEntities =
			EnumSet.of(EntityType.MINECART);
	public static final int PASSENGER_LOCK_DELAY = 2;
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void minecartMotion(VehicleMoveEvent event) {
		Vehicle cart = event.getVehicle();
		if ( !minecartEntities.contains(cart.getType()) ) { return; }
		
		Location here = cart.getLocation();
		Material on = here.getBlock().getType();
		Material beneath = here.clone().add(0, -1, 0).getBlock().getType();
		if (!isLocked(cart)) {
			// Start gauntlet
			if ( !(on == Material.DETECTOR_RAIL && beneath == Material.GOLD_BLOCK) ) { return; }
			double xInBlock = here.getX() - here.getBlockX();
			if ( !(xInBlock - 0.5 <= 0.3) ) { return; }
			double zInBlock = here.getZ() - here.getBlockZ();
			if ( !(zInBlock - 0.5 <= 0.3) ) { return; }
			// Gauntlet cleared!
			cart.setMetadata("locked", new FixedMetadataValue(this, true));
		}
		if (isLocked(cart)) {
			cart.setVelocity(new Vector()); // 0 vector
			here.setX( here.getBlockX() + 0.5 );
			here.setZ( here.getBlockZ() + 0.5 );
			Entity passenger = cart.getPassenger();
			if (passenger != null) {
				// Teleporting the cart won't work while it has a passenger.
				/* The brief disembark is noticeable for players the first tick this happens.
				There might be a way to prevent that. */
				Location seated = passenger.getLocation();
				seated.setX(here.getX());
				seated.setZ(here.getZ());
				cart.eject();
				passenger.teleport(seated); // create the illusion that the passenger hasn't moved
				cart.teleport(here);
				cart.setPassenger(passenger);
			} else {
				cart.teleport(here);
			}
		}
	}
	
	// TODO: Add handler for
	// http://jd.bukkit.org/rb/doxygen/de/d55/classorg_1_1bukkit_1_1event_1_1vehicle_1_1VehicleDamageEvent.html
	
	private void boostMinecart() {
		
	}
	
	private boolean isLocked(Vehicle cart) {
		for (MetadataValue value : cart.getMetadata("locked")) {
			if (value.getOwningPlugin() == this) {
				return value.asBoolean();
			}
		}
		return false;
	}
}
