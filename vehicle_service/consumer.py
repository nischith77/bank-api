import asyncio
import json
import logging
import signal
import time
from datetime import datetime
from typing import Dict, Any, Optional

import redis
from sqlalchemy.exc import SQLAlchemyError
from pydantic import ValidationError

from config import settings, get_redis_connection_kwargs
from database import db_manager
from models import (
    VehicleMessage, TelemetryData, AlertData, VehicleCreate,
    Vehicle, VehicleTelemetry, VehicleAlert
)

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class VehicleDataConsumer:
    def __init__(self):
        self.redis_client = None
        self.pubsub = None
        self.running = False
        self.channels = [
            settings.vehicle_telemetry_channel,
            settings.vehicle_alerts_channel,
            settings.vehicle_updates_channel
        ]
        
    def setup_redis_connection(self):
        """Setup Redis connection and pub/sub."""
        try:
            connection_kwargs = get_redis_connection_kwargs()
            self.redis_client = redis.Redis(**connection_kwargs)
            
            # Test connection
            self.redis_client.ping()
            logger.info("Redis connection established successfully")
            
            # Setup pub/sub
            self.pubsub = self.redis_client.pubsub()
            for channel in self.channels:
                self.pubsub.subscribe(channel)
                logger.info(f"Subscribed to channel: {channel}")
                
        except Exception as e:
            logger.error(f"Failed to setup Redis connection: {e}")
            raise

    def process_telemetry_message(self, data: Dict[str, Any]) -> bool:
        """Process telemetry message and store in database."""
        try:
            # Validate message data
            telemetry = TelemetryData(**data)
            
            # Store in database
            with db_manager.get_session_context() as session:
                # Check if vehicle exists
                vehicle = session.query(Vehicle).filter(
                    Vehicle.vehicle_id == telemetry.vehicle_id
                ).first()
                
                if not vehicle:
                    logger.warning(f"Vehicle {telemetry.vehicle_id} not found, skipping telemetry")
                    return False
                
                # Create telemetry record
                db_telemetry = VehicleTelemetry(
                    vehicle_id=telemetry.vehicle_id,
                    timestamp=telemetry.timestamp,
                    latitude=telemetry.latitude,
                    longitude=telemetry.longitude,
                    speed=telemetry.speed,
                    heading=telemetry.heading,
                    altitude=telemetry.altitude,
                    fuel_level=telemetry.fuel_level,
                    engine_temperature=telemetry.engine_temperature,
                    odometer=telemetry.odometer,
                    battery_voltage=telemetry.battery_voltage,
                    engine_status=telemetry.engine_status,
                    ac_status=telemetry.ac_status
                )
                
                session.add(db_telemetry)
                session.commit()
                
                logger.debug(f"Stored telemetry for vehicle {telemetry.vehicle_id}")
                return True
                
        except ValidationError as e:
            logger.error(f"Invalid telemetry data: {e}")
            return False
        except SQLAlchemyError as e:
            logger.error(f"Database error storing telemetry: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error processing telemetry: {e}")
            return False

    def process_alert_message(self, data: Dict[str, Any]) -> bool:
        """Process alert message and store in database."""
        try:
            # Validate message data
            alert = AlertData(**data)
            
            # Store in database
            with db_manager.get_session_context() as session:
                # Check if vehicle exists
                vehicle = session.query(Vehicle).filter(
                    Vehicle.vehicle_id == alert.vehicle_id
                ).first()
                
                if not vehicle:
                    logger.warning(f"Vehicle {alert.vehicle_id} not found, skipping alert")
                    return False
                
                # Create alert record
                db_alert = VehicleAlert(
                    vehicle_id=alert.vehicle_id,
                    alert_type=alert.alert_type,
                    severity=alert.severity,
                    message=alert.message,
                    timestamp=alert.timestamp,
                    resolved=alert.resolved,
                    resolved_at=alert.resolved_at
                )
                
                session.add(db_alert)
                session.commit()
                
                logger.info(f"Stored {alert.severity} alert for vehicle {alert.vehicle_id}: {alert.message}")
                return True
                
        except ValidationError as e:
            logger.error(f"Invalid alert data: {e}")
            return False
        except SQLAlchemyError as e:
            logger.error(f"Database error storing alert: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error processing alert: {e}")
            return False

    def process_vehicle_update_message(self, data: Dict[str, Any]) -> bool:
        """Process vehicle update message."""
        try:
            # Validate message data
            vehicle_data = VehicleCreate(**data)
            
            # Store/update in database
            with db_manager.get_session_context() as session:
                # Check if vehicle exists
                existing_vehicle = session.query(Vehicle).filter(
                    Vehicle.vehicle_id == vehicle_data.vehicle_id
                ).first()
                
                if existing_vehicle:
                    # Update existing vehicle
                    for field, value in vehicle_data.dict(exclude_unset=True).items():
                        setattr(existing_vehicle, field, value)
                    logger.info(f"Updated vehicle {vehicle_data.vehicle_id}")
                else:
                    # Create new vehicle
                    db_vehicle = Vehicle(**vehicle_data.dict())
                    session.add(db_vehicle)
                    logger.info(f"Created new vehicle {vehicle_data.vehicle_id}")
                
                session.commit()
                return True
                
        except ValidationError as e:
            logger.error(f"Invalid vehicle data: {e}")
            return False
        except SQLAlchemyError as e:
            logger.error(f"Database error storing vehicle: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error processing vehicle update: {e}")
            return False

    def process_message(self, message) -> bool:
        """Process a single message from Redis pub/sub."""
        try:
            if message['type'] != 'message':
                return True
            
            channel = message['channel']
            raw_data = message['data']
            
            # Parse message
            try:
                message_data = json.loads(raw_data)
                vehicle_message = VehicleMessage(**message_data)
            except (json.JSONDecodeError, ValidationError) as e:
                logger.error(f"Invalid message format from {channel}: {e}")
                return False
            
            # Route message based on type and channel
            success = False
            if channel == settings.vehicle_telemetry_channel:
                success = self.process_telemetry_message(vehicle_message.data)
            elif channel == settings.vehicle_alerts_channel:
                success = self.process_alert_message(vehicle_message.data)
            elif channel == settings.vehicle_updates_channel:
                success = self.process_vehicle_update_message(vehicle_message.data)
            else:
                logger.warning(f"Unknown channel: {channel}")
                return False
            
            if success:
                logger.debug(f"Successfully processed message {vehicle_message.message_id} from {channel}")
            
            return success
            
        except Exception as e:
            logger.error(f"Error processing message: {e}")
            return False

    def run(self):
        """Main consumer loop."""
        logger.info("Starting Vehicle Data Consumer")
        
        try:
            # Setup connections
            self.setup_redis_connection()
            
            # Check database health
            if not db_manager.health_check():
                raise Exception("Database health check failed")
            
            self.running = True
            processed_count = 0
            error_count = 0
            last_log_time = time.time()
            
            logger.info("Consumer is ready and listening for messages...")
            
            # Main message processing loop
            for message in self.pubsub.listen():
                if not self.running:
                    break
                
                try:
                    success = self.process_message(message)
                    if success:
                        processed_count += 1
                    else:
                        error_count += 1
                        
                    # Log statistics periodically
                    current_time = time.time()
                    if current_time - last_log_time >= 60:  # Every minute
                        logger.info(f"Processed: {processed_count}, Errors: {error_count}")
                        last_log_time = current_time
                        
                except KeyboardInterrupt:
                    logger.info("Received interrupt signal")
                    break
                except Exception as e:
                    logger.error(f"Error in message processing loop: {e}")
                    error_count += 1
                    time.sleep(1)  # Brief pause before continuing
                    
        except Exception as e:
            logger.error(f"Critical error in consumer: {e}")
            raise
        finally:
            self.cleanup()

    def cleanup(self):
        """Cleanup resources."""
        logger.info("Cleaning up consumer resources...")
        self.running = False
        
        if self.pubsub:
            try:
                self.pubsub.close()
            except Exception as e:
                logger.error(f"Error closing pub/sub: {e}")
        
        if self.redis_client:
            try:
                self.redis_client.close()
            except Exception as e:
                logger.error(f"Error closing Redis connection: {e}")
        
        try:
            db_manager.close()
        except Exception as e:
            logger.error(f"Error closing database: {e}")
        
        logger.info("Consumer cleanup completed")

    def signal_handler(self, signum, frame):
        """Handle shutdown signals."""
        logger.info(f"Received signal {signum}, shutting down gracefully...")
        self.running = False

def main():
    """Main entry point."""
    consumer = VehicleDataConsumer()
    
    # Setup signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, consumer.signal_handler)
    signal.signal(signal.SIGTERM, consumer.signal_handler)
    
    try:
        consumer.run()
    except Exception as e:
        logger.error(f"Consumer failed: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())