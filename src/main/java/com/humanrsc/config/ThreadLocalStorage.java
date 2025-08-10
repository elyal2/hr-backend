package com.humanrsc.config;

/**
 * Almacenamiento local de hilos para mantener el contexto del tenant
 */
public class ThreadLocalStorage {
    private static final ThreadLocal<String> tenant = new ThreadLocal<>();
    
    public static void setTenantID(String tenantID) { 
        tenant.set(tenantID); 
    }
    
    public static String getTenantID() { 
        return tenant.get(); 
    }
    
    public static void clear() {
        tenant.remove();
    }
}
