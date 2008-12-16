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
package de.dal33t.powerfolder.event;

/**
 * Implement this class to receive events from the NodeManagerModel.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom </A>
 * @version $Revision: 1.2 $
 */
public interface NodeManagerModelListener {

    /**
     * Node removed from known nodes.
     *
     * @param e
     */
    void nodeRemoved(NodeManagerModelEvent e);

    /**
     * Node added to known nodes.
     *
     * @param e
     */
    void nodeAdded(NodeManagerModelEvent e);

    /**
     * Major structure change of the known nodes.
     * e.getNode() == null
     *
     * @param e
     */
    void rebuilt(NodeManagerModelEvent e);
}