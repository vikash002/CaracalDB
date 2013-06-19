/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.caracaldb.global;

import com.google.common.io.Closer;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInteger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import se.sics.caracaldb.Key;
import se.sics.kompics.address.Address;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class LookupTable {

    public static final int INIT_REP_FACTOR = 3;
    public static final int NUM_VIRT_GROUPS = 256;
    private static final String EMPTY_TXT = "<EMPTY>";
    private static final Random RAND = new Random();
    private static LookupTable INSTANCE = null; // Don't tell anyone about this! (static fields and simulations oO)
    private ArrayList<Address> hosts;
    private ArrayList<Integer[]> replicationGroups;
    private LookupGroup[] virtualHostGroups;
    private Long[] virtualHostGroupVersions;
    long versionId = 0;

    private LookupTable() {
        virtualHostGroups = new LookupGroup[NUM_VIRT_GROUPS];
        virtualHostGroupVersions = new Long[NUM_VIRT_GROUPS];

    }

    public int numHosts() {
        return hosts.size();
    }

    public Address getHost(int pos) {
        return hosts.get(pos);
    }

    public int numReplicationGroups() {
        return replicationGroups.size();
    }

    public Address[] getHosts(int replicationGroupId) {
        Integer[] group = replicationGroups.get(replicationGroupId);
        if (group == null) {
            return null;
        }
        Address[] hostAddrs = new Address[group.length];
        for (int i = 0; i < group.length; i++) {
            hostAddrs[i] = getHost(i);
        }
        return hostAddrs;
    }
    
    public Address[] getResponsibles(Key k) {
        Integer rgId = virtualHostsGetResponsible(k);
        if (rgId == null) {
            return null;
        }
        return getHosts(rgId);
    }

    /**
     * Find all the virtual nodes at a host.
     *
     * More exactly, find the ids of all virtual nodes that are supposed to be
     * at the given host according to the state of the LUT.
     *
     * This is a horribly slow operation. It's meant for bootup, use it later at
     * your own risk.
     *
     * @param host
     * @return
     */
    public Set<Key> getVirtualNodesAt(Address host) {
        // find host id
        int hostId = -1;
        for (ListIterator<Address> it = hosts.listIterator(); it.hasNext();) {
            int pos = it.nextIndex();
            Address adr = it.next();
            if (adr.equals(host)) {
                hostId = pos;
                break;
            }
        }
        if (hostId < 0) {
            return null; // could also throw an exeception...not sure what is nicer
        }
        // find all replication groups for hostId
        TreeSet<Integer> repGroupIds = new TreeSet<Integer>();
        for (ListIterator<Integer[]> it = replicationGroups.listIterator(); it.hasNext();) {
            int pos = it.nextIndex();
            Integer[] group = it.next();
            for (int i = 0; i < group.length; i++) {
                if (hostId == group[i]) {
                    repGroupIds.add(pos);
                    break;
                }
            }
        }
        if (repGroupIds.isEmpty()) {
            // just return an empty set.
            // if the host is not part of any replication groups
            // clearly there won't be any VNodes on it
            return new HashSet<Key>();
        }

        // now find all the occurences in the lookup groups
        // this is the most horribly inefficient part^^
        HashSet<Key> nodeSet = new HashSet<Key>();
        for (int i = 0; i < NUM_VIRT_GROUPS; i++) {
            if (!virtualHostGroups[i].isEmpty()) {
                nodeSet.addAll(virtualHostGroups[i].getVirtualNodesIn(i));
            }
        }
        return nodeSet;
    }

    /**
     * Builds a readable format of the LUT.
     *
     * This is probably not a good idea for large tables. Use for debugging of
     * small sets only.
     *
     * @param sb
     */
    public void printFormat(StringBuilder sb) {
        sb.append("### LookupTable (v");
        sb.append(versionId);
        sb.append(") ### \n \n");

        sb.append("## Hosts ## \n");
        for (ListIterator<Address> it = hosts.listIterator(); it.hasNext();) {
            int pos = it.nextIndex();
            Address adr = it.next();
            sb.append(pos);
            sb.append(". ");
            if (adr == null) {
                sb.append(EMPTY_TXT);
            } else {
                sb.append(adr);
            }
            sb.append('\n');
        }
        sb.append('\n');

        sb.append("## Replication Groups ## \n");
        for (ListIterator<Integer[]> it = replicationGroups.listIterator(); it.hasNext();) {
            int pos = it.nextIndex();
            Integer[] group = it.next();
            sb.append(pos);
            sb.append(". ");
            if (group == null) {
                sb.append(EMPTY_TXT);
            } else {
                sb.append('{');
                for (int i = 0; i < group.length; i++) {
                    sb.append(group[i]);
                    if (i < (group.length - 1)) {
                        sb.append(',');
                    }
                }
                sb.append('}');
                sb.append('\n');
            }
        }
        sb.append('\n');
        
        sb.append("## Virtual Node Groups ## \n");
        for (int i = 0; i < NUM_VIRT_GROUPS; i++) {
            sb.append("# Group ");
            sb.append(i);
            sb.append(" (v");
            sb.append(virtualHostGroupVersions[i]);
            sb.append(") # \n");
            if (!virtualHostGroups[i].isEmpty()) {
                virtualHostGroups[i].printFormat(sb);
            } else {
                sb.append(EMPTY_TXT);
            }
            sb.append('\n');
        }
        
        sb.append('\n');
        sb.append('\n');
    }

    public byte[] serialise() throws IOException {
        Closer closer = Closer.create();
        try {
            ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
            DataOutputStream w = closer.register(new DataOutputStream(baos));

            w.writeLong(versionId);

            // hosts
            w.writeInt(hosts.size());
            for (Address addr : hosts) {
                serialiseAddress(addr, w);
            }

            // replicationgroups
            w.writeInt(replicationGroups.size());
            for (Integer[] group : replicationGroups) {
                serialiseReplicationGroup(group, w);
            }

            // virtualHostGroups
            for (int i = 0; i < NUM_VIRT_GROUPS; i++) {
                w.writeLong(virtualHostGroupVersions[i]);
                byte[] lgbytes = virtualHostGroups[i].serialise();
                w.writeInt(lgbytes.length);
                w.write(lgbytes);
            }

            w.flush();

            return baos.toByteArray();
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    public static LookupTable deserialise(byte[] bytes) throws IOException {
        Closer closer = Closer.create();
        try {
            ByteArrayInputStream bais = closer.register(new ByteArrayInputStream(bytes));
            DataInputStream r = closer.register(new DataInputStream(bais));

            INSTANCE = new LookupTable();

            INSTANCE.versionId = r.readLong();

            // hosts
            int numHosts = r.readInt();
            INSTANCE.hosts = new ArrayList<Address>(numHosts);
            for (int i = 0; i < numHosts; i++) {
                INSTANCE.hosts.add(deserialiseAddress(r));
            }

            // replicationgroups
            int numRGs = r.readInt();
            INSTANCE.replicationGroups = new ArrayList<Integer[]>(numRGs);
            for (int i = 0; i < numRGs; i++) {
                INSTANCE.replicationGroups.add(deserialiseReplicationGroup(r));
            }

            // virtualHostGroups
            for (int i = 0; i < NUM_VIRT_GROUPS; i++) {
                INSTANCE.virtualHostGroupVersions[i] = r.readLong();
                int groupLength = r.readInt();
                byte[] groupBytes = new byte[groupLength];
                if (r.read(groupBytes) != groupLength) {
                    throw new IOException("Incomplete dataset!");
                }
                INSTANCE.virtualHostGroups[i] = LookupGroup.deserialise(groupBytes);
            }


            return INSTANCE;
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    private static void serialiseReplicationGroup(Integer[] group, DataOutputStream w) throws IOException {
        byte groupSize = UnsignedBytes.checkedCast(group.length);
        w.writeByte(groupSize);
        for (Integer i : group) {
            w.writeInt(i);
        }
    }

    private static Integer[] deserialiseReplicationGroup(DataInputStream r) throws IOException {
        int groupSize = UnsignedBytes.toInt(r.readByte());
        Integer[] group = new Integer[groupSize];
        for (int i = 0; i < groupSize; i++) {
            group[i] = r.readInt();
        }
        return group;
    }

    /*
     * Custom Address serialisation to save some space
     */
    private static void serialiseAddress(Address addr, DataOutputStream w) throws IOException {
        if (addr == null) {
            w.writeInt(0); //simply put four 0 bytes since 0.0.0.0 is not a valid host ip
            return;
        }
        w.write(addr.getIp().getAddress());
        // Write ports as 2 bytes instead of 4
        byte[] portBytes = Ints.toByteArray(addr.getPort());
        w.writeByte(portBytes[2]);
        w.writeByte(portBytes[3]);
        // write ids with 1 bit plus either 1 or 4 bytes
        byte[] id = addr.getId();
        if (id != null) {
            if (id.length <= Key.BYTE_KEY_SIZE) {
                w.writeBoolean(true);
                w.writeByte(id.length);
            } else {
                w.writeBoolean(false);
                w.writeInt(id.length);
            }
            w.write(id);
        } else {
            w.writeBoolean(true);
            w.writeByte(0);
        }

    }

    private static Address deserialiseAddress(DataInputStream r) throws IOException {
        byte[] ipBytes = new byte[4];
        if (r.read(ipBytes) != 4) {
            throw new IOException("Incomplete dataset!");
        }
        if ((ipBytes[0] == 0) && (ipBytes[1] == 0) && (ipBytes[2] == 0) && (ipBytes[3] == 0)) {
            return null; // IP 0.0.0.0 is not valid but null Address encoding
        }
        InetAddress ip = InetAddress.getByAddress(ipBytes);
        byte portUpper = r.readByte();
        byte portLower = r.readByte();
        int port = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
        boolean isByteLength = r.readBoolean();
        int keySize;
        if (isByteLength) {
            keySize = UnsignedBytes.toInt(r.readByte());
        } else {
            keySize = r.readInt();
        }
        byte[] id;
        if (keySize == 0) {
            id = null;
        } else {
            id = new byte[keySize];
            if (r.read(ipBytes) != keySize) {
                throw new IOException("Incomplete dataset!");
            }
        }
        return new Address(ip, port, id);
    }

    public static LookupTable generateInitial(Set<Address> hosts) {
        LookupTable lut = new LookupTable();
        lut.generateHosts(hosts);
        lut.generateReplicationGroups(hosts);
        lut.generateInitialVirtuals();

        INSTANCE = lut;

        return lut;
    }

    private void generateHosts(Set<Address> hosts) {
        this.hosts = new ArrayList<Address>(hosts);
    }

    private void generateReplicationGroups(Set<Address> hosts) {
        ArrayList<Integer> dup1, dup2, dup3;
        List<Integer> nats = naturals(hosts.size());
        dup1 = new ArrayList<Integer>(nats);
        dup2 = new ArrayList<Integer>(nats);
        dup3 = new ArrayList<Integer>(nats);

        replicationGroups = new ArrayList<Integer[]>(INIT_REP_FACTOR * hosts.size());

        for (int n = 0; n < INIT_REP_FACTOR; n++) {
            Collections.shuffle(dup1, RAND);
            Collections.shuffle(dup2, RAND);
            Collections.shuffle(dup3, RAND);
            for (int i = 0; i < hosts.size(); i++) {
                int h1, h2, h3;
                h1 = dup1.get(i);
                h2 = dup2.get(i);
                h3 = dup3.get(i);
                while (h2 == h1) {
                    h2 = RAND.nextInt(hosts.size());
                }
                while ((h3 == h1) || (h3 == h2)) {
                    h3 = RAND.nextInt(hosts.size());
                }
                Integer[] group = new Integer[]{h1, h2, h3};
                int pos = n * hosts.size() + i;
                replicationGroups.add(pos, group);
            }
        }
    }

    private void generateInitialVirtuals() {
        Arrays.fill(virtualHostGroupVersions, 0l);
        for (int i = 0; i < NUM_VIRT_GROUPS; i++) {
            virtualHostGroups[i] = new LookupGroup(Ints.toByteArray(i)[3]);
        }

        // Reserved range from 00 00 00 00 to 00 00 00 01
        virtualHostsPut(new Key(0), 0);
        // Place as many virtual nodes as there are hosts in the system
        // for random (non-schema-aligned) writes (more or less evenly distributed)
        virtualHostsPut(new Key(1), RAND.nextInt(replicationGroups.size()));
        UnsignedInteger incr = (UnsignedInteger.MAX_VALUE.minus(UnsignedInteger.ONE)).dividedBy(UnsignedInteger.fromIntBits(hosts.size()));
        UnsignedInteger last = UnsignedInteger.ONE;
        UnsignedInteger ceiling = UnsignedInteger.MAX_VALUE.minus(incr);
        while (last.compareTo(ceiling) <= 0) {
            last = last.plus(incr);
            virtualHostsPut(new Key(last.intValue()), RAND.nextInt(replicationGroups.size()));
        }
    }

    void virtualHostsPut(Key key, Integer value) {
        int groupId = key.getFirstByte();
        LookupGroup group = virtualHostGroups[groupId];
        group.put(key, value);
    }

    Integer virtualHostsGet(Key key) {
        LookupGroup group = virtualHostGroups[key.getFirstByte()];
        return group.get(key);
    }

    Integer virtualHostsGetResponsible(Key key) {
        int groupId = key.getFirstByte();
        LookupGroup keyGroup = virtualHostGroups[groupId];
        while (true) {
            try {
                Integer i = keyGroup.getResponsible(key);
                return i;
            } catch (NoResponsibleInGroup e) {
                groupId = groupId - 1;
                keyGroup = virtualHostGroups[groupId];
                if (groupId < 0) {
                    return null;
                }
            }
        }
    }

    private static List<Integer> naturals(int upTo) {
        ArrayList<Integer> nats = new ArrayList<Integer>(upTo);
        for (int i = 0; i < upTo; i++) {
            nats.add(i);
        }
        return nats;
    }

    public static class NoResponsibleInGroup extends Throwable {

        public static final NoResponsibleInGroup exception = new NoResponsibleInGroup();
    }
}
