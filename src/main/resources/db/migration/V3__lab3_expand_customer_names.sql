ALTER TABLE customers ADD COLUMN first_name VARCHAR(255);
ALTER TABLE customers ADD COLUMN last_name VARCHAR(255);

CREATE FUNCTION sync_customer_name_parts() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.full_name IS NULL THEN
        NEW.full_name := concat_ws(' ', NEW.first_name, NEW.last_name);
        RETURN NEW;
    END IF;
    NEW.first_name := split_part(NEW.full_name, ' ', 1);
    NEW.last_name := CASE
        WHEN strpos(NEW.full_name, ' ') > 0
        THEN substring(NEW.full_name FROM strpos(NEW.full_name, ' ') + 1)
    END;
    RETURN NEW;
END;
$$;

CREATE TRIGGER customers_sync_name_parts
BEFORE INSERT OR UPDATE OF full_name ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_name_parts();

UPDATE customers
SET first_name = split_part(full_name, ' ', 1),
    last_name = CASE
        WHEN strpos(full_name, ' ') > 0
        THEN substring(full_name FROM strpos(full_name, ' ') + 1)
    END
WHERE first_name IS NULL;

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL;
