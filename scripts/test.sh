#!/bin/bash

# Vehicle Data System Test Script

set -e

echo "🧪 Testing Vehicle Data Pub/Sub System..."

# Check if services are running
if ! docker-compose ps | grep -q "Up"; then
    echo "❌ Services are not running. Please start them first with: ./scripts/start.sh"
    exit 1
fi

echo "📡 Testing API endpoints..."

# Test health endpoint
echo -n "Health check: "
if response=$(curl -s -w "%{http_code}" http://localhost:8000/health); then
    http_code="${response: -3}"
    if [ "$http_code" = "200" ]; then
        echo "✅ Passed"
    else
        echo "❌ Failed (HTTP $http_code)"
    fi
else
    echo "❌ Failed (no response)"
fi

# Test root endpoint
echo -n "Root endpoint: "
if curl -s http://localhost:8000/ > /dev/null 2>&1; then
    echo "✅ Passed"
else
    echo "❌ Failed"
fi

# Test vehicles endpoint
echo -n "Vehicles endpoint: "
if curl -s http://localhost:8000/vehicles > /dev/null 2>&1; then
    echo "✅ Passed"
else
    echo "❌ Failed"
fi

echo ""
echo "📤 Publishing test data..."

# Run the test publisher
if docker-compose exec -T vehicle_publisher python publisher.py test > /dev/null 2>&1; then
    echo "✅ Test data published successfully"
else
    echo "❌ Failed to publish test data"
    exit 1
fi

echo "⏳ Waiting for data to be processed..."
sleep 10

echo ""
echo "🔍 Verifying data was stored..."

# Check statistics
echo -n "Getting system stats: "
stats_response=$(curl -s http://localhost:8000/stats)
if [ $? -eq 0 ]; then
    echo "✅ Success"
    echo "📊 System Statistics:"
    echo "$stats_response" | python3 -m json.tool 2>/dev/null || echo "$stats_response"
else
    echo "❌ Failed"
fi

echo ""
echo -n "Checking vehicles: "
vehicles_response=$(curl -s http://localhost:8000/vehicles)
if [ $? -eq 0 ]; then
    vehicle_count=$(echo "$vehicles_response" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "✅ Found $vehicle_count vehicles"
else
    echo "❌ Failed"
fi

echo ""
echo -n "Checking telemetry data: "
telemetry_response=$(curl -s http://localhost:8000/telemetry/latest)
if [ $? -eq 0 ]; then
    telemetry_count=$(echo "$telemetry_response" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "✅ Found $telemetry_count telemetry records"
else
    echo "❌ Failed"
fi

echo ""
echo -n "Checking alerts: "
alerts_response=$(curl -s http://localhost:8000/alerts/active)
if [ $? -eq 0 ]; then
    alerts_count=$(echo "$alerts_response" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "✅ Found $alerts_count active alerts"
else
    echo "❌ Failed"
fi

echo ""
echo "🔄 Testing continuous data flow..."
echo "Starting continuous publisher for 30 seconds..."

# Start continuous publisher in background
docker-compose exec -d vehicle_publisher python publisher.py continuous 5

echo "⏳ Monitoring for 30 seconds..."
sleep 30

# Stop the continuous publisher
echo "🛑 Stopping continuous publisher..."
docker-compose exec vehicle_publisher pkill -f publisher.py 2>/dev/null || true

echo ""
echo "📊 Final system check..."
final_stats=$(curl -s http://localhost:8000/stats)
echo "$final_stats" | python3 -m json.tool 2>/dev/null || echo "$final_stats"

echo ""
echo "🎉 Test completed!"
echo ""
echo "🔗 Quick links:"
echo "   API Documentation: http://localhost:8000/docs"
echo "   System Stats: http://localhost:8000/stats"
echo "   All Vehicles: http://localhost:8000/vehicles"
echo "   Latest Telemetry: http://localhost:8000/telemetry/latest"
echo "   Active Alerts: http://localhost:8000/alerts/active"