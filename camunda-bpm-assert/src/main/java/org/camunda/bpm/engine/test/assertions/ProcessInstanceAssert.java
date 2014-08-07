package org.camunda.bpm.engine.test.assertions;

import java.util.*;

import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.util.Lists;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.history.*;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.*;
import org.camunda.bpm.engine.task.TaskQuery;
import org.assertj.core.api.Assertions;

/**
 * Assertions for a {@link ProcessInstance}
 * @author Martin Schimak <martin.schimak@plexiti.com>
 * @author Rafael Cordones <rafael@cordones.me>
 */
public class ProcessInstanceAssert extends AbstractProcessAssert<ProcessInstanceAssert, ProcessInstance> {

  protected ProcessInstanceAssert(final ProcessEngine engine, final ProcessInstance actual) {
    super(engine, actual, ProcessInstanceAssert.class);
  }

  protected static ProcessInstanceAssert assertThat(final ProcessEngine engine, final ProcessInstance actual) {
    return new ProcessInstanceAssert(engine, actual);
  }

  @Override
  protected ProcessInstance getCurrent() {
    return processInstanceQuery().singleResult();
  }

  @Override
  protected String toString(ProcessInstance processInstance) {
    return processInstance != null ?
      String.format("actual %s {" +
        "id='%s', " +
        "processDefinitionId='%s', " +
        "businessKey='%s'" +
        "}",
        ProcessInstance.class.getSimpleName(),
        processInstance.getId(),
        processInstance.getProcessDefinitionId(),
        processInstance.getBusinessKey())
      : null;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is currently waiting 
   * at one or more specified activities.
   * 
   * @param   activityIds the id's of the activities the process instance is Expecting to 
   *          be waiting at
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingAt(final String... activityIds) {
    return isWaitingAt(activityIds, true, false);
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is currently NOT waiting 
   * at one or more specified activities.
   *
   * @param   activityIds the id's of the activities the process instance is expected 
   *          not to be waiting at
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotWaitingAt(final String... activityIds) {
    return isWaitingAt(activityIds, false, false);
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is currently waiting 
   * at exactly one or more specified activities.
   * 
   * @param   activityIds the id's of the activities the process instance is Expecting to 
   *          be waiting at
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingAtExactly(final String... activityIds) {
    return isWaitingAt(activityIds, true, true);
  }

  private ProcessInstanceAssert isWaitingAt(final String[] activityIds, boolean isWaitingAt, boolean exactly) {
    ProcessInstance current = getExistingCurrent();
    Assertions.assertThat(activityIds)
      .overridingErrorMessage("Expecting list of activityIds not to be null, not to be empty and not to contain null values: %s."
        , Lists.newArrayList(activityIds))
      .isNotNull().isNotEmpty().doesNotContainNull();
    final List<String> activeActivityIds = runtimeService().getActiveActivityIds(actual.getId());
    final String message = "Expecting %s " +
      (isWaitingAt ? "to be waiting at " + (exactly ? "exactly " : "") + "%s, ": "NOT to be waiting at %s, ") +
      "but it is actually waiting at %s.";
    ListAssert<String> assertion = Assertions.assertThat(activeActivityIds)
      .overridingErrorMessage(message, 
        toString(current),
        Lists.newArrayList(activityIds), 
        activeActivityIds);
    if (exactly) {
        String[] sorted = activityIds.clone();
        Arrays.sort(sorted);
      if (isWaitingAt) {
        assertion.containsExactly(sorted);
      } else {
        throw new UnsupportedOperationException(); 
        // "isNotWaitingAtExactly" is unsupported
      }
    } else {
      if (isWaitingAt) {
        assertion.contains(activityIds);
      } else {
        assertion.doesNotContain(activityIds);
      }
    }
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} has passed one or 
   * more specified activities.
   * 
   * @param   activityIds the id's of the activities expected to have been passed    
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasPassed(final String... activityIds) {
    return hasPassed(activityIds, true);
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} has NOT passed one 
   * or more specified activities.
   *
   * @param   activityIds the id's of the activities expected NOT to have been passed    
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNotPassed(final String... activityIds) {
    return hasPassed(activityIds, false);
  }
  
  private ProcessInstanceAssert hasPassed(final String[] activityIds, boolean hasPassed) {
    isNotNull();
    Assertions.assertThat(activityIds)
      .overridingErrorMessage("Expecting list of activityIds not to be null, not to be empty and not to contain null values: %s." 
        , Lists.newArrayList(activityIds))
      .isNotNull().isNotEmpty().doesNotContainNull();
    List<HistoricActivityInstance> finishedInstances = historicActivityInstanceQuery().finished().orderByHistoricActivityInstanceEndTime().asc().list();
    List<String> finished = new ArrayList<String>(finishedInstances.size());
    for (HistoricActivityInstance instance: finishedInstances) {
      finished.add(instance.getActivityId());
    }
    final String message = "Expecting %s " +
      (hasPassed ? "to have passed activities %s at least once, ": "NOT to have passed activities %s, ") +
      "but actually we instead we found that it passed %s. (Please make sure you have set the history " +
      "service of the engine to at least 'activity' or a higher level before making use of this assertion!)";
    ListAssert<String> assertion = Assertions.assertThat(finished)
      .overridingErrorMessage(message, 
        actual, 
        Lists.newArrayList(activityIds), 
        Lists.newArrayList(finished)
      );
    if (hasPassed)
      assertion.contains(activityIds);
    else
      assertion.doesNotContain(activityIds);
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} holds one or 
   * more process variables with the specified names
   *
   * @param   names the names of the process variables expected to exist    
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasVariables(final String... names) {
    return hasVars(names);
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} holds no 
   * process variables at all.
   *
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNoVariables() {
    return hasVars(null);
  }

  private ProcessInstanceAssert hasVars(final String[] names) {
    boolean shouldHaveVariables = names != null;
    boolean shouldHaveSpecificVariables = names != null && names.length > 0;

    ProcessInstance current = getExistingCurrent();
    Map<String, Object> vars = runtimeService().getVariables(getExistingCurrent().getProcessInstanceId());
    MapAssert<String, Object> assertion = Assertions.assertThat(vars)
      .overridingErrorMessage("Expecting %s to hold " + 
        (shouldHaveVariables ? "process variables" + (shouldHaveSpecificVariables ? " %s, " : ", ") : "no variables at all, ") +
        "instead we found it to hold " + (vars.isEmpty() ? "no variables at all." : "the variables %s."),
        toString(current), shouldHaveSpecificVariables ? Arrays.asList(names) : vars.keySet(), vars.keySet()
      );
    if (shouldHaveVariables) {
      if (shouldHaveSpecificVariables) {
        assertion.containsKeys(names);
      } else {
        assertion.isNotEmpty();
      }
    } else {
      assertion.isEmpty();
    }
    return this;
  }
  
  /**
   * Verifies the expectation that the {@link ProcessInstance} is ended.
   * 
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isEnded() {
    isNotNull();
    final String message = "Expecting %s to be ended, but it is not!. (Please " +
      "make sure you have set the history service of the engine to at least " +
      "'activity' or a higher level before making use of this assertion!)";
    Assertions.assertThat(processInstanceQuery().singleResult())
      .overridingErrorMessage(message,
        toString(actual))
      .isNull();
    Assertions.assertThat(historicProcessInstanceQuery().singleResult())
      .overridingErrorMessage(message, 
        toString(actual))
      .isNotNull();
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is currently 
   * suspended.
   * 
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isSuspended() {
    ProcessInstance current = getExistingCurrent();
    Assertions.assertThat(current.isSuspended())
      .overridingErrorMessage("Expecting %s to be suspended, but it is not!", 
        toString(actual))
      .isTrue();
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is not ended.
   * 
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotEnded() {
    ProcessInstance current = getExistingCurrent();
    Assertions.assertThat(current)
      .overridingErrorMessage("Expecting %s not to be ended, but it is!", 
        toString(current))
      .isNotNull();
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is currently active, 
   * iow not suspended and not ended.
   * 
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isActive() {
    ProcessInstance current = getExistingCurrent();
    isStarted();
    isNotEnded();
    Assertions.assertThat(current.isSuspended())
      .overridingErrorMessage("Expecting %s not to be suspended, but it is!", 
        toString(current))
      .isFalse();
    return this;
  }

  /**
   * Verifies the expectation that the {@link ProcessInstance} is started. This is 
   * also true, in case the process instance already ended.
   * 
   * @return  this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isStarted() {
    Object pi = getCurrent();
    if (pi == null) 
      pi = historicProcessInstanceQuery().singleResult();
    Assertions.assertThat(pi)
      .overridingErrorMessage("Expecting %s to be started, but it is not!", 
        toString(actual))
      .isNotNull();
    return this;
  }

  /**
   * Enter into a chained task assert inspecting the one and mostly 
   * one task currently available in the context of the process instance
   * under test of this ProcessInstanceAssert.
   * 
   * @return  TaskAssert inspecting the only task available. Inspecting a 
   *          'null' Task in case no such Task is available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more 
   *          than one task is delivered by the query (after being narrowed 
   *          to actual ProcessInstance)
   */
  public TaskAssert task() {
    return task(taskQuery());
  }

  /**
   * Enter into a chained task assert inspecting the one and mostly 
   * one task of the specified task definition key currently available in the 
   * context of the process instance under test of this ProcessInstanceAssert.
   * 
   * @param   taskDefinitionKey definition key narrowing down the search for 
   *          tasks
   * @return  TaskAssert inspecting the only task available. Inspecting a 
   *          'null' Task in case no such Task is available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more than one 
   *          task is delivered by the query (after being narrowed to actual 
   *          ProcessInstance)
   */
  public TaskAssert task(String taskDefinitionKey) {
    return task(taskQuery().taskDefinitionKey(taskDefinitionKey));
  }

  /**
   * Enter into a chained task assert inspecting only tasks currently
   * available in the context of the process instance under test of this
   * ProcessInstanceAssert. The query is automatically narrowed down to
   * the actual ProcessInstance under test of this assertion.
   *
   * @param   query TaskQuery further narrowing down the search for tasks
   *          The query is automatically narrowed down to the actual 
   *          ProcessInstance under test of this assertion.
   * @return  TaskAssert inspecting the only task resulting from the given
   *          search. Inspecting a 'null' Task in case no such Task is 
   *          available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more than 
   *          one task is delivered by the query (after being narrowed to 
   *          actual ProcessInstance)
   */
  public TaskAssert task(TaskQuery query) {
    if (query == null)
      throw new IllegalArgumentException("Illegal call of task(query = 'null') - but must not be null!");
    isNotNull();
    TaskQuery narrowed = query.processInstanceId(actual.getId());
    return TaskAssert.assertThat(engine, narrowed.singleResult());
  }

  /**
   * Enter into a chained job assert inspecting the one and mostly 
   * one job currently available in the context of the process 
   * instance under test of this ProcessInstanceAssert.
   * 
   * @return  JobAssert inspecting the only job available. Inspecting 
   *          a 'null' Job in case no such Job is available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more 
   *          than one task is delivered by the query (after being narrowed 
   *          to actual ProcessInstance)
   */
  public JobAssert job() {
    return job(jobQuery());
  }

  /**
   * Enter into a chained task assert inspecting the one and mostly 
   * one task of the specified task definition key currently available in the 
   * context of the process instance under test of this ProcessInstanceAssert.
   *
   * @param   activityId id narrowing down the search for jobs
   * @return  JobAssert inspecting the retrieved job. Inspecting a 
   *          'null' Task in case no such Job is available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more than one 
   *          job is delivered by the query (after being narrowed to actual 
   *          ProcessInstance)
   */
  public JobAssert job(String activityId) {
    Execution execution = executionQuery().activityId(activityId).active().singleResult();
    return JobAssert.assertThat(
      engine,
      execution != null ? jobQuery().executionId(execution.getId()).singleResult() : null
    );
  }
  
  /**
   * Enter into a chained job assert inspecting only jobs currently
   * available in the context of the process instance under test of this
   * ProcessInstanceAssert. The query is automatically narrowed down to
   * the actual ProcessInstance under test of this assertion.
   *
   * @param   query JobQuery further narrowing down the search for 
   *          jobs. The query is automatically narrowed down to the 
   *          actual ProcessInstance under test of this assertion.
   * @return  JobAssert inspecting the only job resulting from the 
   *          given search. Inspecting a 'null' job in case no such job 
   *          is available.
   * @throws  org.camunda.bpm.engine.ProcessEngineException in case more 
   *          than one job is delivered by the query (after being narrowed 
   *          to actual ProcessInstance)
   */
  public JobAssert job(JobQuery query) {
    if (query == null)
      throw new IllegalArgumentException("Illegal call of job(query = 'null') - but must not be null!");
    isNotNull();
    JobQuery narrowed = query.processInstanceId(actual.getId());
    return JobAssert.assertThat(engine, narrowed.singleResult());
  }

  /**
   * Enter into a chained map assert inspecting the variables currently
   * available in the context of the process instance under test of this
   * ProcessInstanceAssert.
   *
   * @return  MapAssert<String, Object> inspecting the process variables. 
   *          Inspecting a 'null' map in case no such variables are available.
   */
  public MapAssert<String, Object> variables() {
    return Assertions.assertThat(runtimeService().getVariables(getExistingCurrent().getProcessInstanceId()));
  }

  /* TaskQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected TaskQuery taskQuery() {
    return super.taskQuery().processInstanceId(actual.getId());
  }

  /* JobQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected JobQuery jobQuery() {
    return super.jobQuery().processInstanceId(actual.getId());
  }

  /* ProcessInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected ProcessInstanceQuery processInstanceQuery() {
    return super.processInstanceQuery().processInstanceId(actual.getId());
  }

  /* ExecutionQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected ExecutionQuery executionQuery() {
    return super.executionQuery().processInstanceId(actual.getId());
  }

  /* VariableInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected VariableInstanceQuery variableInstanceQuery() {
    return super.variableInstanceQuery().processInstanceIdIn(actual.getId());
  }

  /* HistoricActivityInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected HistoricActivityInstanceQuery historicActivityInstanceQuery() {
    return super.historicActivityInstanceQuery().processInstanceId(actual.getId());
  }

  /* HistoricDetailQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected HistoricDetailQuery historicDetailQuery() {
    return super.historicDetailQuery().processInstanceId(actual.getId());
  }

  /* HistoricProcessInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected HistoricProcessInstanceQuery historicProcessInstanceQuery() {
    return super.historicProcessInstanceQuery().processInstanceId(actual.getId());
  }

  /* HistoricTaskInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected HistoricTaskInstanceQuery historicTaskInstanceQuery() {
    return super.historicTaskInstanceQuery().processInstanceId(actual.getId());
  }

  /* HistoricVariableInstanceQuery, automatically narrowed to actual {@link ProcessInstance} */
  @Override
  protected HistoricVariableInstanceQuery historicVariableInstanceQuery() {
    return super.historicVariableInstanceQuery().processInstanceId(actual.getId());
  }

  /* ProcessDefinitionQuery, automatically narrowed to {@link ProcessDefinition} 
   * of actual {@link ProcessInstance} 
   */
  @Override
  protected ProcessDefinitionQuery processDefinitionQuery() {
    return super.processDefinitionQuery().processDefinitionId(actual.getProcessDefinitionId());
  }

}