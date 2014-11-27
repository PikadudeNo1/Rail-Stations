package com.pikadudeno1;
import java.util.EnumSet;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		for (World world : getServer().getWorlds()) {
			for (Vehicle cart : world.getEntitiesByClass(Vehicle.class)) {
				if ( !minecartEntities.contains(cart.getType()) ) { continue; }
				tryLock(cart);
			}
		}
	}
	
	@EventHandler
	public void minecartMotion(VehicleMoveEvent event) {
		Vehicle cart = event.getVehicle();
		if ( !minecartEntities.contains(cart.getType()) ) { return; }
		
		boolean locked = isLocked(cart);
		if (!locked) {
			locked = tryLock(cart);
			if ( !locked ) { return; }
		}
		if (locked) {
			cart.setVelocity(new Vector()); // 0 vector
			Location here = cart.getLocation();
			here.setX( here.getBlockX() + 0.5 );
			here.setZ( here.getBlockZ() + 0.5 );
			/* The brief disembark is noticeable for players the first tick this happens.
			There might be a way to prevent that. */
			moveMinecart(cart, here);
		}
	}
	
	// TODO: Add handler for
	// http://jd.bukkit.org/rb/doxygen/de/d55/classorg_1_1bukkit_1_1event_1_1vehicle_1_1VehicleDamageEvent.html
	
	private void boostMinecart(Vehicle cart, int x, int z) {
		Location startPos = cart.getLocation();
		startPos.add(x * 0.4, 0, z * 0.4);
		Vector startVel = new Vector(x * 20, 0, z * 20);
		cart.setMetadata("locked", new FixedMetadataValue(this, false));
		moveMinecart(cart, startPos);
		cart.setVelocity(startVel);
	}
	
	private void moveMinecart(Vehicle cart, Location to) {
		Entity passenger = cart.getPassenger();
		if (passenger != null) {
			// Teleporting the cart won't work while it has a passenger.
			Location seated = passenger.getLocation();
			seated.setX(to.getX());
			seated.setZ(to.getZ());
			cart.eject();
			passenger.teleport(seated); // create the illusion that the passenger hasn't moved
			cart.teleport(to);
			cart.setPassenger(passenger);
		} else {
			cart.teleport(to);
		}
	}
	
	private boolean isLocked(Vehicle cart) {
		for (MetadataValue value : cart.getMetadata("locked")) {
			if (value.getOwningPlugin() == this) {
				return value.asBoolean();
			}
		}
		return false;
	}
	
	private boolean tryLock(Vehicle cart) {
		Location here = cart.getLocation();
		Material on = here.getBlock().getType();
		Material beneath = here.clone().add(0, -1, 0).getBlock().getType();
		
		if ( !(on == Material.DETECTOR_RAIL && beneath == Material.GOLD_BLOCK) ) { return false; }
		double xInBlock = here.getX() - here.getBlockX();
		if ( !(xInBlock - 0.5 <= 0.3) ) { return false; }
		double zInBlock = here.getZ() - here.getBlockZ();
		if ( !(zInBlock - 0.5 <= 0.3) ) { return false; }
		
		cart.setMetadata("locked", new FixedMetadataValue(this, true));
		return true;
	}
}
