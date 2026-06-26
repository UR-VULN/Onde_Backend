package com.onde.core.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF 방어용 외부 URL 검증 유틸리티.
 * 허용 스킴(http/https)과 화이트리스트 호스트만 통과시킵니다.
 */
public final class SafeUrlValidator {

    private static final Set<String> BLOCKED_SCHEMES = Set.of(
            "file", "jar", "dict", "gopher", "ftp", "mailto", "data"
    );

    private SafeUrlValidator() {
    }

    public static void assertAllowedHttpUrl(String url, Set<String> allowedHosts) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            throw new IllegalArgumentException("Allowed hosts must not be empty.");
        }

        URI uri = parseUri(url.trim());
        String scheme = normalize(uri.getScheme());
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are allowed.");
        }
        if (BLOCKED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Blocked URL scheme: " + scheme);
        }

        String host = normalizeHost(uri.getHost());
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is required.");
        }
        if (!isHostAllowed(host, allowedHosts)) {
            throw new IllegalArgumentException("Host is not in the allowlist: " + host);
        }
        if (isBlockedPrivateAddress(host) && !isExplicitLoopbackAllowlistEntry(host, allowedHosts)) {
            throw new IllegalArgumentException("Private or loopback addresses are not allowed: " + host);
        }
    }

    public static Set<String> parseAllowedHosts(String commaSeparatedHosts) {
        if (commaSeparatedHosts == null || commaSeparatedHosts.isBlank()) {
            return Set.of();
        }
        return Set.of(commaSeparatedHosts.split(",")).stream()
                .map(String::trim)
                .map(SafeUrlValidator::normalizeHost)
                .filter(host -> host != null && !host.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format.", e);
        }
    }

    static boolean isHostAllowed(String host, Set<String> allowedHosts) {
        String normalizedHost = normalizeHost(host);
        for (String allowed : allowedHosts) {
            String normalizedAllowed = normalizeHost(allowed);
            if (normalizedHost.equals(normalizedAllowed)) {
                return true;
            }
            if (normalizedHost.endsWith("." + normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }

    static boolean isBlockedPrivateAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || isUniqueLocalAddress(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve host: " + host, e);
        }
    }

    private static boolean isExplicitLoopbackAllowlistEntry(String host, Set<String> allowedHosts) {
        if (!isLoopbackHost(host)) {
            return false;
        }
        return isHostAllowed(host, allowedHosts);
    }

    private static boolean isLoopbackHost(String host) {
        String normalizedHost = normalizeHost(host);
        return "localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost) || "::1".equals(normalizedHost);
    }

    private static boolean isUniqueLocalAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 16) {
            return false;
        }
        return (bytes[0] & 0xFF) == 0xFD;
    }

    private static String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String normalized = host.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
}
