/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.contextmenu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import jwf.WizardPanel;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.task.CreateFolderOnServerTask;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * This action is called, when the "share folder" context menu item was clicked.<br />
 * <br />
 * <ul>
 * <li>If the UI is disabled, it just creates a new {@link Folder} and starts to
 * synchronize it.</li>
 * <li>If the UI is enabled, but Beginner Mode is configured:
 * <ol>
 * <li>When the Folder does not exist yet, it is created and the
 * {@link ShareFolderNotificationHandler} is called.</li>
 * <li>When the Folder already exists, the Send Invitation Wizard is opened.</li>
 * </ol>
 * </li>
 * <li>If the UI is enabled, and Advanced or Expert Mode is configured:
 * <ol>
 * <li>When the directory was not yet a Folder, the
 * {@link ChooseDiskLocationPanel} is opened</li>
 * <li>When the directory was already a Folder, the
 * {@link PFWizard#openSendInvitationWizard(Controller, FolderInfo)} is opened</li>
 * </ol>
 * </li>
 * </ul>
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class ShareFolderAction extends ContextMenuAction {

    private static final Logger log = Logger.getLogger(ShareFolderAction.class
        .getName());

    private Controller controller;
    private FolderRepository repository;

    ShareFolderAction(Controller controller) {
        this.controller = controller;
        this.repository = controller.getFolderRepository();
    }

    @Override
    public void onSelection(String[] paths) {
        try {
            for (String pathName : paths) {
                Path path = Paths.get(pathName);

                if (!Files.isDirectory(path)) {
                    continue;
                }

                Folder folder = repository.findExistingFolder(path);
                final FolderInfo foInfo = getFolderInfo(path, folder);

                SyncProfile syncProfile = SyncProfile.getDefault(controller);
                boolean backupByServer = controller.getOSClient()
                    .isBackupByDefault();

                if (controller.isUIEnabled()) {
                    if (!PreferencesEntry.EXPERT_MODE
                        .getValueBoolean(controller))
                    {
                        if (folder != null) {
                            controller.getIOProvider().startIO(new Runnable() {
                                @Override
                                public void run() {
                                    PFWizard.openSendInvitationWizard(
                                        controller, foInfo);
                                    controller.getUIController().getMainFrame()
                                        .toFront();
                                }
                            });
                        } else {
                            createFolder(path, foInfo, syncProfile,
                                backupByServer);
                            showNotification(foInfo);
                        }
                    } else {
                        if (folder != null) {
                            controller.getIOProvider().startIO(new Runnable() {
                                @Override
                                public void run() {
                                    PFWizard.openSendInvitationWizard(
                                        controller, foInfo);
                                    controller.getUIController().getMainFrame()
                                        .toFront();
                                }
                            });
                        } else {
                            showFolderSetupWizard(path, foInfo, syncProfile,
                                backupByServer);
                        }
                    }
                } else {
                    createFolder(path, foInfo, syncProfile, backupByServer);
                }
            }
        } catch (RuntimeException re) {
            log.log(Level.WARNING,
                "Problem while trying to share folder " + re, re);
        }
    }

    private FolderInfo getFolderInfo(Path path, Folder folder) {
        FolderInfo foInfo;

        if (folder != null) {
            foInfo = folder.getInfo();
        } else {
            String id = IdGenerator.makeFolderId();
            String name = PathUtils.getSuggestedFolderName(path);

            foInfo = new FolderInfo(name, id);
        }

        return foInfo;
    }

    private void createFolder(Path path, FolderInfo foInfo,
        SyncProfile syncProfile, boolean backupByServer)
    {
        FolderSettings settings = new FolderSettings(
            path,
            syncProfile,
            null,
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS.getValueInt(controller),
            true);
        Folder folder = repository.createFolder(foInfo, settings);
        folder.addDefaultExcludes();
        if (backupByServer) {
            new CreateFolderOnServerTask(foInfo, null).scheduleTask(controller);
        }
    }

    private void showFolderSetupWizard(final Path path,
        final FolderInfo foInfo, final SyncProfile syncProfile,
        final boolean backupByServer)
    {
        controller.getIOProvider().startIO(new Runnable() {
            @Override
            public void run() {
                PFWizard wizard = new PFWizard(controller, Translation
                    .get("wizard.pfwizard.folder_title"));
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.INITIAL_FOLDER_NAME,
                    foInfo.getName());
                wizard.getWizardContext()
                    .setAttribute(
                        WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE,
                        syncProfile);
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.BACKUP_ONLINE_STOARGE,
                    backupByServer);
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.FOLDERINFO_ATTRIBUTE, foInfo);

                WizardPanel nextPanel = new FolderCreatePanel(controller);
                // Setup success panel of this wizard path
                TextPanelPanel successPanel = new TextPanelPanel(controller,
                    Translation.get("wizard.setup_success"),
                    Translation.get("wizard.success_join"));
                wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                    successPanel);
                ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
                    controller, path.toAbsolutePath().toString(), nextPanel);
                wizard.open(panel);
                controller.getUIController().getMainFrame().toFront();
            }
        });
    }

    private void showNotification(FolderInfo foInfo) {
        ShareFolderNotificationHandler notification = new ShareFolderNotificationHandler(
            controller, foInfo);
        notification.show();
    }
}
