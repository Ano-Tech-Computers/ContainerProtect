package no.atc.floyd.bukkit.cp;


import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import no.atc.floyd.bukkit.ban.DbPool;

//import com.nijikokun.bukkit.Permissions.Permissions;







import java.util.logging.Logger;
//import com.nijikokun.bukkit.Permissions.Permissions;


/**
* Chest, furnace and dispenser protection plugin for Bukkit
*
* @author FloydATC
*/
public class ContainerProtect extends JavaPlugin implements Listener {
//    public static Permissions Permissions = null;
//    private final ContainerPlayerListener playerListener = new ContainerPlayerListener(this);
//    private final ContainerBlockListener blockListener = new ContainerBlockListener(this);

    
    String baseDir = "plugins/ContainerProtect";
    String configFile = "settings.txt";

    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();
    public static DbPool dbpool = null;

    private ConcurrentHashMap<String, String> locations = new ConcurrentHashMap<String, String>(); 
    private ConcurrentHashMap<String, String> modes = new ConcurrentHashMap<String, String>(); 
	public static final Logger logger = Logger.getLogger("Minecraft.ContainerProtect");
    
    public void onDisable() {
        // TODO: Place any custom disable code here
    	modes.clear();
    	locations.clear();

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
//    	setupPermissions();
    	loadData();

    	loadSettings();
    	initDbPool();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        //pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        //pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
        //pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Priority.Normal, this);
        //pm.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Priority.Normal, this);
        //pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        if (player == null) {
        	return true;
        }
        
        if (cmdname.equals("cp")) {
//        	if (Permissions.Security.permission(player, "containerprotect.use") == false) {
        	if (! player.hasPermission("containerprotect.use")) {
        		player.sendMessage("§7[§6CP§7] §cPermission denied");
        		return true;
        	}
        	if (args.length == 0) {
        		showPlayerMode(player);
        	} else {
        		String mode = "";
    			for (String arg: args) {
    				if (!mode.equals("")) { mode = mode.concat(" "); }
    				mode = mode.concat(arg);
    			}
        		if (setPlayerMode(player.getName(), mode)) {
            		showPlayerMode(player);
        			logger.info("[CP] " + player.getName() + " set mode " + mode);
        		} else {
        			player.sendMessage("§7[§6CP§7] §cInvalid mode: " + mode);
        			logger.info("[CP] " + player.getName() + " tried to set mode " + mode);
        			return false;
        		}
        	}
    		return true;
        }
        
