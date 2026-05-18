package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@FunctionalInterface
interface RemoteHostResolver {
    List<InetAddress> resolve(String host) throws UnknownHostException;

    static RemoteHostResolver system() {
        return host -> List.of(InetAddress.getAllByName(host));
    }
}
