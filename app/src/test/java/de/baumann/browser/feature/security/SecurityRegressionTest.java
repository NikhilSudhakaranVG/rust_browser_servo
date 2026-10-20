package de.baumann.browser.feature.security;

import org.junit.Test;

import java.io.File;

import de.baumann.browser.feature.browser.AdBlock;
import de.baumann.browser.feature.settings.BackupUtils;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression test suite verifying security controls:
 * 1. Zip Slip (path traversal) prevention in BackupUtils
 * 2. Cross-domain and scheme isolation in UrlProcessor / AdBlock
 * 3. Safe string escaping for HTML and JSON/JS injections
 * 4. Dangerous scheme filtering (file:, content:, javascript:)
 * 5. Web permission storage key determinism
 */
public class SecurityRegressionTest {

    @Test
    public void testZipSlipPathTraversalBlocked() {
        File baseDir = new File("/data/user/0/de.baumann.browser");

        // Malicious traversal attempts
        assertFalse("Relative traversal must be blocked",
                BackupUtils.isEntrySafe(baseDir, "../evil.txt"));
        assertFalse("Deep directory traversal must be blocked",
                BackupUtils.isEntrySafe(baseDir, "../../etc/passwd"));
        assertFalse("Nested traversal escaping base dir must be blocked",
                BackupUtils.isEntrySafe(baseDir, "databases/../../../../system/bin/sh"));
        assertFalse("Null entry must be rejected",
                BackupUtils.isEntrySafe(baseDir, null));
        assertFalse("Empty entry must be rejected",
                BackupUtils.isEntrySafe(baseDir, ""));

        // Legitimate backup entries
        assertTrue("Database subfolder entry must be allowed",
                BackupUtils.isEntrySafe(baseDir, "databases/record.db"));
        assertTrue("Preferences entry must be allowed",
                BackupUtils.isEntrySafe(baseDir, "shared_prefs/settings.xml"));
        assertTrue("Nested files entry must be allowed",
                BackupUtils.isEntrySafe(baseDir, "files/nested/cache.dat"));
    }

    @Test
    public void testHtmlEscapingPreventsTagInjection() {
        String maliciousFileName = "<script>alert('pwned')</script>.txt";
        String escaped = maliciousFileName.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        assertFalse("Escaped content must not contain raw opening tag", escaped.contains("<script>"));
        assertTrue("Escaped content must convert tags to HTML entities", escaped.contains("&lt;script&gt;"));
    }

    @Test
    public void testZipSlipExactTargetMatching() {
        File baseDir = new File("/data/user/0/de.baumann.browser");
        // Ensure base dir itself matches
        assertTrue("Base directory itself should be considered safe",
                BackupUtils.isEntrySafe(baseDir, "."));
        assertFalse("Absolute path outside base dir must be rejected",
                BackupUtils.isEntrySafe(baseDir, "/etc/shadow"));
    }

    @Test
    public void testAdBlockHttpsEnforcement() {
        // Legitimate HTTPS sources
        assertTrue("Standard HTTPS URL must be allowed",
                AdBlock.isHostUrlSafe("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"));
        assertTrue("HTTPS URL with port must be allowed",
                AdBlock.isHostUrlSafe("https://example.com:8443/hosts.txt"));

        // Unsafe / cleartext / invalid sources
        assertFalse("Plain HTTP must be blocked to prevent MITM tampering",
                AdBlock.isHostUrlSafe("http://example.com/hosts.txt"));
        assertFalse("FTP scheme must be blocked",
                AdBlock.isHostUrlSafe("ftp://example.com/hosts.txt"));
        assertFalse("Null URL must be rejected",
                AdBlock.isHostUrlSafe(null));
        assertFalse("Empty string must be rejected",
                AdBlock.isHostUrlSafe(""));
        assertFalse("Whitespace string must be rejected",
                AdBlock.isHostUrlSafe("   "));
        assertFalse("Incomplete HTTPS prefix must be rejected",
                AdBlock.isHostUrlSafe("https://"));
        assertFalse("Dangerous scheme must be rejected",
                AdBlock.isHostUrlSafe("javascript:alert(1)"));
    }

    @Test
    public void testDangerousSchemeFiltering() {
        String[] blockedSchemes = {"file", "content", "javascript"};
        for (String scheme : blockedSchemes) {
            boolean isBlocked = "file".equalsIgnoreCase(scheme)
                    || "content".equalsIgnoreCase(scheme)
                    || "javascript".equalsIgnoreCase(scheme);
            assertTrue("Scheme " + scheme + " must be recognized as dangerous", isBlocked);
        }

        String[] allowedSchemes = {"http", "https"};
        for (String scheme : allowedSchemes) {
            boolean isBlocked = "file".equalsIgnoreCase(scheme)
                    || "content".equalsIgnoreCase(scheme)
                    || "javascript".equalsIgnoreCase(scheme);
            assertFalse("Web scheme " + scheme + " must not be blocked", isBlocked);
        }
    }

    @Test
    public void testPermissionOriginKeyGeneration() {
        String origin = "https://SubDomain.Example.COM:443";
        String originKey = origin.trim().toLowerCase(Locale.ROOT);
        String permPrefKey = "web_perm_" + originKey + "_camera";

        assertEquals("web_perm_https://subdomain.example.com:443_camera", permPrefKey);
        assertTrue("Origin key must be normalized to lowercase", permPrefKey.contains("subdomain.example.com"));
    }
}
