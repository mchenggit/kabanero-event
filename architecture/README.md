# Kabanero Event Architecture

## What is Kabanero 

The goal of Kabanero is to manage cloud native devops infrastructure. Such an infrastructure may contain:
- One or more developer environments. For example:
  - Appsody
  - Eclipse Che
  - no IDE other than text editor
- One or more source control repositories. For Example:
  - Github
  - Gitlab
  - SVN
  - CVS
- One or more build/test pipeline technologies. For example:
  - Tekton
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
- can stand up a standard development environment very quickly. For example, a developer may select a stack from Appsody or Eclipse Che, and get it instantiated within minutes.
- Before committing change into source control, test builds may be triggered with outcome and logs made available to the developer.
- After committing change into source control, additional builds may be triggered, and comprehensive tests run.


Kabanero automates the devops infrastructure as much as possible. It will
- Use organizational web hook to process events for all repositories within the organization where possible
- Install and run the pipelines automatically when configured.
- Trigger new builds if the stack has been updated, for example, with security fixes.

Kabanero is secure. It supports
  - login with different identity providers, for example, login with Github user ID, or with internal user registry
  - RBAC for different roles, for example, role of solution architect, and role of application developer.
  - security audit

Kabanero is extensible. It can be enhanced to support:
- new development environments with new ways to create stacks,
- new source control repositories
- new pipeline technologies

The initial version of Kabanero will support:
- Appsody as development environment
- Github for source repository 
- Tekton for pipeline

In order to ensure our design is extensible,  the remainder of this write-up will use:
- Appsody and Eclipse Che as development environments
- Github and SVN as source control
- Tekton and Openshift source-2-image as pipeline technologies