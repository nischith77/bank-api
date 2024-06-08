API Endpoints:

1. /customer/<customer_id> (GET method)

Description:

Retrieves a customer by their unique identifier (customer_id).

Parameters:

customer_id (string): The unique identifier for the customer. Must be 12 characters long.

Response:

[{"customer_id":"234567890124","email":"alice.smith@example.com","first_name":"Alice","last_name":"Smith","phone_number":"+1 987-654-3210"}]

2. /account/<customer_id>/<account_type> (GET)
   Description:

Retrieves accounts associated with a customer, optionally filtered by account type.

Request Parameters:

customer_id (string): The unique identifier for the customer. Must be 12 characters long.
account_type (string, optional): The type of account to filter by (SAVINGS, INVESTMENT, CASH). If omitted, all account types are returned.

Response:
[{"acount_id":"987654321112346","account_type":"INVESTMENT","customer_id":"234567890124"}]

On success, returns a JSON array containing details of the customer's accounts, potentially filtered by account type.

3. /avg_interest_rate (GET)
   Description:

Retrieves the average interest rate for savings accounts.

Request Parameters:

None
Response:

[{"average_interest_rate":"4.750000"}]

On success, returns a JSON object containing a single field:
average_interest_rate (string): The average interest rate for savings accounts,

4."/investment_performance/<customer_id>

Description:

Retrieves a performance of investment by provided customer_id
Request Parameters:

customer_id (string): The unique identifier for the customer. Must be 12 characters long.

Response:

[{"customer_id":"234567890124","profit_amount":"-16035.99","profit_percentage":"-71.16","total_final_amt":"6500.00","total_purchase_amt":"22535.99"}]
