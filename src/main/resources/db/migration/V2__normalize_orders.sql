CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    UNIQUE NULLS NOT DISTINCT (full_name, address, phone)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT REFERENCES customers(id);

UPDATE orders AS o
SET customer_id = c.id
FROM customers AS c
WHERE c.full_name = o.customer_full_name
  AND c.address IS NOT DISTINCT FROM o.customer_address
  AND c.phone IS NOT DISTINCT FROM o.customer_phone;

ALTER TABLE orders
    ALTER COLUMN customer_id SET NOT NULL,
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

INSERT INTO products (name, price)
SELECT product_name, product_price FROM order_items
EXCEPT
SELECT name, price FROM products;

ALTER TABLE order_items ADD COLUMN product_id BIGINT REFERENCES products(id);

UPDATE order_items AS oi
SET product_id = (
    SELECT MIN(p.id) FROM products AS p
    WHERE p.name = oi.product_name AND p.price = oi.product_price
);

ALTER TABLE order_items
    ALTER COLUMN product_id SET NOT NULL,
    DROP COLUMN product_name,
    DROP COLUMN product_price;

CREATE INDEX orders_customer_id_idx ON orders (customer_id);
CREATE INDEX order_items_product_id_idx ON order_items (product_id);
