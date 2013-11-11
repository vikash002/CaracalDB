/* 
 * This file is part of the CaracalDB distributed storage system.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.caracaldb.system;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.kompics.address.Address;
import static org.junit.Assert.*;
import se.sics.caracaldb.system.Stats.Report;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
@RunWith(JUnit4.class)
public class StatsTest {
    
    private static final int NUM = 100;
    private static final int TIME = 100;
    
    private Address self = null;
    
    @Before
    public void setUp() {
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            fail(ex.getMessage());
        }
        int port = 22333;

        self = new Address(localHost, port, null);
    }

    @Test
    public void basic() {
        
        for (int i = 0; i < NUM; i++) {
            Report r = Stats.collect(self);
            assertNotNull(r);
            System.out.println(r);
            try {
                Thread.sleep(TIME);
            } catch (InterruptedException ex) {
                Logger.getLogger(StatsTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
