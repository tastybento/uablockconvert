package us.talabrek.ultimateskyblock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class PlayerInfo implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String playerName;
    private boolean hasIsland;
    private boolean hasParty;
    private boolean warpActive;
    private List<String> members;
    private List<String> banned;
    private String partyLeader;
    private String partyIslandLocation;
    private String islandLocation;
    private String homeLocation;
    private String warpLocation;
    private String deathWorld;
    private HashMap<String, Boolean> challengeList;
    private float islandExp;
    private int islandLevel;

    public PlayerInfo(String playerName)
    {
	this.playerName = playerName;
	this.members = new ArrayList<String>();
	this.banned = new ArrayList<String>();
	this.hasIsland = false;
	this.warpActive = false;
	this.islandLocation = null;
	this.homeLocation = null;
	this.warpLocation = null;
	this.deathWorld = null;
	this.hasParty = false;
	this.partyLeader = null;
	this.partyIslandLocation = null;
	this.islandExp = 0.0F;
	this.challengeList = new HashMap<String, Boolean>();
	this.islandLevel = 0;
    }

    public String getPIL() {
	return this.partyIslandLocation;
    }

    public String getPlayerName() {
	return this.playerName;
    }

    public boolean getHasIsland() {
	return this.hasIsland;
    }

    public Location getIslandLocation() {
	return getLocationString(this.islandLocation);
    }

    public String getIL() {
	return this.islandLocation;
    }

    public String getHL()
    {
	return this.homeLocation;
    }

    public boolean getHasParty()
    {
	return this.hasParty;
    }

    public List<String> getMembers()
    {
	return this.members;
    }

    public String getPartyLeader() {
	return this.partyLeader;
    }

    public int getIslandLevel() {
	return this.islandLevel;
    }

    private Location getLocationString(String s) {
	if ((s == null) || (s.trim() == "")) {
	    return null;
	}
	String[] parts = s.split(":");
	if (parts.length == 4) {
	    World w = Bukkit.getServer().getWorld(parts[0]);
	    int x = Integer.parseInt(parts[1]);
	    int y = Integer.parseInt(parts[2]);
	    int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }
}
