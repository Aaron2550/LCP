# LCP
A Spigot Plugin that allows you and your Players to claim Regions, simply by placing a Lodestone Block

This is the GitHub Repo for [Lode Claims on Modrinth](https://modrinth.com/mod/lode-claims-plugin)

### Requirements
- Java 16 or newer
- Spigot for Minecraft 1.17 or newer
- WorldGuard 7.0.7 or newer

### Usage
Simply install and configure the Plugin. That's it. No Database or complicated setup requiredd

### Issues and Feedback
Create a new Issue [here](https://github.com/Aaron2550/LCP/issues) to suggest things or get a bug fixed.

### Current Issues
- Only 1 Region per player
- The distance calculation to other regions is a bit wonky as it does not consider that the region is a square
- The owner can mine any lodestone to unclaim the region, not just the one they used to create the region

### Limitations
Keep in mind that WorldGuard cannot override vanilla's spawn protection. This means only server operators can interact with blocks that overlap with the spawn area. You can change `spawn-protection` in your server.properties to remove the vanilla portection and then protect your server's spawn using WorldGuard.
