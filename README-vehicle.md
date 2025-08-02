# Vehicle Data Pub/Sub System

A scalable vehicle data management system that uses Redis pub/sub for real-time message processing and PostgreSQL for data storage. The system handles vehicle telemetry data, alerts, and vehicle updates through a microservices architecture.

## Features

- **Real-time Data Processing**: Redis pub/sub for high-throughput message processing
- **Persistent Storage**: PostgreSQL database with optimized schema for vehicle data
- **RESTful API**: FastAPI-based API for querying and monitoring
- **Docker Support**: Complete containerized setup for local development
- **Data Validation**: Pydantic models for robust data validation
- **Monitoring**: Built-in health checks and statistics endpoints

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Publisher     │───▶│     Redis       │───▶│    Consumer     │
│   (Test Data)   │    │   (Pub/Sub)     │    │   (Processing)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │◀───│   PostgreSQL    │◀───│   Data Storage  │
│   (FastAPI)     │    │   (Database)    │    │   (SQLAlchemy)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Data Models

### Vehicle
- Basic vehicle information (make, model, year, VIN, etc.)
- Status tracking (active, inactive, maintenance, retired)
- Fuel type and other metadata

### Telemetry
- Real-time vehicle data (GPS, speed, fuel level, etc.)
- Engine diagnostics and sensor readings
- Timestamp-based data for historical analysis

### Alerts
- Vehicle alerts and notifications
- Severity levels (low, medium, high, critical)
- Resolution tracking

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Git

### 1. Clone and Setup

```bash
git clone <repository-url>
cd <repository-name>

# Copy environment file
cp .env.example .env

# Optional: Modify .env file with your settings
nano .env
```

### 2. Start the System

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f vehicle_consumer
```

### 3. Verify Setup

```bash
# Check API health
curl http://localhost:8000/health

# View system statistics
curl http://localhost:8000/stats

# List vehicles
curl http://localhost:8000/vehicles
```

### 4. Test with Sample Data

```bash
# Run test scenario (publishes sample data)
docker-compose exec vehicle_publisher python publisher.py test

# Start continuous simulation
docker-compose exec vehicle_publisher python publisher.py continuous 10
```

## Services

### PostgreSQL Database
- **Port**: 5432
- **Database**: vehicle_data
- **Username**: postgres
- **Password**: password

### Redis
- **Port**: 6379
- **Used for**: Pub/sub messaging

### Vehicle Consumer
- Processes messages from Redis channels
- Stores data in PostgreSQL
- Handles data validation and error recovery

### Vehicle API
- **Port**: 8000
- FastAPI-based REST API
- Provides endpoints for querying vehicle data

### Vehicle Publisher (Test)
- Generates sample vehicle data
- Publishes to Redis channels for testing

## API Endpoints

### Health and Status
- `GET /` - API information
- `GET /health` - Health check
- `GET /stats` - System statistics

### Vehicles
- `GET /vehicles` - List vehicles
- `GET /vehicles/{vehicle_id}` - Get specific vehicle
- `GET /vehicles/{vehicle_id}/telemetry` - Get vehicle telemetry
- `GET /vehicles/{vehicle_id}/alerts` - Get vehicle alerts

### Telemetry and Alerts
- `GET /telemetry/latest` - Latest telemetry across all vehicles
- `GET /alerts/active` - Active (unresolved) alerts
- `PATCH /alerts/{alert_id}/resolve` - Mark alert as resolved

### Example API Calls

```bash
# Get all vehicles
curl http://localhost:8000/vehicles

# Get telemetry for specific vehicle (last 24 hours)
curl http://localhost:8000/vehicles/VH001/telemetry

# Get active alerts
curl http://localhost:8000/alerts/active

# Get system statistics
curl http://localhost:8000/stats
```

## Configuration

Configuration is managed through environment variables. See `.env.example` for all available options.

### Key Settings

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://postgres:password@localhost:5432/vehicle_data` |
| `REDIS_URL` | Redis connection string | `redis://localhost:6379` |
| `LOG_LEVEL` | Logging level | `INFO` |
| `TELEMETRY_CHANNEL` | Redis channel for telemetry | `vehicle:telemetry` |
| `ALERTS_CHANNEL` | Redis channel for alerts | `vehicle:alerts` |
| `UPDATES_CHANNEL` | Redis channel for vehicle updates | `vehicle:updates` |

