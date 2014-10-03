package com.wasteofplastic.uablockconverter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Tracks the following info on the player
 */
public class Players {
    private UABlockConverter plugin;
    private YamlConfiguration playerInfo;
    private HashMap<String, Boolean> challengeList;
    private boolean hasIsland;
    private boolean inTeam;
    private String homeLocation;
    private int islandLevel;
    private String islandLocation;
    private List<UUID> members;
    private List<String> memberNames;
    private String teamIslandLocation;
    private UUID teamLeader;
    private String teamLeaderName;
    private UUID uuid;
    private String playerName;
    private int resetsLeft;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final UABlockConverter UABlockConverter, String playerName) {
	this.plugin = UABlockConverter;
	plugin.getLogger().info("Created player object for " + playerName);
	this.uuid = null;
	this.members = new ArrayList<UUID>();
	this.memberNames = new ArrayList<String>();
	this.hasIsland = false;
	this.islandLocation = null;
	this.homeLocation = null;
	this.inTeam = false;
	this.teamLeader = null;
	this.teamLeaderName = "";
	this.teamIslandLocation = null;
	this.challengeList = new HashMap<String, Boolean>();
	this.islandLevel = 0;
	this.playerName = playerName;
	this.resetsLeft = 2;
    }


    /**
     * Saves the player info to the file system
     * @param playerDir 
     */
    public void save(File playerDir) {
	plugin.getLogger().info("Saving player..." + playerName);
	playerInfo = new YamlConfiguration();
	// Save the variables
	playerInfo.set("playerName", playerName);
	playerInfo.set("hasIsland", hasIsland);
	playerInfo.set("islandLocation", islandLocation);
	playerInfo.set("homeLocation", homeLocation);
	playerInfo.set("hasTeam", inTeam);
	// Get team leader UUID
	if (!teamLeaderName.isEmpty()) {
	    if (plugin.response.containsKey(teamLeaderName)) {
		teamLeader = plugin.response.get(teamLeaderName);
	    }
	}
	if (teamLeader == null) {
	    playerInfo.set("teamLeader","");
	} else {
	    playerInfo.set("teamLeader", teamLeader.toString());
	}
	playerInfo.set("teamIslandLocation", teamIslandLocation);
	playerInfo.set("islandLevel", islandLevel);
	// Serialize UUIDs
	List<String> temp = new ArrayList<String>();
	// Get UUID's for members
	if (!memberNames.isEmpty()) {
	    for (String name : memberNames) {
		if (plugin.response.containsKey(name)) {
		    temp.add(plugin.response.get(name).toString());
		}
	    }
	}
	playerInfo.set("members", temp);
	playerInfo.set("resetsLeft", this.resetsLeft);

	// Get the challenges
	for (String challenge : challengeList.keySet()) {
	    playerInfo.set("challenges.status." + challenge, challengeList.get(challenge));
	}
	File file = new File(playerDir, uuid.toString() + ".yml");
	try {
	    playerInfo.save(file);
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    /**
     * @param member
     *            Adds a member to the the player's list
     */
    public void addTeamMember(final UUID member) {
	members.add(member);
    }

    /**
     * Records the challenge as being complete in the player's list If the
     * challenge is not listed in the player's challenge list already, then it
     * will not be recorded! TODO: Possible systemic bug here as a result
     * 
     * @param challenge
     */
    public void completeChallenge(final String challenge) {
	if (challengeList.containsKey(challenge)) {
	    challengeList.remove(challenge);
	    challengeList.put(challenge, Boolean.valueOf(true));
	}
    }

    public boolean hasIsland() {
	return hasIsland;
    }

    /**
     * 
     * @return boolean - true if player is in a team
     */
    public boolean inTeam() {
	if (members == null) {
	    members = new ArrayList<UUID>();
	}
	return inTeam;
    }

    public Location getHomeLocation() {
	if (homeLocation.isEmpty()) {
	    return null;
	}
	// return homeLoc.getLocation();
	Location home = getLocationString(homeLocation).add(new Vector(0.5D,0D,0.5D));
	return home;
    }

    /**
     * @return The island level int. Note this function does not calculate the
     *         island level
     */
    public int getIslandLevel() {
	return islandLevel;
    }

    /**
     * @return the location of the player's island in Location form
     */
    public Location getIslandLocation() {
	return getLocationString(islandLocation);
    }

    /**
     * Converts a serialized location string to a Bukkit Location
     * 
     * @param s
     *            - a serialized Location
     * @return a new Location based on string or null if it cannot be parsed
     */
    private static Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }

    public List<UUID> getMembers() {
	return members;
    }

    public Location getTeamIslandLocation() {
	// return teamIslandLoc.getLocation();
	if (teamIslandLocation.isEmpty()) {
	    return null;
	}
	Location l = getLocationString(teamIslandLocation).add(new Vector(0.5D,0D,0.5D));
	return l;
    }

    public UUID getTeamLeader() {
	return teamLeader;
    }

    public Player getPlayer() {
	return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
	return uuid;
    }

    public String getPlayerName() {
	return playerName;
    }

    public void setPlayerN(String playerName) {
	this.playerName = playerName;
    }

    /**
     * Converts a Bukkit location to a String
     * 
     * @param l
     *            a Bukkit Location
     * @return String of the floored block location of l or "" if l is null
     */

    private String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /**
     * @return the resetsLeft
     */
    public int getResetsLeft() {
	return resetsLeft;
    }

    /**
     * @param resetsLeft the resetsLeft to set
     */
    public void setResetsLeft(int resetsLeft) {
	this.resetsLeft = resetsLeft;
    }

    /**
     * Removes member from player's member list
     * 
     * @param member
     */
    public void removeMember(final UUID member) {
	members.remove(member);
    }


    /**
     * Resets a specific challenge. Will not reset a challenge that does not
     * exist in the player's list TODO: Add a success or failure return
     * 
     * @param challenge
     */
    public void resetChallenge(final String challenge) {
	if (challengeList.containsKey(challenge)) {
	    challengeList.remove(challenge);
	    challengeList.put(challenge, Boolean.valueOf(false));
	}
    }

    public void setHasIsland(final boolean b) {
	hasIsland = b;
    }

    /**
     * Stores the home location of the player in a String format
     * 
     * @param l
     *            a Bukkit location
     */
    public void setHomeLocation(final Location l) {
	homeLocation = getStringLocation(l);
    }

    /**
     * Records the island's level. Does not calculate it
     * 
     * @param i
     */
    public void setIslandLevel(final int i) {
	islandLevel = i;
    }

    /**
     * Records the player's island location in a string form
     * 
     * @param l
     *            a Bukkit Location
     */
    public void setIslandLocation(final Location l) {
	islandLocation = getStringLocation(l);
    }

    /**
     * Records that a player is now in a team
     * 
     * @param leader
     *            - a String of the leader's name
     * @param l
     *            - the Bukkit location of the team's island (converted to a
     *            String in this function)
     */
    public void setJoinTeam(final UUID leader, final Location l) {
	inTeam = true;
	teamLeader = leader;
	teamIslandLocation = getStringLocation(l);
    }

    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation and members array
     */

    public void setLeaveTeam() {
	inTeam = false;
	teamLeader = null;
	islandLevel = 0;
	teamIslandLocation = null;
	members = new ArrayList<UUID>();
    }

    /**
     * @param l
     *            a Bukkit Location of the team island
     */
    public void setTeamIslandLocation(final Location l) {
	teamIslandLocation = getStringLocation(l);
    }

    /**
     * @param leader
     *            a String name of the team leader
     */
    public void setTeamLeader(final UUID leader) {
	teamLeader = leader;
    }

    /**
     * @param s
     *            a String name of the player
     */
    public void setPlayerUUID(final UUID s) {
	uuid = s;
    }

    public void setHL(String hl) {
	homeLocation = hl;
    }


    public void addTeamMember(String name) {
	
	if (!memberNames.contains(name)) {
	    memberNames.add(name);
	}
    }


    /**
     * @return the memberNames
     */
    public List<String> getMemberNames() {
	return memberNames;
    }


    /**
     * @return the teamLeaderName
     */
    public String getTeamLeaderName() {
	return teamLeaderName;
    }


    /**
     * @param memberNames the memberNames to set
     */
    public void setMemberNames(List<String> memberNames) {
	this.memberNames = memberNames;
    }


    /**
     * @param teamLeaderName the teamLeaderName to set
     */
    public void setTeamLeaderName(String teamLeaderName) {
	this.teamLeaderName = teamLeaderName;
    }


    public void setIslandLocation(String islandLocation) {
	this.islandLocation = islandLocation;	
    }


    public void setTeamIslandLocation(String islandLocation) {
	this.teamIslandLocation = islandLocation;

    }


    public void setInTeam(boolean b) {
	this.inTeam = b;	
    }


    public void setUUID(UUID uuid2) {
	this.uuid = uuid2;	
    }


    public UUID getUUID() {
	return uuid;	
    }

}