/*
 *  Copyright (C) 2016 Zombie_Striker
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307 USA
 */
package me.zombie_striker.music;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements Listener {

	protected HashMap<Integer, Long> time = new HashMap<Integer, Long>();
	private HashMap<String, Double> songtime = new HashMap<String, Double>();
	HashMap<String, String> songname = new HashMap<String, String>();

	private List<String> resourcepackLinks = new ArrayList<String>();
	List<Loop> loops = new ArrayList<Loop>();

	protected String songversion = "2.0";

	int Streams = 220;
	public String prefix = ChatColor.BLUE + "[Music]" + ChatColor.WHITE;
	public Music music;

	private boolean debug;

	short data = 0;
	int slotsleft = 108;
	int test;

	public File musicdirectory;

	List<JukeBox> jukeboxes = new ArrayList<JukeBox>();

	public HashMap<UUID, JukeBox> jukeboxSetters = new HashMap<>();

	public static String inventorytitle = "Select a station";
	
	public Loop getLoops(int id) {
		for(Loop l : loops) {
			if(l.id==id) {
				return l;
			}
		}
		return null;
	}

	public Inventory getInventory() {
		Inventory i = Bukkit.createInventory(null, ((loops.size() / 9) + 1) * 9, inventorytitle);
		ItemStack no;
		try {
			no = new ItemStack(Material.BARRIER);
		} catch (Exception | Error e) {
			no = new ItemStack(Material.JUKEBOX);
		}
		ItemMeta im = no.getItemMeta();
		im.setDisplayName(ChatColor.RED + " Turn off jukebox");
		no.setItemMeta(im);
		i.addItem(no);
		for (Loop l : loops)
			i.addItem(createJukeBox(l));
		return i;
	}
	

	public ItemStack createJukeBox(Loop l) {
		Material[] records = {Material.RECORD_3,Material.RECORD_4,Material.RECORD_5,Material.RECORD_6,Material.RECORD_7,Material.RECORD_8,Material.RECORD_9,Material.RECORD_10,Material.RECORD_11,Material.RECORD_12};
		ItemStack item = new ItemStack(l.isActive ? records[l.getInt()%records.length]: Material.OBSIDIAN);
		ItemMeta im = item.getItemMeta();
		im.setDisplayName(ChatColor.GOLD + "" + l.id);
		List<String> lore = new ArrayList<String>();
		if (l.song.size() > 0 && l.activeSong >= 0) {
			lore.add(ChatColor.WHITE + "Station Queue:");
			for (int i = 0; i < l.song.size(); i++) {
				lore.add(
						(l.getActiveSong() == i ? ChatColor.WHITE : ChatColor.GRAY) + "-#" + i + " : " + l.song.get(i));
			}
		}
		im.setLore(lore);
		item.setItemMeta(im);
		return item;
	}

	@SuppressWarnings({ "deprecation", "unused" })
	public void onEnable() {
		play();
		if (!new File(getDataFolder(), "config.yml").exists())
			saveConfig();
		if (!getConfig().contains("debug")) {
			getConfig().set("debug", false);
			saveConfig();
		}
		if (!getConfig().contains("stations")) {
			getConfig().set("stations", 250);
			saveConfig();
		}
		if (!getConfig().contains("auto-update")) {
			getConfig().set("auto-update", true);
			saveConfig();
		}
		this.debug = getConfig().getBoolean("debug");
		this.Streams = getConfig().getInt("stations");

		music = new Music(this);
		new GenerateFiles(this).run();

		if (getConfig().contains("Jukeboxes")) {
			for (String key : getConfig().getConfigurationSection("Jukeboxes").getKeys(false)) {
				JukeBox j = new JukeBox((Location) getConfig().get("Jukeboxes." + key + ".location"),
						UUID.fromString(getConfig().getString("Jukeboxes." + key + ".owner")));
				j.stationId = getConfig().getInt("Jukeboxes." + key + ".station");
				j.setActive(getConfig().getBoolean("Jukeboxes." + key + ".active"));
				jukeboxes.add(j);
			}
		}

		// RegisterEvents
		Bukkit.getPluginManager().registerEvents(new JukeBoxEvents(this), this);
		Bukkit.getPluginManager().registerEvents(this, this);

		updateSongs();

		Object ce = new MusicCommand(this);
		this.getCommand("music").setExecutor((CommandExecutor) ce);
		this.getCommand("music").setTabCompleter((TabCompleter) ce);

		File Fff2 = null;
		for (File f2 : getDataFolder().listFiles()) {
			if (f2.getName().toLowerCase().contains("ResourcePack".toLowerCase())) {
				Fff2 = f2;
				break;
			}
		}
		try {
			InputStream is = null;
			is = new FileInputStream(Fff2);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			for (int i = 0; i < 40; i++) {
				String s = br.readLine();
				if (s.contains("[&&END&&]"))
					break;
				String s2 = br.readLine();
				resourcepackLinks.add(s2);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int j = 0; j < Streams; j++) {
			if (!getConfig().contains("Loop." + j + ".l.x"))
				continue;
			int x = getConfig().getInt("Loop." + j + ".l.x");
			int y = getConfig().getInt("Loop." + j + ".l.y");
			int z = getConfig().getInt("Loop." + j + ".l.z");

			String w = getConfig().getString("Loop." + j + ".l.w");
			int r = getConfig().getInt("Loop." + j + ".r");
			Object s = getConfig().get("Loop." + j + ".s");
			Object owner = getConfig().get("Loop." + j + ".p");
			List<String> b = null;
			UUID uuid = null;
			if (s instanceof String) {
				b = new ArrayList<String>();
				b.add((String) s);
			}
			if (s instanceof List) {
				b = getConfig().getStringList("Loop." + j + ".s");
			}
			if (owner instanceof String) {
				try {
					uuid = UUID.fromString((String) owner);
				} catch (Exception e) {
					try {
						uuid = Bukkit.getOfflinePlayer((String) owner).getUniqueId();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
			loops.add(new Loop(this, j, b, x, y, z, w, uuid, r));
		}

		GithubUpdater.autoUpdate(this, "ZombieStriker", "Music","Music.jar");
		//final Updater updater = new Updater(this, 91836, true);
		// bStats metrics
		Metrics met = new Metrics(this);
		met.addCustomChart(new Metrics.SimplePie("sounds-loaded") {
			@Override
			public String getValue() {
				return String.valueOf(songname.size());
			}
		});
		met.addCustomChart(new Metrics.SimplePie("stations") {
			@Override
			public String getValue() {
				return String.valueOf(Streams);
			}
		});
		met.addCustomChart(new Metrics.SimplePie("updater-active") {
			@Override
			public String getValue() {
				return String.valueOf(getConfig().getBoolean("auto-update"));
			}
		});

		/*
		 * new BukkitRunnable() { public void run() { // TODO: Works well. Make changes
		 * for the updaters of // PixelPrinter and Music later. if
		 * (updater.updaterActive) updater.download(false); }
		 * }.runTaskTimerAsynchronously(this, 20 * 60, 20 * 60 * 6);
		 */
		
		
		try {
			if (Bukkit.getPluginManager().getPlugin("PluginConstructorAPI") == null)
				GithubDependDownloader.autoUpdate(this, new File(getDataFolder().getParentFile(),"PluginConstructorAPI.jar"), "ZombieStriker", "PluginConstructorAPI", "PluginConstructorAPI.jar");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void play() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				for (Loop loop : loops) {
					if (!loop.isActive || loop.song == null) {
						getConfig().set("Loop." + loop.getInt(), null);
						saveConfig();
						continue;
					}
					int i = loop.getInt();
					if (!time.containsKey(i))
						time.put(i, System.currentTimeMillis() + 1000);
					if (time.get(i) <= System.currentTimeMillis()) {
						loop.setActiveSong(loop.getActiveSong() + 1);
						if (loop.getActiveSong() >= loop.getSongs().size())
							loop.setActiveSong(0);
						if (songtime.containsKey(loop.getThisSong())) {
							time.put(i, System.currentTimeMillis()
									+ ((Math.round(songtime.get(loop.getThisSong())) * 1000)));
							for (JukeBox j : jukeboxes)
								if (j.stationId == loop.id)
									if (j.getActive())
										j.jukeBox.getWorld().playSound(j.jukeBox, songname.get(loop.getThisSong()),
												(float) 1.0 * loop.radius, 1);
							// for (Location loc : loop.getLocations())
							// loc.getWorld().playSound(loc, songname.get(loop.getThisSong()),
							// (float) 1.0 * loop.radius, 1);
						}
					}
				}
			}
		}, 5L, 1L);
	}

	public void showPlayerHelpMessage(Player player) {
		player.sendMessage(ChatColor.GOLD + " =====" + prefix + ChatColor.GOLD + "   Help =====");
		player.sendMessage(
				ChatColor.BLUE + "/music <?,help> :" + ChatColor.RESET + " Displays all subcommands and their usages");
		player.sendMessage(
				ChatColor.BLUE + "/music <get, getPack>:" + ChatColor.RESET + " Lists all registered resourcepacks.");
		player.sendMessage(
				ChatColor.BLUE + "/music setUpStation <Song> <StationID> :" + ChatColor.RESET + " Creates a station.");
		player.sendMessage(ChatColor.BLUE + "/music addToQueue <Song> <StationID> :" + ChatColor.RESET
				+ " Adds the song to a station.");
		player.sendMessage(ChatColor.BLUE + "/music removeFromQueue <QueueID> <StationID>:" + ChatColor.RESET
				+ " Removes a song from a station.");
		player.sendMessage(ChatColor.BLUE + "/music listStationSongs <StationID> :" + ChatColor.RESET
				+ " Lists the Queue for a station.");
		player.sendMessage(ChatColor.BLUE + "/music playOnce <Song> :" + ChatColor.RESET + " Plays the song once.");
		player.sendMessage(ChatColor.BLUE + "/music setRadius <StationID> <radius>:" + ChatColor.RESET
				+ " Sets the radius for Station.\n The distance it will be heard is equal to 20*radius.");
		player.sendMessage(ChatColor.BLUE + "/music clearQueue <StationID>:" + ChatColor.RESET
				+ " Clears all songs for a station.");
		player.sendMessage(ChatColor.BLUE + "/music removeStation <StationID>:" + ChatColor.RESET
				+ " Removes a station.");
		player.sendMessage(
				ChatColor.BLUE + "/music ListStations :" + ChatColor.RESET + " Lists all stations that are active.");
		if (player.isOp())
			player.sendMessage(ChatColor.GOLD + "/music createSong <DisplayName> <SongName> <Time> :" + ChatColor.WHITE
					+ " [OP ONLY] Creates the song file so players can hear the music.");
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (!getConfig().contains("UsersWithPacks." + e.getPlayer().getName())
				|| !getConfig().getString("UsersWithPacks." + e.getPlayer().getName()).equals(this.songversion)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!getConfig().contains("UsersWithPacks." + e.getPlayer().getName()) || !getConfig()
							.getString("UsersWithPacks." + e.getPlayer().getName()).equals(songversion)) {
						sendFunMessage(e.getPlayer());
					} else {
						cancel();
					}
				}
			}.runTaskTimer(this, 20, 20 * 60 * 15);
			// Send this message every fifteen minutes until they download it.
		}
	}

	public void updateSongs() {
		songtime.clear();
		songname.clear();
		for (File ff : musicdirectory.listFiles()) {
			try {
				if (ff.getName().length() > 0) {
					InputStream is = null;
					is = new FileInputStream(ff);
					BufferedReader br = new BufferedReader(new InputStreamReader(is));

					String line;
					double time = -1;
					String songName = ff.getName().replace(".txt", "").trim();
					String fileName = ff.getName().replace(".txt", "").trim();
					for (int i = 0; i < 100; i++) {
						line = br.readLine();
						if (line == null)
							break;
						if (line.toLowerCase().startsWith("Soundname:".toLowerCase())
								|| line.toLowerCase().startsWith("Songname:".toLowerCase())) {
							line = line.split(":")[1].trim();
							songName = line;
						}
						if (line.toLowerCase().startsWith("Time:".toLowerCase())) {
							line = line.split(":")[1].trim();
							time = Double.parseDouble(line);
						}
					}
					songtime.put(fileName, time);
					songname.put(fileName, songName);
					if (debug)
						getLogger().log(Level.INFO, prefix + "FileName:" + fileName + "   Songname:" + songName);
					br.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void sendFunMessage(Player player) {
		player.sendMessage(prefix
				+ " Don't miss out on all the fun! Download the Music resourcepacks to hear all the great songs on this server!");
		player.sendMessage(ChatColor.WHITE + " Type " + ChatColor.GRAY + "/music get" + ChatColor.WHITE
				+ " to get the resoucepack!");
	}

}