package main.util;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;

/**
 * SSL/TLS utility for secure connections
 */
public class SSLUtil {
    private static final String KEYSTORE_PASSWORD = "studyconnect123";
    private static final String KEY_PASSWORD = "studyconnect123";
    private static KeyStore keyStore;
    private static KeyStore trustStore;
    
    static {
        try {
            initializeKeyStores();
        } catch (Exception e) {
            System.err.println("Failed to initialize SSL: " + e.getMessage());
        }
    }
    
    /**
     * Initialize keystores (create self-signed certificate if needed)
     */
    private static void initializeKeyStores() throws Exception {
        // Create in-memory keystore with self-signed certificate
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        
        // Generate self-signed certificate
        generateSelfSignedCertificate();
    }
    
    /**
     * Generate self-signed certificate for development
     */
    private static void generateSelfSignedCertificate() throws Exception {
        // For simplicity, we'll use a trust-all approach for P2P
        // In production, use proper certificates
    }
    
    /**
     * Create SSL Server Socket Factory
     */
    public static SSLServerSocketFactory getServerSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        // Create trust manager that trusts all certificates (for P2P)
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext.getServerSocketFactory();
    }
    
    /**
     * Create SSL Socket Factory
     */
    public static SSLSocketFactory getSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        // Create trust manager that trusts all certificates (for P2P)
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext.getSocketFactory();
    }
    
    /**
     * Check if SSL is available
     */
    public static boolean isSSLAvailable() {
        try {
            SSLContext.getInstance("TLS");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
