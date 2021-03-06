package fi.matiaspaavilainen.masuiteteleports.bukkit.commands.force;

import fi.matiaspaavilainen.masuitecore.core.objects.PluginChannel;
import fi.matiaspaavilainen.masuiteteleports.bukkit.MaSuiteTeleports;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Teleport implements CommandExecutor {

    private MaSuiteTeleports plugin;

    public Teleport(MaSuiteTeleports p) {
        plugin = p;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            if (plugin.in_command.contains(sender)) {
                plugin.formator.sendMessage(sender, plugin.config.load(null, "messages.yml").getString("on-active-command"));
                return;
            }

            plugin.in_command.add(sender);

            Player p = (Player) sender;


            switch (args.length) {
                case (1):
                    new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportSenderToTarget", args[0]}).send();
                    break;
                case (2):
                    new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportTargetToTarget", args[0], args[1]}).send();
                    break;
                case (3):
                    // Teleport sender to coordinates
                    if (Double.isNaN(parse(args[0], 0)) && Double.isNaN(parse(args[1], 0)) && Double.isNaN(parse(args[2], 0))) {
                        return;
                    }
                    new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportToXYZ",
                            sender.getName(),
                            parse(args[0], p.getLocation().getX()),
                            parse(args[1], p.getLocation().getY()),
                            parse(args[2], p.getLocation().getZ())}).send();
                    break;
                case (4):
                    if (Double.isNaN(parse(args[1], 0)) && Double.isNaN(parse(args[2], 0)) && Double.isNaN(parse(args[3], 0))) {
                        return;
                    }
                    // If any of the server's worlds match to args[0]
                    if (Bukkit.getWorlds().stream().anyMatch(world -> world.getName().equals(args[0]))) {
                        new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportToCoordinates",
                                sender.getName(),
                                args[0],
                                parse(args[1], p.getLocation().getX()),
                                parse(args[2], p.getLocation().getY()),
                                parse(args[3], p.getLocation().getZ())}).send();
                        break;
                    }

                    // If not, send target to XYZ
                    new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportToXYZ",
                            args[0],
                            parse(args[0], p.getLocation().getX()),
                            parse(args[1], p.getLocation().getY()),
                            parse(args[2], p.getLocation().getZ())}).send();
                    break;
                case (5):
                    // Teleport target to location
                    if (Double.isNaN(parse(args[2], 0)) && Double.isNaN(parse(args[3], 0)) && Double.isNaN(parse(args[4], 0))) {
                        return;
                    }
                    new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "TeleportForceTo", sender.getName(), "TeleportToCoordinates",
                            args[0],
                            args[1],
                            parse(args[2], p.getLocation().getX()),
                            parse(args[3], p.getLocation().getY()),
                            parse(args[4], p.getLocation().getZ())}).send();
                    break;
                default:
                    plugin.formator.sendMessage(sender, plugin.config.load("teleports", "syntax.yml").getString("tp.title"));
                    for (String syntax : plugin.config.load("teleports", "syntax.yml").getStringList("tp.syntaxes")) {
                        plugin.formator.sendMessage(p, syntax);
                    }
                    break;
            }
            plugin.in_command.remove(sender);
        });

        return true;
    }

    // Check if string is parsable to Double
    private boolean isDouble(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private double parse(String string, double currentCoord) {
        if (string.startsWith("~")) {
            if (isDouble(string.replace("~", "") + currentCoord)) {
                String s = string.replace("~", "");
                return !s.isEmpty() ? Double.parseDouble(s) + currentCoord : currentCoord;
            }
        } else if (isDouble(string)) {
            return Double.parseDouble(string);
        }
        return Double.NaN;
    }
}