## Development

### Local Development

```bash
# Install Python dependencies
pip install -r requirements.vehicle.txt

# Set environment variables
export DATABASE_URL="postgresql://postgres:password@localhost:5432/vehicle_data"
export REDIS_URL="redis://localhost:6379"

# Run consumer
cd vehicle_service
python consumer.py

# Run API (in another terminal)
python api.py

# Run publisher for testing (in another terminal)
python publisher.py test
```

### Database Management

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d vehicle_data

# View tables
\dt

# Sample queries
SELECT COUNT(*) FROM vehicles;
SELECT COUNT(*) FROM vehicle_telemetry;
SELECT COUNT(*) FROM vehicle_alerts WHERE resolved = false;
```

### Redis Management

```bash
# Connect to Redis
docker-compose exec redis redis-cli

# Monitor pub/sub messages
MONITOR

# Check active channels
PUBSUB CHANNELS
```

## Message Formats

### Telemetry Message
```json
{
  "message_type": "telemetry",
  "message_id": "uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "data": {
    "vehicle_id": "VH001",
    "timestamp": "2024-01-01T12:00:00Z",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "speed": 65.5,
    "heading": 180.0,
    "fuel_level": 75.5,
    "engine_temperature": 95.0,
    "engine_status": true,
    "ac_status": false
  }
}
```

### Alert Message
```json
{
  "message_type": "alert",
  "message_id": "uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "data": {
    "vehicle_id": "VH001",
    "alert_type": "low_fuel",
    "severity": "medium",
    "message": "Low fuel level detected for vehicle VH001",
    "timestamp": "2024-01-01T12:00:00Z",
    "resolved": false
  }
}
```

### Vehicle Update Message
```json
{
  "message_type": "vehicle_update",
  "message_id": "uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "data": {
    "vehicle_id": "VH001",
    "make": "Toyota",
    "model": "Camry",
    "year": 2022,
    "vin": "1HGBH41JXMN109186",
    "license_plate": "ABC-123",
    "fuel_type": "gasoline",
    "status": "active"
  }
}
```

## Monitoring and Troubleshooting

### Logs

```bash
# View all logs
docker-compose logs

# Follow specific service logs
docker-compose logs -f vehicle_consumer
docker-compose logs -f vehicle_api

# View PostgreSQL logs
docker-compose logs postgres
```

### Performance Monitoring

```bash
# Monitor Redis pub/sub
docker-compose exec redis redis-cli MONITOR

# Check database connections
docker-compose exec postgres psql -U postgres -d vehicle_data -c "SELECT count(*) FROM pg_stat_activity;"

# Monitor system resources
docker stats
```

### Common Issues

1. **Consumer not processing messages**
   - Check Redis connection
   - Verify channel names in configuration
   - Check database connectivity

2. **API returning 503 health check**
   - Ensure PostgreSQL is running
   - Check database connection string
   - Verify database schema is created

3. **Publisher not sending messages**
   - Verify Redis connection
   - Check Redis channels with `PUBSUB CHANNELS`

## Scaling Considerations

### Horizontal Scaling
- Multiple consumer instances can process different channels
- API can be scaled behind a load balancer
- Database connection pooling for high concurrency

### Performance Optimization
- Database indexing on frequently queried columns
- Redis clustering for high availability
- Message batching for bulk processing

### Production Deployment
- Use environment-specific configuration
- Implement proper logging and monitoring
- Set up database backups
- Configure Redis persistence
- Use secrets management for credentials

## Testing

### Unit Tests
```bash
# Run tests (when implemented)
pytest vehicle_service/tests/
```

### Integration Tests
```bash
# Test complete flow
docker-compose exec vehicle_publisher python publisher.py test
sleep 10
curl http://localhost:8000/stats
```

### Load Testing
```bash
# Continuous high-frequency publishing
docker-compose exec vehicle_publisher python publisher.py continuous 1
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Your License Here]

## Support

For questions or issues:
- Check the logs for error messages
- Review the configuration settings
- Create an issue in the repository