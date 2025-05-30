#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

cd "${SCRIPTPATH}/target" || exit 1

docker buildx build -f ./Dockerfile --tag nickas21/smart-solar-tuya:latest .

read -r -p "Push image to Docker Hub? [Y/n]: " PUSH_CHOICE

if [[ "$PUSH_CHOICE" =~ ^[Yy]$ || -z "$PUSH_CHOICE" ]]; then
    docker push nickas21/smart-solar-tuya:latest
else
    echo "Skipping push."
fi
