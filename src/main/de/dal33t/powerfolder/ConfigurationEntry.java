/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Refelects a entry setting in the configuration file. Provides basic method
 * for accessing and setting the configuration.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum ConfigurationEntry {

    // Basics *****************************************************************

    /**
     * If the GUI should be disabled (=console mode). Default: False (Show GUI).
     */
    DISABLE_GUI("disableui", false),

    /**
     * If the user interface is locked and needs to be unlocked before starting
     * PowerFolder.
     * <p>
     * TRAC #1784
     */
    USER_INTERFACE_LOCKED("uilock.locked", false),

    /**
     * If any running instance should be killed when starting PowerFolder.
     * <P>
     * TRAC #2028
     */
    KILL_RUNNING_INSTANCE("kill.running.instance", false),

    /**
     * #2425: Sync and exit after connect to server.
     */
    SYNC_AND_EXIT("sync.exit", false),

    /**
     * Don't prompt on update. Simply auto-upgrade silently
     */
    AUTO_UPDATE("auto.update", false),

    /**
     * If some client options are available only with permissions such as create
     * folder or change client preferences.
     * <p>
     * TRAC #1979
     */
    SECURITY_PERMISSIONS_STRICT("security.permissions.strict", false),

    // Node setup *************************************************************

    /**
     * The nickname to use.
     */
    NICK("nick") {
        @Override
        public String getDefaultValue() {
            String def = null;
            if (StringUtils.isNotBlank(System.getenv("COMPUTERNAME"))) {
                def = System.getenv("COMPUTERNAME").toLowerCase();
            }
            if (def == null) {
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    def = addr.getHostName();
                } catch (UnknownHostException e) {
                }
            }
            if (def == null) {
                def = System.getProperty("user.name");
            }
            return def;
        }
    },

    /**
     * The node id to use. Advanced entry, usually automatically generated and
     * stored in preferences.
     */
    NODE_ID("nodeid"),

    /**
     * The network ID (#1373). PowerFolder can separate logical peer-to-peer
     * networks. Nodes with different network IDs won't connect to each other.
     * They even don't have other nodes in its local peer-to-peer nodes
     * database.
     * <P>
     * The default network ID of the open PowerFolder network is X.
     */
    NETWORK_ID("networkid", "X"),

    // Provider Settings ******************************************************

    /**
     * URL of the PowerFolder homepage
     */
    PROVIDER_URL("provider.url.main", "http://www.powerfolder.com"),

    /**
     * URL of the Online Storage features
     */
    PROVIDER_ABOUT_URL("provider.url.about",
        "http://www.powerfolder.com/wiki/Cloud_Space"),

    /**
     * Quickstart guides to PowerFolder
     */
    PROVIDER_QUICKSTART_URL("provider.url.quickstart",
        "http://www.powerfolder.com/quickstart.html"),

    /**
     * URL of the PowerFolder Support
     */
    PROVIDER_SUPPORT_URL("provider.url.support",
        "http://www.powerfolder.com/support.html"),

    /**
     * URL where bugs or tickets can be filed.
     */
    PROVIDER_SUPPORT_FILE_TICKET_URL("provider.url.ticket",
        "https://www.powerfolder.com/support/index.php?/Tickets/Submit"),

    /**
     * URL of the PowerFolder Pro page.
     * <p>
     * Recommended use: {@link ProUtil#getBuyNowURL(Controller)}
     */
    PROVIDER_BUY_URL("provider.url.buy",
        "http://www.powerfolder.com/buynow.html"),

    /**
     * URL where the contact form resides
     */
    PROVIDER_CONTACT_URL("provider.url.contact",
        "http://www.powerfolder.com/contact.html"),

    /**
     * URL of the PowerFolder Wiki. ATTENTION: This URL gets extended by article
     * URI from many help links
     */
    PROVIDER_WIKI_URL("provider.url.wiki", "http://www.powerfolder.com/wiki"),

    // Distribution infos *****************************************************

    DIST_BINARY_NAME("dist.binaryname", "PowerFolder"),

    DIST_NAME("dist.name", "PowerFolder"),

    DIST_DESCRIPTION("dist.description", "Sync your world"),

    DIST_FOLDERS_BASE_NAME("dist.folderbasename",
        Constants.FOLDERS_BASE_DIR_SUBDIR_NAME),

    // Server settings ********************************************************

    /**
     * The optional name of the sever to connect to.
     */
    SERVER_NAME("server.name", "PowerFolder Cloud"),

    /**
     * The optional url of the server.
     */
    SERVER_WEB_URL("server.url", "https://access.powerfolder.com"),

    /**
     * The node id of the server to connect to. Not mandatory but at
     * recommended. At leat host or ID of a server has to be set to connect to a
     * server.
     */
    SERVER_NODEID("server.nodeid", "WEBSERVICE03"),

    /**
     * The optional server hostname to connect to. Example:
     * server.powerfolder.com:1234
     */
    SERVER_HOST("server.host", "access.powerfolder.com:1337"),

    /**
     * HTTP tunnel relay URL. TODO Change config key.
     */
    SERVER_HTTP_TUNNEL_RPC_URL("provider.url.httptunnel",
        "http://os005.powerfolder.com/rpc"),

    /**
     * #1687: How this computer should behave when the server is not connected.
     */
    SERVER_DISCONNECT_SYNC_ANYWAYS("server.disconnect.sync_anyways", false),

    /**
     * If the config should be update when connection to the server was
     * established
     */
    SERVER_CONFIG_UPDATE("server.config.update", true),

    // Server WEB settings ****************************************************

    /**
     * #2448: Option to disable Web access
     */
    WEB_LOGIN_ALLOWED("web.login.allowed", true),

    /**
     * If WebDAV should be enabled.
     */
    WEB_DAV_ENABLED("web.dav.enabled", true),

    // Config META information ************************************************

    /**
     * Prompt for the server address if currently set server belongs to the
     * powerfolder.com cloud.
     */
    CONFIG_PROMPT_SERVER_IF_PF_COM("config.promptifpf", false),

    /**
     * The URL of the current config
     */
    CONFIG_URL("config.url"),

    /**
     * If this URL should override all previously set or default values.
     */
    CONFIG_OVERWRITE_VALUES("config.overwrite", false),

    /**
     * If all folder settings should be dropped when this config is loaded
     */
    CONFIG_DROP_FOLDER_SETTINGS("config.drop.folders.settings", false),

    /**
     * #2248 Automatically assign client to server by IP address
     */
    CONFIG_ASSIGN_IP_LIST("config.assign.iplist"),

    // Update settings ********************************************************

    /**
     * http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt
     */
    UPDATE_VERSION_URL("update.version.url"),

    /**
     * http://checkversion.powerfolder.com/PowerFolder_DownloadLocation.txt
     */
    UPDATE_DOWNLOADLINK_INFO_URL("update.download_info.url"),

    /**
     * http://download.powerfolder.com/free/PowerFolder_Latest_Win32_Installer.
     * exe
     */
    UPDATE_WINDOWS_EXE_URL("update.windows_exe.url"),
    
    /**
     * PFC-2167: Installer launches PowerFolder under the account used for elevation.
     */
    UPDATE_SILENT_UPDATE_ALLOWED("update.silent_update.allowed", true),

    // Server connection ******************************************************

    /**
     * #1715 If it should be possible to register at the server.
     */
    SERVER_REGISTER_ENABLED("server.register.enabled", true),

    /**
     * If "Password recovery" should be enabled. If not the server automatically
     * sets this. (Should be yes if not LDAP).
     */
    SERVER_RECOVER_PASSWORD_ENABLED("server.recover.password.enabled", true),

    /**
     * #2401: If the login/usernames are Email addresses.
     */
    SERVER_USERNAME_IS_EMAIL("server.username.isemail", "both"),

    /**
     * Username for connection
     */
    SERVER_CONNECT_USERNAME("server.connect.username"),

    /**
     * Password for connection (obfuscated)
     */
    SERVER_CONNECT_PASSWORD("server.connect.passwordobf"),

    /**
     * Password for connection (clear text)
     */
    SERVER_CONNECT_PASSWORD_CLEAR("server.connect.password"),

    /**
     * #2219: Allow clients to login with no password. Use system property
     * "user.name" as login username.
     */
    SERVER_CONNECT_NO_PASSWORD_ALLOWED("server.connect.nopassword.allowed",
        false),

    /**
     * #2518
     */
    SERVER_CONNECT_REMEMBER_PASSWORD_ALLOWED(
        "server.connect.rememberpassword.allowed", true),

    /**
     * #2229: Disallow change of login
     */
    SERVER_CONNECT_CHANGE_LOGIN_ALLOWED("server.connect.changelogin.allowed",
        true),

    /**
     * #2338: Always connect to server, even in LAN only mode
     */
    SERVER_CONNECT_FROM_LAN_TO_INTERNET("server.connect.lan2internet", true),

    // General settings *******************************************************

    /**
     * The networking mode. See class <code>NetworkingMode</code> for more
     * information.
     * 
     * @see NetworkingMode
     */
    NETWORKING_MODE("networkingmode", NetworkingMode.PRIVATEMODE.name()),

    /**
     * The ip/address where powerfolder should bind to.
     */
    NET_BIND_ADDRESS("net.bindaddress"),

    /**
     * The port(s) to bind to.
     */
    NET_BIND_PORT("port"),

    /**
     * If true, powerfolder tries to open it's ports on the firewall. (It also
     * will try to close them when exiting)
     */
    NET_FIREWALL_OPENPORT("net.openport", true),

    /**
     * If relayed or tunnel connections should be tried for LAN based computers.
     * Usually this does not make sense. Only for special scenarios. e.g.
     * ILY-570834
     */
    NET_USE_RELAY_TUNNEL_ON_LAN("net.relaytunnel.lan.enabled", false),

    /**
     * If the {@link RemoteCommandManager} should be started or not.
     */
    NET_RCON_MANAGER("net.rcon", true),

    /**
     * The TCP port for the {@link RemoteCommandManager}
     */
    NET_RCON_PORT("net.rcon.port", 1338),

    /**
     * If broadcast on LAN
     */
    NET_BROADCAST("net.broadcast", true),

    /**
     * Use a random port in the (49152) 0 to 65535 range, overides NET_BIND_PORT
     */
    NET_BIND_RANDOM_PORT("random-port", true),

    /**
     * The TCP/IP socket buffer size for TCP/UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_SIZE("net.socket.internet.buffer.size",
        16 * 1024),

    /**
     * The TCP/IP socket buffer size for TCP/UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_SIZE("net.socket.lan.buffer.size", 64 * 1024),

    /**
     * The TCP/IP socket buffer size limit for UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_LIMIT("net.socket.internet.buffer.limit",
        256 * 1024),

    /**
     * The TCP/IP socket buffer size limit for UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_LIMIT("net.socket.lan.buffer.limit", 1024 * 1024),

    /**
     * Auto detect WAN speeds
     */
    TRANSFER_LIMIT_AUTODETECT("transfer.limit.autodetect", true),

    /**
     * The upload limit for WAN (Internet) connections in KB/s
     */
    UPLOAD_LIMIT_WAN("uploadlimit", 0),

    /**
     * The download limit for WAN (Internet) connections in KB/s
     */
    DOWNLOAD_LIMIT_WAN("downloadlimit", 0),

    /**
     * The upload limit for LAN connections in KB/s
     */
    UPLOAD_LIMIT_LAN("lanuploadlimit", 0),

    /**
     * The download limit for LAN connections in KB/s
     */
    DOWNLOAD_LIMIT_LAN("landownloadlimit", 0),

    /**
     * The percentage to throttle the uploadlimits in paused mode.
     */
    UPLOADLIMIT_PAUSEDMODE_THROTTLE("net.silentmodethrottle"),

    /**
     * The maximum size (in bytes) of an {@link FileChunk} used for file
     * transfers
     */
    TRANSFERS_MAX_FILE_CHUNK_SIZE("transfers.max.file.chunk.size", 32 * 1024),

    /**
     * The maximum number of queued request for {@link FileChunk}s
     */
    TRANSFERS_MAX_REQUESTS_QUEUED("transfers.max.request.queued", 15),

    /**
     * My dynamic dns hostname or fix ip.
     */
    HOSTNAME("hostname") {

        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                value = controller.getConfig().getProperty("mydyndns");
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("mydyndns");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("mydyndns");
        }
    },

    /**
     * Setting to enable/disable zip compression on LAN
     */
    USE_ZIP_ON_LAN("use_zip_on_lan", false),

    /**
     * Setting to enable/disable swarming in an LAN environment. If swarming
     * below is set to false, this is ignored!
     */
    USE_SWARMING_ON_LAN("swarming.lan.enabled", true),

    /**
     * Delta-sync: Enable/Disable it.
     */
    USE_DELTA_ON_INTERNET("deltasync.internet.enabled", true),

    USE_DELTA_ON_LAN("deltasync.lan.enabled", true),

    /**
     * Setting to enable/disable swarming.
     */
    USE_SWARMING_ON_INTERNET("swarming.internet.enabled", true),

    /**
     * The basedir for all powerfolder.
     */
    FOLDER_BASEDIR("foldersbase") {
        @Override
        public String getDefaultValue() {
            String rootDir = System.getProperty("user.home");

            // Also place the base dir into user home on Vista and 7
            if (OSUtil.isWindowsSystem() && OSUtil.isWindowsXPSystem()) {
                WinUtils util = WinUtils.getInstance();
                if (util != null) {
                    String can = util.getSystemFolderPath(
                        WinUtils.CSIDL_PERSONAL, false);
                    if (StringUtils.isNotBlank(can)) {
                        rootDir = can;
                    }
                }
            }
            return rootDir + File.separatorChar
                + Constants.FOLDERS_BASE_DIR_SUBDIR_NAME;
        }
    },

    /**
     * PFC-2165 Base directory does not longer change back to default directory
     * if inaccessible during program start, e.g. USB- or Network-Drive
     */
    FOLDER_BASEDIR_FALLBACK_TO_DEFAULT("folderbase.fallback.enabled", false),

    MASS_DELETE_PROTECTION("mass.delete.protection", false) {

        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                value = PreferencesEntry.MASS_DELETE_PROTECTION
                    .getValueString(controller);
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            PreferencesEntry.MASS_DELETE_PROTECTION.removeValue(controller);
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            PreferencesEntry.MASS_DELETE_PROTECTION.removeValue(controller);
        }

    },

    MASS_DELETE_THRESHOLD("mass.delete.threshold", 95) {

        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                value = PreferencesEntry.MASS_DELETE_THRESHOLD
                    .getValueString(controller);
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            PreferencesEntry.MASS_DELETE_THRESHOLD.removeValue(controller);
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            PreferencesEntry.MASS_DELETE_THRESHOLD.removeValue(controller);
        }
    },

    /**
     * Contains a comma-separated list of all plugins to load.
     */
    PLUGINS("plugins"),

    /**
     * Contains a comma-separated list of all plugins, which are disabled.
     */
    PLUGINS_DISABLED("plugins.disabled"),

    /**
     * Flag if update at start should performed.
     */
    DYNDNS_AUTO_UPDATE("dyndns.autoUpdate", false) {

        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                value = controller.getConfig().getProperty("onStartUpdate");
            }
            return value != null ? value : Boolean.FALSE.toString();
        }

    },

    /**
     * The username to use for the dyndns update.
     */
    DYNDNS_USERNAME("dyndnsUserName"),

    /**
     * The password to use for the dyndns update.
     */
    DYNDNS_PASSWORD("dyndnsPassword"),

    /**
     * The ip of the last dyndns update.
     */
    DYNDNS_LAST_UPDATED_IP("lastUpdatedIP"),

    /**
     * Comma-seperated list of ip-ranges that are (forced) in our LAN.
     */
    LANLIST("lanlist", ""),

    /**
     * Whether to use the PowerFolder icon in Windows Explorer folders.
     */
    USE_PF_ICON("use.pf.icon", true),

    /**
     * Whether to set PowerFolders as a Favorite Link.
     */
    USE_PF_LINK("use.pf.link", true),

    /**
     * #2256: Simple conflict detection.
     */
    CONFLICT_DETECTION("conflict.detection", true),

    /**
     * Whether to log verbose.
     */
    VERBOSE("verbose", false),

    /**
     * The loglevel to write to debug file when verbose=true
     */
    LOG_LEVEL_FILE("log.file.level", Level.FINE.getName()),

    /**
     * #2585
     */
    LOG_FILE_ROTATE("log.file.rotate", true),

    /**
     * The loglevel to print to console when verbose=true
     */
    LOG_LEVEL_CONSOLE("log.console.level", Level.INFO.getName()),

    /**
     * Whether to request debug reports
     * 
     * @see RequestNodeInformation
     */
    DEBUG_REPORTS("debug.reports", false),

    /**
     * If it should be automatically connected to other nodes. FIX: Currently
     * only affects ReconnectManager.
     */
    AUTO_CONNECT("auto.connect", true),

    /**
     * The number of seconds to go on idle between connection tries per
     * Reconnector.
     */
    CONNECT_WAIT("connect.wait.seconds", 120),

    /**
     * Enable/Disable relayed connections.
     */
    RELAYED_CONNECTIONS_ENABLED("connections.relayed", true),

    /**
     * Enable/Disable relayed connections.
     */
    UDT_CONNECTIONS_ENABLED("connections.udt", true),

    /**
     * Enable/Disable node manager (for debugging only)
     */
    NODEMANAGER_ENABLED("nodemanager.enabled", true),

    /**
     * Enable/Disable transfermanager (for debugging only)
     */
    TRANSFER_MANAGER_ENABLED("transfermanager.enabled", true),

    /**
     * Enable/Disable folder repo (for debugging only)
     */
    FOLDER_REPOSITORY_ENABLED("folderepository.enabled", true),

    /**
     * Whether to show preview folders in nav / folders panles.
     */
    HIDE_PREVIEW_FOLDERS("show.preview.folders", false),

    /**
     * The number of seconds to wait to recalc a {@link FolderStatistic} when a
     * change to the folder happened. Applies to large folders only, that host
     * more files than {@link FolderStatistic#MAX_ITEMS}
     */
    FOLDER_STATS_CALC_TIME("filedb.stats.seconds", 30),

    /**
     * The maximum time powerfolder keeps the folder database dirty in memory
     * before writing it to disk in seconds.
     */
    FOLDER_DB_PERSIST_TIME("filedb.persist.seconds", 60),

    /**
     * #2637: Disabling can save OS resources.
     */
    FOLDER_WATCHER_ENABLED("folder.watcher.enabled", true),
    /**
     * #2405: The delay for syncing after folderWatcher detects a change.
     */
    FOLDER_WATCHER_DELAY("folder.watcher.delay.seconds", 1),

    /**
     * The number of seconds between db maintenance scans (1 hour).
     */
    DB_MAINTENANCE_SECONDS("filedb.maintenance.seconds", 3600),

    /**
     * The age of a deleted file until it gets removed by the folder db
     * maintenance. In Seconds! Default: 1 month
     */
    MAX_FILEINFO_DELETED_AGE_SECONDS("filedb.deleted.maxage", 60 * 60 * 24 * 30
        * 3),

    /**
     * The http proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_HOST("http.proxy.host"),

    /**
     * The http proxy port to use for HTTP tunneled connections
     */
    HTTP_PROXY_PORT("http.proxy.port", 80),

    /**
     * The http proxy username to use for HTTP tunneled connections
     */
    HTTP_PROXY_USERNAME("http.proxy.username"),

    /**
     * The http password proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_PASSWORD("http.proxy.password"),

    /**
     * Days until auto cleanup of uploads. Zero = cleanup on completion. NOTE -
     * true cleanup days dereferenced through Constants.CLEANUP_VALUES
     */
    UPLOAD_AUTO_CLEANUP_FREQUENCY("uploads.auto.cleanup.frequency", 2),

    /**
     * Days until auto cleanup of downloads. Zero = cleanup on completion. NOTE
     * - true cleanup days dereferenced through Constants.CLEANUP_VALUES
     */
    DOWNLOAD_AUTO_CLEANUP_FREQUENCY("downloads.auto.cleanup.frequency", 2),

    /** Warning about unsyned folders. */
    FOLDER_SYNC_USE("sync.folder.use", true),

    /** Seconds before warning about unsynced folders (10 days). */
    FOLDER_SYNC_WARN_SECONDS("sync.folder.warn.seconds", 864000) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                try {
                    value = String.valueOf(24L * 60 * 60 * Integer
                        .valueOf(controller.getConfig().getProperty(
                            "sync.folder.warn")));
                } catch (Exception e) {
                }
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("sync.folder.warn");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("sync.folder.warn");
        }
    },

    /**
     * TRAC #1776
     * <p>
     * Checks and avoids duplicate folders with the same name or base dir.
     * Duplicate folders by should be automatically prevented.
     */
    FOLDER_CREATE_AVOID_DUPES("create.folder.avoid.dupes", false),

    /**
     * Uses any existing directory found at the default path, even if not empty.
     */
    FOLDER_CREATE_USE_EXISTING("create.folder.use.existing", false),

    /** Online storage only client. */
    BACKUP_ONLY_CLIENT("backup.only.client", false),

    /** The archive mode to use when creating a new folder. */
    DEFAULT_ARCHIVE_MODE("default.archive.mode", ArchiveMode.FULL_BACKUP.name()),

    /** The number of file versions to use when creating a new folder. */
    DEFAULT_ARCHIVE_VERSIONS("default.archive.versions", 5),

    /**
     * How many days before an archive file is cleaned up. Values 1, 7, 31, 365,
     * 0 (== never)
     */
    DEFAULT_ARCHIVE_CLEANUP_DAYS("archive.cleanup.days", 0),

    /**
     * #2132: This transfer mode will be recommend by default.
     */
    DEFAULT_TRANSFER_MODE("default.transfer.mode",
        SyncProfile.AUTOMATIC_SYNCHRONIZATION.getFieldList()),

    /**
     * Automatically setup all folders the user has access to and also
     * automatically accept folder invites.
     */
    AUTO_SETUP_ACCOUNT_FOLDERS("auto.setup.account.folders", false),

    LOOK_FOR_FOLDER_CANDIDATES("look.for.folder.candidates", true),

    REMOVED_FOLDER_FILES("removed.folder.files", ""),

    /**
     * #2485: {@link Integer#MAX_VALUE} for never resume. 0 for adaptive resume
     * = resume when user is not working on his PC
     */
    PAUSE_RESUME_SECONDS("pause.resume.seconds", 3600); // One hour default.

    // Methods/Constructors ***************************************************

    private static final Logger LOG = Logger.getLogger(ConfigurationEntry.class
        .getName());

    private final String configKey;
    protected final String defaultValue;

    ConfigurationEntry(String aConfigKey) {
        this(aConfigKey, null);
    }

    ConfigurationEntry(String aConfigKey, String theDefaultValue) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        Reject.ifTrue(
            aConfigKey.startsWith(FolderSettings.FOLDER_SETTINGS_PREFIX_V4),
            "Config entries must not start with '"
                + FolderSettings.FOLDER_SETTINGS_PREFIX_V4 + '\'');
        configKey = aConfigKey;
        if (theDefaultValue != null) {
            defaultValue = theDefaultValue;
        } else {
            // Try harder. Use getter. might have been overridden
            defaultValue = getDefaultValue();
        }
    }

    ConfigurationEntry(String aConfigKey, boolean theDefaultValue) {
        this(aConfigKey, String.valueOf(theDefaultValue));
    }

    ConfigurationEntry(String aConfigKey, int theDefaultValue) {
        this(aConfigKey, String.valueOf(theDefaultValue));
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return If a value was set for this entry.
     */
    public boolean hasValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return controller.getConfig().getProperty(configKey) != null;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or
     */
    public String getValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        String value = controller.getConfig().getProperty(configKey);
        if (value == null) {
            value = getDefaultValue();
        }
        return value;
    }

    /**
     * Parses the configuration entry into a Integer.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Integer getValueInt(Controller controller) {
        String value = getValue(controller);
        if (value == null) {
            value = getDefaultValue();
        }
        try {
            return new Integer(value.trim());
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                + configKey + "' into a int. Value: " + value, e);
            return new Integer(getDefaultValue());
        }
    }

    /**
     * Parses the configuration entry into a Boolen.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Boolean getValueBoolean(Controller controller) {
        String value = getValue(controller);
        if (value == null) {
            value = getDefaultValue();
        }
        try {
            return value.trim().equalsIgnoreCase("true");
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                + configKey + "' into a boolean. Value: " + value, e);
            return "true".equalsIgnoreCase(getDefaultValue());
        }
    }

    /**
     * Creates a model containing the value of the configuration entry.
     * <p>
     * Changes from "below" won't be reflected.
     * <p>
     * TODO Resolve problem: Model not buffered!
     * 
     * @param controller
     * @return a value model bound to the configuration entry.
     * @deprecated do not use util problems are resolved
     */
    @Deprecated
    public ValueModel getModel(final Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        ValueModel model = new ValueHolder(getValue(controller), false);
        model.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setValue(controller, (String) evt.getNewValue());
            }
        });
        return model;
    }

    /**
     * Sets the value of this config entry.
     * 
     * @param controller
     *            the controller of the config
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, String value) {
        Reject.ifNull(controller, "Controller is null");
        controller.getConfig().setProperty(configKey, value);
    }

    /**
     * Sets the value of this config entry.
     * 
     * @param controller
     *            the controller of the config
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, boolean value) {
        setValue(controller, String.valueOf(value));
    }

    /**
     * Sets the value of this config entry.
     * 
     * @param controller
     *            the controller of the config
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, int value) {
        setValue(controller, String.valueOf(value));
    }

    /**
     * Removes the entry from the configuration.
     * 
     * @param controller
     *            the controller to use
     */
    public void removeValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        controller.getConfig().remove(configKey);
    }

    /**
     * @return the default value for this config entry.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the key in config
     */
    public String getConfigKey() {
        return configKey;
    }
}
