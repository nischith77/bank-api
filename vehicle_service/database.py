import os
import logging
from typing import Generator
from contextlib import contextmanager

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.pool import StaticPool

from models import Base

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class DatabaseManager:
    def __init__(self, database_url: str = None):
        if database_url is None:
            database_url = os.getenv(
                "DATABASE_URL", 
                "postgresql://postgres:password@localhost:5432/vehicle_data"
            )
        
        self.database_url = database_url
        self.engine = create_engine(
            database_url,
            pool_pre_ping=True,
            pool_recycle=300,
            echo=os.getenv("SQL_DEBUG", "").lower() == "true"
        )
        
        self.SessionLocal = sessionmaker(
            autocommit=False, 
            autoflush=False, 
            bind=self.engine
        )
        
        # Create tables if they don't exist
        try:
            Base.metadata.create_all(bind=self.engine)
            logger.info("Database tables created/verified successfully")
        except Exception as e:
            logger.error(f"Error creating database tables: {e}")
            raise

    def get_session(self) -> Generator[Session, None, None]:
        """Get a database session with automatic cleanup."""
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except SQLAlchemyError as e:
            logger.error(f"Database error: {e}")
            session.rollback()
            raise
        except Exception as e:
            logger.error(f"Unexpected error: {e}")
            session.rollback()
            raise
        finally:
            session.close()

    @contextmanager
    def get_session_context(self):
        """Context manager for database sessions."""
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except SQLAlchemyError as e:
            logger.error(f"Database error: {e}")
            session.rollback()
            raise
        except Exception as e:
            logger.error(f"Unexpected error: {e}")
            session.rollback()
            raise
        finally:
            session.close()

    def health_check(self) -> bool:
        """Check if database connection is healthy."""
        try:
            with self.get_session_context() as session:
                session.execute("SELECT 1")
            return True
        except Exception as e:
            logger.error(f"Database health check failed: {e}")
            return False

    def close(self):
        """Close database connections."""
        self.engine.dispose()
        logger.info("Database connections closed")

# Global database manager instance
db_manager = DatabaseManager()

# Dependency function for FastAPI
def get_db() -> Generator[Session, None, None]:
    """Dependency function to get database session in FastAPI endpoints."""
    yield from db_manager.get_session()