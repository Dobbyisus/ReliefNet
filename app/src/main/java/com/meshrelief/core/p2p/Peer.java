package com.meshrelief.core.p2p;

public class Peer {

    private final String id;
    private final String name;
    private final PeerStatus status;

    public Peer(String id, String name, PeerStatus status) {

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        this.id = id;
        this.name = name;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PeerStatus getStatus() {
        return status;
    }
}