package fi.matiaspaavilainen.masuiteteleports.managers;

import fi.matiaspaavilainen.masuitecore.Debugger;
import fi.matiaspaavilainen.masuitecore.chat.Formator;
import fi.matiaspaavilainen.masuitecore.config.Configuration;
import fi.matiaspaavilainen.masuiteteleports.database.Database;
import fi.matiaspaavilainen.masuitecore.managers.Location;
import fi.matiaspaavilainen.masuiteteleports.MaSuiteTeleports;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Spawn {

    private Database db = MaSuiteTeleports.db;
    private String server;
    private Location location;
    private Connection connection = null;
    private PreparedStatement statement = null;
    private Configuration config = new Configuration();
    private String tablePrefix = config.load(null, "config.yml").getString("database.table-prefix");
    private Debugger debugger = new Debugger();

    public Spawn() {
    }

    public Spawn(String server, Location location) {
        this.server = server;
        this.location = location;
    }


    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Spawn find(String server) {
        Spawn spawn = new Spawn();
        ResultSet rs = null;
        String type = config.load("teleports", "settings.yml").getString("spawn-type");
        String select = null;
        if (type.equalsIgnoreCase("server")) {
            select = "SELECT * FROM " + tablePrefix + "spawns WHERE server = ?;";
        }

        if (type.equalsIgnoreCase("global")) {
            select = "SELECT * FROM " + tablePrefix + "spawns;";

        }
        try {
            connection = db.hikari.getConnection();
            statement = connection.prepareStatement(select);
            if (type.equalsIgnoreCase("server")) {
                statement.setString(1, server);
            }
            rs = statement.executeQuery();

            boolean empty = true;
            while (rs.next()) {
                spawn.setServer(rs.getString("server"));
                spawn.setLocation(new Location(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                debugger.sendMessage("[MaSuite] [Teleports] [Spawn] spawn loaded.");
                empty = false;
            }
            if (empty) {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return spawn;
    }

    public Boolean spawn(ProxiedPlayer p, MaSuiteTeleports plugin) {
        Spawn spawn = new Spawn().find(p.getServer().getInfo().getName());
        if (spawn == null) {
            new Formator().sendMessage(p, config.load("teleports", "messages.yml").getString("spawn.not-found"));
            return false;
        }
        try {
            plugin.positions.requestPosition(p);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("MaSuiteTeleports");
            out.writeUTF("SpawnPlayer");
            out.writeUTF(p.getName());
            Location loc = spawn.getLocation();
            out.writeUTF(loc.getWorld() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch());
            if (!spawn.getServer().equals(p.getServer().getInfo().getName())) {
                p.connect(ProxyServer.getInstance().getServerInfo(spawn.getServer()));
                ProxyServer.getInstance().getScheduler().schedule(plugin, () -> p.getServer().sendData("BungeeCord", b.toByteArray()), 500, TimeUnit.MILLISECONDS);
            } else {
                p.getServer().sendData("BungeeCord", b.toByteArray());
            }

            debugger.sendMessage("[MaSuite] [Teleports] [Spawn] spawned player.");
        } catch (IOException e) {
            e.getStackTrace();
        }
        return true;
    }

    public boolean create(Spawn spawn) {
        String type = config.load("teleports", "settings.yml").getString("spawn-type");
        String insert = null;
        if (type.equalsIgnoreCase("server")) {
            insert = "INSERT INTO " + tablePrefix +
                    "spawns (server, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?;";
        }

        if (type.equalsIgnoreCase("global")) {
            if (spawn.all().size() > 0) {
                insert = "UPDATE " + tablePrefix + "spawns " +
                        "server = ?, world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?;";
            } else {
                insert = "INSERT INTO " + tablePrefix +
                        "spawns (server, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?);";
            }

        }

        try {
            connection = db.hikari.getConnection();
            statement = connection.prepareStatement(insert);
            statement.setString(1, spawn.getServer());
            statement.setString(2, spawn.getLocation().getWorld());
            statement.setDouble(3, spawn.getLocation().getX());
            statement.setDouble(4, spawn.getLocation().getY());
            statement.setDouble(5, spawn.getLocation().getZ());
            statement.setFloat(6, spawn.getLocation().getYaw());
            statement.setFloat(7, spawn.getLocation().getPitch());
            if (type.equalsIgnoreCase("server")) {
                statement.setString(8, spawn.getLocation().getWorld());
                statement.setDouble(9, spawn.getLocation().getX());
                statement.setDouble(10, spawn.getLocation().getY());
                statement.setDouble(11, spawn.getLocation().getZ());
                statement.setFloat(12, spawn.getLocation().getYaw());
                statement.setFloat(13, spawn.getLocation().getPitch());
            }

            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Set<Spawn> all() {
        Set<Spawn> spawns = new HashSet<>();
        ResultSet rs = null;

        try {
            connection = db.hikari.getConnection();
            statement = connection.prepareStatement("SELECT * FROM " + tablePrefix + "spawns;");
            rs = statement.executeQuery();
            while (rs.next()) {
                Spawn spawn = new Spawn();
                spawn.setServer(rs.getString("server"));
                spawn.setLocation(new Location(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                spawns.add(spawn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return spawns;
    }

    public Boolean delete(String spawn) {
        try {
            connection = db.hikari.getConnection();
            statement = connection.prepareStatement("DELETE FROM " + tablePrefix + "spawns WHERE server = ?");
            statement.setString(1, spawn);
            statement.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkTable() {
        try {
            connection = db.hikari.getConnection();
            statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tablePrefix + "spawns' AND COLUMN_NAME = 'type';");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                if (rs.getInt(1) == 0) {
                    statement = connection.prepareStatement("ALTER TABLE " + tablePrefix + "spawns ADD COLUMN type TINYINT(1) NULL DEFAULT 0;");
                    statement.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
