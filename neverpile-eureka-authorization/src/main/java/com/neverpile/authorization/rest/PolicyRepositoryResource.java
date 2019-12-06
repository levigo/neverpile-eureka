package com.neverpile.authorization.rest;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.config.EnableEntityLinks;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.authorization.api.Action;
import com.neverpile.authorization.api.ActionHints;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.CoreActions;
import com.neverpile.authorization.api.HintRegistrations;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.MutablePolicyRepository;
import com.neverpile.authorization.policy.ResourceHints;
import com.neverpile.authorization.policy.SubjectHints;
import com.neverpile.authorization.policy.impl.CompositeAuthorizationContext;
import com.neverpile.authorization.policy.impl.PolicyBasedAuthorizationService;
import com.neverpile.authorization.rest.ValidationResult.Type;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path = "/api/v1/authorization/policy", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@EnableEntityLinks
@OpenAPIDefinition(tags = @Tag(name = "Authorization"))
@Import(PolicyRepositoryResourceHints.class)
public class PolicyRepositoryResource {
  static final String POLICY_RESOURCE_SPECIFIER = "authorization.policy";

  @Autowired
  private MutablePolicyRepository policyRepository;

  @Autowired
  private PolicyBasedAuthorizationService authService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired(required = false)
  @ActionHints
  private List<HintRegistrations> actionHintRegistrations;

  @Autowired(required = false)
  @SubjectHints
  private List<HintRegistrations> subjectHintRegistrations;

  @Autowired(required = false)
  @ResourceHints
  private List<HintRegistrations> resourceHintRegistrations;

