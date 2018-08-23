/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;

import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Octane's listener for WorkflowRun events
 * - this listener should handle Pipeline's STARTED and FINISHED events
 * - this listener should handle each stage's STARTED and FINISHED events
 *
 * User: gadiel
 * Date: 07/06/2016
 * Time: 17:21
 */

@Extension
public class WorkflowListener implements GraphListener {
	private static final Logger logger = LogManager.getLogger(WorkflowListener.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Override
	public void onNewHead(FlowNode flowNode) {
		if (flowNode.getAction(WorkspaceAction.class) != null) {
			System.out.println(flowNode);
		}

		if (BuildHandlerUtils.isWorkflowStartNode(flowNode)) {
			sendPipelineStartedEvent(flowNode);
		} else if (BuildHandlerUtils.isWorkflowEndNode(flowNode)) {
			sendPipelineFinishedEvent((FlowEndNode) flowNode);
		} else if (BuildHandlerUtils.isStageStartNode(flowNode)) {
			sendStageStartedEvent((StepStartNode) flowNode);
		} else if (BuildHandlerUtils.isStageEndNode(flowNode)) {
			sendStageFinishedEvent((StepEndNode) flowNode);
		}
	}

	private void sendPipelineStartedEvent(FlowNode flowNode) {
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowNode);
		CIEvent event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.STARTED)
				.setProject(BuildHandlerUtils.getJobCiId(parentRun))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setParameters(ParameterProcessors.getInstances(parentRun))
				.setStartTime(parentRun.getStartTimeInMillis())
				.setEstimatedDuration(parentRun.getEstimatedDuration())
				.setCauses(CIEventCausesFactory.processCauses(parentRun));

		if (parentRun.getParent().getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
			event
					.setParentCiId(parentRun.getParent().getParent().getFullName())
					.setMultiBranchType(MultiBranchType.MULTI_BRANCH_CHILD)
					.setProjectDisplayName(parentRun.getParent().getFullName());
		}

		OctaneSDK.getInstance().getEventsService().publishEvent(event);
	}

	private void sendPipelineFinishedEvent(FlowEndNode flowEndNode) {
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowEndNode);

		//boolean hasTests = testListener.processBuild(r);

		CIEvent event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.FINISHED)
				.setProject(BuildHandlerUtils.getJobCiId(parentRun))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setParameters(ParameterProcessors.getInstances(parentRun))
				.setStartTime(parentRun.getStartTimeInMillis())
				.setEstimatedDuration(parentRun.getEstimatedDuration())
				.setDuration(parentRun.getDuration())
				.setResult(BuildHandlerUtils.translateRunResult(parentRun))
				.setCauses(CIEventCausesFactory.processCauses(parentRun))
				.setTestResultExpected(false);
		OctaneSDK.getInstance().getEventsService().publishEvent(event);
	}

	private void sendStageStartedEvent(StepStartNode stepStartNode) {
		CIEvent event;
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(stepStartNode);
		event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.STARTED)
				.setPhaseType(PhaseType.INTERNAL)
				.setProject(stepStartNode.getDisplayName())
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setStartTime(TimingAction.getStartTime(stepStartNode))
				.setCauses(CIEventCausesFactory.processCauses(stepStartNode));
		OctaneSDK.getInstance().getEventsService().publishEvent(event);
	}

	private void sendStageFinishedEvent(StepEndNode stepEndNode) {
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(stepEndNode);
		StepStartNode stepStartNode = stepEndNode.getStartNode();
		CIEvent event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.FINISHED)
				.setPhaseType(PhaseType.INTERNAL)
				.setProject(stepStartNode.getDisplayName())
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setStartTime(TimingAction.getStartTime(stepStartNode))
				.setDuration(TimingAction.getStartTime(stepEndNode) - TimingAction.getStartTime(stepStartNode))
				.setResult(extractFlowNodeResult(stepEndNode))
				.setCauses(CIEventCausesFactory.processCauses(stepEndNode));
		OctaneSDK.getInstance().getEventsService().publishEvent(event);
	}

	private CIBuildResult extractFlowNodeResult(FlowNode node) {
		for (Action action : node.getActions()) {
			if (action.getClass().equals(ErrorAction.class))
				return CIBuildResult.FAILURE;
		}
		return CIBuildResult.SUCCESS;
	}
}