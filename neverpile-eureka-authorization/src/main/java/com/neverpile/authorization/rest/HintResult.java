package com.neverpile.authorization.rest;

import java.util.List;

import com.neverpile.common.authorization.api.HintRegistrations.Hint;

public class HintResult {
  private List<Hint> actions;
  
  private List<Hint> subjects;
  
  private List<Hint> resources;

  public HintResult() {
  }
  
  public List<Hint> getActions() {
    return actions;
  }

  public void setActions(final List<Hint> actions) {
    this.actions = actions;
  }

  public List<Hint> getSubjects() {
    return subjects;
  }

  public void setSubjects(final List<Hint> subjects) {
    this.subjects = subjects;
  }

  public List<Hint> getResources() {
    return resources;
  }

  public void setResources(final List<Hint> resources) {
    this.resources = resources;
  }
}
