#!/bin/bash
# Run script for StudyConnect (Maven build)

echo "========================================"
echo "Running StudyConnect Application"
echo "========================================"
echo

if [ ! -f "target/StudyConnect-1.0.0.jar" ]; then
    echo "JAR file not found! Please build first using ./build.sh"
    exit 1
fi

#!/bin/bash
# Run StudyConnect P2P Application

java -cp "target/StudyConnect-1.0.0.jar:target/lib/*" main.StudyConnectMain
