# TWSManagement

## General

Plugin to provide management functionalities for Minecraft servers. Specifically, this plugin was created for the The Wooden Spoon Minecraft Server. You can check out their website @ https://tws.gg/.

### Running the Plugin

This plugin runs on Minecraft SpigotMC 1.6.1.

#### Requirements

The following symbols are used to indicate if a dependency (plugin also running on the server) is essential or not:

- ❗ Essential
- ❓ Not Essential

To run this plugin you must have the following dependencies also running on your Minecraft Server:

- ❓ [TAB](https://www.spigotmc.org/resources/tab-1-5-x-1-16-1-rgb-support.57806/) - Displays the AFK suffix tag next to your name, in the tab leaderboard.
- ❓ [LuckPerms](https://www.spigotmc.org/resources/luckperms.28140/) - Used when checking if players have a permission node when automatically unwhitelisting.

## Developers

### Build

To build this you will need to download the dependencies (not hosted in a repository) to a folder `lib`. This folder must exist at the root of the project. You will need to download the following dependencies from SpigotMC:

1. [TAB v2.8.1](https://www.spigotmc.org/resources/tab-1-5-x-1-16-1-rgb-support.57806/update?update=343537)

After downloading the dependencies, move them to the `lib` folder and rename them as they are found in the `build.gradle` file.
