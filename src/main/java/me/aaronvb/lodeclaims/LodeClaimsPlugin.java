package me.aaronvb.lodeclaims;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class LodeClaimsPlugin extends JavaPlugin {

	private static LodeClaimsPlugin PluginInstance;
	static FileConfiguration PluginConfiguration;

	@Override
	public void onEnable() {
		PluginInstance = this;
		PluginConfiguration = this.getConfig();

		this.saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new EventListener(), this);

		//Remove Vanilla Lodestone Recipe and add our own
		if (PluginConfiguration.getBoolean("useCustomRecipe")) {
			Bukkit.getRecipesFor(new ItemStack(Material.LODESTONE, 1)).forEach(recipe -> {
				if (recipe instanceof Keyed key) {
					Bukkit.removeRecipe(key.getKey());
				}
			});

			ShapedRecipe lodestoneRecipe = new ShapedRecipe(new NamespacedKey("lodeclaims", "lodestonerecipe"), new ItemStack(Material.LODESTONE, 1));
			lodestoneRecipe = lodestoneRecipe.shape("SHS", "CIC", "SOS");
			lodestoneRecipe = lodestoneRecipe
					.setIngredient('C', Material.CHISELED_STONE_BRICKS)
					.setIngredient('S', Material.STONE)
					.setIngredient('O', Material.COMPASS)
					.setIngredient('H', Material.SHIELD)
					.setIngredient('I', Material.IRON_INGOT);

			Bukkit.addRecipe(lodestoneRecipe);
		}
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
	}

	public static LodeClaimsPlugin getPluginInstance() {
		return PluginInstance;
	}

	public static FileConfiguration getPluginConfiguration() {
		return PluginConfiguration;
	}
}
