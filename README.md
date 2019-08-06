# kabanero-event

## Introduction

This repository contains a prototype that:
- receives a message from kafka about a GIT push event to a appsody project
- matches the apposdy project to a collection in Kabanero
- Finds the associated Tekton pipeline for the collection
- Creates the Git PipelineResource  for the appsody project, the Docker iamge pipelineResource for the output of the pipeline, and a PipelineRun resource to trigger a new run of the pipeline.

## Build

For local build: `mvn package`

For docker build: `build.sh` creates an image called kabanero-event

## Run

Currently the image is designed to be run in the kabanero namespace, and requires RBAC to be set up for the kabanero system account. For now, we'll set it up as cluster-admin, but we may end up with more restrictive roles in the future:
```
oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:kabanero:default

```

To push the image to the open shift internal registry:
- `docker tag kabanero-event docker-registry-default.sires1.fyre.ibm.com/kabanero/kabanero-event`, substituting `docker-registry-default.sires1.fyre.ibm.com` with the host of your open shift registry.
- `docker push docker-registry-default.sires1.fyre.ibm.com/kabanero/kabanero-event`

To run the image: 
- `oc project kabanero`
- `oc new-app kabanero-event`

This creates a new DeploymentConfig. After this, pushing a new version of the image will trigger a new pod to be created to run the new images.
