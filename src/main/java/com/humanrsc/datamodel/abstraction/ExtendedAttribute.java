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
public class ExtendedAttribute implements Serializable {

    @Column(name = "attribute_key")
    private String key;

    @Column(name = "attribute_value", length = 1000)
    private String value;

    @Column(name = "attribute_type")
    private String type = "STRING";

    public ExtendedAttribute(String key, String value) {
        this.key = key;
        this.value = value;
        this.type = "STRING";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtendedAttribute that = (ExtendedAttribute) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "ExtendedAttribute{key='" + key + "', value='" + value + "', type='" + type + "'}";
    }

    public static ExtendedAttribute of(String key, String value) {
        return new ExtendedAttribute(key, value);
    }

    public static ExtendedAttribute of(String key, String value, String type) {
        return new ExtendedAttribute(key, value, type);
    }
}