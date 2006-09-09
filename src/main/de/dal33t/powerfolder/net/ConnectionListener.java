/* $Id: ConnectionListener.java,v 1.41 2006/04/23 16:36:40 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.KnownNodes;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * Listens on a local port for incoming connections
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.41 $
 */
public class ConnectionListener extends PFComponent implements Runnable {
    // 
    // constants
    //
    public static final int DEFAULT_PORT = 1337;

    // return constants from dyndns validation
    public static final int OK = 0; // validation succeeded
    public static final int CANNOT_RESOLVE = 1; // dyndns could not be resolved
    public static final int VALIDATION_FAILED = 2; // dyndns does not match the
    // local host

    private Thread myThread;
    private ServerSocket serverSocket;
    private InetSocketAddress myDyndns;
    private int port;
    private boolean hasIncomingConnection;

    public ConnectionListener(Controller controller, int port)
        throws ConnectionException
    {
        super(controller);
        if (port < 0) {
            port = DEFAULT_PORT;
        }
        this.port = port;
        this.hasIncomingConnection = false;

        // check our own dyndns address
        String dns = ConfigurationEntry.DYNDNS_HOSTNAME
            .getValue(getController());
        String clidns = controller.getCommandLine() != null ? controller
            .getCommandLine().getOptionValue("d") : null;

        if (!StringUtils.isEmpty(clidns)) {
            // Overwrite dyndns entry by commandline server address
            dns = clidns;
        }

        // set the dyndns without any validations
        // assuming it has been validated on the pevious time
        // round when it was set.
        setMyDynDns(dns, false);

        // Open server socket
        openServerSocket();
    }

    /**
     * Opens the serversocket
     * 
     * @throws ConnectionException
     *             if port is blocked
     */
    private void openServerSocket() throws ConnectionException {
        try {
            log().verbose("Opening listener on port " + port);
            String bind = ConfigurationEntry.NET_BIND_ADDRESS
                .getValue(getController());
            InetAddress bAddress = null;
            if (bind != null && !StringUtils.isBlank(bind)) {
                try {
                    bAddress = InetAddress.getByName(bind);
                } catch (UnknownHostException e) {
                    log().info("Bad BIND address: " + bind);
                }
            }
            serverSocket = new ServerSocket(port,
                Constants.MAX_INCOMING_CONNECTIONS * 2, bAddress);
        } catch (IOException e) {
            throw new ConnectionException(Translation.getTranslation(
                "dialog.unable_to_open_port", port + ""), e);
        }

        log()
            .info(
                "Listening for incoming connections on port "
                    + serverSocket.getLocalPort()
                    + (myDyndns != null
                        ? ", own dyndns address: " + myDyndns
                        : ""));
    }

    /**
     * Answers if the server socket is opened
     * 
     * @return
     */
    private boolean isServerSocketOpen() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * parse entered dyndns and gets rid of any 'http://' found at the beginning
     * of it
     * 
     * @param newDns
     * @return string
     */
    private String parseString(String newDns) {

        if (newDns.startsWith("http://")) {
            int index = newDns.indexOf("//");
            newDns = newDns.substring(index + 2);
        }
        return newDns;
    }

    /**
     * get local networ interfaces.
     * 
     * @return an array of local inet addresses
     */
    private ArrayList getNetworkInterfaces() {
        Enumeration en;
        ArrayList getLocalNI = new ArrayList();
        ArrayList getLocalNIAddrs = new ArrayList();

        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            log().error("Unable to get local network interfaces");
            log().verbose(e);
            return null;
        }

        while (en.hasMoreElements()) {
            getLocalNI.add(en.nextElement());
        }

        for (int i = 0; i < getLocalNI.size(); i++) {
            NetworkInterface ni = (NetworkInterface) getLocalNI.get(i);

            en = ni.getInetAddresses();
            while (en.hasMoreElements()) {
                getLocalNIAddrs.add(en.nextElement());
            }
        }

