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
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Request to start download a file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class RequestDownload extends Message {
    private static final long serialVersionUID = 100L;

    public FileInfo file;
    public long startOffset;

    public RequestDownload() {
        // Serialisation constructor
    }

    /**
     * Start a complete new download
     */
    public RequestDownload(FileInfo file) {
        this(file, 0);
    }

    /**
     * Requests file download, starting at offset
     * 
     * @param file
     * @param startOffset
     */
    public RequestDownload(FileInfo file, long startOffset) {
        super();
        this.file = file;
        this.startOffset = startOffset;
    }

    public String toString() {
        return "Request to download: " + file.toDetailString()
            + ", starting at " + startOffset;
    }
}