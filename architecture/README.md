# Kabanero Event Architecture

## Table of Contents
* [Introduction](#Introduction)
* [What is Kabanero?](#What_is_Kabanero)
* [Kabanero High Level Usage Scenarios](#Usage_Scenarios)
* [Events Functional Specification](#Functional_Specification)


<a name="Introduction"></a>
## Introduction

This document contains the design of events in Kabanero. We start with an introduction to Kabanero and its value as a management infrastructure for cloud native devops. We then present the usage scenarios for Kabanero via three sample applications and their devops life cycle. This is followed by the functional specification of events to support the usage scenarios. 

<a name="What_is_Kabanero"></a>
## What is Kabanero?

The goal of Kabanero is to manage cloud native devops infrastructure. Such an infrastructure may contain many components (though not all are in the Kabanero roadmap) :
- One or more developer environments. For example:
  - Appsody
  - Eclipse Che, or Openshift Codeready Workspaces
  - Eclipse with WDT on developer desktop. 
  - no IDE other than text editor
- One or more source control repositories. For Example:
  - Github
  - Gitlab
  - SVN
  - CVS
- One or more build/test pipeline technologies. For example:
  - Tekton, or Openshift Pipelines
  - Openshift Source-2-Image
  - Jenkins
- One or more stages in the continuous delivery pipeline. For example,
  - dev
  - test
  - integration
  - production


Kabanero allows a devops/solution architect to define:
- Standardized development environments. For example:
  - Which stacks are available for Appsody
  - Which stacks are available for Eclipse Che or Openshift Codeready Workspaces.
- How to push an application/service through different build/test stages,  from source control repository to production. The architect defines:
  - What actions to take when source code is checked into source control. 
  - which Pipelines to execute
  - how to promote to the next stage of the pipeline

Kabanero allows a developer to concentrate as much on coding/testing inner loop as possible. A developer
- can stand up a standard development environment very quickly. For example, a developer may create a new stack from Appsody or Eclipse Che within minutes.
- Can rely on the environment to automatically trigger test builds before committing change into source control, with logs made available to the developer.
- Can rely on the environment to trigger additional release builds and run additional tests, and to be able to debug in the test environment when required.

Kabanero automates the devops infrastructure as much as possible. It will
- offer easy to use defaults.
- Use organizational web hook to process events for all repositories within the organization when configured.
- Install and run the pipelines automatically when configured.
- Trigger new builds if the stack has been updated, for example, with security fixes.

Kabanero is secure. It supports:
  - login with different identity providers, for example, login with Github user ID, or with internal user registry
  - Role Based Access Control(RBAC) for different roles, for example, role of solution architect, and role of application developer.
  - security audit
  - automatic security scans for source code and binaries.

Kabanero is extensible. It can be enhanced to support:
- new development environments with new ways to create stacks,
- new source control repositories
- new pipeline technologies

The initial version of Kabanero will support:
- Appsody as development environment
- Github for source repository 
- Tekton for pipeline

In order to ensure the design is extensible,  the remainder of this document covers more than one development environments, source control repositories, and pipeline technologies, even though some may not be in the Kabanero roadmap.

<a name="Usage_Scenarios"></a>
## Kabanero High Level Usage Scenarios

This section captures the usage scenarios, from which we can derive the functional specification. We begin with a sample production environment consisting of 3 different applications/services. This is followed by discussion on the devops pipeline that produces the production environment. Note that the usage scenarios presented in this section are not recommendations. They are meant to capture a variety of scenarios to ensure the functional specification is sufficiently useful.

### The Production Environment

The production environment is in its own cluster `Prod-cluster`, and consists of 3 different applications:
![Production Environment](Productioncluster.jpg)

The first application is an external facing UI application in the namespace ui-prod.  Its image is in the ui-prod namespace, and named ui. It depends on the service svc-a, and another service svc-b.

The second application is a microservice svc-a that is deployed in namespace a-prod. Its image is in the internal production repository, and in the namespace a-prod. This is a standalone shared service that may be called by other applications as well.  

The third and final application is a microservice svc-b. It is deployed in the namespace b-prod, with a corresponding images svc-b, and helper-b. The pod for svc-b runs both images svc-b and helper-b. 


### Usage Scenarios for ui

The `UI` application example is meant as a continuous deployment sample, where code in the `master` branch is always ready to deploy, and for this example, always deployed immediately. It follows a true cloud-native development process:
-  It uses Github or Github Enterprise as the source repository.  
- It uses the Github branching strategy:
  - Developers are required to create branches for all source code changes.
  - A pull request requires the following status checks before code may be merged back to `master`:
     - code review 
     - test build 
- A test build is triggered automatically for each pull request, and whenever a new commit is added to the pull request. Developer is not allowed to merge code without code review and successful test build. 
- The test build also runs automated tests.
- Developers use Appsody on their local machines as development environment.
- Builds from the `master` branch are automatically triggered when a Pull Request is merged into the `master` branch. If successfully, the new image is automatically deployed to production.


The pipeline is shown below:

![UI Pipeline](UIPipeline.jpg)


#### Usage Scenario for Devops/Solution Architect

For the `ui` application, the developers use appsody as their local development environment. The architect is responsible for creating the Kabanero collection where the standardized appsody stacks are defined, and making the collection active for developer . **TBD**: Get link to docs.

The architect creates the Tekton Pipeline and Tasks for the `ui` application. This involves:
- Using any existing pipelines and tasks that may already be available for building node.js applications as the basis to create a new pipeline.
- Using any existing pipeline tasks for interacting with Github:
  - When build is kicked off, update the Github status check with a warning that build is on-going, to prevent the user from merging code. 
  - When build is successful,  additional Pipeline Tasks are used to install `ui-test`, `ui`, `svc-a` and `svc-b` images, and run the `ui-test` verification test.
- The last step of the pipeline is to record the github status and how to access the build. 
   - successful if all tasks succeed.
   - fail if some tasks fails

For the source repository, the architect:
- Creates the Github organizational functional IDs and webhook 
  - Finds the Kabanero Github event listener URL and secret to receive Github events: 
  - The Github events to be received are:
      - Pull Requests
- Creates the Kuberentes secrets required to access the repositories in the organization, depending on what is supported by the build pipeline, and Github: https://developer.github.com/v3/guides/managing-deploy-keys/. 
- Creates the `ui` repository under a Github organization. 
- Authorizes developers by adding them as collaborators.

The architect configures Kabanero what actions to take for pull request on an `ui` repository:
- Input: Github
  - Repository location: URL for the `ui` repository.
  - Event type: Pull Request. 
  - Option to Record Github status check for test build.
- Output: Docker
  - image location

The architect provides a second pipeline for building from the `master` branch. This pipeline is required to build the official image for production.
- The trigger for the pipeline is a new commit to `master` branch.
- If the build is successful, the new image is tagged as `latest`.
- The image is automatically deployed to production once tagged.


#### Usage Scenario for Developer

Prerequisites for the Developers:
- Developer installs Kabanero Client to local laptop/desktop
- Developer installs Appsody on laptop/desktop.  (**TBD**: Is this a special version that knows where the standardized stacks are, or will it go to Kabanero to get the location)
- For local testing, the example shows re-deployment of svc-a and svc-b for every build. Other options are possible:
  - Use of stubs for svc-a and svc-b, 
  - or a working svc-a and svc-b service for local testing has been set up and made available for developers

The developer sets up a new environment for `ui` development as follows:
- Developer creates a branch and clones the branch into local environment.
- Developer logs into Kabanero
- Developer calls `appsody init Node.js --no-template` to initialize the node.js local development environment
- Developer inner loop:
   - Make code change
  - Call `appsody build`, or `appsody run`, or `appsody debug` as needed.
- Developer periodically pushes changes to the  branch.

When the developer is ready to merge to master:
- Developer creates a pull request, requesting code review from team member
  - A test build is automatically triggered. 
- Developer waits for status change on the Github checks:
   - code review approval
   - test build result
- When all status checks are successful, developer merges the change into `master`

If the test build is unsuccessful, developer receives an email from Github with instructions on how to access the build and tests.  
  - developer is given access to the build pod and test pod.


#### Kabanero Internals

Upon receipt of a PullRequest event from Github, the Kabanero listener posts a Github PullRequest event to the SourceRepository topic.

A built-in SourceRepository consumer listens for events on the SourceRepository topic. 
- It matches the repository URL to the repositories that have been configured. 
- It finds all the actions it is capable of handling for the event. In this case, the action is for starting a Tekton run
- It creates the Tekton Resources for the run, and starts the run.
- **TBD**:  resource management:
   - Number of concurrent runs
   - Garbage collection to remove old runs.

When code is merged into `master`, The Kabanero listener posts a Github Push event to the SoruceRepository topic. (**TBD**: or is it a variation of the PullRequest event?)
- A new run of the pipeline is started

### Usage Scenarios for svc-a

![svc-a Pipeline](SVCAPipeLine.jpg)

The second application, `svc-a`,  is meant to capture a more complex workflow for continuous delivery. In this workflow, code is delivered continuously. However, unlike the `ui` application, it is not continuously deployed to production.  It uses a branching strategy similar to `Git flow`.
- It was developed using Openshift Codeready Workspaces
- It uses Github or Github Enterprise as the source repository.  
- Developers are required to create branches or forks from `develop` branch for all source code changes. There may be many such branches, such as having a separate branch per feature or defect.
- A pull request requires the following status checks before code may be merged to `develop` branch:
   - code review 
   - test build 
- A test build is triggered automatically for each pull request, and whenever a new commit is added to the pull request. Developer is not allowed to merge code without code review and successful test build.
- A test build from the `develop` branch is kicked off from a timer and additional tests run to ensure the quality of the code.  For example, hourly or nightly. (Note: this is not yet shown in the diagram.) 
- Code is not merge to `master` branch from `develop` branch until it is ready for the next release, after system test.
- Code merged into `master` is tagged with version number, such as 1.0.0. 
- The pipeline to the system test is triggered manually for specific commits built from `develop`.

**Note:** This scenario also uses Openshift Pipelines, which is only available starting Openshift 4.

#### Functional Changes to Existing Software:

Kabanero operator changes:
- Install of Kabanero should not install Tekton. It should use pre-existing Openshift Pipelines based on Tekton.

Eclipse Che/Redhat Codeready Workspaces changes:
- Pick up predefined stacks from Kabanero

#### Usage Scenario for Devops/Solution Architect

For the `svc-a` application, the architect is responsible for creating the Kabanero collection where the standardized stacks are defined for Eclipse Che and made available to Eclipse Che. This may include existing stacks already available in Openshift Codeready Workspaces, and any additional.  **TBD**:
- How to ensure Eclipse che/Codeready Workspaces only pick up standardized stacks
- Do we want to prevent developers from creating their own stack?

The architect configures Github in the same way as for `ui` application. The main difference is the creation of `develop` integration branch. Developers may create their own feature or defect forks or branches.

The architect configures Tekton pipelines. The main difference is that there are additional pipeline for builds from `develop` branch, and for system test. Another difference is that many of the existing stacks in Codeready Workspaces can be built using existing source-2-image builders. The s2i step may be incorporated as part of the pipeline. See example here: https://github.com/openshift/pipelines-tutorial


#### Usage Scenario for Developer

The developer creates a workspace in Codeready workspaces:
- Login to Codeready Workspaces
- Select one of the predefined stacks
- Create a new workspace
- Configure the credentials required to access the Github `svc-a` repository
- Create a new branch
- Clone branch from Github

The developer code/debug inner loop:
- Make local code changes directly in Eclipse che
- Run unit test and debugs directly in Eclipse Che
- Push changes  to branch one or more times

The developer flow for creating the pull request is the same as that for the `ui` application.

### Usage Scenarios for svc-b

The purpose of the sample `svc-b` is to capture other scenarios currently out of scope. Possible scenarios, which may require adding additional applications, include:
- custom builds that produce more than one docker image.
- Builds that use the `docker` build strategy in openshift
- Builds that use the `jenkins` build strategy in openshift
- Builds that use the `s2i` build strategy in openshift, but does not use the Tekton pipeline.

**The details of these additional scenarios are TBD.** 

![svc-b Pipeline](SVCBPipeline.jpg)

### Summary of the Usage Scenarios

#### What triggers a new run of pipeline

The following Github/GHE events may be configured to trigger a new run of the pipeline:
- Push: rebuild the repository up to the commits in the push.
- Pull request: Rebuild upstream repository merged with the commits in the pull request.

A new run may also be triggered on a timer. One example is an integration branch that is built nightly.

A new run may be triggered manually, for example, when manual action following approval is required to proceed to the next stage, or for system or long run tests.

A new run may be triggered when one or more dependent images changes. For example, an intermediate stage pipeline is triggered whenever one of its dependent images changes. The devops architect may define what is an `image change`. For example,
- whenever the SHA for the `latest` tag changes
- Whenever a new tag is created that matches a prescribed pattern, such as `3.0.0.1`

A new run may be triggered when the pipeline definition and its dependency changes. For example, 
- When the specification of the pipeline changes.
- When a builder image is modified to incorporate new versions of operating system, and prerequisite software.

#### Promotion to Next Stage

Promotion to next stage is supported via tagging the output image for the next stage followed by a push if required. This may be done:
- via the execution of pipeline (or by Kabanero if the run succeeds??)
- Manually by the administrator.

Customization for the next stage may be required. For now, we delegate these to the next stage pipeline itself. Examples include:
- Changing the values of environment variables

#### Repository Specific Workflows

For Github and Github Enterprise:
- repository specific webhook
- organizational webhooks that covers multiple repositories within the organization.
- Updating pull request status check for builds

#### Support for existing Openshift technologies

**This is currently out of scope.**
- Build technologies:
  - s2i
  - Docker
  - Jenkins
- Runtimes
  - Read Hat runtimes and their corresponding builders. (Note: this likely is the same set of builders as last bullet. )
- Development:
  - Codeready Workspaces
- Pipelines:
  - Openshift Pipelines



<a name="Functional_Specification"></a>
## Events Functional Specification

Kabanero broadcasts relevant events on various topics. This allows for an extensible framework whereby new subscribers may be added to implement additional logic triggered by the events. 

The built-in event topics include:
- SourceRepository topic with web hook related events.
- KabaneroManagement topic with Kabanero Management API call related events.
- KabaneroClient topic with Kabanero API call related events.
- KubernetesAPI topic with Kubernetes API call related events.
- Pipeline topic with events related to pipeline execution.

The built-in event consumers include:
- A pipeline run consumer that listens for SourceRepository events and initiates runs on pipelines.
- A Pipeline status consumer that listens for KabernetesAPI events, and emits Pipeline events.

Additional use cases may be added in the future. For example, 
- Support additional source repositories and pipelines
- Update status of various components to a slack channel
- Sending urgent notification to an administrator

### Event Topics

#### Topic: SourceRepository

The attributes:
- eventName: The name of the event in the repository, currently only `Push` and `PullRequest`.
- repositoryType : type of repository, currently only `Github`
- rawData: The actual JSON object coming from the repository, or a mapping of the data if original data is not JSON

#### Topic: KabaneroManagement

The attributes:
- eventName: one of `list`, `login`, `logout`, `onboard`, `refresh`
- user: The ID of the user who initiated the call
- **TBD**: API specific parameters

#### Topic: KabaneroClient

The attributes of the events are:
- eventName: one of `login`, `logout`, etc
- user: The ID of the user who initiated the call
- **TBD**: API specific parameters

#### Topic: KubernetesAPI

The attributes are:
- eventType: One of `create`, `modify`, `delete`, and `statusChange`
- kind: kind of the resource
- namespace: namespace of the resource, if resource is namespaced
- name: name of the resource
- resource: JSON specification of a Kubernetes resource
- oldResource: for `modify` event type, JSON specification of the old resource.
  
Filter to Kubernetes APIs may be defined using the following custom resource definition. If multiple custom resources are defined, an event for a resource is only emitted if it passes both filters.
```
apiVersion: kabanero.io/v1alpha1
kind: KubernetesAPIFilter
metadata:
  name: apiFilter
  namespace: kabanero
spec:
  allowedNamespace:
      - ns1*
      - ns2
  disallowedNamespace:
      - ns1-test
  allowedAPIGroups:
      - tekton.*
      - kabanero.*
  disallowedAPIGroups:
      - tekton.test
  allowedKinds:
      - Kabanero*
      - Pipeline*
  disallowedKinds:
      - *Test
```

Note: for security reasons, Kubernetes resources for secrets are not stored in the `resource` or `oldResource` attributes. 

#### Topic: Pipeline

The attributes are:
- pipelineType: currently only `Tekton`
- eventType: one of `startRun`, `endRun`, `modifyRun`, `deleteRun`, `statusChange`, `message`
- `message` eventType is a message outside the context of a pipeline run.
   - messageType: type of message, one of `error`, `warning`, `info`
   - messageText: message text associated with the message.
- For `deleteRun`:
  - `name`: name of the run
  - `namespace`: namespace of the run
- For other eventTypes:
  - `name`: name of the run
  - `namespace`: namespace of the run
  - `message`: any message associated with the run.
  - `completionTime`: when the run completed, when available.
  - `triggerMessage`: how the run was triggered, for `startRun`
  - `lastConditionTime`: time of last condition update, if available
  - `conditionMessage`: message for the condition
  - `conditionReason`: reason for the condition
  - `conditionStatus`: status for the condition
  - `conditionType`: type for the condition
  - `resource`: JSON Kubernetes resource. Use this to fetch status of individual TaskRuns.
  - `oldResource: JSON of old Kubernetes resource for `modifyRUn`, if available.
  
Get get the status of the task runs for a PipelineRun, check the Kuburnetes `status` sub-resource embedded in the `resource` field. Here is an example:
```
status:
  completionTime: 2019-08-06T22:23:33Z
  conditions:
  - lastTransitionTime: 2019-08-06T22:23:33Z
    message: PipelineRun "appsody-manual-pipeline-run" failed to finish within "1h0m0s"
    reason: PipelineRunTimeout
    status: "False"
    type: Succeeded
  startTime: 2019-08-06T21:23:33Z
  taskRuns:
    appsody-manual-pipeline-run-appsody-build-b5b72:
      pipelineTaskName: appsody-build
      status:
        completionTime: 2019-08-06T22:23:33Z
        conditions:
        - lastTransitionTime: 2019-08-06T22:23:33Z
          message: TaskRun "appsody-manual-pipeline-run-appsody-build-b5b72" failed
            to finish within "1h0m0s"
          reason: TaskRunTimeout
          status: "False"
          type: Succeeded
        podName: appsody-manual-pipeline-run-appsody-build-b5b72-pod-316a16
        startTime: 2019-08-06T21:23:33Z
```

### Event Consumers

#### PipelineRun SourceRepository Event Consumer

The PipelineRun SourceRepository Event consumer reacts to SourceRepository events to trigger pipeline to run.  Currently it only supports Github repository events and Tekton pipeline.  The steps to define the pipeline and trigger is as follows:
- Define Tekton Pipelines. 
- Define the PipelineTrigger CustomResource Definition.
- Set up a per-repository or organizational web hook.


##### Defining Tekton Pipeline

For automatic trigger from source repository, the pipeline must have 
- one input resource that is a Github repository
- zero or more input resource that is a docker image
- Zero or more output resources that are docker images.

Here is an example of a Pipeline that that has one input from Github, one input docker image, and one output docker image:

```
apiVersion: tekton.dev/v1alpha1
kind: Pipeline
metadata:
  annotations:
     kabanerio.io/stacks: 
         - appsody/nodejs-0.2.2
         - appsody/nodejs-0.2.3
  labels:
    kabanerio.io/stack: appsody.nodejs.0.2.2
  name: appsody-build-pipeline
  namespace: kabanero
spec:
  resources:
  - name: git-source
    type: git
  - name: docker-source
    type: image
  - name: docker-image
    type: image
  tasks:
  - name: appsody-build
    params:
    - name: appsody-deploy-file-name
      value: appsody-service.yaml
    resources:
      inputs:
      - name: git-source
        resource: git-source
      - name: docker-source
        resource: docker-source
      outputs:
      - name: docker-image
        resource: docker-image
    taskRef:
      name: appsody-build-task
```


##### Defining PipelineRunTrigger custom resource

The association between a SourceRepositoryEvent and a Pipeline is defined in a PipelineTrigger Custom Resource. Here is a long example with all options filled in:

```
apiVersion: kabanero.io/v1alpha1
kind: PipelineTrigger
metadata:
  name: my-pipeline-trigger
  namespace: kabanero
spec:
   repositorySelector:
    type: git
    repository: https://github.com/*
    namespace: ${repositoryName}
    triggers:
      - type: cron
        interval: "mm HH DD MM DW"
        branches:
          - "integration"
      - type: SourceRepositoryEvent
        eventNames: 
            - PullRequest
            - Push
        includeBranches:
          - "master"
          - "develop"
          - "*[0-9]+.[0-9]+.[0-9]+.[0-9]+"
        excludeBranches:
          - "*-test"
    pipelineSelector:
      - method: fromSourceRepository
        fileName: ".kabanero-config.yaml"
        fileAttribute: "stack"
        labelName: "kabanerio.io/stack"
      - method: fromSourceRepository
        fileName: ".appsody-config.yaml"
        fileAttribute: "stack"
        annotationName: "kabanerio.io/stacks"
      - method: pipeline
        kind: "Pipeline"
        selector : 
            label: "*nodejs.0.2.*"
            annotation: "*nodejs.0.2.*"
  inputResources:
    - name: docker-source
      value: docker-registry.default.svc:5000/${namespace}/testHelper
  outputResources:
    - name: docker-image
      value: docker-registry.default.svc:5000/${namespace}/${repositoryOwner}-${repositoryName}
  serviceAccount: appsody-sa
  timeout: 1h0m0s
```

Several variables are available when defining namespace of the run, and the docker image URL:
- `repositoryOwner`: The owner of the source repository. 
- `repositoryName`: The name of the source repository
- `repositoryBranch`: the branch of the source repository being built.
- `namespace`: The namespace of the run. Can not be used when defining the namepace.


There are several ways to trigger a pipeline:
- By timer. For our example, it's defined with the syntax for `cron`, and is triggered for the `integration` branch.
- By a SourceRepositoryEvent. For our example, it's triggered by either a PullRequest or a Push event. 
   - The `includeBranches` attribute specifies which branches are allowed. If not is specified, then it assumes "*".
   - the `excludeBranches` specifies which branches should not be built, even if they match the `includeBranches`. For our example, anything that end with "-test". 

Once a the source repository URL and branch are known, they can  be used to find a candidate pipeline to run. There are several ways to find a candidate:
- by matching an attribute contained in a yaml or JSON file on the resource repository. 
  - Our first example matches a YAML file `.kabanero-config.yaml` to values stored in the label whose name is `kabaner.io/stack` in  the pipeline. Note that the string "appsosody/node-js:0.2.2" is not a valid label value in Kubernetes. That's the reason that it is stored as "appsody.node.js.0.2.2".
  - Our second example matches a YAML file ".appsody-config.yaml" to values stored in the anntoation whose name is "kabanerio.io/stacks". The reason for using annotations to store multiple stack names is due to the limitations in the number of characters allowed for a label value.


Several default values are stored in a ConfigMap named kabanero.pipeline.trigger.default.values. These values may be changed by the administrator:
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: kabanero.pipeline.trigger.default.values
  namespace: kabanero
data:
  repositoryType: git
    default-namespace: ${repository-name}
    default-Source-repository-config-files:
      - .kabanero-config.yaml
      - .appsody-config.yaml
    default-allowed-branches: 
      - master
      - deveolop
    default-source-repository-events:
      - PullRequest
      - Push
  default-docker-registry: docker-registry.default.svc:5000
  default-output-url: ${default-docker-registry}/{namespace}/{repository-name}
```

Here is the same example taking all defaults:
```
apiVersion: kabanero.io/v1alpha1
kind: PipelineTrigger
metadata:
  name: hello-world-trigger
  namespace: kabanero
spec:
   repositorySelector:
    repository: https://github.com/some-user/*
  inputResources:
    - name: docker-source
      value: docker-registry.default.svc:5000/${namespace}/testHelper
  serviceAccount: appsody-sa
```

**TBD: It's not yet clear the same pipeline may be used for both Push and PullRequest.**


Generated PipelneRun:

```
apiVersion: tekton.dev/v1alpha1
kind: PipelineRun
metadata:
  annotation:
      kabanerio.io/pipelinetrigger : "my-pipeine-trigger"
      kabanerio.io/triggeredBy: "Push request"
  name: 
  namespace:  my-pipeline-trigger-1
spec:
  pipelineRef:
    name: appsody-build-pipeline
  resources:
  - name: git-source
    resourceRef:
      name: my-pipeline-trigger-1-git-source
  - name: docker-image
    resourceRef:
      name: my-pipeline-trigger-1-docker-image
  serviceAccount: appsody-sa
  timeout: 1h0m0s

```
The PipelineRun must be associated with exactly one PipelineRunResource that points to a Github repository. Here is an example:
```
apiVersion: tekton.dev/v1alpha1
kind: PipelineResource
metadata:
  name: git-source
  namespace: kabanero
spec:
  params:
  - name: revision
    value: master
  - name: url
    value: https://github.com/user/appsody-hello-world/
  type: git
```

```
apiVersion: tekton.dev/v1alpha1
kind: PipelineResource
metadata:
  name: docker-image
  namespace: kabanero
spec:
  params:
  - name: url
    value: docker-registry.default.svc:5000/kabanero/java-microprofile
  type: image
  ```


##### Configuring Webhooks

A github organizational webhook enables the administrator to configure the webhook just once for all repositories within the organization. Per-repository webhook is not required. The instructions are here: 
https://help.github.com/en/articles/configuring-webhooks-for-organization-events-in-your-enterprise-account.

Alternatively, a repository webhook may be configured for a private repository. The instructions are here: **TBD: get link**.