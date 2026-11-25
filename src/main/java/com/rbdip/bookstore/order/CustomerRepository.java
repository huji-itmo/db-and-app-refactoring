package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends Repository<Customer, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO customers (first_name, last_name, address, phone)
            VALUES (:firstName, :lastName, :address, :phone)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("firstName") String firstName, @Param("lastName") String lastName, @Param("address") String address, @Param("phone") String phone);

    Customer findByFirstNameAndLastNameAndAddressAndPhone(
            String firstName, String lastName, String address, String phone);
}
