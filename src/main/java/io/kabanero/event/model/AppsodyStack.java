package io.kabanero.event.model;

public class AppsodyStack {
    private String user;
    private String project;
    private String version;

    public AppsodyStack(String stack) {
        int projectIndex = stack.indexOf('/') + 1;
        int versionIndex = stack.lastIndexOf(':') + 1;
        user = stack.substring(0, projectIndex - 1);
        project = stack.substring(projectIndex, versionIndex - 1);
        version = stack.substring(versionIndex);
    }

    public String getUser() {
        return user;
    }

    public String getProject() {
        return project;
    }

    public String getVersion() {
        return version;
    }
}