  @GetMapping("/current")
  @Operation(summary = "Fetch the currently valid authorization policy")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Policy found")
  })
  @Timed(description = "get current policy", extraTags = {
      "operation", "retrieve", "target", "policy"
  }, value = "eureka.authorization.policy.get-current")
  public AccessPolicy getCurrent() {
    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, Collections.singleton(CoreActions.GET),
        createAuthorizationContext()))
      throw new AccessDeniedException("Retrieval of authorization policy denied");

    return policyRepository.getCurrentPolicy();
  }

  @GetMapping
  @Operation(summary = "Query access policies by start-of-validity date range")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Policy found")
  })
  @Timed(description = "get current policy", extraTags = {
      "operation", "retrieve", "target", "policy"
  }, value = "eureka.authorization.policy.get-current")
  public List<AccessPolicy> query(
      @Parameter(description = "The start of the date range to query for") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Optional<Date> from,
      @Parameter(description = "The end of the date range to query for") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Optional<Date> to,
      @Parameter(description = "The maximum number if policies to return") @RequestParam(required = false) final Optional<Integer> limit) {
    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, singleton(CoreActions.QUERY),
        createAuthorizationContext()))
      throw new AccessDeniedException("Retrieval of authorization policy denied");

    return policyRepository.queryRepository(from.orElse(new Date(0)), to.orElse(new Date(Long.MAX_VALUE)),
        limit.orElse(Integer.MAX_VALUE));
  }

  private AuthorizationContext createAuthorizationContext() {
    return new CompositeAuthorizationContext(); // we don't have anything special here
  }

  @GetMapping(value = "{startOfValidity}")
  @Operation(summary = "Fetch the authorization policy with the given start-of-validity date")
  @ApiResponse(responseCode = "200", description = "Policy found")
  @Timed(description = "get policy by start-of-validity date", extraTags = {
      "operation", "retrieve", "target", "policy"
  }, value = "eureka.authorization.policy.get")
  public AccessPolicy get(
      @Parameter(description = "The start-of-validity date of the policy to be fetched", schema = @Schema(format = "date-time")) @PathVariable("startOfValidity") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date startOfValidity) {
    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, Collections.singleton(CoreActions.GET),
        createAuthorizationContext()))
      throw new AccessDeniedException("Retrieval of authorization policy denied");

    return policyRepository.get(startOfValidity);
  }

  @PutMapping(value = "{startOfValidity}")
  @Operation(summary = "Create or update the authorization policy with the given start-of-validity date")
  @ApiResponse(responseCode = "200", description = "Policy found")
  @Timed(description = "create or update policy by start-of-validity date", extraTags = {
      "operation", "create/update", "target", "policy"
  }, value = "eureka.authorization.policy.put")
  @Transactional
  public void put(
      @Parameter(description = "The start-of-validity date of the policy to be created/updated", schema = @Schema(format = "date-time")) @PathVariable("startOfValidity") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date startOfValidity,
      @Parameter @RequestBody final AccessPolicy policy) {
    policy.setValidFrom(startOfValidity);

    createOrUpdate(policy);
  }

  @DeleteMapping(value = "{startOfValidity}")
  @Operation(summary = "Delete the authorization policy with the given start-of-validity date")
  @ApiResponse(responseCode = "200", description = "Policy found")
  @Timed(description = "delete policy by start-of-validity date", extraTags = {
      "operation", "delete", "target", "policy"
  }, value = "eureka.authorization.policy.delete")
  @Transactional
  public void delete(
      @Parameter(description = "The start-of-validity date of the policy to be deleted", schema = @Schema(format = "date-time")) @PathVariable("startOfValidity") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date startOfValidity) {
    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, Collections.singleton(CoreActions.DELETE),
        createAuthorizationContext()))
      throw new AccessDeniedException("Deletion of authorization policy denied");

    policyRepository.delete(startOfValidity);
  }

  @PostMapping()
  @Operation(summary = "Create or update an authorization policy with the start-of-validity date taken from the supplied policy")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Policy found")
  })
  @Timed(description = "create or update policy by start-of-validity date", extraTags = {
      "operation", "create/update", "target", "policy"
  }, value = "eureka.authorization.policy.post")
  @Transactional
  public void post(@Parameter @RequestBody final AccessPolicy policy) {
    createOrUpdate(policy);
  }

  @PostMapping("/validate")
  @Operation(summary = "Validate an authorization policy. Report problems as a list of errors and/or warnings")
  @ApiResponse(responseCode = "200", description = "Policy validated")
  public List<ValidationResult> validate(@Parameter @RequestBody final String policyJson) {
    authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, Collections.singleton(CoreActions.VALIDATE),
        createAuthorizationContext());

    List<ValidationResult> result = new ArrayList<>();

    try {
      AccessPolicy unmarshalledPolicy = objectMapper.readValue(policyJson, AccessPolicy.class);

      if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER,
          new HashSet<Action>(Arrays.asList(CoreActions.UPDATE, CoreActions.CREATE)), createAuthorizationContext(),
          unmarshalledPolicy))
        result.add(new ValidationResult(Type.WARNING,
            "This policy will not allow further updates by the current principal (you),"
                + " which may mean that you are locking yourself out"));

      // FIXME: more ideas for validation?
    } catch (JsonProcessingException e) {
      result.add(new ValidationResult(Type.ERROR, "Invalid policy specification: " + e.getMessage()));
    } catch (Exception e) {
      result.add(new ValidationResult(Type.ERROR, "Cannot parse policy: " + e.getMessage()));
    }

    return result;
  }

  private void createOrUpdate(final AccessPolicy policy) {
    AccessPolicy existing = policy.getValidFrom() != null ? policyRepository.get(policy.getValidFrom()) : null;

    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER,
        Collections.singleton(null != existing ? CoreActions.UPDATE : CoreActions.CREATE),
        createAuthorizationContext()))
      throw new AccessDeniedException("Create/update of authorization policy denied");

    policyRepository.save(policy);
  }

  @GetMapping(value = "/hints")
  @Operation(summary = "Fetch the authorization policy hints")
  @ApiResponse(responseCode = "200", description = "Hints found")
  public HintResult getHints() {
    if (!authService.isAccessAllowed(POLICY_RESOURCE_SPECIFIER, Collections.singleton(CoreActions.GET),
        createAuthorizationContext()))
      throw new AccessDeniedException("Retrieval of authorization policy denied");

    HintResult result = new HintResult();

    if (null != actionHintRegistrations)
      result.setActions(actionHintRegistrations.stream().flatMap(r -> r.getHints().stream()).collect(toList()));

    if (null != resourceHintRegistrations)
      result.setResources(resourceHintRegistrations.stream().flatMap(r -> r.getHints().stream()).collect(toList()));

    if (null != subjectHintRegistrations)
      result.setSubjects(subjectHintRegistrations.stream().flatMap(r -> r.getHints().stream()).collect(toList()));

    return result;
  }
}
