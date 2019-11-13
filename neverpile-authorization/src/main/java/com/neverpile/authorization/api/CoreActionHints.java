package com.neverpile.authorization.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
@ActionHints
public class CoreActionHints implements HintRegistrations {

  @Override
  public List<Hint> getHints() {
    return Stream.of(CoreActions.values()).map(
        a -> new Hint(a.key(), a.getClass().getSimpleName() + "." + a.name())).collect(Collectors.toList());
  }

}
