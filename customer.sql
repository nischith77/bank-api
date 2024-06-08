CREATE TABLE customers (
  customer_id VARCHAR(12)  PRIMARY KEY,
  first_name VARCHAR(256) NOT NULL,
  last_name VARCHAR(256) NOT NULL,
  email VARCHAR(256),
  phone_number VARCHAR(20)
);

INSERT INTO customers (customer_id,first_name, last_name, email, phone_number)
VALUES
  (234567890123,'John', 'Doe', 'john.doe@example.com', '+1 123-456-7890'),
  (234567890124,'Alice', 'Smith', 'alice.smith@example.com', '+1 987-654-3210'),
  (234567890125,'Bob', 'Johnson', 'bob.johnson@example.com', '+1 555-123-4567'),
  (234567890126,'Justin', 'duren', 'just.duren@example.com', '+1 555-123-4567');
