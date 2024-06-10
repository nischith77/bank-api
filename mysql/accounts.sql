CREATE TABLE accounts (
    account_id Double PRIMARY KEY,
    customer_id VARCHAR(12),
    account_type VARCHAR(256),
    currency     varchar(3),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);
insert into accounts(account_id,customer_id,account_type,currency) values(987654321112345,234567890123,'SAVINGS','SEK');
insert into accounts(account_id,customer_id,account_type,currency) values(987654321112346,234567890124,'INVESTMENT','SEK');
insert into accounts(account_id,customer_id,account_type,currency) values(987654321112347,234567890125,'CASH','SEK');
insert into accounts(account_id,customer_id,account_type,currency) values(987654321112348,234567890126,'SAVINGS','SEK');

