# Kabanero Event Architecture

## Table of Contents
* [Introduction](#Introduction)
* [What is Kabanero?](#What_is_Kabanero)
* [Usage Scenarios](#Usage_Scenarios)
* [Functional Specification](#Functional_Specification)
* [List of Events](#List_Events)


<a name="Introduction"></a>
## Introduction

This document contains the design of events in Kabanero. We start with an introduction to Kabanero and its value as a management infrastructure for cloud native devops. We then present the usage scenarios for Kabanero via three sample applications and their devops life cycle. This is followed by the functional specification to support the usage scenarios. Finally, we list the events that are required to support the functional specification.

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
## Usage Scenarios

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
## Functional Specification

In this section, we will cover the specification of how to create a multi-stage pipelines for a devops workflow.  We will limit the design to only what Kabanero currently supports:
- Github for source control
- Tekton for pipeline 
- Openshift internal docker registry 


## Design Requirements

Below is an example of a multi-stage pipeline to illustrate the design requirements:
![Multi-Stage Pipeline](PipelineStages.jpg)

The requirements specify how the workflow is triggered, and order of execution of the different stages of the pipelines:
- The workflow is divided into stages, where each stage is a different pipeline.
- There is a single top level stage where the workflow is triggered initially. (For example, `build` stage ) The supported triggers include:
  - Github events
  - timer based
  - manual trigger
- Subsequent stages of the pipeline may only be triggered after the successful completion of dependent stage(s). The trigger may be automatic or manual. For example,
   - the `qa` stage is triggered automatically after the `build` stage succeeds.
   - The `staging` stage is triggered automatically after both the `qa` and `system` stages succeed.
   - The `system` stage is triggered manually after successful completion of `build` stage.
- Some stages may run concurrently, for example, the `qa` and `system` stages.


Each stage of the pipeline has its own input and output resources. The specification allows for sharing of these resources where  required. For example, the resource requirements for the above workflow is shown below:

![Resource Dependencies](PipelineResources.jpg)

- The `build` stage:
  -  uses an input resource that is a source repository containing the source code for service  `svc-a`. 
  - Produces an output docker image `svc-a`.
- All subsequent stages make use of the same docker image `svc-a` produced during the `build` stage, consistent with the notion of using an immutable image from build to production. 
- The `system` stage depends on both the `svc-a` image, and on source repository `repo-sve`. This repository contains the source code for the tests that are to be compiled and run against the service `svc-a`. 

## Sample Configuration

A Custom Resource Definition is used to define the stages of the workflow.  For our example, it looks like:

```
apiVersion: tekton.dev/v1alpha1
kind: PipelineWorkflow
metadata:
  name: svc-a
  namespace: kabanero
spec:
  service : "svc-a"
  stages:
      - stage: 
        name : build
        namespace: svc-a-build
        triggers:
            cron: 
                interval: "mm HH DD MM DW"
                branches: 
                    - branch: "master"
                    - branch: "develop"
            input: 
                name: git-source
                    - event: "Push"
                    - includeBranches:
                        - barnch: "master*"
                        - branch: "develop*"
                    - excludeBranches: 
                        - branch: "masterful"
        pipeline: 
           name: appsody-bld
           - input: 
               name: git-source
               repository: http://github.ibm.com/mcheng/svc-a
           - output:
               name: image
               image: svc-a
      - stage: 
        name : qa
        namespace: svc-a-qa
        dependsOn: "stage"
        - input:
            name: test-image
            image: test-a
        - input:
             name: application-image
             stage: build
             output: image
      - stage: 
        name : system
        namespace: svc-a-system
        dependsOn: "build"
        autoTrigger: false
        - input:
            name: git-source
            repository: https://github.ibm.com/mcheng/svt
            branch: "master"
      - stage: 
        name : system
        namespace: svc-a-staging
        dependsOn: "qa, system"
        - input:
            name: svc-b-imge
            image: svc-b
        - input:
             name: application-image
             stage: build
             output: image
```

Here is a pipeline just for PullRequest test builds:
```
apiVersion: tekton.dev/v1alpha1
kind: PipelineWorkflow
metadata:
  name: svc-a-pr
  namespace: kabanero
spec:
  service : "svc-a"
  stages:
      - stage: 
        name : build
        namespace: svc-a-build
        triggers:
            input: 
                name: git-source
                    - event: "PullRequest"
                    - includeBranches:
                        - barnch: "master*"
                        - branch: "develop*"
        pipeline: 
           name: appsody-bld-pr
           - input: 
               name: git-source
               repository: http://github.ibm.com/mcheng/svc-a
           - output:
               name: image
               image: svc-a
```


To initiate a new run manually, use the following custom resource:
```
apiVersion: tekton.dev/v1alpha1
kind: PipelineWorkflowRun
metadata:
  name: svc-a-run-1
  namespace: kabanero
spec:
  workflow: svc-a
  input: 
      name: git-source
      branch: "master"
```

**TBD:** Post condition for a stage:
- Can enhance the design to allow post-conditions. For example, manually marking the stage as completed.

## Configuring Webhooks

A github organizational webhook enables the administrator to configure the webhook just once for all repositories within the organization. Per-repository webhook is not required. The instructions are here: 
https://help.github.com/en/articles/configuring-webhooks-for-organization-events-in-your-enterprise-account.

Alternatively, a repository webhook may be configured for a private repository. The instructions are here: **TBD: get link**.


<a name="List_Events"></a>

## List of Events

### Topic: Pipeline

The name of the topic is "Pipeline"

The attributes are:
- pipelineType: the type of pipeline, currently only `Tekton`
- action: action performed, one of:
  - `started`: a new run has started
  - `completed`: a run has completed. 
  - `deleted`: a run has been deleted.
  - `statusUpdate`: The status of a run has changed.
- name: the name of the run, should be a Kubernetes resource name. 
  - For Tekton, it's the name of the pipeline run.
- details: pipeline independent details related to an action
   - for action == `started`, what triggered the new run:
     - manual: manual trigger + who triggered it.
     - repository: details of change to the repository
     - dependentImage: dependent images changed + details of change to the image
     - PipelineSpecification: the specification of the pipeline has changed. This includes the pipeline specification itself + changes to images in the pipeline infrastructure.
   - for action == `completed`:
     - completion status: succeeded, failed, timed out
   - for action == `deleted`:
     - reason for deletion if known
       - for example, garbage collection
   - for action == `statusUpdate`:
       - **TBD**: pipeline neutral status update??

**TBD:** Kabanero emitted events, such as how it matches repository to a pipeilne.

### Topic: SourceRepository

The name of the topic is `SourceRepository`.
The attributes of the events are:
- repositoryType : type of repository. 
- eventName: The name of the event in the repository
- eventData: The actual JSON object coming from the repository, or a mapping of the data if original data is not JSON

Currently, these are supported:
- repositoryType:  only github
  - eventName:
    - Push
    - PullRequest

### Topic: ImageRepository

**TBD:** Still need to work out the details.
The name of the topic is "ImageRpository". The attributes of the event are:
- repositoryType: type of image repository. Currently `docker`.
- eventName: name of the event
      - `tagged`: an image has been tagged
      - `pushed`: new image has been pushed.
      - `updated`: Image updated with a new SHA
- repositoryLocation  location of the image
- imageName: name of the image
- eventData: The actual JSON object associated with the event, or a mapping of the original data to JSON if the original data is not JSON
    - for docker, at least SHA for the image.


### Topic: KabaneroAPI

Audit of calls to Kabanero API

 For Kabanero-mgmt:
- list
- login
- logout
- onboard
- refresh

**TBD**: How about client side CLI?

**TBD**: any other API? How about any that may be called by Appsody?