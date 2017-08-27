package org.whitesource.bamboo.plugin.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.whitesource.bamboo.plugin.freestyle.WssUtils;


public abstract class AbstractMavenConfig {
	private static final Logger log = Logger.getLogger(AbstractMavenConfig.class);
	private static final String CFG_ENVIRONMENT_VARIABLES = TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES;
	private static final String LOG_COMPONENT = "AbstractMavenConfig";

	private static final String CFG_PROJECT_FILENAME = TaskConfigConstants.CFG_PROJECT_FILENAME;
	public static final String CFG_USE_MAVEN_RETURN_CODE = "useMavenReturnCode";
	public static final String CFG_ALTERNATE_USER_SETTINGS_FILE = "alternateUserSettingsFile";
	public static final String CFG_LOCAL_REPOSITORY_PATH = "localRepositoryPath";
	private final String executableName;
	private final File workingDirectory;

	private final List<String> commandline = Lists.newArrayList();
	protected final Map<String, String> extraEnvironment = Maps.newHashMap();
	protected String builderPath;

	public AbstractMavenConfig(@NotNull TaskContext taskContext, @NotNull CapabilityContext capabilityContext,
							   @NotNull EnvironmentVariableAccessor environmentVariableAccessor, @NotNull String capabilityPrefix,
							   @NotNull String mvn2OrMvn3, @NotNull String executableName, Map<String, String> bambooSystemProperties) {
		String finalCapabilityPrefix = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX != null ? capabilityPrefix :
				bambooSystemProperties.get(CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX) != null ?
						bambooSystemProperties.get(CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX) + mvn2OrMvn3 : null ;

		this.executableName = executableName;
		String buildLabel = null;
		if (bambooSystemProperties.get(TaskConfigConstants.CFG_BUILDER_LABEL) == null) {
			buildLabel = Preconditions.checkNotNull(
					taskContext.getBuildContext().getBuildDefinition().getTaskDefinitions().get(1)
							.getConfiguration().get(TaskConfigConstants.CFG_BUILDER_LABEL),
					"Builder label is not defined, fill {} = label_value in Bamboo system properties field",
					TaskConfigConstants.CFG_BUILDER_LABEL);
		} else {
			buildLabel = bambooSystemProperties.get(TaskConfigConstants.CFG_BUILDER_LABEL);
		}
		final String builderLabel = buildLabel;

		builderPath = Preconditions.checkNotNull(capabilityContext.getCapabilityValue(finalCapabilityPrefix + "." + builderLabel),
				"Builder path is not defined, fill {} = {} value in Bamboo system properties field",
				capabilityPrefix, capabilityPrefix);
		final String environmentVariables = bambooSystemProperties.get(CFG_ENVIRONMENT_VARIABLES) == null ?
				findMavenTask(taskContext).getConfiguration().get(CFG_ENVIRONMENT_VARIABLES) : bambooSystemProperties.get(CFG_ENVIRONMENT_VARIABLES);
		if (environmentVariables == null) {
			log.warn(WssUtils.logMsg(LOG_COMPONENT, CFG_ENVIRONMENT_VARIABLES +
					" is an optional field, make sure to fill it in bamboo system properties field if necessary"));
		}

		final String projectFilename = taskContext.getConfigurationMap().get(CFG_PROJECT_FILENAME);
		workingDirectory = taskContext.getWorkingDirectory();

		// set commandline
		commandline.add(getMavenExecutablePath(builderPath));
		if (StringUtils.isNotEmpty(projectFilename)) {
			commandline.addAll(Arrays.asList("-f", projectFilename));
		}

		// set extraEnvironment
		extraEnvironment.putAll(environmentVariableAccessor.splitEnvironmentAssignments(environmentVariables, false));
	}

	@NotNull
	protected String getMavenExecutablePath(@NotNull String homePath) {
		String pathToExecutable = StringUtils.join(new String[] { homePath, "bin", executableName }, File.separator);

		if (StringUtils.contains(pathToExecutable, " ")) {
			try {
				File f = new File(pathToExecutable);
				pathToExecutable = f.getCanonicalPath();
			} catch (IOException e) {
				log.warn("IO Exception trying to get executable", e);
			}
		}
		return pathToExecutable;
	}
	
	
	public static TaskDefinition findMavenTask(TaskContext taskContext) {
		List<TaskDefinition> taskDefinations = taskContext.getBuildContext().getBuildDefinition().getTaskDefinitions();
		TaskDefinition 	mavenTask;
			mavenTask = Iterables.find(taskDefinations, new Predicate<TaskDefinition> () {
		        public boolean apply(TaskDefinition taskDef) {
		            return taskDef.getPluginKey().equalsIgnoreCase("com.atlassian.bamboo.plugins.maven:task.builder.mvn3") || 
		            		taskDef.getPluginKey().equalsIgnoreCase("com.atlassian.bamboo.plugins.maven:task.builder.mvn2");
		        }
		        
			},null);
		
		return mavenTask;
	}

	public List<String> getCommandline() {
		return commandline;
	}

	public Map<String, String> getExtraEnvironment() {
		return extraEnvironment;
	}

	public File getWorkingDirectory() {
		return workingDirectory;
	}

}
