/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.caracaldb.system;

import se.sics.caracaldb.global.LookupService;
import se.sics.caracaldb.global.MaintenanceService;
import se.sics.caracaldb.store.Store;
import se.sics.kompics.Component;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.VirtualNetworkChannel;
import se.sics.kompics.timer.Timer;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class VirtualSharedComponents {

    private VirtualNetworkChannel net;
    private byte[] id;
    private Address self;
    private Positive<LookupService> lookup;
    private Positive<Store> store;
    private Positive<MaintenanceService> maintenance;
    private Positive<Timer> timer;

    public VirtualSharedComponents(byte[] id) {
        this.id = id;
    }

    void setSelf(Address self) {
        this.self = self;
    }

    public Address getSelf() {
        return self;
    }

    void setNetwork(VirtualNetworkChannel vnc) {
        this.net = vnc;
    }

    public void connectNetwork(Component c) {
        net.addConnection(id, c.getNegative(Network.class));
    }

    public void disconnectNetwork(Component c) {
        net.removeConnection(id, c.getNegative(Network.class));
    }

    public byte[] getId() {
        return id;
    }
    
    public void setLookup(Positive<LookupService> lookup) {
        this.lookup = lookup;
    }
    
    public Positive<LookupService> getLookup() {
        return lookup;
    }
    
    public void setStore(Positive<Store> store) {
        this.store = store;
    }
    
    public Positive<Store> getStore() {
        return store;
    }

    /**
     * @return the maintenance
     */
    public Positive<MaintenanceService> getMaintenance() {
        return maintenance;
    }

    /**
     * @param maintenance the maintenance to set
     */
    public void setMaintenance(Positive<MaintenanceService> maintenance) {
        this.maintenance = maintenance;
    }

    /**
     * @return the timer
     */
    public Positive<Timer> getTimer() {
        return timer;
    }

    /**
     * @param timer the timer to set
     */
    public void setTimer(Positive<Timer> timer) {
        this.timer = timer;
    }
}