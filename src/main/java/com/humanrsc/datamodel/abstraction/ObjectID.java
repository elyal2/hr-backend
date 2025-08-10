package com.humanrsc.datamodel.abstraction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectID implements Serializable {

    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "tenant_id", length = 100)
    private String tenantID;

    // Explicit constructor to avoid Lombok processing issues
    public ObjectID(String id, String tenantID, boolean dummy) {
        this.id = id;
        this.tenantID = tenantID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectID objectID = (ObjectID) o;
        return Objects.equals(id, objectID.id) && 
               Objects.equals(tenantID, objectID.tenantID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantID);
    }

    @Override
    public String toString() {
        return "ObjectID{id='" + id + "', tenantID='" + tenantID + "'}";
    }

    public static ObjectID of(String id, String tenantID) {
        ObjectID oid = new ObjectID();
        oid.setId(id);
        oid.setTenantID(tenantID);
        return oid;
    }
}