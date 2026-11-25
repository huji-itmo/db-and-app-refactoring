DROP TRIGGER customers_sync_name_parts ON customers;
DROP FUNCTION sync_customer_name_parts();
ALTER TABLE customers
    DROP COLUMN full_name,
    ADD UNIQUE NULLS NOT DISTINCT (first_name, last_name, address, phone);
