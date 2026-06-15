package com.meshrelief.core.transport;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Collection;

public interface PeerDiscoveryListener {

    void onPeersDiscovered(
            Collection<WifiP2pDevice> peers
    );
}