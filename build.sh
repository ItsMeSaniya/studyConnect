#!/bin/bash
# Build script for StudyConnect (Linux/Mac)

echo "========================================"
echo "Building StudyConnect Application"
echo "========================================"
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven not found! Please install Maven or compile manually."
    echo
    echo "Manual compilation:"
    echo "1. Download FlatLaf JAR files from: https://github.com/JFormDesigner/FlatLaf/releases"
    echo "2. Place them in the lib folder"
    echo "3. Run: ./compile-manual.sh"
    exit 1
fi

echo "Building with Maven..."
mvn clean package

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "Build Successful!"
    echo "========================================"
    echo
    echo "To run the application:"
    echo "  java -cp \"target/StudyConnect-1.0.0.jar:target/lib/*\" main.StudyConnectMain"
    echo
    echo "Or use: ./run.sh"
else
    echo
    echo "Build failed! Please check the errors above."
fi
