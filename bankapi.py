import os
from flask import jsonify, request, Flask
from flaskext.mysql import MySQL
import logging

logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')

# Create a logger object
logger = logging.getLogger(__name__)

app = Flask(__name__)
mysql = MySQL()


# MySQL configurations
app.config["MYSQL_DATABASE_USER"] = "root"
app.config["MYSQL_DATABASE_PASSWORD"] = os.getenv("db_root_password")
app.config["MYSQL_DATABASE_DB"] = os.getenv("db_name")
app.config["MYSQL_DATABASE_HOST"] = os.getenv("MYSQL_SERVICE_HOST")
app.config["MYSQL_DATABASE_PORT"] = int(os.getenv("MYSQL_SERVICE_PORT"))
mysql.init_app(app)


@app.route("/customer/<customer_id>", methods=["GET"])
def user(customer_id):

    if len(customer_id) != 12:
        return jsonify({"error": "Invalid customer ID length. Must be 12 characters."}), 400  # Bad Request
    query_string=f"SELECT * FROM customers WHERE customer_id='{customer_id}'"
    conn = mysql.connect()
    response=execute_query(conn,query_string)
    return response
    
@app.route("/account/<customer_id>", methods=["GET"])
def accounts(customer_id):

    account_type = request.args.get('account_type')

    if len(customer_id) != 12:
        return jsonify({"error": "Invalid customer ID length. Must be 12 characters."}), 400  # Bad Request
    
    query_string=f"SELECT cast(account_id as char) acount_id,account_type,customer_id FROM accounts  WHERE customer_id= '{customer_id}'"
    
    if(account_type and (account_type=='savings' or account_type=='investment' or account_type=='cash')) :
        query_string += f" AND account_type = '{account_type}'"

        logger.info(query_string)
      
    conn = mysql.connect()
    response=execute_query(conn,query_string)
    return response
    

@app.route("/avg_interest_rate", methods=["GET"])
def interestrate():
    query_string="select cast(avg(interest_rate) as char) average_interest_rate from savings"
    conn = mysql.connect()
    response=execute_query(conn,query_string)
    return response

    
@app.route("/investment_performance/<customer_id>", methods=["GET"])
def investment_performance(customer_id):

    if len(customer_id) != 12:
        return jsonify({"error": "Invalid customer ID length. Must be 12 characters."}), 400  # Bad Request
    query_string=f"SELECT * FROM investment_performance_vw WHERE customer_id= '{customer_id}'"    
    conn = mysql.connect()
    response=execute_query(conn,query_string)
    return response
    
def execute_query(conn, query):
    cursor = None
    try:
        conn = mysql.connect()
        cursor = conn.cursor()
        cursor.execute(query)
        row = cursor.description  # Get column descriptions
        column_names = [col[0] for col in row]  # Extract column names
        data = cursor.fetchall()
        
        if not data:  # Check for empty result set
         return jsonify({"message": "No data found"}), 404  # Not Found

        response_data = [dict(zip(column_names, account)) for account in data]
        return jsonify(response_data)
    
    except Exception as e:
        # Log the error for debugging purposes
        logger.error(f"An error occurred: {str(e)}")

        # Return a generic error message to the client 
        return jsonify({"error": "Internal server error"}), 500  # Internal Server Error

    finally:
        if cursor:
            cursor.close()    
    

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)


    