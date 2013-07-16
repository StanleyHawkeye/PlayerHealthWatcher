package me.hawkeye.minecraft.bukkitplugins.PlayerHealthWatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlayerHealthWatcher plugin for bukkit
 * @version 0.1
 *
 */
public class PlayerHealthWatcher extends JavaPlugin implements Listener
{	
	/**
	 * After this time players will get notified in chat
	 */
	private static final long MAX_TIME = 18000000; // five hours in milliseconds

	/**
	 * How often to check the players 
	 */
	private static final long CHECK_DELAY = 12000; // ten minutes in ticks

	/**
	 * Id of the bukkit task checking players
	 */
	private int checkingTaskId = 0; 
	
	public void onEnable() 
	{
		getServer().getPluginManager().registerEvents(this,this);
		startOnlineTimeCheckingTask();		
		log.info("PlayerHealthWatcher enabled.");
	}

	public void onDisable() 
	{
		stopOnlineTimeCheckingTask();
		log.info("PlayerHealthWatcher disabled.");
	}

	public final Logger log = Logger.getLogger(PlayerHealthWatcher.class.getName());
	
	private final HashMap<String, Long> playerJoinTimes = new HashMap<String, Long>();
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("mph"))
		{
			String subCommand = (args.length > 0)? args[0] : "help";
			
			if(subCommand.equals("list"))
			{
				if(!sender.hasPermission("mph.list"))
				{
					sender.sendMessage("You don't have a permission to use this command.");
					return false;
				}
				
				if(playerJoinTimes.size() == 0)
				{
					sender.sendMessage("There are no players");
				}
				else
				{
					sender.sendMessage("Player online time statistics:");
					for(Map.Entry<String, Long> entry : playerJoinTimes.entrySet())
					{
						sender.sendMessage("- "+ChatColor.GREEN+entry.getKey()+ChatColor.WHITE+" is online for "+((System.currentTimeMillis()-entry.getValue())/3600000d)+" hours.");
					}
				}
			}
			else if(subCommand.equals("me"))
			{
				if(!sender.hasPermission("mph.me"))
				{
					sender.sendMessage("You don't have a permission to use this command.");
					return false;
				}
				
				if(sender instanceof Player)
				{
					long joinTime = playerJoinTimes.get(sender.getName().toLowerCase());
					sender.sendMessage("You are online for "+((System.currentTimeMillis()-joinTime)/3600000d)+" hours.");
				}
				else
					sender.sendMessage("This command can only be called by a player");
			}
			else if(subCommand.equals("help"))
			{
				sender.sendMessage(new String[]{
						"List of available subcommands of /mph:",
						"  "+ChatColor.YELLOW+"help"+ChatColor.WHITE+" - "+ChatColor.AQUA+" displays this help message",
						"  "+ChatColor.YELLOW+"me"+ChatColor.WHITE+" - "+ChatColor.AQUA+" shows your statistics",
				});
				if(sender.hasPermission("mph.list"))
					sender.sendMessage("  "+ChatColor.YELLOW+"list"+ChatColor.WHITE+" - "+ChatColor.AQUA+" list all players and their online time");
			}
			else
			{
				sender.sendMessage(ChatColor.RED+"There is no such subcommand. Try /mph help for the list of available commands.");
			}
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		playerJoinTimes.put(event.getPlayer().getName().toLowerCase(),System.currentTimeMillis());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		playerJoinTimes.remove(event.getPlayer().getName().toLowerCase());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event)
	{
		playerJoinTimes.remove(event.getPlayer().getName().toLowerCase());
	}
	
	/**
	 * Starts the bukkit task checking players
	 */
	private void startOnlineTimeCheckingTask()
	{
		stopOnlineTimeCheckingTask();
		checkingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run()
			{
				checkPlayers();
			}
		}, CHECK_DELAY, CHECK_DELAY);
	}
	
	/**
	 * Stops the task checking players
	 */
	private void stopOnlineTimeCheckingTask()
	{
		if(checkingTaskId == 0)
			return;
		
		Bukkit.getScheduler().cancelTask(checkingTaskId);
	}
	
	/**
	 * Checks all the online players if they haven't been playing for too long.
	 */
	private void checkPlayers()
	{
		for(Player player : Bukkit.getServer().getOnlinePlayers())
		{
			long onlineTime =  System.currentTimeMillis() - playerJoinTimes.get(player.getName().toLowerCase());
			if(onlineTime > MAX_TIME)
			{
				player.sendMessage(ChatColor.LIGHT_PURPLE+"Playing for an extend period of time can damage your health."+ChatColor.AQUA+" Please consider taking a break.");
			}
		}
	}
}
