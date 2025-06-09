#!/bin/bash

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN} Setting up development environment${NC}"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Install Helm if not present
if ! command_exists helm; then
    echo -e "${YELLOW} Installing Helm...${NC}"
    curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi

# Install kubectl if not present
if ! command_exists kubectl; then
    echo -e "${YELLOW} Please install kubectl manually${NC}"
    echo "Visit: https://kubernetes.io/docs/tasks/tools/"
fi

# Install Docker if not present
if ! command_exists docker; then
    echo -e "${YELLOW} Please install Docker manually${NC}"
    echo "Visit: https://docs.docker.com/get-docker/"
fi

echo -e "${GREEN}Environment setup completed!${NC}"