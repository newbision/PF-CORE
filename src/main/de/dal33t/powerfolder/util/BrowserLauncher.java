package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Bare Bones Browser Launch
 * <p>
 * Version 1.5
 * <p>
 * December 10, 2005
 * <p>
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP
 * <p>
 * Example Usage: String url = "http:www.centerkey.com/";
 * BareBonesBrowserLaunch.openURL(url);
 * <p>
 * Public Domain Software -- Free to Use as You Like
 * 
 * @version $Revision: 1.5 $
 */
public class BrowserLauncher {

    private static final String errMsg = "Error attempting to launch web browser";

    public static void openURL(String url) throws IOException {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec(
                    "rundll32 url.dll,FileProtocolHandler " + url);
            } else { // assume Unix or Linux
                String[] browsers = {"firefox", "opera", "konqueror",
                    "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++)
                    if (Runtime.getRuntime().exec(
                        new String[]{"which", browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                }
                Runtime.getRuntime().exec(new String[]{browser, url});
            }
        } catch (Exception e) {
            throw (IOException) new IOException(errMsg).initCause(e);
        }
    }
}