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
* $Id$
*/
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Wizard for selecting folders to join that are Online Storage and not locally managed.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.12 $
 */
public class SelectOnlineStoragePanel extends PFWizardPanel {

    private Map<FolderInfo, Boolean> folderMap;

    private JCheckBox createDesktopShortcutBox;
    private JCheckBox useRecycleBinBox;

    public SelectOnlineStoragePanel(Controller controller,
                                 List<FolderInfo> possibleFolders) {
        super(controller);
        folderMap = new HashMap<FolderInfo, Boolean>();
        for (FolderInfo possibleFolder : possibleFolders) {
            folderMap.put(possibleFolder, false);
        }
    }

    public boolean hasNext() {
        for (FolderInfo folderInfo : folderMap.keySet()) {
            Boolean selected = folderMap.get(folderInfo);
            if (selected) {
                return true;
            }
        }
        return false;
    }

    public WizardPanel next() {

        getWizardContext().setAttribute(
                WizardContextAttributes.CREATE_DESKTOP_SHORTCUT,
                createDesktopShortcutBox.isSelected());

        getWizardContext().setAttribute(
                WizardContextAttributes.USE_RECYCLE_BIN,
                useRecycleBinBox.isSelected());

        List<FolderInfo> folderInfos = new ArrayList<FolderInfo>();
        for (FolderInfo folderInfo : folderMap.keySet()) {
            Boolean selected = folderMap.get(folderInfo);
            if (selected) {
                folderInfos.add(folderInfo);
            }
        }
        getWizardContext().setAttribute(WizardContextAttributes.FOLDER_INFOS,
                folderInfos);

        // Show success panel
        return new MultiOnlineStorageSetupPanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
                "max(pref;140dlu), pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.add(new JLabel(Translation.getTranslation(
                "wizard.select_online_storage.info")), cc.xyw(1, row, 2));
        row += 2;

        JPanel selectionPanel = createSelectionPanel();
        JScrollPane scrollPane = new JScrollPane(selectionPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        builder.add(scrollPane, cc.xy(1, row, CellConstraints.DEFAULT, CellConstraints.TOP));

        row += 2;

        builder.add(useRecycleBinBox, cc.xyw(1, row, 2));

        row += 2;

        if (OSUtil.isWindowsSystem()) {
            builder.add(createDesktopShortcutBox, cc.xyw(1, row, 2));
            row += 2;
        }

        return builder.getPanel();
    }

    /**
     * Add all online folders to a panel as check boxes.
     * @return
     */
    private JPanel createSelectionPanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        MyActionListener myActionListener = new MyActionListener();
        int row = 1;
        for (Iterator<FolderInfo> iter = folderMap.keySet().iterator();
             iter.hasNext();) {
            FolderInfo possibleFolder = iter.next();
            JCheckBox checkBox = new JCheckBox(possibleFolder.name);
            checkBox.addActionListener(myActionListener);
            builder.add(checkBox, cc.xy(1, row));
            if (iter.hasNext()) {
                row += 1;
                builder.appendRow("pref");
            }
        }
        JPanel panel = builder.getPanel();
        panel.setOpaque(true);
        panel.setBackground(SystemColor.text);
        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        useRecycleBinBox = new JCheckBox(Translation.getTranslation(
                "wizard.select_online_storage.recycle_bin.text"));
        useRecycleBinBox.setSelected(ConfigurationEntry.USE_RECYCLE_BIN
                .getValueBoolean(getController()));
        createDesktopShortcutBox = new JCheckBox(Translation.getTranslation(
                "wizard.select_online_storage.desktop_shortcut.text"));
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.select_online_storage.title");
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) source;
                FolderInfo selectedFolderInfo = null;
                for (FolderInfo folderInfo : folderMap.keySet()) {
                    if (folderInfo.name.equals(cb.getText())) {
                        // found the correct cb.
                        selectedFolderInfo = folderInfo;
                        break;
                    }
                }

                // Update with cb selection.
                if (selectedFolderInfo != null) {
                    folderMap.put(selectedFolderInfo, cb.isSelected());
                }
            }
            updateButtons();
        }
    }
}