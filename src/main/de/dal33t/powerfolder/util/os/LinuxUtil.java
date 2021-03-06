/*
 * Copyright 2004 - 2016 Christian Sprajc. All rights reserved.
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

package de.dal33t.powerfolder.util.os;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities for linux
 *
 * @author <a href="mailto:kappel@powerfolder">Christoph Kappel</a>
 * @version $Id$
 */
public class LinuxUtil {

    private LinuxUtil() {
        /* Prevent instances of this class */
    }

    /**
     * Get current desktop environment (like: Unity, KDE) based on $XDG_CURRENT_DESKTOP env variable
     *
     * @return name of current desktop environment or null if unset
     */
    public static String getDesktopEnvironment() {
        return System.getenv("XDG_CURRENT_DESKTOP");
    }

    /**
     * Whether current desktop environment is NOT KDE
     *
     * @return true when it's not KDE or otherwise false
     */
    public static boolean isNotKDE() {
        String deskEnv = getDesktopEnvironment();

        return (null == deskEnv || !deskEnv.contains("KDE"));
    }

    /**
     * Get path to desktop directory based on system settings
     *   Order: $XDG_CURRENT_DIR > xdg-user-dir > $HOME/Desktop
     *
     * @return path to the desktop dir or null if none set/found
     */
    public static Path getDesktopDirPath() {
        /* 1. Check environment variable XDG_DESKTOP_DIR */
        String path = System.getenv("XDG_DESKTOP_DIR");

        if (null == path) {
            /* 2. Check commandline tool xdg-user-dir */
            ProcessBuilder pb = new ProcessBuilder("xdg-user-dir", "DESKTOP");

            try {
                Process proc = pb.start();

                BufferedReader stdOut = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));

                path = stdOut.readLine();

                stdOut.close();
            } catch (IOException e) {
                /* We ignore that here */
            }

            if (null == path) {
                /* 3. Fallback to educated guess */
                path = "" + System.getProperty("user.home") + ///< Avoid NullPointerException when property is unset
                        System.getProperty("file.separator") + "Desktop";
            }
        }

        return (null != path ? Paths.get(path) : null);
    }

    /**
     * Mount given WebDAV url
     *
     * @param serverClient Instance of our {@link ServerClient}
     * @param webDAVURL    WebDAV url to use
     *
     * @return Either Y on success; otherwise N with error messages
     */
    public static String mountWebDAV(ServerClient serverClient, String webDAVURL)
    {
        /* Check suffix */
        String name = FilenameUtils.getBaseName(webDAVURL);

        if (!name.endsWith(Constants.FOLDER_WEBDAV_SUFFIX)) {
            name += Constants.FOLDER_WEBDAV_SUFFIX;
        }

        /* Assemble mount path */
        Path mountPath = serverClient.getController().getFolderRepository()
                .getFoldersBasedir().resolve(name);

        String password = (serverClient.isTokenLogin() ? serverClient.getWebDavToken() :
                serverClient.getPasswordClearText());
        String username;
        Account account = serverClient.getAccount();
        if (account.isValid()) {
            username = account.getUsername();
        } else {
            username = serverClient.getUsername();
        }
        return mountWebDAV(username, password, webDAVURL, mountPath, false);
    }

    /**
     * Mount given WebDAV url
     *
     * @param webDAVURL WebDAV url to use. Url notation: webdav://<username>:<password>@<WebDAV resource>
     *
     * @return Either Y on success; otherwise N with error messages
     */
    public static String mountWebDAV(String webDAVURL, Path mountPath) throws MalformedURLException {

        String username = null;
        String password = null;

        URL wUrl = new URL(webDAVURL);

        String protocol = wUrl.getProtocol();
        String authority = wUrl.getAuthority();
        String webDAVHost = webDAVURL.substring(webDAVURL.lastIndexOf("@") + 1, webDAVURL.length());

        if (null != authority) {
            username = authority.substring(0, authority.indexOf(":"));
            password = authority.substring(authority.indexOf(":") + 1, authority.lastIndexOf("@"));
        }

        webDAVURL = !webDAVHost.contains(Constants.FOLDER_WEBDAV_HTTP_PREFIX) ? protocol + "://" + webDAVHost : webDAVHost;

        return mountWebDAV(username, password, webDAVURL, mountPath, true);
    }

    /**
     * Mount given WebDAV url at given path
     *
     * @param username   Webdav username
     * @param password   Webdav password
     * @param webDAVURL  WebDAV url to use
     * @param mountPath  Mount to path at
     * @param useSudo    Use sudo instead of pkexec
     *
     * @return Either Y on success; otherwise N with error messages
     */
    public static String mountWebDAV(String username, String password,
                                     String webDAVURL, Path mountPath, boolean useSudo)
    {
        /* Check environment */
        Path shPath     = Paths.get("/bin/bash");
        Path sudoPath   = Paths.get("/usr/bin/sudo");
        Path pkexecPath = Paths.get("/usr/bin/pkexec");
        Path davfsPath  = Paths.get("/sbin/mount.davfs");

        if(Files.notExists(shPath)) {
            return "N" + Translation.get("dialog.webdav.install_missing", "bash");
        }

        if(useSudo && Files.notExists(sudoPath)) {
            return "N" + Translation.get("dialog.webdav.install_missing", "sudo");
        }

        if(!useSudo && Files.notExists(pkexecPath)) {
            return "N" + Translation.get("dialog.webdav.install_missing", "pkexec");
        }

        if(Files.notExists(davfsPath)) {
            return "N" + Translation.get("dialog.webdav.install_missing", "davfs2");
        }

        /* Check mount path */
        try {
            if(Files.notExists(mountPath)) {
                Files.createDirectories(mountPath);
            }
        } catch(IOException e) {
            return "N" + e.getMessage();
        }

        /* Call command (DO NO MESS WITH IT UNLESS YOU KNOW WHAT YOU ARE DOING!) */
        String command = String.format("echo '%s' | %s %s %s -o users,username=%s,uid=%s %s",
                password.replace("\'", "\\\'"), (useSudo ? sudoPath : pkexecPath),
                davfsPath, webDAVURL, username, System.getProperty("user.name"), mountPath);

        String[] commands = new String[] {
            shPath.toString(), "-c", command
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);

            Process proc = pb.start();

            BufferedReader stdErr = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream()));

            StringBuilder err = new StringBuilder();
            for (String line = stdErr.readLine(); line != null; line = stdErr.readLine()) {
                err.append(line + " ");
            }

            stdErr.close();

            return StringUtils.isBlank(err.toString()) ? "Y" : "N" + err;
        } catch (IOException e) {
            return "N" + e.getMessage();
        }
    }

    public static String checkRequirementsForWebDAV(){

        if (!OSUtil.isLinux()){
            return Translation.get("dialog.webdav.install_missing");
        }

        Path shPath     = Paths.get("/bin/bash");
        Path sudoPath   = Paths.get("/usr/bin/sudo");
        Path pkexecPath = Paths.get("/usr/bin/pkexec");
        Path davfsPath  = Paths.get("/sbin/mount.davfs");

        if(Files.notExists(shPath)) {
            return Translation.get("dialog.webdav.install_missing", "bash");
        }

        if(Files.notExists(sudoPath)) {
            return Translation.get("dialog.webdav.install_missing", "sudo");
        }

        if(Files.notExists(pkexecPath)) {
            return Translation.get("dialog.webdav.install_missing", "pkexec");
        }

        if(Files.notExists(davfsPath)) {
            return Translation.get("dialog.webdav.install_missing", "davfs2");
        }

        return null;
    }
}
