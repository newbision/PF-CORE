/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: ComputersList.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.computers;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

public class ComputersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel computerListPanel;
    private final NodeManagerModel nodeManagerModel;
    private final Set<ExpandableComputerView> viewList;
    private ExpansionListener expansionListener;
    private ComputersTab computersTab;
    private volatile boolean populated;
    private volatile boolean multiGroup;

    // Only access these when synchronized on viewList.
    private final Set<Member> previousMyComputers;
    private final Set<Member> previousFriends;
    private final Set<Member> previousConnectedLans;

    private boolean collapseMyComputers;
    private boolean collapseFriends;
    private boolean collapseConnectedLans;

    private JLabel myComputersLabel;
    private JLabel myComputersIcon;
    private JLabel friendsLabel;
    private JLabel friendsIcon;
    private JLabel connectedLansLabel;
    private JLabel connectedLansIcon;

    /**
     * Constructor
     * 
     * @param controller
     */
    public ComputersList(Controller controller, ComputersTab computersTab) {
        super(controller);
        this.computersTab = computersTab;
        expansionListener = new MyExpansionListener();
        nodeManagerModel = getUIController().getApplicationModel()
            .getNodeManagerModel();
        viewList = new CopyOnWriteArraySet<ExpandableComputerView>();

        previousConnectedLans = new TreeSet<Member>();
        previousFriends = new TreeSet<Member>();
        previousMyComputers = new TreeSet<Member>();

        myComputersLabel = new JLabel(Translation
            .getTranslation("computers_list.my_computers"));
        myComputersIcon = new JLabel(Icons.getIconById(Icons.EXPAND));
        myComputersLabel.addMouseListener(new MyComputersListener());
        myComputersIcon.addMouseListener(new MyComputersListener());
        friendsLabel = new JLabel(Translation
            .getTranslation("computers_list.friends"));
        friendsIcon = new JLabel(Icons.getIconById(Icons.COLLAPSE));
        friendsLabel.addMouseListener(new FriendsListener());
        friendsIcon.addMouseListener(new FriendsListener());
        connectedLansLabel = new JLabel(Translation
            .getTranslation("computers_list.lan"));
        connectedLansIcon = new JLabel(Icons.getIconById(Icons.EXPAND));
        connectedLansLabel.addMouseListener(new ConnectedLansListener());
        connectedLansIcon.addMouseListener(new ConnectedLansListener());
    }

    /**
     * Get the UI component
     * 
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Build the UI
     */
    private void buildUI() {

        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow", "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(computerListPanel, cc.xy(1, 1));
        uiComponent = GradientPanel.create(builder.getPanel());
    }

    /**
     * Initialize the componets
     */
    private void initComponents() {
        computerListPanel = new JPanel();
        computerListPanel.setLayout(new BoxLayout(computerListPanel,
            BoxLayout.PAGE_AXIS));
        getUIController().getApplicationModel().getNodeManagerModel()
            .addNodeManagerModelListener(new MyNodeManagerModelListener());
        rebuild(false);
    }

    /**
     * Rebuild the whole list, if there is a significant change. This detects
     * things like Ln nodes becoming friends, etc.
     * 
     * @param expCol
     *            true if expand or collapse change - MUST redisplay, even if
     *            previous are all the same.
     */
    private void rebuild(boolean expCol) {

        // Do nothing until populate command is called.
        if (!populated) {
            return;
        }

        // Create a working copy of the node manager's nodes.
        Set<Member> nodes = new TreeSet<Member>();
        nodes.addAll(nodeManagerModel.getNodes());

        // Split nodes into three groups:
        // 1) My Computers,
        // 2) Friends and
        // 3) Connected LAN
        // Use maps to sort by name.
        Map<String, Member> myComputers = new TreeMap<String, Member>();
        Map<String, Member> friends = new TreeMap<String, Member>();
        Map<String, Member> connectedLans = new TreeMap<String, Member>();

        for (Iterator<Member> iterator = nodes.iterator(); iterator.hasNext();)
        {
            Member member = iterator.next();
            // My computers should get automatically friends by
            // ServerClient.updateFriendsList(..)
            if (member.isFriend() && member.isMyComputer()) {
                myComputers.put(member.getNick().toLowerCase(),member);
                iterator.remove();
            }
        }

        for (Iterator<Member> iterator = nodes.iterator(); iterator.hasNext();)
        {
            Member member = iterator.next();
            if (member.isOnLAN() && member.isCompletelyConnected()) {
                connectedLans.put(member.getNick().toLowerCase(),member);
                iterator.remove();
            }
        }

        for (Member member : nodes) {
            if (member.isFriend()) {
                friends.put(member.getNick().toLowerCase(),member);
            }
        }

        synchronized (viewList) {

            // Are the nodes same as current views?
            boolean different = expCol;
            if (previousConnectedLans.size() == connectedLans.size()
                && previousFriends.size() == connectedLans.size()
                && previousMyComputers.size() == myComputers.size())
            {
                for (Member member : myComputers.values()) {
                    if (!previousMyComputers.contains(member)) {
                        different = true;
                        break;
                    }
                }
                if (!different) {
                    for (Member member : connectedLans.values()) {
                        if (!previousConnectedLans.contains(member)) {
                            different = true;
                            break;
                        }
                    }
                    if (!different) {
                        for (Member member : friends.values()) {
                            if (!previousFriends.contains(member)) {
                                different = true;
                                break;
                            }
                        }
                    }
                }
            } else {
                different = true;
            }

            if (!different) {
                return;
            }

            // Update for next time.
            previousConnectedLans.clear();
            previousFriends.clear();
            previousMyComputers.clear();
            previousConnectedLans.addAll(connectedLans.values());
            previousMyComputers.addAll(myComputers.values());
            previousFriends.addAll(friends.values());

            // Clear view listeners
            Member expandedNode = null;
            for (ExpandableComputerView view : viewList) {
                if (view.isExpanded()) {
                    expandedNode = view.getNode();
                }
                view.removeExpansionListener(expansionListener);
                view.removeCoreListeners();
            }
            viewList.clear();
            computerListPanel.removeAll();

            // If there is only one group, do not bother with separators
            multiGroup = (myComputers.isEmpty() ? 0 : 1)
                + (connectedLans.isEmpty() ? 0 : 1)
                + (friends.isEmpty() ? 0 : 1) > 1;

            // First show my computers.
            boolean firstMyComputer = true;
            for (Member node : myComputers.values()) {
                if (firstMyComputer && multiGroup) {
                    firstMyComputer = false;
                    addSeparator(collapseMyComputers, myComputersIcon,
                        myComputersLabel);
                }
                if (!multiGroup || !collapseMyComputers) {
                    addView(node, expandedNode);
                }
            }

            // Then friends.
            boolean firstFriend = true;
            for (Member node : friends.values()) {
                if (firstFriend && multiGroup) {
                    firstFriend = false;
                    addSeparator(collapseFriends, friendsIcon, friendsLabel);
                }
                if (!multiGroup || !collapseFriends) {
                    addView(node, expandedNode);
                }
            }

            // Then others (connected on LAN).
            boolean firstLan = true;
            for (Member node : connectedLans.values()) {
                if (firstLan && multiGroup) {
                    firstLan = false;
                    addSeparator(collapseConnectedLans, connectedLansIcon,
                        connectedLansLabel);
                }
                if (!multiGroup || !collapseConnectedLans) {
                    addView(node, expandedNode);
                }
            }

            computersTab.updateEmptyLabel();
            getUIComponent().revalidate();
        }
    }

    private void addSeparator(boolean collapsed, JLabel icon, JLabel label) {
        FormLayout layout = new FormLayout(
            "3dlu, pref, 3dlu, pref, 3dlu, pref:grow, 3dlu", "pref, 4dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        icon.setIcon(collapsed ? Icons.getIconById(Icons.EXPAND) : Icons
            .getIconById(Icons.COLLAPSE));
        icon.setToolTipText(collapsed ? Translation
            .getTranslation("computers_list.expand_hint") : Translation
            .getTranslation("computers_list.collapse_hint"));
        label.setToolTipText(collapsed ? Translation
            .getTranslation("computers_list.expand_hint") : Translation
            .getTranslation("computers_list.collapse_hint"));
        builder.add(icon, cc.xy(2, 1));
        builder.add(label, cc.xy(4, 1));
        builder.add(new JSeparator(), cc.xy(6, 1));
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        computerListPanel.add(panel);
    }

    private void addView(Member node, Member expandedNode) {

        ExpandableComputerView view = new ExpandableComputerView(
            getController(), node);
        computerListPanel.add(view.getUIComponent());
        viewList.add(view);
        if (expandedNode != null && node.equals(expandedNode)) {
            // Maintain expanded state of view on rebuild.
            view.expand();
        }
        view.addExpansionListener(expansionListener);
    }

    public boolean isEmpty() {
        // If multigroup, always show, even if all collapsed.
        return viewList.isEmpty() && !multiGroup;
    }

    /**
     * Enable the view processing methods so that views get processed. This is
     * done so views do not get added before Synthetica has set all the colors,
     * else views look different before and after.
     */
    public void populate() {
        populated = true;
        rebuild(false);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * Node Manager Model listener.
     */
    private class MyNodeManagerModelListener implements
        NodeManagerModelListener
    {

        public void changed() {
            rebuild(false);
        }
    }

    /**
     * Expansion listener to collapse views.
     */
    private class MyExpansionListener implements ExpansionListener {

        public void collapseAllButSource(ExpansionEvent e) {
            synchronized (viewList) {
                for (ExpandableComputerView view : viewList) {
                    if (!view.equals(e.getSource())) {
                        // No source, so collapse.
                        view.collapse();
                    }
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyComputersListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseMyComputers = !collapseMyComputers;
            rebuild(true);
        }
    }

    private class FriendsListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseFriends = !collapseFriends;
            rebuild(true);
        }
    }

    private class ConnectedLansListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseConnectedLans = !collapseConnectedLans;
            rebuild(true);
        }
    }
}