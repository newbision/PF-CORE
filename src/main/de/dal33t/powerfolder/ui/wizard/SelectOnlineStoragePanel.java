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

import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Wizard for selecting folders to join that are Online Storage and not locally
 * managed.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.12 $
 */
public class SelectOnlineStoragePanel extends PFWizardPanel {

    private final Map<FolderInfo, Boolean> folderMap;

    private JCheckBox createDesktopShortcutBox;
    private JComboBox archiveMode;

    public SelectOnlineStoragePanel(Controller controller,
        List<FolderInfo> possibleFolders)
    {
        super(controller);
        folderMap = new HashMap<FolderInfo, Boolean>();
        for (FolderInfo possibleFolder : possibleFolders) {
            folderMap.put(possibleFolder, false);
        }
    }

    @Override
    public boolean hasNext() {
        for (FolderInfo folderInfo : folderMap.keySet()) {
            Boolean selected = folderMap.get(folderInfo);
            if (selected) {
                return true;
            }
        }
        return false;
    }

    @Override
    public WizardPanel next() {

        getWizardContext().setAttribute(
            WizardContextAttributes.CREATE_DESKTOP_SHORTCUT,
            createDesktopShortcutBox.isSelected());

        getWizardContext().setAttribute(WizardContextAttributes.ARCHIVE_MODE,
            archiveMode.getSelectedItem());

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

    @Override
    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("180dlu, pref:grow",
            "pref, 3dlu, fill:100dlu, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        int row = 1;

        builder.add(new JLabel(Translation
            .getTranslation("wizard.select_online_storage.info")), cc.xyw(1,
            row, 2));
        row += 2;

        JPanel selectionPanel = createSelectionPanel();
        JScrollPane scrollPane = new JScrollPane(selectionPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        builder.add(scrollPane, cc.xy(1, row, CellConstraints.FILL,
            CellConstraints.FILL));
        row += 2;

        // JLabel label = new JLabel(Translation
        // .getTranslation("settings_tab.archive_mode"));
        // label.setOpaque(false);
        // builder.add(label, cc.xy(1, row));
        // builder.add(archiveMode, cc.xy(1, row, CellConstraints.RIGHT,
        // CellConstraints.DEFAULT));
        // row += 2;

        if (OSUtil.isWindowsSystem()) {
            builder.add(createDesktopShortcutBox, cc.xy(1, row));
            row += 2;
        }

        return builder.getPanel();
    }

    /**
     * Add all online folders to a panel as check boxes.
     * 
     * @return
     */
    private JPanel createSelectionPanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        MyActionListener myActionListener = new MyActionListener();
        int row = 1;
        for (Iterator<FolderInfo> iter = folderMap.keySet().iterator(); iter
            .hasNext();)
        {
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
    @Override
    protected void initComponents() {
        createDesktopShortcutBox = new JCheckBox(
            Translation
                .getTranslation("wizard.select_online_storage.desktop_shortcut.text"));
        createDesktopShortcutBox.setOpaque(false);
        archiveMode = new JComboBox(EnumSet.allOf(ArchiveMode.class).toArray());
        archiveMode.setSelectedItem(ArchiveMode.NO_BACKUP);
    }

    @Override
    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    @Override
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