# Kabanero Event Architecture


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
  - Which stacks are available for Eclipse Che
- What actions to take when source code is checked into source control. For example,
  - which Pipelines to execute
  - When to promote to the next stage of the pipeline

Kabanero allows a developer to concentrate as much on coding/testing inner loop as much. A developer
- can stand up a standard development environment very quickly. For example, a developer may create a stack from Appsody or Eclipse Che within minutes.
- Can rely on the environment to automatically trigger test builds before committing change into source control, with logs made available to the developer.
- Can rely on the environment to trigger additional release builds and run additional tests, and to be able to debug in the test environment when required.

Kabanero automates the devops infrastructure as much as possible. It will
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

In order to ensure the design is extensible,  the remainder of this write-up covers more than one development environments, source control repositories, and pipeline technologies, even though some may not be in the current Kabanero roadmap.

## Usage Scenarios

This section captures the usage scenarios, from which we can derive the event types, and producers and consumers of events. We begin with a sample production environment consisting of 3 different applications/services. This is followed by discussion on the devops pipeline that produces the production environment.

### The Production Environment

The production environment is in its own cluster `Prod-cluster`, and is consist of 3 different applications:
![Production Environment](Productioncluster.jpg)

The first application is an external facing UI application in the namespace ui-prod.  Its image is in the ui-prod namespace, and named ui. It depends on the service svc-a, and another service svc-b.

The second application is a microservice svc-a that is deployed in namespace a-prod. Its image is in the internal production repsitory, and in the namespace a-prod. This is a standalone shared service that may be called by other applications as well.  

The third and final application is a microservice svc-b. It is deployed in the namespace b-prod, with a corresponding images svc-b, and helper-b. The pod for svc-b runs both images svc-b and helper-b. 


### Usage Scenarios for ui

The UI application follows a true cloud-native development process:
-  It uses Github or Github Enterprise as the source repository.  
- It follows the paradigm that code in the `master` branch is always ready to deploy. 
- Developers are required to create branches for all source code changes.
- A pull request requires the following status checks before code may be merged back to `master`:
   - code review 
   - test build 
- A test build is triggered automatically for each pull request, and upon successful test build, the status check for test build is automatically marked successful in the pull request.

The pipeline is shown below:

![UI Pipeline](UIPipeline.jpg)

#### Usage Scenario for Developer

Prerequisites for the enterprise:
- A Github organization has been created.
- A Github repository for the ui application has been created, and developers authorized.
- The devops architect has pre-configured a Github organizational webook to automatically trigger a Tekton build when a pull request is created or updated. The configuration for the `ui` piepline has been provided to Kabanero so that it knows how to trigger a build for the `ui` repository.  The end of a successful test build automatically marks the `test build` status check for the pull request as successful.  
- Upon successful merge into `master`, the `svc-a` image in the `ui-test` namespace is automatically tagged, and the `ui` application redeployed.  **TBD**: Is a build from master required, or is the image built from the PR always the latest?
- Appsody has been configured with standardized stacks.

Prerequisites for the Developers:
- Developer installs Kabanero Client to local laptop/desktop
- Developer installs Appsody on laptop/desktop.  (**TBD**: Is this a special version that knows where the standardized stacks are, or will it go to Kabanero to get the location)
- For local testing, either there are stubs for svc-a and svc-b, or a working svc-a and svc-b service for local testing has been set up and made available for developers. (This is outside the scope of this document.)

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

If the test build is unsuccessful, developer receives an email from Github with instructions on how to access the test build.  **TBD:**
- How does the developer access the build logs:
  - the logs may be copied to a remote file system, or
  - developer may be given access to the build pod, or
  - A front end application may be provided to shield the details.
- It's possible for a build to be successful, but test may fail. What's the best way to help developer debug failed tests? 
   - Developer is given instructions on how to login to the pod.

#### Usage Scenario for Devops/Solution Architect

The architect is responsible for creating the appsody stack and making it active . **TBD**: Get link to docs.

The architect creates the Github organizational webhook following instructions here: https://help.github.com/en/articles/configuring-webhooks-for-organization-events-in-your-enterprise-account. The architect finds the Kabanero Github event listener URL and secret to receeive Github events by: **TBD**. The Github events to be received are **TBD**.
The architect sets up ssh key to access the repositories via instructions here: https://developer.github.com/v3/guides/managing-deploy-keys/. **TBD**: When the pipeline needs to extract from Github, it needs to be informed of what SSH key to use. When the piepeline needs to record the outcome of a test build, it needs the API key to call the GIthub API. The instructions are **TBD**.

The architect creates the Tekton Pipeline and Tasks for the `ui` application. This involves:
- Using any builder images that may already be available from Tekton team for building node.js applications. **TBD**: Are such images available? **TBD** Are they installed as part of Kabanero?
- Using any pre-existing Tekton Tasks that may be shipped with Tekton or Kabanero. **TBD**: Are they available?
- If the build is successful,  additional Pipeline Tasks will install `ui-test`, `ui`, `svc-a` and `svc-b` images, and run the `ui-test`.
- the last step of the task is to record the github status for test-build. (**TBD**: ship a task to do this? ):
   - successful if all tests pass
   - failed if some tests fails
- The build fails if any of the step fails.

**TBD**: If the build succeeds, the image is tagged and pushed to the `ui-test` repository. Provide a task to do this?

**TBD**: When does the image get tagged and pushed to the production cluster? Manually by the administrator?

**TBD**: Recommended strategy for tagging.

The Architect informs configures Kabanero what actions to take for pull request on an `ui` repository:
- Input: Github
  - Repository location: URL for the `ui` repository.
  - Event type: Pull Request. **TBD**: Can this be more generic?
  - Record Github status check for test build.
- Output: Docker
  - image location


#### The Kabanero Flow

Upon receipt of an event from Github, the Kabanero listener posts the event to the sourceRepositoryEvent topic. **TBD**: list of event types and their content

The SourceRepositoryEvent event consumer listens for events on the SourceRepositoryEvent topic. 
- It matches the repository URL to the repositories that have been configured. (**TBD**: default rules.)
- It finds all the actions it is capable of handling for the event. In this case, the action is for starting a Tekton run
- It creates the Tekton Resources for the run, and starts the run.
- **TBD**:  resource management:
   - Number of concurrent runs
   - Garbage collection to remove old runs.
- It monitors the results of the run, and informs the user of the outcome, if configured. **TBD** how does the user get informed?

### Usage Scenarios for svc-a

![svc-a Pipeline](SVCAPipeLine.jpg)

### Usage Scenarios for svc-b

![svc-b Pipeline](SVCBPipeline.jpg)
