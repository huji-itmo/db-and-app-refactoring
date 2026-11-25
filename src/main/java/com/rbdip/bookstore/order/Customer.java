package com.rbdip.bookstore.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private Long id;

    private String firstName;
    private String lastName;
    private String address;
    private String phone;

    protected Customer() {
        // for JPA
    }

    public String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
