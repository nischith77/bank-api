import os
from flask import jsonify, request, Flask
from flaskext.mysql import MySQL
from utils.query_execute import execute_query
from config.app import app
from config.config import mysql

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
    
    query_string=f"SELECT cast(account_id as char) account_id,account_type,customer_id FROM accounts  WHERE customer_id= '{customer_id}'"
    
    if(account_type and (account_type=='savings' or account_type=='investment' or account_type=='cash')) :
        query_string += f" AND account_type = '{account_type}'"

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
        

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)


    