        return getLocalNIAddrs;
    }

    /**
     * Tries to set a new dyndns address.
     * 
     * @newDns new dyndns to set
     * @validate flag indicating whether to perform dyndns validtion or just to
     *           set it
     * @return OK if succeded, CANNOT_RESOLVE if dyndns could not be resolved
     *         and VALIDATION_FAILED if dyndns does not match the local host
     */
    public int setMyDynDns(String newDns, boolean validate) {
        log().debug(
            "Setting own dns to " + newDns + ". was: "
                + (myDyndns != null ? myDyndns.getHostName() : ""));

        // FIXME Don't reset!!! If nothing has changed! CLEAN UP THIS MESS!
        if (validate) {
            // show wait message box to the user
            getController().getDynDnsManager().show(newDns);
        } else {
            if (myDyndns != null && myDyndns.getHostName().equals(newDns)) {
                // Not restetting supernode state
                log().warn("Not resetting supernode state");
                return OK;
            }
            log().info("Resetting supernode state");
        }

        // Reset my setting
        myDyndns = null;
        getController().getMySelf().getInfo().isSupernode = false;

        if (!StringUtils.isBlank(newDns)) {
            log().verbose("Resolving " + newDns);

            // parses the string in case it contains http://
            newDns = parseString(newDns).trim();

            myDyndns = new InetSocketAddress(newDns, port);

            if (myDyndns.isUnresolved()) {

                if (validate) {
                    getController().getDynDnsManager().close();
                    getController().getDynDnsManager().showWarningMsg(
                        CANNOT_RESOLVE, myDyndns.getHostName());
                }

                log().warn(
                    "Unable to resolve own dyndns address '" + newDns + "'");
                myDyndns = null;
                return CANNOT_RESOLVE;
            }

            if (validate) {
                log().verbose("Validating " + newDns);

                InetAddress myDyndnsIP = myDyndns.getAddress(); // the entered
                // dyndns
                // address
                ArrayList localIPs = getNetworkInterfaces(); // list of all
                // local host
                // IPs
                String strDyndnsIP = myDyndnsIP.getHostAddress(); // dyndns IP
                // address
                String externalIP = getController().getDynDnsManager()
                    .getDyndnsViaHTTP(); // internet IP of the local host

                boolean checkOK = false;

                // check if dyndns really matches the own host
                for (int i = 0; i < localIPs.size(); i++) {
                    InetAddress niAddrs = (InetAddress) localIPs.get(i);

                    if (Util.compareIpAddresses(myDyndnsIP.getAddress(),
                        niAddrs.getAddress()))
                    {
                        checkOK = true;
                        break;
                    }
                }

                if (!checkOK) {
                    if (externalIP.equals(strDyndnsIP)) {
                        log().verbose(
                            "DynDns matches with external IP " + newDns);
                        checkOK = true;
                    }
                }

                if (!checkOK) {
                    getController().getDynDnsManager().close();

                    log()
                        .warn(
                            "Own dyndns address "
                                + newDns
                                + " does not match any of the local network intergaces");
                    return VALIDATION_FAILED;
                }

                // close message box
                getController().getDynDnsManager().close();

                if (externalIP == "") {
                    log().warn("cannot determine the external IP of this host");
                    return VALIDATION_FAILED;
                }

                // check if dyndns really matches the external IP of this host
                if (!externalIP.equals(strDyndnsIP)) {
//                    getController().getDynDnsManager().showWarningMsg(
//                        VALIDATION_FAILED, myDyndns.getHostName());
                    log().warn(
                        "Own dyndns address " + newDns
                            + " does not match the external IP of this host");
                    return VALIDATION_FAILED;
                }
            }
        }

        if (myDyndns != null) {
            log().verbose(
                "Successfully set dyndns to " + myDyndns.getHostName());
        } else {
            log().debug("Dyndns not set");
        }
        return OK;
    }

    /**
     * Parse the HTML string and filter everything out but the ip address
     * 
     * @param str
     * @return
     */
    /*
     * private String filterIPs(String txt){ String ip = null; Pattern p =
     * Pattern.compile("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}"); Matcher m =
     * p.matcher(txt); if ( m.find() ) { // ip match is found ip =
     * txt.substring(m.start(), m.end()); } return ip; }
     */

    /**
     * Starts the connection listener
     * 
     * @throws ConnectionException
     *             if port is blocked
     */
    public void start() throws ConnectionException {
        if (!isServerSocketOpen()) {
            // Open the server socket if required
            openServerSocket();
        }

        myThread = new Thread(this, "Listener on port "
            + serverSocket.getLocalPort());
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
        log().debug("Started");
    }

    /**
     * Shuts the listener down
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log().verbose(e.toString());
            }
        }
        log().debug("Stopped");
    }

    /**
     * Returns my dyndns entry if available otherwise <code>null</code>
     * 
     * @return own dyndns address
     */
    public InetSocketAddress getMyDynDns() {
        return myDyndns;
    }

    /**
     * Address where incoming connects are possible. returns the own dyndns
     * address if available
     * 
     * @return
     */
    public InetSocketAddress getLocalAddress() {
        return (myDyndns != null) ? myDyndns : (serverSocket == null)
            ? null
            : (InetSocketAddress) serverSocket.getLocalSocketAddress();
    }

    /**
     * Answers if we have incoming connections
     * 
     * @return
     */
    public boolean hasIncomingConnections() {
        return hasIncomingConnection;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // accept a clients socket and add it to the connection pool
                if (logVerbose) {
                    log().verbose(
                        "Listening for new connections on " + serverSocket);
                }
                Socket nodeSocket = serverSocket.accept();
                NetworkUtil.setupSocket(nodeSocket);

                if (getController().isLanOnly()
                    && !NetworkUtil.isOnLanOrLoopback(nodeSocket
                        .getInetAddress()))
                {
                    nodeSocket.close();
                    continue;
                }

                hasIncomingConnection = true;
                if (logVerbose) {
                    log().verbose(
                        "Incoming connection from: "
                            + nodeSocket.getInetAddress() + ":"
                            + nodeSocket.getPort());
                }

                if (myDyndns != null
                    && !getController().getMySelf().getInfo().isSupernode)
                {
                    // ok, act as supernode
                    log().info("Acting as supernode on address " + myDyndns);
                    getController().getMySelf().getInfo().isSupernode = true;
                    getController().getMySelf().getInfo().setConnectAddress(
                        getLocalAddress());
                    // Broadcast our new status, we want stats ;)
                    getController().getNodeManager().broadcastMessage(
                        new KnownNodes(getController().getMySelf().getInfo()));
                }

                // new member, accept it
                getController().getNodeManager()
                    .acceptNodeAsynchron(nodeSocket);

                Thread.sleep(getController().getWaitTime() / 4);
                // Thread.sleep(50);
            } catch (SocketException e) {
                log().debug(
                    "Listening socket on port " + serverSocket.getLocalPort()
                        + " closed");
                break;
            } catch (IOException e) {
                log().error(e);
            } catch (InterruptedException e) {
                log().verbose("Shutting down", e);
                break;
            }
        }
    }
}