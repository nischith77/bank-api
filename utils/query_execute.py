from flask import jsonify, request, Flask
import logging

logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')

logger = logging.getLogger(__name__)


def execute_query(conn, query):
    cursor = None
    try:
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
            conn.close()