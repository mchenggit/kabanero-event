#!/bin/bash

IMAGE=kabanero-event

echo "Building ${IMAGE}"

docker build -t ${IMAGE} . 
