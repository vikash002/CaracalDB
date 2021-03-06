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
package se.sics.caracaldb.paxos;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.caracaldb.Address;
import se.sics.caracaldb.CoreSerializer;
import se.sics.caracaldb.KeyRange;
import se.sics.caracaldb.View;
import se.sics.caracaldb.paxos.Paxos.Accept;
import se.sics.caracaldb.paxos.Paxos.Accepted;
import se.sics.caracaldb.paxos.Paxos.Forward;
import se.sics.caracaldb.paxos.Paxos.Install;
import se.sics.caracaldb.paxos.Paxos.Instance;
import se.sics.caracaldb.paxos.Paxos.NoPromise;
import se.sics.caracaldb.paxos.Paxos.Prepare;
import se.sics.caracaldb.paxos.Paxos.Promise;
import se.sics.caracaldb.paxos.Paxos.Rejected;
import se.sics.caracaldb.replication.log.Noop;
import se.sics.caracaldb.replication.log.Reconfigure;
import se.sics.caracaldb.replication.log.Value;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author lkroll
 */
@RunWith(JUnit4.class)
public class SerializationTest {

    static {
        Serializers.register(CoreSerializer.PAXOS.instance, "paxosS");
        Serializers.register(Paxos.PaxosMsg.class, "paxosS");
        Serializers.register(Paxos.Forward.class, "paxosS");
        Serializers.register(CoreSerializer.VALUE.instance, "valueS");
        Serializers.register(Value.class, "valueS");
    }

    @Test
    public void messageTest() throws UnknownHostException {
        InetAddress ip = InetAddress.getLocalHost();
        Address source = new Address(ip, 1234, "abcd".getBytes());
        Address dest = new Address(ip, 5678, "efgh".getBytes());

        ByteBuf buf = Unpooled.buffer();

        PaxosSerializer paxosS = CoreSerializer.PAXOS.instance;

        // PREPARE
        Prepare prepare = new Prepare(source, dest, 1);
        paxosS.toBinary(prepare, buf);
        Prepare prepare2 = (Prepare) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(prepare.getSource(), prepare2.getSource());
        Assert.assertEquals(prepare.getDestination(), prepare2.getDestination());
        Assert.assertEquals(prepare.ballot, prepare2.ballot);
        buf.clear();

        // PROMISE
        Instance i = Instance.noop(10, 1);
        View v = new View(ImmutableSortedSet.of(source, dest), 1);
        Promise promise = new Promise(source, dest, 1, ImmutableSet.of(i), v);
        paxosS.toBinary(promise, buf);
        Promise promise2 = (Promise) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(promise.getSource(), promise2.getSource());
        Assert.assertEquals(promise.getDestination(), promise2.getDestination());
        Assert.assertEquals(promise.ballot, promise2.ballot);
        Assert.assertEquals(promise.maxInstances.size(), promise2.maxInstances.size());
        Assert.assertEquals(promise.view, promise2.view);
        buf.clear();

        // NO_PROMISE
        NoPromise noPromise = new NoPromise(source, dest, 1);
        paxosS.toBinary(noPromise, buf);
        NoPromise noPromise2 = (NoPromise) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(noPromise.getSource(), noPromise2.getSource());
        Assert.assertEquals(noPromise.getDestination(), noPromise2.getDestination());
        Assert.assertEquals(noPromise.ballot, noPromise2.ballot);
        buf.clear();

        // ACCEPT
        Accept accept = new Accept(source, dest, 1, i);
        paxosS.toBinary(accept, buf);
        Accept accept2 = (Accept) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(accept.getSource(), accept2.getSource());
        Assert.assertEquals(accept.getDestination(), accept2.getDestination());
        Assert.assertEquals(accept.ballot, accept2.ballot);
        Assert.assertEquals(accept.i, accept2.i);
        buf.clear();

        // ACCEPTED
        Accepted accepted = new Accepted(source, dest, 1, i, v);
        paxosS.toBinary(accepted, buf);
        Accepted accepted2 = (Accepted) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(accepted.getSource(), accepted2.getSource());
        Assert.assertEquals(accepted.getDestination(), accepted2.getDestination());
        Assert.assertEquals(accepted.ballot, accepted2.ballot);
        Assert.assertEquals(accepted.i, accepted2.i);
        Assert.assertEquals(accepted.view, accepted2.view);
        buf.clear();

        // REJECTED
        Rejected rejected = new Rejected(source, dest, 1, i);
        paxosS.toBinary(rejected, buf);
        Rejected rejected2 = (Rejected) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(rejected.getSource(), rejected2.getSource());
        Assert.assertEquals(rejected.getDestination(), rejected2.getDestination());
        Assert.assertEquals(rejected.ballot, rejected2.ballot);
        Assert.assertEquals(rejected.i, rejected2.i);
        buf.clear();

        // INSTALL
        Reconfigure reconf = new Reconfigure(new UUID(0, 1), v, 3, 0, KeyRange.EMPTY);
        ImmutableSortedMap<Long, Value> bla = ImmutableSortedMap.of(1l, (Value) Noop.val, 2l, (Value) Noop.val); // how hard can it be to detec covariance correctly -.-
        Install install = new Install(source, dest, 1, reconf, 10, bla);
        paxosS.toBinary(install, buf);
        Install install2 = (Install) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(install.getSource(), install2.getSource());
        Assert.assertEquals(install.getDestination(), install2.getDestination());
        Assert.assertEquals(install.ballot, install2.ballot);
        Assert.assertEquals(install.highestDecided, install2.highestDecided);
        Assert.assertEquals(install.event, install2.event);
        Assert.assertEquals(install.log.size(), install2.log.size());
        buf.clear();

        // FORWARD
        Forward forward = new Forward(source, dest, source, Noop.val);
        paxosS.toBinary(forward, buf);
        Forward forward2 = (Forward) paxosS.fromBinary(buf, Optional.absent());
        Assert.assertEquals(forward.getSource(), forward2.getSource());
        Assert.assertEquals(forward.getDestination(), forward2.getDestination());
        Assert.assertEquals(forward.getOrigin(), forward2.getOrigin());
        Assert.assertEquals(forward.p, forward2.p);
        buf.clear();

        buf.release();
    }
}
