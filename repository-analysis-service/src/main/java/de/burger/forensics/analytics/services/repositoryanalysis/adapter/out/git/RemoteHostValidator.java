package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;

final class RemoteHostValidator {
    private final RemoteHostResolver resolver;

    RemoteHostValidator(RemoteHostResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    static RemoteHostValidator system() {
        return new RemoteHostValidator(RemoteHostResolver.system());
    }

    ValidatedRemoteHost requirePubliclyRoutable(RepositoryReference repository) {
        var uri = URI.create(repository.remoteUrl());
        var host = uri.getHost();
        var addresses = resolve(host);
        if (addresses.isEmpty() || addresses.stream().anyMatch(RemoteHostValidator::isUnsafeAddress)) {
            throw new IllegalArgumentException("repository remote host must resolve to public addresses only");
        }
        return new ValidatedRemoteHost(host, uri.getPort() < 0 ? 443 : uri.getPort(), addresses);
    }

    private List<InetAddress> resolve(String host) {
        try {
            return resolver.resolve(host);
        } catch (UnknownHostException error) {
            throw new IllegalArgumentException("repository remote host could not be resolved safely", error);
        }
    }

    private static boolean isUnsafeAddress(InetAddress address) {
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || isUniqueLocalIpv6(address)
            || isCarrierGradeNat(address);
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        var first = address.getAddress()[0] & 0xff;
        return (first & 0xfe) == 0xfc;
    }

    private static boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        var bytes = address.getAddress();
        var first = bytes[0] & 0xff;
        var second = bytes[1] & 0xff;
        return first == 100 && second >= 64 && second <= 127;
    }

    record ValidatedRemoteHost(String host, int port, List<InetAddress> addresses) {
        ValidatedRemoteHost {
            host = Objects.requireNonNull(host, "host must not be null");
            addresses = List.copyOf(addresses);
        }

        List<String> curlResolveOptions() {
            return addresses.stream()
                .map(address -> host + ":" + port + ":" + curlAddress(address))
                .toList();
        }

        private static String curlAddress(InetAddress address) {
            if (address instanceof Inet6Address) {
                return "[" + address.getHostAddress() + "]";
            }
            return address.getHostAddress();
        }
    }
}
