
CREATE TABLE Investments (
  investment_id INT PRIMARY KEY,
  account_id Double NOT NULL,
  instrument_type VARCHAR(255) NULL,
  purchase_date DATE NOT NULL,
  purchase_amount decimal(10,2) NULL DEFAULT 0.00,
  sales_date  DATE null,
  sales_amount decimal(10,2) NULL DEFAULT 0.00,
  current_market_value  decimal(10,2),
  FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

insert into Investments(investment_id,account_id,instrument_type,purchase_date,purchase_amount,sales_date,
    sales_amount,current_market_value) values(235689,987654321112346,'STOCK','2022-06-02',20000.99,null,0,3000);
    
insert into Investments(investment_id,account_id,instrument_type,purchase_date,purchase_amount,sales_date,
    sales_amount,current_market_value) values(345678,987654321112346,'BOND','2022-06-02',2535,null,0,3500);
