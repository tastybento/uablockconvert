package com.wasteofplastic.uablockconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import us.talabrek.ultimateskyblock.PlayerInfo;

import com.evilmidget38.UUIDFetcher;

public class UABlockConverter extends JavaPlugin implements Listener {
    File plugins;
    File ASkyBlockConfig;
    File uSkyBlockConfig;
    FileConfiguration ASkyBlockConf;
    FileConfiguration uSkyBlockConf;
    // List of names that will be sent to Mojang to be converted to UUIDs
    List<String> playerNames = new ArrayList<String>();
    // Internal list of player objects with name as a key. Can be accessed case-insensitively.
    CaseInsensitiveMap players = new CaseInsensitiveMap();
    boolean UUIDflag;
    boolean cancelled;
    BukkitTask check;
    // True if this is <2.0 of uSkyblock
    boolean oldVersion;
    // Offline conversion
    boolean offline = false;

    Map<String, UUID> response = new HashMap<String, UUID>();

    @Override
    public void onEnable() {
	// Check to see if USkyBlock is active or ASkyBlock
	if (getServer().getPluginManager().isPluginEnabled("uSkyBlock")) {
	    getLogger().severe("uSkyBlock is active - please remove uskyblock.jar from plugins before running this converter.");
	    getServer().getPluginManager().disablePlugin(this);
	}
	if (getServer().getPluginManager().isPluginEnabled("ASkyBlock")) {
	    getLogger().severe("A SkyBlock is active - please remove askyblock.jar from plugins before running this converter.");
	    getServer().getPluginManager().disablePlugin(this);
	}
	// Check that directories exist
	plugins = getDataFolder().getParentFile();
	uSkyBlockConfig = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "config.yml");
	if (!uSkyBlockConfig.exists()) {
	    getLogger().severe("There appears to be no uSkyBlock folder or config in the plugins folder!");
	    getServer().getPluginManager().disablePlugin(this);
	} else {
	    getLogger().info("Found uSkyBlock config.");
	    // Now look for the version based on the folder structure (which is most important)
	    File islands = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "islands");
	    if (!islands.exists()) {
		getLogger().info("Could not find an islands folder, so this looks like the old version of uSkyblock");
		oldVersion = true;
	    } else {
		getLogger().info("Found islands folder, so this looks like the new version of uSkyblock");
		oldVersion = false;
	    }
	}
	ASkyBlockConfig = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "config.yml");
	if (!ASkyBlockConfig.exists()) {
	    getLogger().severe("There appears to be no ASkyBlock folder or config in the plugins folder!");
	    getServer().getPluginManager().disablePlugin(this);
	} else {
	    getLogger().info("Found ASkyBlock config in the plugins folder.");
	}
    }
    @Override
    public void onDisable() {
	getLogger().info("uSkyblock to A Skyblock converter disabled");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
	// Check if this should be run offline completely or not
	if (args.length > 0) {
	    if (args[0].equalsIgnoreCase("offline")) {
		if (!getServer().getOnlineMode()) {
		    sender.sendMessage(ChatColor.GOLD + "All UUID's for players will be offline UUID's");
		    offline = true;
		} else {
		    sender.sendMessage(ChatColor.RED + "Server MUST be in offline mode to use this option! Put server in offline mode then run again.");
		    return true;
		}
	    }
	}
	// Just do it
	sender.sendMessage(ChatColor.GREEN + "Starting conversion...");
	// Set up configs first
	ASkyBlockConf = YamlConfiguration.loadConfiguration(ASkyBlockConfig);
	uSkyBlockConf = YamlConfiguration.loadConfiguration(uSkyBlockConfig);
	/*  USkyblock config (NEW):
	 * options:
  general:
    maxPartySize: 4
    worldName: skyworld
    spawnSize: 150
    cooldownInfo: 30
    cooldownRestart: 600
    biomeChange: 3600
  island:
    schematicName: yourschematicname
    distance: 110
    removeCreaturesByTeleport: false
    height: 150
    chestItems: 79:2 360:1 81:1 327:1 40:1 39:1 361:1 338:1 323:1
    addExtraItems: true
    extraPermissions:
      smallbonus: 4:16 320:5
      mediumbonus: 50:16 327:1
      largebonus: 3:5 12:5
      giantbonus: 2:1 110:1
      extremebonus: 352:8 263:4
      donorbonus: 261:1 262:32 272:1
    protectWithWorldGuard: false
    protectionRange: 105
    allowPvP: deny
    allowIslandLock: true
    useOldIslands: false
    useIslandLevel: true
    useTopTen: true

	 */
	// Chest items
	String chestItems = uSkyBlockConf.getString("options.island.chestItems","");
	getLogger().info("uSkyBlock: Chest items = " + chestItems);
	String aChestItems = "";
	if (!chestItems.isEmpty()) {
	    // Parse
	    String[] items = chestItems.split(" ");
	    for (String item : items){
		//getLogger().info("DEBUG: parsing = " + item);
		String[] split = item.split(":");
		Material material = Material.getMaterial(Integer.valueOf(split[0]));
		if (material != null) {
		    if (aChestItems.isEmpty()) {
			aChestItems = material.toString() + ":" + split[1]; 
		    } else {
			aChestItems = aChestItems + " " + material.toString() + ":" + split[1]; 
		    }
		}
	    }
	    getLogger().info("ASkyBlock: Chest items = " + aChestItems);
	    ASkyBlockConf.set("island.chestItems", aChestItems);
	}
	// World name
	String world = uSkyBlockConf.getString("options.general.worldName","skyworld");
	ASkyBlockConf.set("general.worldName", world );
	// reset wait
	ASkyBlockConf.set("general.resetwait", uSkyBlockConf.getInt("options.general.cooldownRestart",600));
	// distance
	ASkyBlockConf.set("island.distance", uSkyBlockConf.getInt("options.island.distance",110));
	ASkyBlockConf.set("island.protectionRange", uSkyBlockConf.getInt("options.island.protectionRange",105));
	// Height
	int height = uSkyBlockConf.getInt("options.island.height",150);
	ASkyBlockConf.set("general.islandlevel", height);
	// PVP
	ASkyBlockConf.set("island.allowPVP", uSkyBlockConf.getString("options.island.allowPVP","deny"));
	// Teleport mob removal
	ASkyBlockConf.set("general.islandremovemobs", uSkyBlockConf.getBoolean("options.island.removeCreaturesByTeleport",false));
	ASkyBlockConf.set("general.loginremovemobs", uSkyBlockConf.getBoolean("options.island.removeCreaturesByTeleport",false));
	// Max team size
	ASkyBlockConf.set("island.maxteamsize", uSkyBlockConf.getInt("options.general.maxPartySize",4));
	try {
	    ASkyBlockConf.save(ASkyBlockConfig);
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	sender.sendMessage(ChatColor.GREEN + "Completed config.yml transfer");

	// If new version
	if (!oldVersion) {
	    // Go to the islands folder and see how many there are
	    File islandDir = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "islands");
	    if (!islandDir.exists()) {
		sender.sendMessage(ChatColor.RED + "There are no islands folder in uSkyBlock!");
		return true;
	    }
	    // Make an islands folder in ASkyBlock too
	    File asbIslandDir = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "islands");
	    if (!asbIslandDir.exists()) {
		asbIslandDir.mkdir();
	    }
	    int total = islandDir.listFiles().length-2;
	    sender.sendMessage("There are " + total + " islands to convert");
	    int count = 1;
	    // General idea - load all the data, do the name lookups then create the new files

	    for (File island : islandDir.listFiles()) {
		// Ignore the null filename
		if (!island.getName().equalsIgnoreCase("null.yml") && island.getName().endsWith(".yml") && !island.getName().equalsIgnoreCase(".yml")) {
		    String islandName = island.getName().substring(0, island.getName().length() -4);
		    if (!islandName.isEmpty()) {
			sender.sendMessage("Loading island #" + (count++) + " of " + total + " at location " + islandName);
			// Copy the name to the ASkyBlock folder
			File newIsland = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "islands" + File.separator + islandName + ".yml");
			// Find out who the owners are of this island
			YamlConfiguration config = new YamlConfiguration();
			try {
			    // Save file
			    //sender.sendMessage("DEBUG: Saving " + newIsland.getAbsolutePath());
			    newIsland.createNewFile();
			    config.load(island);
			    // Get island info
			    // Location
			    String[] split = islandName.split(",");
			    String islandLocation = world + ":" + split[0] + ":" + height + ":" + split[1];
			    // Get island level
			    int level = config.getInt("general.level",0);
			    // Get the island leader
			    String islandOwnerName = config.getString("party.leader","");
			    if (!islandOwnerName.isEmpty()) {
				getLogger().info("Leader is :"+islandOwnerName);
				// Create this player
				Players leader = new Players(this,islandOwnerName);
				leader.setHasIsland(true);
				leader.setIslandLocation(islandLocation);

				// Problem - will be recalculated
				leader.setIslandLevel(level);
				
				/*
				 * Scenarios:
				 * Players cannot be on more than one team. If when adding team members they are found to be already
				 * known, they are not added to the team. If later they are found to have their own island, or be a leader
				 * of a team, then they are removed from that team and made the leader. If they are found to have more
				 * than one leadership position, the first one found counts.
				 * 
				 * 1. Player is "leader" of an island and nothing else - this means he is the owner of an island
				 * 1.1 Player is a leader and has party members
				 * 2. Player is leader of more than one island
				 * 2.1 All of those islands have no party members
				 * 2.2 Some of those islands have party members
				 * 3. Player is a leader of one island, but also is a member of a team
				 * 3.1 Player is a leader and also a member of more than one team
				 * 3.2 Player is a leader of one island team and leader of another island team
				 * 4. Player is not a leader of any island, but is a member of a team
				 * 4.1 The player is a member of more than one team
				 * 
				 * 
				 */
				
				if (players.containsKey(islandOwnerName)) {
				    getLogger().severe(islandOwnerName + " already known.");
				    // The player is already known. That means they are duplicated somewhere
				    // Find out if they are a team leader, or a team member
				    Players leaderCheck = players.get(islandOwnerName);
				    // if it's the same island, ignore
				    if (!leader.getIslandLocation().equals(leaderCheck.getIslandLocation())){
					// Check to see if this player is in a team. If they are not, then they are just a leader of their own island
					if (leaderCheck.inTeam()) {
					    // They are in a team.
					    // Find out who the team leader is
					    if (leaderCheck.getTeamLeaderName().equalsIgnoreCase(islandOwnerName)) {
						// Scenario #3.2
						getLogger().severe(islandOwnerName + " is already a leader of an island team!");
						getLogger().severe("Island is at " + leaderCheck.getIslandLocation());
						getLogger().severe("Team members are: ");
						for (String member : leaderCheck.getMemberNames()) {
						    getLogger().severe("   " + member);
						}
						getLogger().severe("So skipping this entry. Island at " + leader.getIslandLocation() + " will be unowned.");
					    } else {
						// This player is in a team, but not the leader
						// Scenario #3
						String oldLeaderName = leaderCheck.getTeamLeaderName();
						ConfigurationSection party = config.getConfigurationSection("party.members");
						if (party.getKeys(false).size() == 1) {
						    getLogger().severe(islandOwnerName + " is in a party of just themselves. Ignoring.");
						} else {
						    getLogger().severe(islandOwnerName + " already in a team lead by " + oldLeaderName + ", but also leads a team. Leadership take precidence - removing from previous team...");
						    getLogger().severe("There are " + party.getKeys(false).size() + " members in the new party");
						    // The island leadership role takes priority
						    players.put(islandOwnerName,leaderCheck);

						    // Step through the names on this island
						    for (String name : party.getKeys(false)) {
							getLogger().info(name);
							if (!name.equals(islandOwnerName) && !name.isEmpty()) {
							    // Check to see if this player is already on a team
							    if (players.containsKey(name)) {
								getLogger().info(name + " is already on a team or a team leader so cannot be on this team");
							    } else {
								getLogger().info("Adding " + name + " to new team.");
								// Team member
								Players teamMember = new Players(this,name);
								leaderCheck.addTeamMember(name);
								leaderCheck.addTeamMember(islandOwnerName);
								leaderCheck.setTeamLeaderName(islandOwnerName);
								leaderCheck.setTeamIslandLocation(islandLocation);
								leaderCheck.setInTeam(true);
								teamMember.setTeamLeaderName(islandOwnerName);
								teamMember.setTeamIslandLocation(islandLocation);
								teamMember.setInTeam(true);
								players.put(name,teamMember);
								playerNames.add(name);
							    }
							} 
						    }


						    // Clean up the previous team
						    Players oldLeader = players.get(oldLeaderName);
						    getLogger().severe("The old team members are:");
						    for (String member : oldLeader.getMemberNames()) {
							if (member.equalsIgnoreCase(oldLeaderName)) {
							    member = member + " (Leader)";
							}
							getLogger().severe("   " + member);
						    }
						    oldLeader.removeMember(islandOwnerName);
						    getLogger().severe("Reduced team size is now " + oldLeader.getMemberNames().size());
						    if (oldLeader.getMemberNames().size() == 1) {
							getLogger().severe("Team is now only 1 so removing team from " + oldLeaderName);
							// Need to clear the team now
							oldLeader.setInTeam(false);
							oldLeader.setMemberNames(new ArrayList<String>());
							oldLeader.setTeamIslandLocation("");
							leaderCheck.setTeamLeaderName("");
						    }
						}

					    }
					} else {
					    // Scenario #2
					    // They have their own island already, but with no team. Team will take precedence
					    getLogger().severe(islandOwnerName + " had an island of their own already so cannot be leader of another island!");
					    /*
					    getLogger().severe(leaderName + " had an island of their own and also are leader of an island! Single island ownership will be removed.");
					    getLogger().severe("The island at " + leaderCheck.getIslandLocation() + " will be unowned");
					    leaderCheck.setIslandLocation(islandLocation);
					    // The island leadership role takes priority
					    players.put(leaderName,leaderCheck);
					    ConfigurationSection party = config.getConfigurationSection("party.members");
					    // Step through the names on this island
					    for (String name : party.getKeys(false)) {
						//getLogger().info("DEBUG: name in file = " + name);
						if (!name.equals(leaderName) && !name.isEmpty()) {
						    // Check to see if this player is already on a team
						    if (players.containsKey(name)) {
							getLogger().info(name + " is already on a team or a team leader so cannot be on this team");
						    } else {
							getLogger().info("Adding " + name + " to new team.");
							// Team member
							Players teamMember = new Players(this,name);
							leaderCheck.addTeamMember(name);
							leaderCheck.addTeamMember(leaderName);
							leaderCheck.setTeamLeaderName(leaderName);
							leaderCheck.setTeamIslandLocation(islandLocation);
							leaderCheck.setInTeam(true);
							teamMember.setTeamLeaderName(leaderName);
							teamMember.setTeamIslandLocation(islandLocation);
							teamMember.setInTeam(true);
							players.put(name,teamMember);
							playerNames.add(name);
						    }
						} 
					    }
					    */
					}
				    } else {
					getLogger().info(islandOwnerName + " duplicate found");
				    }
				} else {
				    // Scenario #1
				    playerNames.add(islandOwnerName);
				    players.put(islandOwnerName,leader);
				    
				    // Check to see if there are any party members
				    // Scenario #1.1
				    ConfigurationSection party = config.getConfigurationSection("party.members");
				    // Step through the names on this island
				    for (String name : party.getKeys(false)) {
					//getLogger().info("DEBUG: name in file = " + name);
					if (!name.equals(islandOwnerName) && !name.isEmpty()) {
					    // Check to see if this player is already on a team
					    if (players.containsKey(name)) {
						// Duplicate found. Cannot add to team no matter what.
						getLogger().info(name + " is already on a team or a team leader so cannot be on this team");
						// Result is that player ends up on the first team they are assigned to.
					    } else {
						getLogger().info("Adding team member " + name);
						// Team member
						Players teamMember = new Players(this,name);
						leader.addTeamMember(name);
						leader.addTeamMember(islandOwnerName);
						leader.setTeamLeaderName(islandOwnerName);
						leader.setTeamIslandLocation(islandLocation);
						leader.setInTeam(true);
						teamMember.setTeamLeaderName(islandOwnerName);
						teamMember.setTeamIslandLocation(islandLocation);
						teamMember.setInTeam(true);
						players.put(name,teamMember);
						playerNames.add(name);
					    }
					} 
				    }
				}
			    }
			} catch (FileNotFoundException e) {
			    sender.sendMessage(islandName + " suddenly disappeared! Skipping...");
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (IOException e) {
			    sender.sendMessage(islandName + " problem! Skipping...");
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (InvalidConfigurationException e) {
			    sender.sendMessage(islandName + " YAML is corrupted! Skipping...");
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (Exception e) {
			    sender.sendMessage(islandName + " problem! Skipping...");
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}

		    }
		}
	    }
	    sender.sendMessage(ChatColor.GREEN + "Loaded islands. Now loading home locations of players...");
	    // We now have a full list of all player's names and have build player objects and teams. 
	    // Next we need to grab all the home locations
	    // Go to the islands folder and see how many there are
	    File playerDir = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "players");
	    if (!playerDir.exists()) {
		sender.sendMessage(ChatColor.RED + "There is no players folder in uSkyBlock!");
		return true;
	    }
	    int totalPlayers = playerDir.listFiles().length-1;
	    sender.sendMessage(ChatColor.DARK_BLUE + "There are " + totalPlayers + " files in the players folder");
	    int playerCount = 0;
	    for (File playerFile : playerDir.listFiles()) {
		// Only grab yml files
		String playerFileName = playerFile.getName();
		if (playerFileName.endsWith(".yml")) {
		    String playerName = playerFileName.substring(0, playerFileName.length()-4);
		    //sender.sendMessage("Loading home for " + playerName + ", player " + (playerCount++) + " of " + totalPlayers);
		    if (playerNames.contains(playerName)) {
			playerCount++;
			Players thisPlayer = players.get(playerName);
			YamlConfiguration p = new YamlConfiguration();
			try {
			    p.load(playerFile);
			    String hl = world + ":" + p.getInt("player.homeX") + ":" + p.getInt("player.homeY") + ":" + p.getInt("player.homeZ");
			    thisPlayer.setHL(hl);
			} catch (Exception e) {
			    sender.sendMessage("Problem with " + playerName + " skipping");
			}
		    }
		}
	    }
	    sender.sendMessage(ChatColor.DARK_BLUE + "Found " + playerCount + " players with islands.");
	} else {
	    // Old version
	    // Only player folder
	    // Make an islands folder in ASkyBlock
	    File asbIslandDir = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "islands");
	    if (!asbIslandDir.exists()) {
		asbIslandDir.mkdir();
	    } 
	    // Go to the islands folder and see how many there are
	    File oldPlayerDir = new File(plugins.getPath() + File.separator + "uSkyblock" + File.separator + "players");
	    if (oldPlayerDir.exists())
	    {
		// Load players
		for (File player : oldPlayerDir.listFiles()) {
		    try
		    {
			PlayerInfo playerInfo = (PlayerInfo)SLAPI.load(player.getAbsolutePath());
			// Transfer the player info
			Players newPlayer = new Players(this, playerInfo.getPlayerName());
			playerNames.add(player.getName());
			newPlayer.setHasIsland(playerInfo.getHasIsland());
			if (playerInfo.getHL() != null) {
			    //getLogger().info("DEBUG: Location is " + playerInfo.getHL());
			    newPlayer.setHL(playerInfo.getHL());
			}
			newPlayer.setInTeam(playerInfo.getHasParty());
			if (playerInfo.getIL() != null) {
			    newPlayer.setIslandLocation(playerInfo.getIL());
			    if (playerInfo.getHasIsland()) {
				// Write this island location to the list of islands
				// Copy the name to the ASkyBlock folder
				// Build the island name
				String islandName = playerInfo.getIslandLocation().getBlockX() + "," + playerInfo.getIslandLocation().getBlockZ();
				File newIsland = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "islands" + File.separator + islandName + ".yml");
				newIsland.createNewFile();
			    }
			}
			if (playerInfo.getPIL() != null)
			    newPlayer.setTeamIslandLocation(playerInfo.getPIL());
			newPlayer.setIslandLevel(playerInfo.getIslandLevel());
			if (playerInfo.getMembers() != null) {
			    newPlayer.setMemberNames(playerInfo.getMembers());
			    // Add members to the list (may not be required)
			}
			if (playerInfo.getPartyLeader() != null)
			    newPlayer.setTeamLeaderName(playerInfo.getPartyLeader());
			players.put(player.getName(), newPlayer);
			//getLogger().info("Added player " + player.getName());
		    }
		    catch (StreamCorruptedException e)
		    {
			getLogger().warning("Skipping file " + player.getName());
		    }
		    catch (Exception e) {
			getLogger().warning("Skipping file " + player.getName());
			//e.getMessage();
			e.printStackTrace();
		    }
		}
	    }
	}

	// Check if this should be run offline completely or not
	if (offline) {
	    finish();
	    return true;
	}

	// Now get the UUID's
	sender.sendMessage(ChatColor.GREEN + "Now contacting Mojang to obtain UUID's for players. This could take a while, see console and please wait...");
	sender.sendMessage(ChatColor.GREEN + "Requesting " + playerNames.size() + " player names.");
	sender.sendMessage(ChatColor.GREEN + "There are " + players.size() + " known players.");
	// Check for any blank or null names
	if (playerNames.contains(null)) {
	    sender.sendMessage(ChatColor.RED + "null player name found - deleting");
	    playerNames.remove(null);
	}
	final UUIDFetcher fetcher = new UUIDFetcher(this, playerNames,true);
	UUIDflag = false;
	// Kick off an async task and grab the UUIDs.
	getServer().getScheduler().runTaskAsynchronously(this, new Runnable(){

	    @Override
	    public void run() {
		// Fetch UUID's
		try {
		    response = fetcher.call();
		} catch (Exception e) {
		    getLogger().warning("Exception while running UUIDFetcher");
		    e.printStackTrace();
		}
		UUIDflag = true;
	    }});
	cancelled = false;
	// Kick of a scheduler to check if the UUID results are in yet
	check = getServer().getScheduler().runTaskTimer(this, new Runnable(){
	    @Override
	    public void run() {
		getLogger().info("Checking for name to UUID results");
		if (!cancelled) {
		    // Check to see if UUID has returned
		    if (UUIDflag) {
			getLogger().info("Received " + response.size() + " UUID's");
			cancelled = true;
			finish();
			check.cancel();
		    } else {
			getLogger().info("Waiting...");
		    }
		}
	    }}, 20L, 60L);
	return true;
    }
    protected void finish() {
	if (!offline) {
	    // finishes the conversion
	    getLogger().info("Received " + response.size() + " UUID's");
	    // Now complete the player objects
	    for (String name : response.keySet()) {
		//getLogger().info("Set UUID for " + name);
		UUID uuid = response.get(name);
		// DEBUG - step through name character by character
		/*
	    for (int c = 0; c < name.length(); c ++) {
		getLogger().info("Char " + name.charAt(c) + " value " + (int)name.charAt(c));
	    }*/

		if (players.get(name.trim()) != null) {
		    players.get(name.trim()).setUUID(uuid);
		}

	    }
	} else {
	    // Create an offline response
	    for (String name : players.keySet()) {
		UUID offlineUUID = getServer().getOfflinePlayer(name).getUniqueId();
		if (offlineUUID != null) {
		    //getLogger().warning("Setting *offline* UUID for " + name);
		    response.put(name,offlineUUID);
		    players.get(name).setUUID(offlineUUID);
		}
	    }

	}
	File playerDir = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "players");
	if (!playerDir.exists()) {
	    playerDir.mkdir();
	}
	// Now save all the players
	List<String> noUUIDs = new ArrayList<String>();
	for (String name : players.keySet()) {
	    if (players.get(name).getUUID() != null) {
		players.get(name).save(playerDir);
	    } else {
		/*
		// Try and obtain local UUID if offline mode is true
		if (!getServer().getOnlineMode() || offline) {
		    @SuppressWarnings("deprecation")
		    UUID offlineUUID = getServer().getOfflinePlayer(name).getUniqueId();
		    if (offlineUUID != null) {
			getLogger().warning("Setting *offline* UUID for " + name);
			players.get(name).setUUID(offlineUUID);
			players.get(name).save(playerDir);
		    }
		} else {
		 */
		getLogger().warning(name + " has no UUID. Cannot save this player!");
		noUUIDs.add(name);
		//}
	    }  
	}
	if (!noUUIDs.isEmpty()) {
	    getLogger().warning("The following player names have no UUID (according to Mojang or offline server) so had to be skipped:");
	    for (String n : noUUIDs) {
		getLogger().warning(n);
	    }
	}
	getLogger().info("***** All Done! *****");
	getLogger().info("Stop server and check that config.yml in askyblock folder is okay");
	getLogger().info("Then copy askyblock.jar to /plugins folder. Remove uaconv.jar and then restart server.");
    }

}
