CREATE TABLE savings (
  account_id Double  PRIMARY KEY,
  interest_rate DECIMAL(4,2) NOT NULL,
  balance DECIMAL(10,2) NOT NULL,
  opening_date DATE NOT NULL,
  closing_date DATE,
  FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

insert into savings(account_id,interest_rate,balance,opening_date,closing_date) values(987654321112345,4.50,200000,'2022-06-02',null);
insert into savings(account_id,interest_rate,balance,opening_date,closing_date) values(987654321112346,5.0,20000,'2023-06-02',null)
