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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.Profiling;
import de.dal33t.powerfolder.ProfilingEntry;
import junit.framework.TestCase;

public class ProfilingTest extends TestCase {

    protected void tearDown() throws Exception {
        super.tearDown();
        Profiling.setEnabled(false);
    }

    public void testTiming() {
        Profiling.setEnabled(true);
        ProfilingEntry profilingEntry1 = Profiling.startProfiling("Test profile 1");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(profilingEntry1.elapsedMilliseconds() == 10000);
        assertTrue(profilingEntry1.getOperationName().equals("Test profile 1"));

        ProfilingEntry profilingEntry2 = Profiling.startProfiling("Test profile 2");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(profilingEntry2.elapsedMilliseconds() == 2000);
        assertTrue(profilingEntry2.getOperationName().equals("Test profile 2"));
    }

    public void testDisabled() {
        Profiling.setEnabled(false);
        ProfilingEntry profilingEntry1 = Profiling.startProfiling("Test profile 1");
        assertNull(profilingEntry1);
    }
}