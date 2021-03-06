package com.pikadudeno1;
import java.util.EnumSet;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

/**
 * @author Pikadude No. 1
 */
public class RailStations extends JavaPlugin implements Listener {
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		for (World world : getServer().getWorlds()) {
			for (Vehicle cart : world.getEntitiesByClass(Minecart.class)) {
				tryLock(cart);
			}
		}
	}
	
	@EventHandler
	public void minecartMotion(VehicleMoveEvent event) {
		Vehicle cart = event.getVehicle();
		if ( !(cart instanceof Minecart) ) { return; }
		
		boolean locked = isLocked(cart);
		if (!locked) {
			locked = tryLock(cart);
			if ( !locked ) { return; }
		}
		if (locked) {
			Location here = cart.getLocation();
			if (here.getBlock().getType() != Material.DETECTOR_RAIL) {
				cart.setMetadata("locked", new FixedMetadataValue(this, false));
				return;
			}
			cart.setVelocity(new Vector()); // 0 vector
			here.setX( here.getBlockX() + 0.5 );
			here.setZ( here.getBlockZ() + 0.5 );
			/* The brief disembark is noticeable for players the first tick this happens.
			There might be a way to prevent that. */
			moveMinecart(cart, here);
		}
	}
	
	@EventHandler
	public void minecartPlacement(VehicleCreateEvent event) {
		Vehicle cart = event.getVehicle();
		if ( !(cart instanceof Minecart) ) { return; }
		tryLock(cart);
	}
	
	@EventHandler
	public void pushUnlock(VehicleEntityCollisionEvent event) {
		Entity pusher = event.getEntity();
		Vehicle cart = event.getVehicle();
		if (!( pusher instanceof Player && !(cart.getPassenger() instanceof Player)
				&& isLocked(cart) )) {
			return;
		}
		Location difference = cart.getLocation().subtract(pusher.getLocation());
		// It doesn't matter what Location object we call setDirection on
		boostMinecart(cart, difference.setDirection( difference.toVector() ));
	}
	
	public static EnumSet<Action> riderUnlockOn =
			EnumSet.of(Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR);
	@EventHandler
	public void riderUnlock(PlayerInteractEvent event) {
		Entity rider = event.getPlayer();
		Vehicle cart;
		if (rider.getVehicle() instanceof Vehicle) {
			cart = (Vehicle) rider.getVehicle();
		} else {
			return;
		}
		if (!( riderUnlockOn.contains(event.getAction()) && isLocked(cart) )) {
			return;
		}
		event.setCancelled(true);
		boostMinecart(cart, rider.getLocation());
	}
	
	private void boostMinecart(Vehicle cart, Location direction) {
		float yaw = direction.getYaw();
		if (yaw < 0) { yaw += 360; }
		if (yaw >= 315 || yaw < 45) {
			boostMinecart(cart, 0, 1);
		}
		else if (yaw >= 45 && yaw < 135) {
			boostMinecart(cart, -1, 0);
		}
		else if (yaw >= 135 && yaw < 225) {
			boostMinecart(cart, 0, -1);
		}
		else if (yaw >= 225 && yaw < 315) {
			boostMinecart(cart, 1, 0);
		}
	}
	private void boostMinecart(Vehicle cart, int x, int z) {
		Location startPos = cart.getLocation();
		Block nextBlock = startPos.clone().add(x, 0, z).getBlock();
		if (nextBlock.getType() == Material.AIR) {
			nextBlock = nextBlock.getRelative(0, -1, 0);
		}
		if ( !(nextBlock.getState().getData() instanceof Rails) ) {
			return;
		}
		startPos.add(x * 0.6, 0, z * 0.6);
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
		// Try to capture cornering carts?
		Location here = cart.getLocation();
		Block on = here.getBlock();
		
		if ( !(on.getType() == Material.DETECTOR_RAIL &&
				on.getRelative(0, -1, 0).getType() == Material.GOLD_BLOCK) ) { return false; }
		if ( ((Rails) on.getState().getData()).isOnSlope() ) { return false; }
		double xInBlock = here.getX() - here.getBlockX();
		if ( !(xInBlock - 0.5 <= 0.3) ) { return false; }
		double zInBlock = here.getZ() - here.getBlockZ();
		if ( !(zInBlock - 0.5 <= 0.3) ) { return false; }
		
		cart.setMetadata("locked", new FixedMetadataValue(this, true));
		return true;
	}
}
