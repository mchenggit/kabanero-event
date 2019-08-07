package io.kabanero.event.model;

import javax.json.bind.annotation.JsonbProperty;

public class Content {
    public String name;
    public String path;
    public String sha;
    public int size;
    public String url;
    @JsonbProperty("html_url") public String htmlUrl;
    @JsonbProperty("git_url") public String gitUrl;
    @JsonbProperty("download_url") public String downloadUrl;
    public String type;
    private String content;
    public String encoding;

    public void setContent(String content) {
        this.content = content.replace("\n", "");
    }

    public String getContent() {
        return content;
    }
}
