package me.aaronvb.lodeclaims;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class EventListener implements Listener {

	private static final List<Player> RecentClicks = new ArrayList<>();
	private static final List<Block> UnconfirmedLodestones = new ArrayList<>();
	private static final List<AbstractMap.SimpleEntry<Player, Block>> UnconfirmedClicks = new ArrayList<>();

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() != Material.LODESTONE) {
			return;
		}

		Player player = event.getPlayer();
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));

		ProtectedRegion toBeRemoved = null;

		assert manager != null;
		for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
			ProtectedRegion region = entry.getValue();

			if (region.contains(BlockVector3.at(block.getX(), block.getY(), block.getZ()))) {
				if (region.getOwners().contains(player.getUniqueId())) {
					if (UnconfirmedLodestones.contains(block)) {
						UnconfirmedLodestones.remove(block);
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have unclaimed this Region"));

						toBeRemoved = region;
						break;
					} else {
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Mine the Lodestone again to confirm"));
						event.setCancelled(true);

						UnconfirmedLodestones.add(block);
						Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> {
							if (UnconfirmedLodestones.contains(block) && manager.getRegion("Region-" + player.getName()) != null) {
								player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You did not confirm the unclaim"));
							}

							UnconfirmedLodestones.remove(block);
						}, 200);
					}
				} else {
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You are not the Owner of this Region"));
					event.setCancelled(true);
					break;
				}
			}
		}

		if (toBeRemoved != null) {
			manager.removeRegion(toBeRemoved.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Player clickedPlayer)) {
			return;
		}

		Player player = event.getPlayer();
		boolean isInList = false;
		AbstractMap.SimpleEntry<Player, Block> data = null;

		for (AbstractMap.SimpleEntry<Player, Block> entry : UnconfirmedClicks) {
			if (entry.getKey().equals(player)) {
				isInList = true;
				data = entry;
				break;
			}
		}

		if (!isInList) {
			return;
		}

		if (RecentClicks.contains(player)) {
			return;
		}

		RecentClicks.add(player);
		Bukkit.getScheduler().runTaskLater(LodeClaimsPlugin.getPluginInstance(), () -> RecentClicks.remove(player), 100);

		//For some Reason, the event got fired multiple Times, even with the RecentClicks.contains(player) check.
		//So i just want to make sure this works...
		UnconfirmedClicks.remove(data);
		UnconfirmedClicks.remove(data);
		UnconfirmedClicks.remove(data);

		Block block = data.getValue();
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
		assert manager != null;

		ProtectedRegion region = manager.getApplicableRegions(BlockVector3.at(block.getX(), block.getY(), block.getZ())).getRegions().iterator().next();
		DefaultDomain members = region.getMembers();

		if (members.contains(clickedPlayer.getUniqueId())) {
			members.removePlayer(clickedPlayer.getUniqueId());
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(clickedPlayer.getName()  + " was removed from your Region"));
			clickedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(player.getName() + " has removed you from their Region"));
		} else {
			members.addPlayer(clickedPlayer.getUniqueId());
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(clickedPlayer.getName()  + " was added to your Region"));
			clickedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(player.getName() + " has added you to their Region"));
		}

		region.setMembers(members);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		if (block == null || block.getType() != Material.LODESTONE) {
			return;
		}

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
		assert manager != null;

		ApplicableRegionSet regions = manager.getApplicableRegions(BlockVector3.at(block.getX(), block.getY(), block.getZ()));
		ProtectedRegion region = null;
		for (ProtectedRegion protectedRegion : regions.getRegions()) {
			if (protectedRegion.getOwners().contains(player.getUniqueId()) && protectedRegion.getId().toLowerCase().contains("region-")) {
				region = protectedRegion;
				break;
			}
		}

		if (region == null) {
			Bukkit.getLogger().severe("Failed to find User Region");
			return;
		}

		AbstractMap.SimpleEntry<Player, Block> data = new AbstractMap.SimpleEntry<>(player, block);
		UnconfirmedClicks.add(data);
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Click a player to add them to your Region"));

		Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> {
			if (UnconfirmedClicks.contains(data)) {
				UnconfirmedClicks.remove(data);
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("No Player was added to your Region"));
			}
		}, 200);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();

		if (player.isSneaking()) {
			return;
		}

		Block block = event.getBlock();
		if (block.getType() != Material.LODESTONE) {
			return;
		}

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
		BlockVector3 placedBlockVector = BlockVector3.at(block.getX(), block.getY(), block.getZ());

		assert manager != null;
		boolean canCreateRegion = true;

		for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
			ProtectedRegion region = entry.getValue();
			if (region.getOwners().contains(player.getUniqueId())) {
				event.setCancelled(true);

				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have already claimed a Region - Sneak to place without claiming a Region"));
				Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have already claimed a Region - Sneak to place without claiming a Region")), 30);
				Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have already claimed a Region - Sneak to place without claiming a Region")), 30);

				canCreateRegion = false;
				break;
			}

			BlockVector3 middlePoint =  region.getMinimumPoint().add(region.getMaximumPoint());
			middlePoint = middlePoint.divide(2);

			if (middlePoint.distance(placedBlockVector) < 272d) {
				event.setCancelled(true);

				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You are too close to another Region - Sneak to place without claiming a Region"));
				Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You are too close to another Region - Sneak to place without claiming a Region")), 30);
				Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You are too close to another Region - Sneak to place without claiming a Region")), 30);


				canCreateRegion = false;
				break;
			}
		}

		if (!canCreateRegion) {
			return;
		}

		BlockVector3 point1 = placedBlockVector;
		BlockVector3 point2 = placedBlockVector;

		point1 = point1.add(64, 0, 64).withY(block.getWorld().getMaxHeight() - 1);
		point2 = point2.subtract(64, 0, 64).withY(block.getWorld().getMinHeight());

		ProtectedRegion newRegion = new ProtectedCuboidRegion("Region-" + player.getName(), point1, point2);
		DefaultDomain owners = new DefaultDomain();

		owners.addPlayer(player.getUniqueId());
		newRegion.setOwners(owners);
		newRegion.setPriority(10);
		newRegion.setFlag(Flags.GREET_TITLE, player.getName() + "'s Region");
		newRegion.setFlag(Flags.DENY_MESSAGE, "");

		manager.addRegion(newRegion);
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have claimed a 129x129 Region - Sneak to place without claiming a Region"));
		Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have claimed a 129x129 Region - Sneak to place without claiming a Region")), 30);
		Bukkit.getScheduler().scheduleSyncDelayedTask(LodeClaimsPlugin.getPluginInstance(), () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You have claimed a 129x129 Region - Sneak to place without claiming a Region")), 30);
	}
}