import os
from typing import Optional
from pydantic import BaseSettings, Field

class Settings(BaseSettings):
    # Database settings
    database_url: str = Field(
        default="postgresql://postgres:password@localhost:5432/vehicle_data",
        env="DATABASE_URL"
    )
    
    # GCP Pub/Sub settings
    gcp_project_id: str = Field(default="your-project-id", env="GCP_PROJECT_ID")
    gcp_credentials_path: Optional[str] = Field(default=None, env="GOOGLE_APPLICATION_CREDENTIALS")
    
    # Pub/Sub Topics and Subscriptions
    vehicle_telemetry_topic: str = Field(default="vehicle-telemetry", env="TELEMETRY_TOPIC")
    vehicle_alerts_topic: str = Field(default="vehicle-alerts", env="ALERTS_TOPIC")
    vehicle_updates_topic: str = Field(default="vehicle-updates", env="UPDATES_TOPIC")
    
    vehicle_telemetry_subscription: str = Field(default="vehicle-telemetry-sub", env="TELEMETRY_SUBSCRIPTION")
    vehicle_alerts_subscription: str = Field(default="vehicle-alerts-sub", env="ALERTS_SUBSCRIPTION")
    vehicle_updates_subscription: str = Field(default="vehicle-updates-sub", env="UPDATES_SUBSCRIPTION")
    
    # Consumer settings
    consumer_batch_size: int = Field(default=100, env="CONSUMER_BATCH_SIZE")
    consumer_timeout: int = Field(default=5, env="CONSUMER_TIMEOUT")
    max_retry_attempts: int = Field(default=3, env="MAX_RETRY_ATTEMPTS")
    retry_delay: int = Field(default=1, env="RETRY_DELAY")
    
    # Application settings
    app_name: str = Field(default="Vehicle Data Service", env="APP_NAME")
    app_version: str = Field(default="1.0.0", env="APP_VERSION")
    log_level: str = Field(default="INFO", env="LOG_LEVEL")
    environment: str = Field(default="development", env="ENVIRONMENT")
    
    # API settings
    api_host: str = Field(default="0.0.0.0", env="API_HOST")
    api_port: int = Field(default=8000, env="API_PORT")
    api_workers: int = Field(default=1, env="API_WORKERS")
    
    # Health check settings
    health_check_interval: int = Field(default=30, env="HEALTH_CHECK_INTERVAL")
    
    # Database pool settings
    db_pool_size: int = Field(default=10, env="DB_POOL_SIZE")
    db_max_overflow: int = Field(default=20, env="DB_MAX_OVERFLOW")
    db_pool_timeout: int = Field(default=30, env="DB_POOL_TIMEOUT")
    
    # Debug settings
    sql_debug: bool = Field(default=False, env="SQL_DEBUG")
    redis_debug: bool = Field(default=False, env="REDIS_DEBUG")
    
    class Config:
        env_file = ".env"
        case_sensitive = False

# Global settings instance
settings = Settings()

def get_redis_connection_kwargs():
    """Get Redis connection parameters as a dictionary."""
    kwargs = {
        "host": settings.redis_host,
        "port": settings.redis_port,
        "db": settings.redis_db,
        "decode_responses": True,
        "socket_connect_timeout": 5,
        "socket_timeout": 5,
        "retry_on_timeout": True
    }
    
    if settings.redis_password:
        kwargs["password"] = settings.redis_password
    
    return kwargs