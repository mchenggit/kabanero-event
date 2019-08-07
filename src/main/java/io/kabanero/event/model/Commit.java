package io.kabanero.event.model;

import java.util.List;

public class Commit {
    public String ref;
    public String before;
    public Repository repository;
    public Pusher pusher;
    public List<Commit> commits;
    public String description;
}
