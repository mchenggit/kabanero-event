package io.kabanero.event.model;

import javax.json.bind.annotation.JsonbProperty;
import java.net.URL;

public class Repository {
    public String name;
    public String id;
    @JsonbProperty("full_name") public String fullName;
    public URL url;
    @JsonbProperty("git_url") public String gitUrl;
}
