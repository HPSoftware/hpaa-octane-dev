/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.model.processors.projects;

import hudson.model.Job;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by gadiel on 30/11/2016.
 * <p>
 * Job processors factory - should be used as a 'static' class, no instantiation, only static method/s
 */

public class JobProcessorFactory {
	public static final String WORKFLOW_JOB_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";
	public static final String WORKFLOW_RUN_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowRun";
	public static final String FOLDER_JOB_NAME = "com.cloudbees.hudson.plugins.folder.Folder";
	public static final String WORKFLOW_MULTI_BRANCH_JOB_NAME = "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";
	public static final String MULTIJOB_JOB_NAME = "com.tikal.jenkins.plugins.multijob.MultiJobProject";
	public static final String MAVEN_JOB_NAME = "hudson.maven.MavenModuleSet";
	public static final String MATRIX_JOB_NAME = "hudson.matrix.MatrixProject";
	public static final String MATRIX_CONFIGURATION_NAME = "hudson.matrix.MatrixConfiguration";
	public static final String FREE_STYLE_JOB_NAME = "hudson.model.FreeStyleProject";
	public static final String GITHUB_ORGANIZATION_FOLDER = "jenkins.branch.OrganizationFolder";

	private JobProcessorFactory() {
	}

	public static <T extends Job> AbstractProjectProcessor<T> getFlowProcessor(T job) {
		Set<Job> processedJobs = new HashSet<>();
		return getFlowProcessor(job, processedJobs);
	}

	public static <T extends Job> AbstractProjectProcessor<T> getFlowProcessor(T job, Set<Job> processedJobs) {
		AbstractProjectProcessor flowProcessor = new UnsupportedProjectProcessor(job);
		processedJobs.add(job);

		switch (job.getClass().getName()) {
			case FREE_STYLE_JOB_NAME:
				flowProcessor = new FreeStyleProjectProcessor(job, processedJobs);
				break;
			case MATRIX_JOB_NAME:
				flowProcessor = new MatrixProjectProcessor(job, processedJobs);
				break;
			case MATRIX_CONFIGURATION_NAME:
				flowProcessor = new MatrixConfigurationProcessor(job, processedJobs);
				break;
			case MAVEN_JOB_NAME:
				flowProcessor = new MavenProjectProcessor(job, processedJobs);
				break;
			case MULTIJOB_JOB_NAME:
				flowProcessor = new MultiJobProjectProcessor(job, processedJobs);
				break;
			case WORKFLOW_JOB_NAME:
				flowProcessor = new WorkFlowJobProcessor(job);
				break;
		}

		processedJobs.remove(job);
		return flowProcessor;
	}
}
