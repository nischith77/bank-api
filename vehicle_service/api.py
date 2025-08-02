from datetime import datetime, timedelta
from typing import List, Optional
from uuid import UUID

from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from sqlalchemy import desc, func

from config import settings
from database import get_db, db_manager
from models import (
    Vehicle, VehicleTelemetry, VehicleAlert,
    VehicleResponse, TelemetryResponse, AlertResponse
)

# Create FastAPI app
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Vehicle Data Management API"
)

@app.get("/")
async def root():
    """Root endpoint with basic API information."""
    return {
        "name": settings.app_name,
        "version": settings.app_version,
        "status": "running",
        "environment": settings.environment
    }

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    db_healthy = db_manager.health_check()
    
    return JSONResponse(
        status_code=200 if db_healthy else 503,
        content={
            "status": "healthy" if db_healthy else "unhealthy",
            "database": "connected" if db_healthy else "disconnected",
            "timestamp": datetime.utcnow().isoformat()
        }
    )

@app.get("/vehicles", response_model=List[VehicleResponse])
async def get_vehicles(
    status: Optional[str] = None,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: Session = Depends(get_db)
):
    """Get list of vehicles with optional filtering."""
    query = db.query(Vehicle)
    
    if status:
        query = query.filter(Vehicle.status == status)
    
    vehicles = query.offset(skip).limit(limit).all()
    return vehicles

@app.get("/vehicles/{vehicle_id}", response_model=VehicleResponse)
async def get_vehicle(vehicle_id: str, db: Session = Depends(get_db)):
    """Get a specific vehicle by ID."""
    vehicle = db.query(Vehicle).filter(Vehicle.vehicle_id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle

@app.get("/vehicles/{vehicle_id}/telemetry", response_model=List[TelemetryResponse])
async def get_vehicle_telemetry(
    vehicle_id: str,
    hours: int = Query(24, ge=1, le=168),  # Last 24 hours by default, max 1 week
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: Session = Depends(get_db)
):
    """Get telemetry data for a specific vehicle."""
    # Check if vehicle exists
    vehicle = db.query(Vehicle).filter(Vehicle.vehicle_id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    
    # Calculate time threshold
    time_threshold = datetime.utcnow() - timedelta(hours=hours)
    
    # Query telemetry data
    telemetry = db.query(VehicleTelemetry).filter(
        VehicleTelemetry.vehicle_id == vehicle_id,
        VehicleTelemetry.timestamp >= time_threshold
    ).order_by(desc(VehicleTelemetry.timestamp)).offset(skip).limit(limit).all()
    
    return telemetry

@app.get("/vehicles/{vehicle_id}/alerts", response_model=List[AlertResponse])
async def get_vehicle_alerts(
    vehicle_id: str,
    resolved: Optional[bool] = None,
    severity: Optional[str] = None,
    hours: int = Query(168, ge=1, le=720),  # Last week by default, max 30 days
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: Session = Depends(get_db)
):
    """Get alerts for a specific vehicle."""
    # Check if vehicle exists
    vehicle = db.query(Vehicle).filter(Vehicle.vehicle_id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    
    # Calculate time threshold
    time_threshold = datetime.utcnow() - timedelta(hours=hours)
    
    # Build query
    query = db.query(VehicleAlert).filter(
        VehicleAlert.vehicle_id == vehicle_id,
        VehicleAlert.timestamp >= time_threshold
    )
    
    if resolved is not None:
        query = query.filter(VehicleAlert.resolved == resolved)
    
    if severity:
        query = query.filter(VehicleAlert.severity == severity)
    
    alerts = query.order_by(desc(VehicleAlert.timestamp)).offset(skip).limit(limit).all()
    return alerts

@app.get("/telemetry/latest", response_model=List[TelemetryResponse])
async def get_latest_telemetry(
    limit: int = Query(50, ge=1, le=500),
    db: Session = Depends(get_db)
):
    """Get the latest telemetry data across all vehicles."""
    # Subquery to get the latest telemetry for each vehicle
    subquery = db.query(
        VehicleTelemetry.vehicle_id,
        func.max(VehicleTelemetry.timestamp).label('max_timestamp')
    ).group_by(VehicleTelemetry.vehicle_id).subquery()
    
    # Join with the subquery to get full records
    telemetry = db.query(VehicleTelemetry).join(
        subquery,
        (VehicleTelemetry.vehicle_id == subquery.c.vehicle_id) &
        (VehicleTelemetry.timestamp == subquery.c.max_timestamp)
    ).order_by(desc(VehicleTelemetry.timestamp)).limit(limit).all()
    
    return telemetry

@app.get("/alerts/active", response_model=List[AlertResponse])
async def get_active_alerts(
    severity: Optional[str] = None,
    limit: int = Query(100, ge=1, le=500),
    db: Session = Depends(get_db)
):
    """Get active (unresolved) alerts."""
    query = db.query(VehicleAlert).filter(VehicleAlert.resolved == False)
    
    if severity:
        query = query.filter(VehicleAlert.severity == severity)
    
    alerts = query.order_by(desc(VehicleAlert.timestamp)).limit(limit).all()
    return alerts

@app.get("/stats")
async def get_statistics(db: Session = Depends(get_db)):
    """Get system statistics."""
    # Vehicle statistics
    total_vehicles = db.query(Vehicle).count()
    active_vehicles = db.query(Vehicle).filter(Vehicle.status == 'active').count()
    
    # Telemetry statistics (last 24 hours)
    time_threshold = datetime.utcnow() - timedelta(hours=24)
    telemetry_count_24h = db.query(VehicleTelemetry).filter(
        VehicleTelemetry.timestamp >= time_threshold
    ).count()
    
    # Alert statistics
    total_alerts = db.query(VehicleAlert).count()
    active_alerts = db.query(VehicleAlert).filter(VehicleAlert.resolved == False).count()
    critical_alerts = db.query(VehicleAlert).filter(
        VehicleAlert.resolved == False,
        VehicleAlert.severity == 'critical'
    ).count()
    
    return {
        "vehicles": {
            "total": total_vehicles,
            "active": active_vehicles,
            "inactive": total_vehicles - active_vehicles
        },
        "telemetry": {
            "records_24h": telemetry_count_24h
        },
        "alerts": {
            "total": total_alerts,
            "active": active_alerts,
            "critical": critical_alerts
        },
        "timestamp": datetime.utcnow().isoformat()
    }

@app.patch("/alerts/{alert_id}/resolve")
async def resolve_alert(alert_id: UUID, db: Session = Depends(get_db)):
    """Mark an alert as resolved."""
    alert = db.query(VehicleAlert).filter(VehicleAlert.id == alert_id).first()
    if not alert:
        raise HTTPException(status_code=404, detail="Alert not found")
    
    alert.resolved = True
    alert.resolved_at = datetime.utcnow()
    db.commit()
    
    return {"message": "Alert resolved successfully", "alert_id": str(alert_id)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api:app",
        host=settings.api_host,
        port=settings.api_port,
        workers=settings.api_workers,
        log_level=settings.log_level.lower()
    )