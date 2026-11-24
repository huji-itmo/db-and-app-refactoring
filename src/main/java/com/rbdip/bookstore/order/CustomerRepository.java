package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends Repository<Customer, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO customers (full_name, address, phone)
            VALUES (:fullName, :address, :phone)
            ON CONFLICT (full_name, address, phone) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("fullName") String fullName, @Param("address") String address, @Param("phone") String phone);

    Customer findByFullNameAndAddressAndPhone(String fullName, String address, String phone);
}