        return false;
    }

    @EventHandler
    public void onPlayerInteract( PlayerInteractEvent event	) {
    	if (! isContainer(event)) { return; }
    	if (event.getPlayer() != null) {
    		Player p = (Player) event.getPlayer();
       		Block b = event.getClickedBlock();
       		if (allowInteract(p, b) == false) {
       			event.setCancelled(true);
       			p.sendMessage("§7[§6CP§7] §cPermission denied");
//       			if (!plugin.Permissions.Security.permission(p, "containerprotect.no-log-deny")) {
       			if (! p.hasPermission("containerprotect.no-log-deny")) {
       				logger.info("[CP] " + p.getName() + " tried to use container with mode " + clearMode(b));
       			}
       		} else {
//       			if (!plugin.Permissions.Security.permission(p, "containerprotect.no-log-permit")) {
       			if (! p.hasPermission("containerprotect.no-log-permit")) {
       				logger.info("[CP] " + p.getName() + " used container with mode " + clearMode(b));
       			}
       		}
    	}
    }

    @EventHandler
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
    	if (event.getItem().getTypeId() == 326) {
    		event.setCancelled(true); // Water bucket
    		logger.info("[CP] BlockDispenseEvent blocked: "+event.getItem().getType()+" at x="+event.getBlock().getX()+" y="+event.getBlock().getY()+" z="+event.getBlock().getZ());
    	}
    	if (event.getItem().getTypeId() == 327) {
    		event.setCancelled(true); // Lava bucket
    		logger.info("[CP] BlockDispenseEvent blocked: "+event.getItem().getType()+" at x="+event.getBlock().getX()+" y="+event.getBlock().getY()+" z="+event.getBlock().getZ());
    	}
    	if (event.getItem().getTypeId() == 259) {
    		event.setCancelled(true); // Flint and Steel
    		logger.info("[CP] BlockDispenseEvent blocked: "+event.getItem().getType()+" at x="+event.getBlock().getX()+" y="+event.getBlock().getY()+" z="+event.getBlock().getZ());
    	}
    	if (event.getItem().getTypeId() == 385) {
    		event.setCancelled(true); // Fire Charge
    		logger.info("[CP] BlockDispenseEvent blocked: "+event.getItem().getType()+" at x="+event.getBlock().getX()+" y="+event.getBlock().getY()+" z="+event.getBlock().getZ());
    	}
    }
    
    @EventHandler
    public void onBlockDamage( BlockDamageEvent event	) {
    	if (!isContainer(event)) { return; }
   		Player p = event.getPlayer();
   		Block b = event.getBlock();
   		if (allowDestroy(p, b) == false) {
   			event.setCancelled(true);
   			return;
   		} else {
	   		// If player is holding a chest (id=54), update mode for the container being hit
	   		if (p.getItemInHand().getTypeId() == 54) {
	   			String oldmode = getContainer(b.getX(), b.getY(), b.getZ());
	   	    	String newmode = getPlayerMode(p.getName());
	   	    	if (newmode.equals("2")) {
	   	    		newmode = newmode.concat(" " + p.getName());
	   	    	}
	   	    	if (newmode.equals(oldmode) == false) {
	   	    		addContainer(b.getX(), b.getY(), b.getZ(), newmode);
	   	    		p.sendMessage("§7[§6CP§7] §aChanged to mode=" + clearMode(b));
	   				logger.info("[CP] " + p.getName() + " changed protection on a chest");
	   	    	}
	   		}
   		}
    }

    @EventHandler
    public void onBlockPlace( BlockPlaceEvent event	) {
    	if (!isContainer(event)) { return; }
   		Player p = event.getPlayer();
   		Block b = event.getBlock();
   		Boolean cancel = false;
   		
   		// If placing a chest:
   		// Check for adjacent chest that player does not have access to
   		if (isChest(b)) {
   			Block check;
   			World w = b.getWorld();
   			Integer x = b.getX();
   			Integer y = b.getY();
   			Integer z = b.getZ();

//			logger.info("[CP] " + p.getName() + " wants to place a container");

//			logger.info("[CP] checking x+1");
   			check = w.getBlockAt(x+1, y, z);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
//			logger.info("[CP] checking x-1");
   			check = w.getBlockAt(x-1, y, z);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
//			logger.info("[CP] checking z+1");
   			check = w.getBlockAt(x, y, z+1);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
//			logger.info("[CP] checking z-1");
   			check = w.getBlockAt(x, y, z-1);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
//			logger.info("[CP] checking y+1");
   			check = w.getBlockAt(x, y+1, z);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
//			logger.info("[CP] checking y-1");
   			check = w.getBlockAt(x, y+1, z);
   			if (isChest(check) && !allowInteract(p, check) ) { cancel = true; }
   			
   			if (cancel) {
   				p.sendMessage("§7[§6CP§7] §cYou're not allowed to place a chest here");
   				logger.info("[CP] " + p.getName() + " tried to bypass protection on a chest");
   				event.setCancelled(true);
   				return;
   			} else {
//   				logger.info("[CP] " + p.getName() + " placed container");
   			}
   		}
   		
   		
    	String mode = getPlayerMode(p.getName());
    	if (mode.equals("2")) {
    		mode = mode.concat(" " + p.getName());
    	}
		addContainer(b.getX(), b.getY(), b.getZ(), mode);
    	p.sendMessage("§7[§6CP§7] §aPlaced container with mode=" + clearMode(b));
    }

    @EventHandler
    public void onBlockBreak( BlockBreakEvent event	) {
    	if (!isContainer(event)) { return; }
   		Player p = event.getPlayer();
   		Block b = event.getBlock();
   		if (allowDestroy(p, b) == false) {
   			event.setCancelled(true);
   			logger.info("[CP] " + p.getName() + " tried to destroy container with mode " + clearMode(b));
   		} else {
   			logger.info("[CP] " + p.getName() + " destroyed container with mode " + clearMode(b));
   			removeContainer(b.getX(), b.getY(), b.getZ());
   		}
    }

    // Protect containers from ever catching fire
    @EventHandler
    public void onBlockIgnite( BlockIgniteEvent event	) {
    	if (!isContainer(event)) { return; }
    	event.setCancelled(true);
    }


    private void showPlayerMode(Player player) {
		String mode = getPlayerMode(player.getName());
    	String[] parts = mode.split(" ", 2);
    	if (parts[0].equals("1")) {
    		player.sendMessage("§7[§6CP§7] §7Current mode=Public");
    	}
    	if (parts[0].equals("2")) {
    		player.sendMessage("§7[§6CP§7] §7Current mode=Private");
    	}
    	if (parts[0].equals("3")) {
    		player.sendMessage("§7[§6CP§7] §7Current mode=Players: " + parts[1]);
    	}
    	if (parts[0].equals("4")) {
    		player.sendMessage("§7[§6CP§7] §7Current mode=Groups: " + parts[1]);
    	}
    }
    
    private void loadData() {
    	String fname = "plugins/ContainerProtect/locations.txt";
    	File f;
    	
    	// Ensure that directory exists
    	String pname = baseDir;
    	f = new File(pname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "[CP] Created directory '" + pname + "'" );
    		}
    	}
    	// Ensure that locations.txt exists
    	f = new File(fname);
    	if (!f.exists()) {
			BufferedWriter output;
			//String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.close();
    			logger.info( "[CP] Created data file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}

    	
    	try {
        	BufferedReader input =  new BufferedReader(new FileReader(fname));
    		String line = null;
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.matches("^#.*") && !line.matches("")) {
    				String[] parts = line.split("=");
    				if (parts.length == 2) {
    					locations.put(parts[0], parts[1]);
    				} else {
    					logger.warning("[CP] Invalid line in " + fname + ":" + line);
    				}
    			}
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		logger.warning("[CP] Error reading config file '" + fname + "': " + e.getLocalizedMessage());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

    }
   
    private synchronized void saveData() {
    	String fname = "plugins/ContainerProtect/locations.txt";
   		BufferedWriter output;
		String newline = System.getProperty("line.separator");
   	    try {
       		output = new BufferedWriter(new FileWriter(fname));
       		for (String location: locations.keySet()) {
       			String data = locations.get(location);
       			if (data != null) {
       				output.write( location + "=" + data + newline );
       			}
       		}
       		output.close();
   	    }
   	    catch (Exception e) {
    		e.printStackTrace();
   	    }

    }

    public Boolean addContainer(Integer x, Integer y, Integer z, String mode) {
    	locations.put(x + "," + y + "," + z, mode);
    	saveData();
    	return true;
    }
    
    public Boolean removeContainer(Integer x, Integer y, Integer z) {
    	locations.remove(x + "," + y + "," + z);
    	saveData();
    	return true;
    }

    public String getContainer(Integer x, Integer y, Integer z) {
    	String data = locations.get(x + "," + y + "," + z);
    	if (data == null || data.equals("")) {
    		return "1";	// Default = public
    	} else {
    		return data;
    	}
    }
    
    public Boolean setPlayerMode(String playername, String mode) {
    	if (mode.matches("^([1-4]|pu.*?|pr.*?|pl.*?|g.*?)(\\s[0-9a-zA-Z_]+)*$")) {
    		String[] parts = mode.split(" ", 2);
    		// Mode may now be typed as mnemonic instead of numeric code
    		if (parts[0].toLowerCase().startsWith("pu")) { parts[0] = "1"; }
    		if (parts[0].toLowerCase().startsWith("pr")) { parts[0] = "2"; }
    		if (parts[0].toLowerCase().startsWith("pl")) { parts[0] = "3"; }
    		if (parts[0].toLowerCase().startsWith("g")) { parts[0] = "4"; }

    		// Perform error checking and apply mode 
    		if (parts[0].equals("1") || parts[0].equals("2") || parts.length == 2) {
    			if (parts[0].equals("1")) { mode = "1"; } // Arguments forbidden
    			if (parts[0].equals("2")) { mode = "2 " + playername; } // Argument forced
    			if (parts[0].equals("3")) { mode = "3 " + parts[1]; } // Argument forced
    			if (parts[0].equals("4")) { mode = "4 " + parts[1]; } // Argument forced
    			modes.put(playername, mode);
    			return true;
    		}
//    		System.out.println("Regex matches but data is invalid");
    	}
        return false;
    }

    public String getPlayerMode(String playername) {
    	String mode = modes.get(playername);
    	if (mode == null || mode.equals("")) {
    		return "2";	// Default = private
    	} else {
    		return mode;
    	}
    }

    public Boolean allowInteract(Player p, Block b) {
    	p.sendMessage("§7[§6CP§7] Container mode=" + clearMode(b));
    	if (allowPlayer(p, b)) {
    		return true;
    	}
//    	if (Permissions.Security.permission(p, "containerprotect.open-any")) {
    	if (p.hasPermission("containerprotect.open-any")) {
    		return true;
    	}
    	return false;
    }

    public Boolean allowDestroy(Player p, Block b) {
    	if (allowPlayer(p, b)) {
    		return true;
    	}
//    	if (Permissions.Security.permission(p, "containerprotect.destroy-any")) {
    	if (p.hasPermission("containerprotect.destroy-any")) {
    		return true;
    	}
    	return false;
    }
    
    public Boolean allowPlayer(Player p, Block b) {
    	String mode = getContainer(b.getX(), b.getY(), b.getZ());
    	String[] parts = mode.split(" ", 2);
    	//System.out.println("DEBUG allowPlayer(): mode is ["+mode+"]");
    	if (parts[0].equals("1")) { return true; }
    	if (parts[0].equals("2")) {
    		if (parts[1].equalsIgnoreCase(p.getName())) {
    			return true;
    		}
    		if (match_via_uuid(parts[1], p.getName())) {
    			p.sendMessage("§7[§6CP§7]§4 WARNING: Please update container permissions with correct name!");
    			return true;
    		}
    		return false;
    	}
    	if (parts[0].equals("3")) {
    		for (String name: parts[1].split(" ")) {
    			if (name.equalsIgnoreCase(p.getName())) {
    				return true;
    			}
    			if (match_via_uuid(name, p.getName())) {
        			p.sendMessage("§7[§6CP§7]§4 WARNING: Please update container permissions with correct name!");
    				return true;
    			}
    		}
    		return false;
    	}
    	if (parts[0].equals("4")) {
//    		String playername = p.getName();
//    		String world = p.getLocation().getWorld().getName();
//    		String[] memberships = Permissions.Security.getGroups(world, playername);
    		for (String group: parts[1].split(" ")) {
    			if (p.hasPermission("group." + group)) {
    				return true;
    			}
//    			for (String membership: memberships) {
//    				if (group.equalsIgnoreCase(membership)) {
//    					return true;
//    				}
//    			}
    		}
    		return false;
    	}
    	logger.warning("[CP] Invalid protection mode ("+mode+") encountered while checking container accessed by " + p.getName());
    	return true;
    }
    
    public String clearMode(Block b) {
    	String mode = getContainer(b.getX(), b.getY(), b.getZ());
    	String[] parts = mode.split(" ", 2);
    	//System.out.println("DEBUG clearMode(): mode is ["+mode+"]");
    	if (parts[0].equals("1")) {
    		return "Public";
    	}
    	if (parts[0].equals("2")) {
    		return "Private: " + parts[1];
    	}
    	if (parts[0].equals("3")) {
    		return "Players: " + parts[1];
    	}
    	if (parts[0].equals("4")) {
    		return "Groups: " + parts[1];
    	}
    	return "Unknown";
    }
    
    // Return true if event is for a container block
    public Boolean isContainer(BlockEvent event) {
    	Block block = event.getBlock();
    	if (block == null) {
    		return false;
    	}
    	Integer type = block.getTypeId();
    	if (type == 23) { return true; }	// Dispenser
    	if (type == 61) { return true; }	// Furnace
    	if (type == 62) { return true; }	// Burning furnace
    	if (type == 54) { return true; }	// Chest
    	if (type == 130) { return true; }	// Ender chest
    	if (type == 146) { return true; }	// Trapped chest
    	if (type == 154) { return true; }   // Hopper
    	if (type == 158) { return true; }   // Dropper
    	
    	return false;
    }

    // Return true if event is for a chest
    public Boolean isChest(Block block) {
    	if (block == null) {
    		return false;
    	}
    	Integer type = block.getTypeId();
    	if (type == 54) { return true; }	// Chest
    	if (type == 154) { return true; }   // Hopper
    	if (type == 158) { return true; }   // Dropper
    	return false;
    }


    public Boolean isContainer(PlayerInteractEvent event) {
    	Block block = event.getClickedBlock();
    	if (block == null) {
    		return false;
    	}
    	Integer type = block.getTypeId();
    	if (type == 23) { return true; }	// Dispenser
    	if (type == 61) { return true; }	// Furnace
    	if (type == 62) { return true; }	// Burning furnace
    	if (type == 54) { return true; }	// Chest
    	if (type == 130) { return true; }	// Ender chest
    	if (type == 146) { return true; }	// Trapped chest
    	if (type == 154) { return true; }   // Hopper
    	if (type == 158) { return true; }   // Dropper
//		logger.info("[CP] not a container");
    	return false;
    }

    private void initDbPool() {
    	try {
	    	dbpool = new DbPool(
	    		settings.get("db_url"), 
	    		settings.get("db_user"), 
	    		settings.get("db_pass"),
	    		Integer.valueOf(settings.get("db_min")),
	    		Integer.valueOf(settings.get("db_max"))
	    	);
    	} catch (RuntimeException e) {
    		logger.warning("[CP] Init error: "+e.getLocalizedMessage());
    	}
    }

    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("db_url", "");
		settings.put("db_user", "");
		settings.put("db_pass", "");
		settings.put("db_min", "2");
		settings.put("db_max", "10");
		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], pair[1]);
    			}
    		}
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "[CP] Error reading " + e.getLocalizedMessage() + ", using defaults" );
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private boolean match_via_uuid(String oldname, String newname) {
    	boolean match = false;
		if (dbpool == null) {
			logger.info("[CP] Retrying dbpool initialization...");
			initDbPool();
		}
   	    if (dbpool != null) { 
   	    	Connection dbh = dbpool.getConnection();
   	        if (dbh != null) {
   	        	
	   	 		try {
	   		        PreparedStatement sth;
	   		        sth = dbh.prepareStatement(
	   		          "SELECT old.name " +
	   		          "FROM logins AS old, logins AS new " +
	   		          "WHERE old.uuid = new.uuid " +
	   		          "AND old.last_login < new.last_login " +
	   		          "AND old.name LIKE ? " +
	   		          "AND new.name LIKE ? "
	   		        );
	   		        sth.setNString(1, oldname);
	   		        sth.setNString(2, newname);
	   	   			ResultSet result = sth.executeQuery();
	   	   			while (result.next()) {
	   	   				if (result.getString("name").equalsIgnoreCase(oldname)) {
	   	   					match = true;
	   	   				}	
	   	   			}
	   			} catch (SQLException e) {
	   				e.printStackTrace();
	   				logger.warning("[CP] SQL error: "+e.getLocalizedMessage());
	   			}
   	        	
       	        dbpool.releaseConnection(dbh);
   	        }
   	    }

        return match;
    }
    
}
