
create or replace view investment_performance_vw as
with 
cust_acct as (
select c.customer_id,a.account_id from customers c,
accounts a where c.customer_id=a.customer_id),

investment as (select customer_id,ca.account_id,purchase_amount,
sales_amount,current_market_value from Investments i,cust_acct ca
where i.account_id=ca.account_id),

investment_performanance as (
select customer_id,
sum(purchase_amount) total_purchase_amt,
sum(sales_amount)+sum(current_market_value) total_final_amt
from investment
 group by customer_id)

 select customer_id,cast(total_purchase_amt as char) total_purchase_amt,
 cast(total_final_amt as char) total_final_amt, 
 cast((total_final_amt-total_purchase_amt) as char) profit_amount, 
 cast(round(((total_final_amt-total_purchase_amt)/total_purchase_amt)*100 ,2) as char) profit_percentage
 from investment_performanance;