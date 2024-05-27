package my_protocol;

import framework.*;

import java.util.*;

/**
 * @version 12-03-2019
 *
 * Copyright University of Twente,  2013-2024
 *
 **************************************************************************
 *                          = Copyright notice =                          *
 *                                                                        *
 *            This file may ONLY  be distributed UNMODIFIED!              *
 * In particular, a correct solution to the challenge must  NOT be posted *
 * in public places, to preserve the learning effect for future students. *
 **************************************************************************
 */
public class MyRoutingProtocol implements IRoutingProtocol {
    private LinkLayer linkLayer;

    private Set<Integer> connected = new HashSet<>();
    // You can use this data structure to store your routing table.
    private HashMap<Integer, MyRoute> myRoutingTable = new HashMap<>();

    @Override
    public void init(LinkLayer linkLayer) {
        this.linkLayer = linkLayer;
    }


    @Override
    public void tick(PacketWithLinkCost[] packetsWithLinkCosts) {
        connected.clear();
        // Get the address of this node
        int myAddress = this.linkLayer.getOwnAddress();
        System.out.println("tick; received " + packetsWithLinkCosts.length + " packets");
        int i;

        // first process the incoming packets; loop over them:
        for (i = 0; i < packetsWithLinkCosts.length; i++) {
            Set<Integer> received = new HashSet<>();
            Packet packet = packetsWithLinkCosts[i].getPacket();
            int neighbour = packet.getSourceAddress();
            int linkcost = packetsWithLinkCosts[i].getLinkCost();
            DataTable dt = packet.getDataTable();
            System.out.printf("received packet from %d with %d rows and %d columns of data%n", neighbour, dt.getNRows(), dt.getNColumns());
            connected.add(neighbour);

            if (!myRoutingTable.containsKey(neighbour)) {
                received.add(neighbour);
                MyRoute route = new MyRoute();
                route.nextHop = neighbour;
                route.cost = linkcost;
                myRoutingTable.put(neighbour , route);
            }

            for(int j = 0; j < dt.getNRows() ; j++){
                int node = dt.get(j,0);
                if(node != 0) {
                    received.add(node);
                    if ((myRoutingTable.get(node) == null ||
                            dt.get(j, 1) + linkcost < myRoutingTable.get(node).cost) &&
                            node != myAddress) {
                        if(myRoutingTable.get(node) == null  && dt.get(j,2) == myAddress){

                        } else {
                            MyRoute r = new MyRoute();
                            r.cost = dt.get(j, 1) + linkcost;
                            r.nextHop = neighbour;
                            myRoutingTable.put(node, r);
                        }
                    }
                }
            }
            Set<Integer> fakes = new HashSet<>();
            for(int l : myRoutingTable.keySet()) {
                if ((!received.contains(l)) && (neighbour == myRoutingTable.get(l).nextHop) && (l != neighbour)){
                    System.out.println("Received packets for route to: " + received);
                    System.out.println("Fake found: " + l + " is not a valid route");
                    fakes.add(l);
                }
            }
            for(int fake : fakes){
                myRoutingTable.remove(fake);
            }
        }
        Set<Integer> fakes = new HashSet<>();
        for(int a : myRoutingTable.keySet()) {
            int next = myRoutingTable.get(a).nextHop;
            if(!connected.contains(next)){
                fakes.add(a);
            }
        }
        for(int fake : fakes){
            myRoutingTable.remove(fake);
        }

        DataTable dt = new DataTable(3);
        for (int key : myRoutingTable.keySet()) {
            dt.set(key, 0, key);
            dt.set(key, 1, myRoutingTable.get(key).cost);
            dt.set(key,2,myRoutingTable.get(key).nextHop);
        }

        Packet pkt = new Packet(myAddress, 0, dt);
        this.linkLayer.transmit(pkt);
    }

    public Map<Integer, Integer> getForwardingTable() {
        // This code extracts from your routing table the forwarding table.
        // The result of this method is send to the server to validate and score your protocol.

        // <Destination, NextHop>
        HashMap<Integer, Integer> ft = new HashMap<>();

        for (Map.Entry<Integer, MyRoute> entry : myRoutingTable.entrySet()) {
            ft.put(entry.getKey(), entry.getValue().nextHop);
        }

        return ft;
    }
}
