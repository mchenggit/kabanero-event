# Kabanero Event Architecture

## Table of Contents
* [Introduction](#Introduction)
* [What is Kabanero?](#What_is_Kabanero)
* [Kabanero High Level Usage Scenarios](#Usage_Scenarios)
* [Introduction to Event Triggered Pipeline Strategies](#Event_Intro)
* [Events Functional Specification](#Functional_Specification)
* [Staging](#Staging)


<a name="Introduction"></a>
## Introduction

This document contains the design of events in Kabanero. We start with an introduction to Kabanero and its value as a management infrastructure for cloud native devops. We then present the usage scenarios for Kabanero via three sample applications and their devops life cycle. This is followed by the functional specification of events to support the usage scenarios. 

<a name="What_is_Kabanero"></a>
## What is Kabanero?

The goal of Kabanero is to manage cloud native devops infrastructure. Such an infrastructure may contain many components (though not all are in the current Kabanero roadmap) :
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

Kabanero supports separation of responsibilities. It allows a developer to concentrate as much on coding/testing inner loop as possible. A developer
- can stand up a standard development environment very quickly. For example, a developer may create a new stack from Appsody or Eclipse Che within minutes.
- Can rely on the environment to automatically trigger test builds before committing change into source control, with logs made available to the developer.
- Can rely on the environment to trigger additional release builds and run additional tests, and to be able to debug in the test environment when required.

At the same time, Kabanero allows a devops/solution architect to maintain and control the infrastructure to be made available the devops environment. The architect can define:
- Standardized development environments. For example:
  - Which stacks and versions are available for Appsody
  - Which stacks and versions are available for Eclipse Che or Openshift Codeready Workspaces.
- How to push an application/service through different build/test stages,  from source control repository to production. The architect defines:

  - What actions to take when source code is checked into source control. 
  - which Pipelines to execute
  - how to promote to the next stage of the pipeline

Kabanero automates the devops infrastructure as much as possible. It will
- offer easy to use defaults.
- Use organizational web hook to process events for all repositories within the organization when configured.
- Install and run the pipelines automatically when configured.
- Trigger new builds if the stack has been updated, for example, with security fixes.

Kabanero is secure. It supports:
  - login with different identity providers, for example, login with Github user ID, or with internal user registry
  - Role Based Access Control(RBAC) for different roles, for example, role of devops architect, and role of application developer.
  - security audit
  - automatic security scans for source code and binaries.

Kabanero is extensible. It can be enhanced to support:
- new development environments with new ways to create stacks,
- new source control repositories
- new pipeline technologies
- additional behavior based on internal events.

In an enterprise where there many be hundreds or even thousands of applications,  it is not the case that every devops team is in charge of all stages of an application, though it may be for some teams. It is not the case that the same source repository branching or pipeline strategy applies to all application. And it is not the case that every application uses the same continuous integration tools. The value of Kabanero is to give the devops/solution architect a way to define the processes required to manage such disparate environments. Note that having a process is not antithetical giving teams control. Well defined processes are still required when a team has full control. Examples of such processes include:
- Deploying latest operating system or software prerequisite security fixes.
- Compiling and running only with per-certified prerequisites.
- When and how to promote an application to the next stage.
- How multiple interdependent applications work with each other.
- How to roll out an application, e.g. helm, Gitops, or  Razee.
- Creation and configuration of the Kubernetes namespaces to run the pipelines and host the different stages of applications.

The initial version of Kabanero will support:
- Appsody as development environment
- Github for source repository 
- Tekton for pipeline

In order to ensure the design is extensible,  the remainder of this document covers more than one development environments, source control repositories, and pipeline technologies, even though some may not be in the current Kabanero roadmap.

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
- Developer installs Appsody on laptop/desktop.  
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

When code is merged into `master`, The Kabanero listener posts a Github Push event to the SoruceRepository topic. 
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

The architect configures Github in the same way as `ui` application. The main difference is the creation of `develop` integration branch. Developers may create their own feature or defect forks or branches.

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


<a name="Event_Intro"></a>
## Introduction to Event Triggered Pipeline Strategies

Kabanero enables a devops/solution architect to define the processes to manage a devops environment. At the center of this architecture is an event triggered pipeline strategy.

###  Motivating Scenarios

The diagram below is meant to illustrate a variety of scenarios that are enabled via Kabnaero.  Note it is not meant as a prescription for how to construct specific pipeline topologies:


![Motivating Scenarios ](MotivationScenarios.jpg)

First, definitions:
- A `strategy` defines a single high level pipeline, consisting of one or more `stages.
- A `strategy` is triggered by an event to its starting `stage`. For example, the `stage` Build is triggered by the `Start_build` event.
- Each of the internal `stages` is triggered by an event internal to the strategy.
- Each `stage` of strategy is implemented by some other tool,  such as a Travis build (not yet supported by Kanabero), or a Tekton pipeline.  
- Though not shown in the diagram, each stage may contain input and output parameters.

The scenarios depicted in the above diagram include:
- Parallel execution of stages, such as `FVT` and `SVT` stages of strategy `Svc-A`.
- Manual approval before promotion to next sage, such as `Deploy_approval`.
- Error handling, such as `FVT_Error` stage. Error handling may include creating Github issues, or sending other messages or notifications.
- Interaction between different applications.  For our example, the application `myapp` depends on service `Svc-A`.  The owners of `Svc-A` have set up an integration environment deployed with the latest non-production `Svc-A` for integration testing. 
  - When a new version of the service `Svc-A` is deployed to the integration environment, the event `Integration_A` is sent. This allows all interested parties to react to the event.  In our example, it triggers the strategy `myap-retest` to run to re-test `Svc-A`. The result of rerunning the test may trigger additional events to be emitted.
- Usage of different tools for each stage.

### Sample Two Stage Build

![Two Stage Strategy](TwoStageBuild.jpg)

This section contains a walkthrough for how to configure a Two stage strategy.

#### Prerequisites

When a collection is installed,  strategy CRD instances are also installed. These instances are applied and available in Kubernetes. For our example, they include:
- Definition of the `Build` stage
- Definition of the `Deploy` stage
- Definition of of the `two_stage` strategy


The definition of the `Build` stage uses the `TektonStage` custom resource. The `TekonStage` custom resource allows Kabanero to use a Tekton pipeline for a stage. it defines:
- the underlying Tekton pipeline to use
- triggering event
- output event

The input and output resources are the same as the pipeline. Pre-defined variable  `kabaoer.stage.status` contains the status of the stage.


```
apiVersion: kabanero.io/v1alpha1
kind: TektonStage
metadata:
  name: appsody_build
  namespace: kabanero
spec:
  pipeline: appsody_build
  pipeline-namespace: kabanero
  trigger:
    - Start_build: 
  emits:
    name: Build
    status: ${kabanero.stage.status}
```

The `Deploy` stage is a hypothetical custom stage provided by the user. A custom stage has its own CRD, but must provide input/output resources, event trigger definitions, and events it emits.

```
apiVersion: mygroup/v1alpha1
kind: MyDeployStage
metadata:
  name: my-deploy
  namespace: kabanero
spec:
  resources:
    input:
      - name: image
        type: docker
  trigger:
    - name: Build
      status: Success
  emits:
    - name: Deploy
      status: ${kabanero.stage.status}
```

The StrategyDefinition CRD defines a strategy instance.  For our example, it includes:
- strategy wide variables and their types: `source`, and `app`.
- stages to incorporate. In our example, it is the build and deploy stages.
- mapping of strategy wide resources to stage resources.

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyDefinition
metadata:
  name: two-stage
  namespace: kabanero
spec:
  - strategy-variables:
      - name: "stack"
        type: "string"
      - name: "source"
        type: "Github"
      - name: "app"
        type: "image"
  - stage-bindings:
    - stageName: appsody_build
      stageKind: TektonStage
      stage-local-name: Build
      resourceBindings:
        - inputBinding:
            - stageVariable: stack
              strategyVariable: stack
            - stageVariable: source
              strategyVariable: source
        - outputBinding:
            - stageVariable: image
              strategyVariable: app
    - stageName: my-deploy
      stageKind: MyDeployStage
      stage-local-name: Deploy
      resourceBindings:
        - inputBinding:
            - stageVariable: image
              strategyVariable: app
```



Triggers and variable substitutions and resources are used to define when and which strategy to apply. For our example, the trigger looks like:

```
- event: Push
  allowedBranches: master
  resourceDirectory: strategies/pushMaster
- event: Push
  allowedBranches: *
  disAllowedBranches: master
  resourceDirectory: strategies/pushNonMaster
- event: PullRequest
  allowedBranches: master
  resourcesDirectory: strategies/pullRequestMaster
- variables:
  - hello-world.stack: "kabanero/node-js:0.2"
```


Each of the `resoucreDirectory` contains the resources to be applied to implement the strategy, with appropriate variable substitution. For example, the directory strategies/pushMaster may contain a StrategyRun resource used to initiate a run of a strategy:
```
apiVersion: kabanero.io/v1alpha1
kind: StrategyRun
metadata:
  name: myapp-two-stage-${kabanero.context.ID}
  namespace: mayapp-strategy
spec:
  strategy: two-stage
  variables: 
    - stack: "${hello-world.stack}"
    - name: source
      url: ${kabanero.repository.url}
      revision: ${kabanero.repository.branch}
    - name:  image
      location: ${kabanero.default.registry}/my-app
  emit:
     name: Start_Build
```

See discussions later about whether triggers,  variable substitutions, and resoruces should be stored in the source repository or with the collections.

#### Register Web hook

Create and apply GithubEventSource.yaml to register a web hook. This example uses a repository web hook. **TBD: This is to be implemented in Tekton.**

```
apiVersion: tekton.dev/v1alpha1
kind: GithubEventSource
metadata:
  name: user-hello-world-github
  namespace: kabanero
spec:
    url: https://github.com/user/hello-world
    apiSecret: user-hello-world-github-api
```

#### Web hook Processing

Tekton Web hook listener receives web hook POST operation.  Kabanero plugin intercepts the event and emits Push event to the `/kabanero/instance/repository/github/org/repository` topic.


####  Push event Processing

Kabanero listener receives the Push event, locates the trigger file, then fetches the StrategyRun resource.  The resource is applied after variable substitution. The actual StrategyRun resource applied may look like:

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyRun
metadata:
  name: my-app-strategy-201909030817000
  namespace: mayapp-strategy
spec:
  strategy: two_stage
  variables: 
    - stack: "kabanero/node-js:0.2"
    - name: source
      url: https://github.com/user/hello-world
      revision: master
    - name:  image
      location: docker-registry.default.svc:5000/my-app
  emit:
     name: Start_Build
```


### Running the Strategy

The controller for StrategyRun finds the corresponding StrategyDefinition `two_stage`, and creates the corresponding StageRuns. For our example, there are two stages:

```
apiVersion: kabanero.io/v1alpha1
kind: StageRun
metadata:
  name: appsody_build_201909030817000
  namespace: kabanero
spec:
  stageName: appsody_build
  stageKind: TektonStage
  stage-local-name: Build
  contextID: 201909030817000
  strategy_run: two_stage_201909030817000
status:
  state: in-progress
```

The Controller for TektonStage processes the creation of the StageRun object as follows:
- Ensure that the stageKind is `TektonStage`, otherwise the resource is ignored.
- Locate the `TektonStage` whose name is `appsody_build` and register to receive trigger events `Start_Build`.


```
apiVersion: kabanero.io/v1alpha1
kind: StageRun
metadata:
  name: my-deploy-201909030817000
  namespace: kabanero
spec:
  stageName: my-deploy
  stageKind: MyDeployStage
  contextID: 201909030817000
  strategy_run: two_stage_201909030817000
status:
  state: in-progress
```

The Controller for MyDeployStage processes the creation of the StageRun object as follows:
- Ensure that the stageKind is `MyDeployStage`, otherwise the resource is ignored.
- Locate the `MyDeployStage` whose name is `my-deploy` and register to receive trigger events `Build(success)`.


After all the StageRun resources are created, the controller emits the `Start_build` event to trigger the first stage.


### Running the Stages

The controller for each stage is responsible for:
- Creating listeners to listen for trigger events after `StageRun` resource is created.
- When triggered, creating any additional resources, and run the stage.
- When completed, emit any configured events.

For our example, the controller for TektonStage  
- Creates a listener to listen for `Start_build` event after the StageRun resource is created.
- When `Start-build` event is received, creates Tekton related resources to start the pipeline:
   - `PipelineResource`
   - `PipelineRun`
- When the Tekton pipeline completes, emits `Build` event.

The controller for MyDeployStage
- Creates a listener to listen for `Build(success)` event after the StageRun resource is created.
- when `Build(sucess)` event is received, performs whatever custom action is needed.
- Upon completion, emit the `Deploy` event.


The controller for the StrategyRun monitors the status for each stage, and updates the overall status within the StrategyRun resource.

### Other Usage Scenarios

To start a strategy manually, create a StrategyRun resource and apply it. For example:

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyRun
metadata:
  name: my-app-strategy-test-run
  namespace: mayapp-strategy
spec:
  strategy: two_stage
  variables: 
    - stack: "kabanero/node-js:0.2"
    - name: source
      url: https://github.com/user/hello-world
      revision: master
    - name:  image
      location: docker-registry.default.svc:5000/my-app
  emit:
     name: Start_Build
```

To start a strategy on a timer, create a `cron` based StrategyTrigger. This trigger may be stored with the collection and started automatically. For example, to do hourly build from master branch of myapp:
```
apiVersion: kabanero.io/v1alpha1
kind: StrategyTrigger
metadata:
  name: hourly-build-myapp
  namespace: kabanero
spec:
  cron: "00 * * * *"
  resourcesDirectory: myapp/strategies/buildMaster
```


To start a strategy based on an event, use an event based StrategyTrigger. For example, to re-run FVT of myapp based on Integration_A event:
```
apiVersion: kabanero.io/v1alpha1
kind: StrategyTrigger
metadata:
  name: myapp-svc-a-trigger
  namespace: kabanero
spec:
  - trigger:
    - name: Integration_a
  - resourcdeDirectory: myapp/strategies/rerun-fvt
  - variables
    name1: ${kabanero.event.attribute1}
    name2: ${kabanero.event.attribute2}
```


### Discussion on Where to Store Strategy Parameters for a Repository

The strategy parameters are the definitions of triggers, variable substitutions, and resources required initiate a strategy based on operations to the source repository. Where these parameters are stored have implications on how to support these scenarios:
- Reverting to an older commit. For example, reverting from 2.0.10 to 2.0.8
- Creating a branch based on an old commit. For example, creating a branch based on 2.0.5.

The first option is to store the parameters within the source repository. This is the easiest way to support the above scenarios. Reversion or creating from an old commit simply returns the repository to the older version of the parameters.

The second option is to store a pointer within the source repository, with the parameters stored within the collection. The main differences with this approach are:
- less information is stored in the source repository.
- Updates to the parameters requires updating two places:
  - First, update the collection and create a new pointer
  - Second, update the source repository with the new pointer.
- Old pointers have to remain valid after reversion.

The third option is to store the parameters outside of the source repository with additional bookkeeping to identify which parameters are suitable for which branches/commits of the source repository. This option bypasses using the source repository to maintain history, is the most difficult to implement, but has the advantage of isolating the management of the parameters outside of the source repository.

The recommendation is to store the parameters with the source repository until there is a strong requirement for isolation. Existing CI/CD tools such as Travis, Jenkins, Gitlab, Azure Pipelines store configuration within the source repository. 

### Discussion on Forked Repository

When a repository is forked, strategy parameters are forked as well. These parameters are not applicable to the forked repository. If code changes in the forked repository only run through unit tests in the developer environment, then is not an issue.  After unit testing, a pull request back to the original repository may be used, and it works the same way as a pull request from a branch in the source repository.

If the requirement is to run separate strategies for the forked repository, then it can be done as follows:
- A new web hook listener is created for the forked repository. 
- New strategy parameters are created for the forked repository, stored in a non-default location to not conflict with the original.
- PullRequests may:
   - cherry-pick only those commits related to source code changes, or
   - include strategy parameters  changes at non-default location if the owner of original repository allows it. This reduces the overhead of having to cherry-pick commits commits.


### Discussion on Semantic Versioning 

The requirements are:
- Enable developers to use semantic versioning to automatically pick up new stacks
- Stacks may be updated without informing the developer.
- Allow broken projects time to get fixed.

The first option to support above scenarios is to allow developers to use semantic versioning in the development environment, but to use exact version in the strategy parameters. The process to update to a compatible stack is as follows:
- For each project, create an "update" branch, and change the stack version number, e.g., from 0.2.2 to 0.2.3.
- Create a Pull Request, and let the update strategy run. 
- If the strategy succeeds, merge takes place, and the project is now at the updated version.
- If the strategy fails, an issue is created, and additional code added to the PR until it succeeds.
- Upon completion of all projects, the stack 0.2 is updated to be the same as 0.2.3 so that developer automatically picks up the latest.

Note that when reverting back to an old commit, it also reverts back to the old version.  To move up to the latest, a new PR is needed.


The second option is to use semantic versioning for the strategy parameters as well. The process is as follows:
- For each project, create an "update" branch, and change the stack version number to an exact version, e.g., from 0.2 to 0.2.3.
- A "update" strategy is run against the "update" branch.
- For each application where the strategy fails :
   - Fix the code first in the "update" branch for "0.2.3"
   - use PR to merge source code changes back to the main branch.
- Upon completion of all projects, the stack 0.2 is updated to be the same as 0.2.3 so that developer and strategies automatically picks up the latest.

Note that when reverting back to an old commit, semantic version still takes place for the strategy.

<a name="Functional_Specification"></a>
## Events Functional Specification

Kabanero hosts an event infrastructure to allow system components to communicate with each other asynchronously. This enables an extensible framework whereby event topics, producers, and consumers may be added to implement additional system level function. 


One key component enabled by events is the Devops Strategy, which allows the devops architect to link different stages via events to form a larger strategy. We will start with a sample usage scenario for a multi-stage devops strategy, and show how the stages are configured.  This is followed by more detailed design.

### Definitions

A Kabanero `installation` is a set of Kubernetes resources used to host the Kabanero runtime. **TBD: Whether to support multiple installs in the same cluster.** This includes:
- Custom resource definitions
- namespaces
- deployed containres
- security policies
- other co-installed infrastructure, such as Tekton and Kafka.


One Kabanero `installation` may support multiple `instances`. Each instance provides an isolated devops environment. An `instance` is consist of
- A unique name
- A collection repository
- A web hook listener URL
- a set of workspaces.

A workspace defines the devops execution environment for one or more related applications. It contains
- A set of namespaces
- Security policies for the resources created in the namespaces
- Other Kubernetes resoruces.


### Message Format

Kabanero uses JSON as the event message format, and JSON path when filtering events.


### Webhook Setup

**TBD: This part of the design been changed, and will depend on Tekton webhook listener**.

To set up web hooks to github, create and apply a GithubEventSource. Here is an example for setting up an organizational web hook:

```
apiVersion: kabanero.io/v1alpha1
kind: GithubEventSource
metadata:
  name: myorg-github
  namespace: kabanero
spec:
    url: https://github.com/enterprises/myorg
    apiSecret: myorg-github-api
```

Here is an example to set up a per-repository web hook to Github as an event source. 

```
apiVersion: kabanero.io/v1alpha1
kind: GithubEventSource
metadata:
  name: user-hello-world-github
  namespace: kabanero
spec:
    url: https://github.com/user/hello-world
    apiSecret: user-hello-world-github-api
```

The status of the event source shows whether the web hook was configured. For example:
```
apiVersion: kabanero.io/v1alpha1
kind: GithubEventSource
metadata:
  name: myorg-github
  namespace: kabanero
spec:
    url: https://github.com/enterprises/myorg
    apiSecret: myorg-github-api
status:
  configured: true
  message: "web hook created for https://github.com/entgerprise/myorg with web hook URL: https://my-openshift-cluster/kabanero/webhook using secret  defaul-kabanero-webhook-secret"
```

### Kabanero web hook Listener

the Kabanero web hook listener plugs into the Tekton web hook listener. When the Tekton listener receives POST events from a repository, the Kabanero listener emits SourceRepository events.

For Github, the topic path is: /`instance`/repository/github/`url`/`user`/`repository`, where
- `instance` is the name of the kabanero `instance`.
- `url` is the URL of the repository, such as `github.com`.
- `user` is the name of the user or organziation for the repository
- `repository` is the name of the repository.

An example for a topic path:  `/kabanero/repository/github/github.com/myorg/hello-world`.

For a `Push` request, the event looks like:
```
{
  "repositoryType": "github",
  "eventName" : "Push",
  "user" : "myorg",
  "repository": "hello-world",
  "location" : "https://github.com/myorg/hello-world",
  "branch" : "master",
  "commit" : "1234567",
- "rawData": JSON of the request
}
```



### Four Types of Repositories

Kabanero recognizes these four types of repositories:
1) Source repository created via Appsody
2) A Collection repository storing collections of stacks + strategies
   - Kabanero moving to a new version of the collection
   - Collection repository being updated and tested
3) source code repository not created via Appsody
4) multiple interdependent services, each may be created via 1 or 3.

#### Source code repository created via Appsody

For this scenario:
- The source repository contains the name and version of the strategy to use. For example, main-strategy:1.0
- the source repository contains the name and version of the stack, e.g., appsody/node-js:0.2.2
- There is an exact match from the strategy/stack to the configured collection.

The resources for a strategy are applied when it has been triggered.
**TBD: Garbage collection?**

Note: For strategy triggers that are organization or repository specific, put them in a separate repo. They are applied during initialization of Kabanero. A "Strategy Trigger" strategy can be used to update the triggers when it changes. 

No semantic versioning
  - Use Pull Request to change exact version. (Can be automated.)
  - Won't accidentally go back to old version if available version changes.
  - Can easily reproduce the original build on rebuild.
  - Applications that fail on new version can be fixed at their own pace by adding code to Pull Request or moving to another version.

Can provide pre-defined strategies. For example,
- One-stage strategy
   - Build-only
   - calling other CI/CD, such as Jenkins Strategy
- Github branching strategy
- Gitflow branching strategy
- Jenkins-X strategy: Jenkins-x Dev, Jenkins-X stage, Jenkins-X prod

Jenkin-X uses Github repositories and PullRequest to manage devops stages and stage promotions. 

![Jenkis X ](JenkinsX.jpg)

Above is an example of the three stages of a typical Jenkins-X pipeline.
- The Pull Request for dev stage triggers a build. If the build is successful, a Pull Request is made against the Staging stage, modifying the version number of the application in a configuration file.
- The Pull Request for the Staging repository triggers a deployment and test of the given application version.  If successful, a the version number is updated for the Prod stage, and a new PullRquest is created.
- The PullRequest for the prod stage is generated, and triggers a pre-production test ( if needed).   If successful, and the PullRequest is merged, the final pipeline deploys to production.


#### Lifecycle of a collection

Logically, a collection is the union of all stacks and strategies. **It is immutable once it has been put into service.** Only operation for pre-existing stacks/strategies is activate/deactivate. This makes it possible to provide consistent rebuilds. (This assumes that the old containers for the pipelines are also versioned and available, and that the Kabanero runtime is backwards compatible.)


To put an updated collection into service:
- On Push , "Collection Update strategy" can be used to load the updated collection. 
- If  deactivating some strategies/stacks:
    - Update all affected apps to use a  different strategy/stack by changing their dependent strategy/version via a Pull Request (can be automated)
    - Wait for all updates to complete.
- Deactivate old strategy/collection.


To test a collection during development:
- Provide a special "Kabanero collection testing" strategy.  
- Upon "Pull Request" of the collection, the Kabanero testing strategy takes over to run tests:
   - Create a new "workspace" and point it to the collection being tested
   - run tests using the collection within the new workspace 

Each workspace contains its own:
- collection repository
- web hook listener
- **TBD: namespaces for running the pipelines.**

#### Source code repository not created via Appsody (Custom)

The strategy and "stack" to be used is stored within the source repository. Pre-defined custom strategies may be created as well. For example, a "s2i" related stack and strategy. 

**TBD: Since Appsody & s2i are really just about builds, should create a "build pack" stage that for all types of source repositories.  This allows creation of strategy independent of how source is built**


#### Two Service Pipeline

There are multiple ways to configure an application and test its dependent service(s). Below is an example showing one service dependent on another service:

![Pipeline for TwoServices ](TwoServicePipelines.jpg)

Note that: 
- svc-a is the dependent service.  As part of its pipeline, it deploys the service in an integration environment for integration testing.
- The application that depends on the service has its own pipeline as well.  Its FVT stage makes use of svc-a that is deployed to the integration environment.
- Whenever the deployment of the service to the integration environment changes, a new message `Integration_A` is emitted.  
- In our example, the message triggers a new run of the application's FVT.
- The result of the run is broadcast as another message.  This is used to inform interested parties whether or not the latest deployment of the svc-a  is stable.  

Note that there other other ways to configure the dependencies between the application and its service:
- The Integration stage of the svc-a pipeline may be the one to emit the message
- The processing of the Integration_A event for the application, if using repository for promotion, is to to create a PullRequest to update the FVT repository.



### Sample Multi-stage Devops Strategy

Shown in the diagram below is a sample Devops Strategy involving multiple stages: 
![StateGraphWithResources](StateGraphWithResources.jpg)


The sample strategy is defined by the devops architect, and is consist of:
- A build stage that performs an appsody build
- If the build succeeds, two additional stages are run in parallel: FVT and SVT, which are both Tekton pipelines.
- Once both stages complete successfully, a `Deploy_Approval` stage is used to get manual approval. This stage is implemented via custom code.
- Once approved, the `Deploy` stage is used to perform deployment via another Tekton pipeline.

Each of these stages is triggered by one or more events, and emits additional events upon completion of the stage. Note that:
- Parallel operations are possible when one event trigger more than one subsequent stages, such as FVT and SVT.
- A stage may be triggered by more than one event, supporting "AND" relationship, such as Deploy_Approval.
- Currently  the `OR` relationship is not yet defined, where a stage may be triggered by one of many events. 
- Circular loop across stages is also not yet undefined.
- Though not shown, compensation stages may be defined by with events for failed stages.


Each stage defines a set of input and output resources. Variables at the strategy level may be used to bind to resources at each of the stages. For example,
- the `source` variable resolves to the input repository of the `Build` stage.
- the `app` variable resolves to the output resource of the `Build` stage, and input resources of the `FVT`, `SVT`, and `Deploy` stages.
- The `email` variable resolves to the input of the `Deploy_Approved` state.
- The `app_tagged` variable resolves to the output resource of the `Deploy` stage.

A strategy is activated by defining external events to trigger its execution. In our sample, it is activated by Push events created as a result of push to a github repository.  Each Push event initiates a new instance of the strategy.

### Configuring Sample Strategy

Another way to express the sample strategy is via a state diagram that look like:
![State Diagram](StateDiagram.jpg)


The devops architect defines the strategy as follows:
- Choose the lower level processes that will make up each stage. Examples include Tekton pipeline, and custom code.
- Define the strategy event triggers for each stage, and the output strategy events
- Define the mapping of strategy wide variables to per-stage variables.

The devops architect activates the strategy by defining external event triggers (such as Push), and mapping of external events to actual values in strategy side variables.


#### Incorporating Stages into a Strategy

A `stage` implements a specific devops operation, may contain input/output resources, and is configured via a custom resource. Kabanero comes with a set of pre-defined stages, which include:
- A stage to perform appsody build
- A stage to run arbitrary Tekton pipeline

 In addition, the framework is extensible to allow custom stages to be deployed.  The devops architect incorporates the stages needed to implement the strategy.


For our sample, the architect picks the following stages:

##### Build stage

The build stage uses the following built-in StageDefinition:

```
apiVersion: kabanero.io/v1alpha1
kind: AppsodyBuild
metadata:
  name: appsody_build
  namespace: kabanero
spec:
  resources:
    input:
      - name: source
        type: github
    output:
      - name: image
        type: docker
```

The implementation of the Appsody build stage does the following:
- Fetch `appsody-config.yaml` from the `source` repository
- Find the value of the `stack` attribute in `appsody-config.yaml`, for example, `appsody/node-js:0.2.2.`
- Find matching collection using exact match
- Find build pipeline for the given stack that matches `pipeline` variable.
- Builds the application image and stores it under `image`

**TBD:**: If there are more than 1 build pipeline per collection, will need a new variable to track which pipeline to use.

##### FVT stage

The FVT stage uses a Tekton pipeline stage that is not associated with a collection.  It specifies the name and namespace of the pipeline. The input/output resources of the stage are the same as the pipeline. 

```
apiVersion: kabanero.io/v1alpha1
kind: TektonStage
metadata:
  name: FVT
  namespace: kabanero
spec:
  resources:
    input:
      - name: image
        type: docker
  pipelineName: TEST
  pipelineNamespace kabanero
```

##### SVT Stage

The SVT stage makes use of another Tekton pipeline.  

```
apiVersion: kabanero.io/v1alpha1
kind: TektonStage
metadata:
  name: SVT
  namespace: kabanero
spec:
  resources:
    input:
      - name: image
        type: docker
  pipelineName: SYSTEM_TEST
  pipelineNamespace kabanero
```

##### Approval Stage

For the purpose of this sample, the Approval Stage is a custom stage  installed by the devops architect. It requires an input variable `approvers`, for the email address of the approvers. Its function is to send email to list of approvers, and to update the status of the approval once one of the approvers replies with approval.

```
apiVersion: kabanero.io/v1alpha1
kind: EamilApprovalStage
metadata:
  name: deploy_approval
  namespace: kabanero
spec:
  resources:
    input:
      - name: approvers
        type: string
```

##### Deploy Stage

The Deploy stage makes use of another Tekton pipeline:

```
apiVersion: kabanero.io/v1alpha1
kind: TektonStage
metadata:
  name: deploy
  namespace: kabanero
spec:
  resources:
    input:
      - name: app
        type: docker
    output:
      - name: app_tagged
        type: docker
  pipelineName: Deploy
  pipelineNamespace kabanero
```


#### Defining a Strategy 

To describe how the different stages are to work together, the devops architect creates the StrategyDefinition, which contains:
- strategy wide variables 
- Binding of strategy wide variables to per-stage resources
- Event triggers for each stage
- Events emitted for each stage.

Here is an example:

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyDefinition
metadata:
  name: main
  namespace: kabanero
spec:
  - strategy_variables:
      - name: "src_repo"
        type: "Github"
      - name: "app_image"
        type: "image"
      - name: "app_image_tagged"
        type: "image"
      - name: approver_emails
        type: string
  - stage_bindings:
    - stageName: appsody_build
      stageKind: AppsodyBuild
      resourceBindings:
        - inputBinding:
            - stageVariable: source
              strategyVariable: repo
        - outputBinding:
            - stageVariable: image
              strategyVariable: app
      triggers:
          - attribute: name
            value: "Start_Build"
      emit:
        - event:
          - attribute: name
            value:  Build
          - attributge: status
            value: $stage_status
    - stageName: FVT
      stageKind: TektonStage
      resourceBindings:
        - inputBinding:
           - stageVaraible: image
             strategyVariable: app_image
      triggers:
          - attribute: name
            value: "Build"
          - attribute: status
            value: "Success"
      emit:
        - event:
          - attribute: name
            value:  FVT
          - attributge: status
            value: $stage_status
    - stageName: SVT
      stageKind: TektonStage
      name: SVT
      resourceBindings:
        - inputBinding:
           - stageVaraible: image
             strategyVariable: app_image
      triggers:
          - attribute: name
            value: "Build"
          - attribute: status
            value: "Success"
      emit:
        - event:
          - attribute: name
            value:  SVT
          - attributge: status
            value: $stage_status
    - stageName: Approval
      stageKind: EmailApprovalStage
      triggers:
          - trigger
            - attribute: name
              value: "FVT"
            - attribute: status
              value: "Success"
          - trigger
            - attribute: name
              value: "SVT"
            - attribute: status
              value: "Success"
      emit:
        - event:
          - attribute: name
            value:  Deploy
          - attributge: status
            value: $stage_status
    - stageName: Deploy
      stageKind: TektonStage
      resourceBindings:
        - outputBinding:
           - stageVaraible: image
             strategyVariable: app_image_tagged
      triggers:
          - trigger
            - attribute: name
              value: "deploy_approval"
            - attribute: status
              value: "Success"
      emit:
        - event:
          - attribute: name
            value:  Deploy
          - attributge: status
            value: $stage_status
```


#### Strategy Trigger

A strategy trigger defines an external trigger for the strategy:

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyTrigger
metadata:
  name: main
  namespace: kabanero
spec:
  - strategy: main
  - events:
    - trigger:
       attribute: name
       value : Push
       filter:
          - attribute: branch
            allowed: master
            disallowed: *test
       variableBindings: 
          - name: src_repo
            location: $event.resource
            branch: ${event.branch}
            commit: ${event.commit}
          - name: app_image
            location: ${docker_registry}/{$event.repoisotry}
          - name: app_image_tagged
            location: ${docker_registry}/${event.repository}:${event.commit}
          - name: approver_emails
            value: "champ@mycompany.com"
        emit: 
          attribute: name
          value: Start_build
```

#### Strategy Runs

Each trigger of the strategy creates a running strategy instance. Strategy Runs may also be created manually. 

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyRun
metadata:
  name: main_201909030817000
  namespace: kabanero
spec:
  strategy: main
  contextID: 201909030817000
  variables: 
    - name: src_repo
      location: https://www.github.com/user/hello-world
      commit: 12345
    - name: app_image
      location: mydocker-registry.com/hello-world
    - name: app_image_tagged
      location: mydocker-registry.com/hello-world:12345
    - name: approver_emails
      value: "champ@mycompany.com"
  emit: 
    attribute: name
    value: Start_build
```

The controller for StrategyRun also creates a run for each stage.  For example:

```
apiVersion: kabanero.io/v1alpha1
kind: StageRun
metadata:
  name: appsody_build_201909030817000
  namespace: kabanero
spec:
  stageName: appsody_build
  stageKind: AppsodyBuildStage
  contextID: 201909030817000
  strategy_run: main_201909030817000
```

The controller for each stage processes the creation of StageRun by:
- listening for event triggers for the stage. 
- After trigger, perform the actions of the stage by creating and running additional resources, such as Tekton pipeline
- Emit completion events after completion of the action.

#### Status of Strategy Runs

Each of the StrategyRun and StageRun resource is expected to update the status of the run. For example,

```
apiVersion: kabanero.io/v1alpha1
kind: StrategyRun
metadata:
  name: main_123456
  namespace: kabanero
spec:
  strategy: main
  contextID: 201909030817000
  variableBindings: 
    - name: src_repo
      location: https://www.github.com/user/hello-world
      commit: 12345
    - name: app_image
      location: mydocker-registry.com/hello-world
    - name: app_image_tagged
      location: mydocker-registry.com/hello-world:12345
    - name: approver_emails
      value: "champ@mycompany.com"
  emit: 
    attribute: name
    value: Start_build
  status:
    state: in-pgoress
    stages:
      - stage: Build
        status: success
      - stage: FVT
        status: failed

```

```
apiVersion: kabanero.io/v1alpha1
kind: StageRun
metadata:
  name: appsody_build_abcdefg12345
  namespace: kabanero
spec:
  stage: appsody_build
  contextID: 201909030817000
  strategy_run: main_123456
status:
  status: Success
```

#### Tracking context

Each run of the strategy and the associated stages is under a different context.  The contextID is created and stored as part of each StrategyRun and StageRun resource.  In addition, the contextID is an implicit attribute stored within each intra-strategy event (such as Build, FVT, etc). This allows the implementations of each stage to distinguish between different runs. 

#### Adding a Custom Stage

The first step to creating a custom stage is to define a custom resource for the stage. The custom resource must contain the declarations of the with the input and output resources, but may contain more configuration attributes for the particular stage.  Say we wish to define a stage that builds 3 different platforms. Its custom resource definition may look like:

```
apiVersion: kabanero.io/v1alpha1
kind: MultiPlatformBuild
metadata:
  name: multi-platform-build
  namespace: kabanero
spec:
  resources:
    input:
      - name: source
        type: github
    output:
      - name: image_amd64
        type: docker
      - name: image_ppcl3
        type: docker
      - name: image_os390
        type: docker
```

The next step is to develop a Kubernetes controller that reacts to changes to the StageRun resource. Upon the creation of the StageRun resource, the controller will
- determine whether it needs to handle the run by matching the stage in the Strategy resource.
- If it needs to handle the run
  - Start to listen for trigger event
  - Once triggered, carry out action of the stage
  - When action is completed, emit end of action event.

If the StageRun resource is deleted, the controller cancels any ongoing action, and stops to listen for trigger events.

**TBD: A stage development library will be useful**



### Event Topics

The content of Kabanero events are JSON objects, and will be filtered via JSON PATH.

#### Topic: SourceRepository

The attributes:
- repositoryType : type of repository, currently only `Github`
- eventName: The name of the event in the repository, currently only `Push` and `PullRequest`.
- location: The location of the repository
- rawData: The actual JSON object coming from the repository, or a mapping of the data if original data is not JSON

#### Topic: KabaneroManagement

The attributes:
- eventName: one of `list`, `login`, `logout`, `onboard`, `refresh`
- user: The ID of the user who initiated the call
- status: The status of the API call
- **TBD**: API specific parameters

#### Topic: KabaneroClient

The attributes of the events are:
- eventName: one of `login`, `logout`, etc
- user: The ID of the user who initiated the call
- status: The status of the API call
- **TBD**: API specific parameters

#### Topic: KubernetesAPI

The attributes are:
- eventType: One of `create`, `modify`, and `delete`
- kind: kind of the resource
- namespace: namespace of the resource, if resource is namespaced
- name: name of the resource
- resource: JSON specification of a Kubernetes resource
- oldResource: for `modify` event type, JSON specification of the old resource.
  
Filters may be defined on the resources through a ConfigMap whose name is kabanero.kubernetes.api.event.filter. The filter for namespaced resources is based on namespace, and filter for resources without namespace is based Kind. If the Configmap is not defined, it is equivalent to:
```
apiVersion: kabanero.io/v1alpha1
kind: ConfigMap
metadata:
  name: kaganero.kubernetes.api.event.filter
  namespace: kabanero
spec:
  data:
    allowedNamespaces:
      - *
    disAllowedNoNamespacedKinds:
      - *
```

Here is an example with more extensive specification:
```
apiVersion: kabanero.io/v1alpha1
kind: ConfigMap
metadata:
  name: kaganero.kubernetes.api.event.filter
  namespace: kabanero
spec:
  data:
    allowedNamespaces:
      - app1*
      - app2*
    disallowedNamespaces:
      - *temp
    allowedNoNamespacedKinds:
      - kind1*
      - kind2*
```

A namespaced resource whose namespace is allowed by `allowedNamespaces` filter, and is not disallowed by the `disallowedNamespaces` passed the filter.  A resource without namespace whose kind is allowed by allowedNoNamespacedKinds and not disallowed by the disallowedNoNamespacedKind passes the filter.

Note: For security reasons, Kubernetes Secrets are not stored in the `resource` or `oldResource` attributes of the KubernetesAPI event.

#### Topic: KabaneroOperator

**TBD: Kabanero Operator related events**


<a name="Staging"></a>
## Staging

October 2019:
- Organizational web hook only without any of the CRDs

Or prioritized list (unlikely in Oct 2019):
- update kabanero operator to install new code
- No events: web hook listener directly creates StrategyRun resources.
- Initial controller implementation to support CRDs (go?):
   - TektonStage
   - StrategyDefinition
   - StrategyRun
   - StageRun
- automatic configuration of web hook via EventSource CRD (go?)
- Install event infrastructure 
- web hook listener uses events
- Refine controllers