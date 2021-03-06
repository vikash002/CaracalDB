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
package se.sics.caracaldb.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import se.sics.caracaldb.Address;
import se.sics.caracaldb.global.ReadOnlyLUT;
import se.sics.caracaldb.operations.CaracalResponse;
import se.sics.kompics.Init;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class ClientWorkerInit extends Init<ClientWorker> {
    public final BlockingQueue<CaracalResponse> q;
    public final Address self;
    public final Address bootstrapServer;
    public final int sampleSize;
    public final ReadOnlyLUT lut;
    public final ReadWriteLock lutLock;
    
    public ClientWorkerInit(BlockingQueue<CaracalResponse> q, Address self, Address bootstrapServer, int sampleSize, ReadOnlyLUT lut, ReadWriteLock lutLock) {
        this.q = q;
        this.self = self;
        this.bootstrapServer = bootstrapServer;
        this.sampleSize = sampleSize;
        this.lut = lut;
        this.lutLock = lutLock;
    }
}
