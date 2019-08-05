#!/bin/bash
set -e pipefail
if [ $(arch) = "ppc64le" ]; then
  ARCH=ppc64le
elif [ $(arch) = "s390x" ]; then
  ARCH=s390x
else
  ARCH=amd64
fi

IMAGE=prism-was-controller
registry=hyc-icpcontent-docker-local.artifactory.swg-devops.com

echo "$ARTIFACTORY_DOCKER_PWD" | docker login $registry -u "$ARTIFACTORY_DOCKER_USER" --password-stdin

docker pull ${registry}/prism-jre:8-jre-ubi-min-${ARCH}
docker tag ${registry}/prism-jre:8-jre-ubi-min-${ARCH} ibmjava:8-jre-ubi-min

echo "Building ${IMAGE}"

docker build -t ${IMAGE} . 
