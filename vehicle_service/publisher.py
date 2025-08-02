import asyncio
import json
import logging
import random
import time
from datetime import datetime, timezone
from typing import List, Dict, Any

import redis
from pydantic import ValidationError

from config import settings, get_redis_connection_kwargs
from models import VehicleMessage, TelemetryData, AlertData, VehicleCreate, FuelType, VehicleStatus, AlertSeverity

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class VehicleDataPublisher:
    def __init__(self):
        self.redis_client = None
        self.vehicles = [
            "VH001", "VH002", "VH003", "VH004", "VH005"
        ]
        self.running = False
        
        # Sample locations (lat, lon) for different cities
        self.locations = [
            (40.7128, -74.0060),  # New York
            (34.0522, -118.2437), # Los Angeles
            (41.8781, -87.6298),  # Chicago
            (29.7604, -95.3698),  # Houston
            (33.4484, -112.0740), # Phoenix
        ]
        
    def setup_redis_connection(self):
        """Setup Redis connection."""
        try:
            connection_kwargs = get_redis_connection_kwargs()
            self.redis_client = redis.Redis(**connection_kwargs)
            
            # Test connection
            self.redis_client.ping()
            logger.info("Redis connection established successfully")
            
        except Exception as e:
            logger.error(f"Failed to setup Redis connection: {e}")
            raise

    def publish_message(self, channel: str, message: VehicleMessage) -> bool:
        """Publish a message to Redis channel."""
        try:
            message_json = message.json()
            self.redis_client.publish(channel, message_json)
            logger.debug(f"Published message to {channel}: {message.message_id}")
            return True
        except Exception as e:
            logger.error(f"Failed to publish message to {channel}: {e}")
            return False

    def generate_telemetry_data(self, vehicle_id: str) -> TelemetryData:
        """Generate realistic telemetry data for a vehicle."""
        # Get a random base location
        base_lat, base_lon = random.choice(self.locations)
        
        # Add small random variations to simulate movement
        lat_offset = random.uniform(-0.01, 0.01)
        lon_offset = random.uniform(-0.01, 0.01)
        
        return TelemetryData(
            vehicle_id=vehicle_id,
            timestamp=datetime.now(timezone.utc),
            latitude=base_lat + lat_offset,
            longitude=base_lon + lon_offset,
            speed=random.uniform(0, 120),  # 0-120 km/h
            heading=random.uniform(0, 360),
            altitude=random.uniform(0, 1000),
            fuel_level=random.uniform(10, 100),
            engine_temperature=random.uniform(80, 110),
            odometer=random.uniform(10000, 200000),
            battery_voltage=random.uniform(12.0, 14.5),
            engine_status=random.choice([True, False]),
            ac_status=random.choice([True, False])
        )

    def generate_alert_data(self, vehicle_id: str) -> AlertData:
        """Generate realistic alert data for a vehicle."""
        alert_types = [
            ("low_fuel", "Low fuel level detected"),
            ("engine_warning", "Engine temperature high"),
            ("maintenance_due", "Scheduled maintenance due"),
            ("speed_violation", "Speed limit exceeded"),
            ("battery_low", "Battery voltage low"),
            ("tire_pressure", "Tire pressure low"),
            ("oil_change", "Oil change required"),
            ("brake_warning", "Brake system warning")
        ]
        
        alert_type, base_message = random.choice(alert_types)
        severity = random.choice(list(AlertSeverity))
        
        return AlertData(
            vehicle_id=vehicle_id,
            alert_type=alert_type,
            severity=severity,
            message=f"{base_message} for vehicle {vehicle_id}",
            timestamp=datetime.now(timezone.utc),
            resolved=False
        )

    def generate_vehicle_update(self) -> VehicleCreate:
        """Generate a new vehicle registration."""
        makes_models = [
            ("Toyota", "Camry"), ("Honda", "Civic"), ("Ford", "F-150"),
            ("Tesla", "Model 3"), ("BMW", "X5"), ("Mercedes", "C-Class"),
            ("Audi", "A4"), ("Nissan", "Altima"), ("Hyundai", "Elantra")
        ]
        
        make, model = random.choice(makes_models)
        vehicle_id = f"VH{random.randint(100, 999)}"
        
        return VehicleCreate(
            vehicle_id=vehicle_id,
            make=make,
            model=model,
            year=random.randint(2018, 2024),
            vin=f"{''.join(random.choices('0123456789ABCDEFGHJKLMNPRSTUVWXYZ', k=17))}",
            license_plate=f"{''.join(random.choices('ABCDEFGHIJKLMNOPQRSTUVWXYZ', k=3))}-{random.randint(100, 999)}",
            fuel_type=random.choice(list(FuelType)),
            status=VehicleStatus.ACTIVE
        )

    def publish_telemetry_batch(self, count: int = 10):
        """Publish a batch of telemetry messages."""
        logger.info(f"Publishing {count} telemetry messages...")
        
        for _ in range(count):
            vehicle_id = random.choice(self.vehicles)
            telemetry = self.generate_telemetry_data(vehicle_id)
            
            message = VehicleMessage(
                message_type="telemetry",
                data=telemetry.dict()
            )
            
            self.publish_message(settings.vehicle_telemetry_channel, message)
            time.sleep(0.1)  # Small delay between messages

    def publish_alert_batch(self, count: int = 3):
        """Publish a batch of alert messages."""
        logger.info(f"Publishing {count} alert messages...")
        
        for _ in range(count):
            vehicle_id = random.choice(self.vehicles)
            alert = self.generate_alert_data(vehicle_id)
            
            message = VehicleMessage(
                message_type="alert",
                data=alert.dict()
            )
            
            self.publish_message(settings.vehicle_alerts_channel, message)
            time.sleep(0.2)  # Small delay between messages

    def publish_vehicle_update(self):
        """Publish a vehicle update message."""
        logger.info("Publishing vehicle update...")
        
        vehicle_data = self.generate_vehicle_update()
        
        message = VehicleMessage(
            message_type="vehicle_update",
            data=vehicle_data.dict()
        )
        
        self.publish_message(settings.vehicle_updates_channel, message)

    def run_continuous_simulation(self, interval: int = 30):
        """Run continuous simulation publishing messages at regular intervals."""
        logger.info(f"Starting continuous simulation with {interval}s intervals")
        self.running = True
        
        try:
            while self.running:
                # Publish various types of messages
                self.publish_telemetry_batch(count=random.randint(5, 15))
                
                # Occasionally publish alerts
                if random.random() < 0.3:  # 30% chance
                    self.publish_alert_batch(count=random.randint(1, 3))
                
                # Rarely publish vehicle updates
                if random.random() < 0.1:  # 10% chance
                    self.publish_vehicle_update()
                
                logger.info(f"Batch published. Waiting {interval} seconds...")
                time.sleep(interval)
                
        except KeyboardInterrupt:
            logger.info("Received interrupt signal, stopping simulation...")
            self.running = False

    def run_test_scenario(self):
        """Run a predefined test scenario."""
        logger.info("Running test scenario...")
        
        # Publish some vehicle updates first
        logger.info("Publishing vehicle registrations...")
        for _ in range(3):
            self.publish_vehicle_update()
            time.sleep(1)
        
        # Wait for vehicles to be processed
        time.sleep(5)
        
        # Publish telemetry data
        logger.info("Publishing telemetry data...")
        self.publish_telemetry_batch(count=20)
        
        # Publish some alerts
        logger.info("Publishing alerts...")
        self.publish_alert_batch(count=5)
        
        logger.info("Test scenario completed!")

    def cleanup(self):
        """Cleanup resources."""
        self.running = False
        if self.redis_client:
            try:
                self.redis_client.close()
            except Exception as e:
                logger.error(f"Error closing Redis connection: {e}")

def main():
    """Main entry point."""
    import sys
    
    publisher = VehicleDataPublisher()
    
    try:
        publisher.setup_redis_connection()
        
        # Check command line arguments
        if len(sys.argv) > 1:
            if sys.argv[1] == "test":
                publisher.run_test_scenario()
            elif sys.argv[1] == "continuous":
                interval = int(sys.argv[2]) if len(sys.argv) > 2 else 30
                publisher.run_continuous_simulation(interval)
            else:
                print("Usage: python publisher.py [test|continuous [interval]]")
                return 1
        else:
            # Default: run test scenario
            publisher.run_test_scenario()
            
    except KeyboardInterrupt:
        logger.info("Publisher interrupted by user")
    except Exception as e:
        logger.error(f"Publisher failed: {e}")
        return 1
    finally:
        publisher.cleanup()
    
    return 0

if __name__ == "__main__":
    exit(main())