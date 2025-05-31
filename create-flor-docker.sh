#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

cd "$SCRIPTPATH" || exit 1
mvn clean package || exit 1

cd "${SCRIPTPATH}/target" || exit 1

docker buildx build -f ./Dockerfile --tag nickas21/smart-solar-tuya:latest .

read -r -p "Push image to Docker Hub? [Y/n]: " PUSH_CHOICE

if [[ "$PUSH_CHOICE" =~ ^[Yy]$ || -z "$PUSH_CHOICE" ]]; then
    docker push nickas21/smart-solar-tuya:latest
else
    echo "Skipping push."
fi

# Приклад локального запуску контейнера для перевірки:
# docker run --rm -it nickas21/smart-solar-tuya:latest

# Приклад кубернет:
  # Stop
#kubectl scale deployment my-deployment --replicas=0
  # Рестарт Pod-а (через Deployment) - smart-solar-tuya-deployment
# kubectl rollout restart deployment smart-solar-tuya-deployment
  # Run
#kubectl scale deployment my-deployment --replicas=1
