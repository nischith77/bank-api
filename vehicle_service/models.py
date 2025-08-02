from datetime import datetime
from typing import Optional
from enum import Enum
from decimal import Decimal
from uuid import UUID, uuid4

from pydantic import BaseModel, Field, validator
from sqlalchemy import (
    create_engine, Column, String, Integer, DateTime, Boolean, 
    Numeric, ForeignKey, Text, Enum as SQLEnum
)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.dialects.postgresql import UUID as PGUUID
from sqlalchemy.orm import relationship, sessionmaker
from sqlalchemy.sql import func

Base = declarative_base()

# Enums
class VehicleStatus(str, Enum):
    ACTIVE = "active"
    INACTIVE = "inactive"
    MAINTENANCE = "maintenance"
    RETIRED = "retired"

class FuelType(str, Enum):
    GASOLINE = "gasoline"
    DIESEL = "diesel"
    ELECTRIC = "electric"
    HYBRID = "hybrid"
    LPG = "lpg"
    CNG = "cng"

class AlertSeverity(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

# SQLAlchemy Models
class Vehicle(Base):
    __tablename__ = "vehicles"
    
    id = Column(PGUUID(as_uuid=True), primary_key=True, default=uuid4)
    vehicle_id = Column(String(50), unique=True, nullable=False, index=True)
    make = Column(String(50), nullable=False)
    model = Column(String(50), nullable=False)
    year = Column(Integer, nullable=False)
    vin = Column(String(17), unique=True)
    license_plate = Column(String(20))
    fuel_type = Column(SQLEnum(FuelType), nullable=False)
    status = Column(SQLEnum(VehicleStatus), default=VehicleStatus.ACTIVE)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    telemetry = relationship("VehicleTelemetry", back_populates="vehicle", cascade="all, delete-orphan")
    alerts = relationship("VehicleAlert", back_populates="vehicle", cascade="all, delete-orphan")

class VehicleTelemetry(Base):
    __tablename__ = "vehicle_telemetry"
    
    id = Column(PGUUID(as_uuid=True), primary_key=True, default=uuid4)
    vehicle_id = Column(String(50), ForeignKey("vehicles.vehicle_id"), nullable=False, index=True)
    timestamp = Column(DateTime(timezone=True), nullable=False, index=True)
    latitude = Column(Numeric(10, 8))
    longitude = Column(Numeric(11, 8))
    speed = Column(Numeric(5, 2))  # km/h
    heading = Column(Numeric(5, 2))  # degrees
    altitude = Column(Numeric(8, 2))  # meters
    fuel_level = Column(Numeric(5, 2))  # percentage
    engine_temperature = Column(Numeric(5, 2))  # celsius
    odometer = Column(Numeric(10, 2))  # kilometers
    battery_voltage = Column(Numeric(4, 2))  # volts
    engine_status = Column(Boolean)
    ac_status = Column(Boolean)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    vehicle = relationship("Vehicle", back_populates="telemetry")

class VehicleAlert(Base):
    __tablename__ = "vehicle_alerts"
    
    id = Column(PGUUID(as_uuid=True), primary_key=True, default=uuid4)
    vehicle_id = Column(String(50), ForeignKey("vehicles.vehicle_id"), nullable=False, index=True)
    alert_type = Column(String(50), nullable=False)
    severity = Column(SQLEnum(AlertSeverity), nullable=False)
    message = Column(Text, nullable=False)
    timestamp = Column(DateTime(timezone=True), nullable=False, index=True)
    resolved = Column(Boolean, default=False, index=True)
    resolved_at = Column(DateTime(timezone=True))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    vehicle = relationship("Vehicle", back_populates="alerts")

# Pydantic Models for validation
class VehicleBase(BaseModel):
    vehicle_id: str = Field(..., min_length=1, max_length=50)
    make: str = Field(..., min_length=1, max_length=50)
    model: str = Field(..., min_length=1, max_length=50)
    year: int = Field(..., ge=1900, le=2030)
    vin: Optional[str] = Field(None, min_length=17, max_length=17)
    license_plate: Optional[str] = Field(None, max_length=20)
    fuel_type: FuelType
    status: VehicleStatus = VehicleStatus.ACTIVE

class VehicleCreate(VehicleBase):
    pass

class VehicleUpdate(BaseModel):
    make: Optional[str] = Field(None, min_length=1, max_length=50)
    model: Optional[str] = Field(None, min_length=1, max_length=50)
    year: Optional[int] = Field(None, ge=1900, le=2030)
    vin: Optional[str] = Field(None, min_length=17, max_length=17)
    license_plate: Optional[str] = Field(None, max_length=20)
    fuel_type: Optional[FuelType] = None
    status: Optional[VehicleStatus] = None

class VehicleResponse(VehicleBase):
    id: UUID
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True

class TelemetryData(BaseModel):
    vehicle_id: str = Field(..., min_length=1, max_length=50)
    timestamp: datetime
    latitude: Optional[float] = Field(None, ge=-90, le=90)
    longitude: Optional[float] = Field(None, ge=-180, le=180)
    speed: Optional[float] = Field(None, ge=0)
    heading: Optional[float] = Field(None, ge=0, lt=360)
    altitude: Optional[float] = None
    fuel_level: Optional[float] = Field(None, ge=0, le=100)
    engine_temperature: Optional[float] = None
    odometer: Optional[float] = Field(None, ge=0)
    battery_voltage: Optional[float] = Field(None, ge=0)
    engine_status: Optional[bool] = None
    ac_status: Optional[bool] = None

    @validator('timestamp', pre=True)
    def parse_timestamp(cls, v):
        if isinstance(v, str):
            return datetime.fromisoformat(v.replace('Z', '+00:00'))
        return v

class TelemetryResponse(TelemetryData):
    id: UUID
    created_at: datetime
    
    class Config:
        from_attributes = True

class AlertData(BaseModel):
    vehicle_id: str = Field(..., min_length=1, max_length=50)
    alert_type: str = Field(..., min_length=1, max_length=50)
    severity: AlertSeverity
    message: str = Field(..., min_length=1)
    timestamp: datetime
    resolved: bool = False
    resolved_at: Optional[datetime] = None

    @validator('timestamp', pre=True)
    def parse_timestamp(cls, v):
        if isinstance(v, str):
            return datetime.fromisoformat(v.replace('Z', '+00:00'))
        return v

class AlertResponse(AlertData):
    id: UUID
    created_at: datetime
    
    class Config:
        from_attributes = True

# Message models for pub/sub
class VehicleMessage(BaseModel):
    message_type: str  # "telemetry", "alert", "vehicle_update"
    data: dict
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    message_id: str = Field(default_factory=lambda: str(uuid4()))