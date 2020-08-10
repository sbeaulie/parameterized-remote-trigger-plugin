/*
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.ParameterizedRemoteTrigger.pipeline;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BasicBuildContext;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.RemoteBuildConfiguration;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.RemoteJenkinsServer;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2.Auth2Descriptor;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.NullAuth;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils.FormValidationUtils;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils.FormValidationUtils.AffectedField;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils.FormValidationUtils.RemoteURLCombinationsResult;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class RemoteBuildPipelineStep extends Step {

	private RemoteBuildConfiguration remoteBuildConfig;

	@DataBoundConstructor
	public RemoteBuildPipelineStep(String job) {
		remoteBuildConfig = new RemoteBuildConfiguration();
		remoteBuildConfig.setJob(job);
		remoteBuildConfig.setShouldNotFailBuild(false); // We need to get notified. Failure feedback is collected async
														// then.
		remoteBuildConfig.setBlockBuildUntilComplete(true); // default for Pipeline Step
	}

	@DataBoundSetter
	public void setAbortTriggeredJob(boolean abortTriggeredJob) {
		remoteBuildConfig.setAbortTriggeredJob(abortTriggeredJob);
	}

	@DataBoundSetter
	public void setMaxConn(int maxConn) {
		remoteBuildConfig.setMaxConn(maxConn);
	}

	@DataBoundSetter
	public void setAuth(Auth2 auth) {
		remoteBuildConfig.setAuth2(auth);
	}

	@DataBoundSetter
	public void setRemoteJenkinsName(String remoteJenkinsName) {
		remoteBuildConfig.setRemoteJenkinsName(remoteJenkinsName);
	}

	@DataBoundSetter
	public void setRemoteJenkinsUrl(String remoteJenkinsUrl) {
		remoteBuildConfig.setRemoteJenkinsUrl(remoteJenkinsUrl);
	}

	@DataBoundSetter
	public void setShouldNotFailBuild(boolean shouldNotFailBuild) {
		remoteBuildConfig.setShouldNotFailBuild(shouldNotFailBuild);
	}

	@DataBoundSetter
	public void setPreventRemoteBuildQueue(boolean preventRemoteBuildQueue) {
		remoteBuildConfig.setPreventRemoteBuildQueue(preventRemoteBuildQueue);
	}

	@DataBoundSetter
	public void setPollInterval(int pollInterval) {
		remoteBuildConfig.setPollInterval(pollInterval);
	}

	@DataBoundSetter
	public void setBlockBuildUntilComplete(boolean blockBuildUntilComplete) {
		remoteBuildConfig.setBlockBuildUntilComplete(blockBuildUntilComplete);
	}

	@DataBoundSetter
	public void setToken(String token) {
		remoteBuildConfig.setToken(token);
	}

	@DataBoundSetter
	public void setParameters(String parameters) {
		remoteBuildConfig.setParameters(parameters);
	}

	@DataBoundSetter
	public void setEnhancedLogging(boolean enhancedLogging) {
		remoteBuildConfig.setEnhancedLogging(enhancedLogging);
	}

	@DataBoundSetter
	public void setLoadParamsFromFile(boolean loadParamsFromFile) {
		remoteBuildConfig.setLoadParamsFromFile(loadParamsFromFile);
	}

	@DataBoundSetter
	public void setParameterFile(String parameterFile) {
		remoteBuildConfig.setParameterFile(parameterFile);
	}

	@DataBoundSetter
	public void setUseJobInfoCache(boolean useJobInfoCache) {
		remoteBuildConfig.setUseJobInfoCache(useJobInfoCache);
	}

	@DataBoundSetter
	public void setUseCrumbCache(boolean useCrumbCache) {
		remoteBuildConfig.setUseCrumbCache(useCrumbCache);
	}
	
	@DataBoundSetter
	public void setDisabled(boolean disabled) {
		remoteBuildConfig.setDisabled(disabled);
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new Execution(context, remoteBuildConfig);
	}

	@Extension(optional = true)
	public static final class DescriptorImpl extends StepDescriptor {

		@Override
		public String getFunctionName() {
			return "triggerRemoteJob";
		}

		@Override
		public String getDisplayName() {
			return "Trigger Remote Job";
		}

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			Set<Class<?>> set = new HashSet<Class<?>>();
			Collections.addAll(set, Run.class, FilePath.class, Launcher.class, TaskListener.class);
			return set;
		}

		@Restricted(NoExternalUse.class)
		@Nonnull
		public ListBoxModel doFillRemoteJenkinsNameItems() {
			RemoteBuildConfiguration.DescriptorImpl descriptor = Descriptor.findByDescribableClassName(
					ExtensionList.lookup(RemoteBuildConfiguration.DescriptorImpl.class),
					RemoteBuildConfiguration.class.getName());
			if (descriptor == null)
				throw new RuntimeException("Could not get descriptor for RemoteBuildConfiguration");
			return descriptor.doFillRemoteJenkinsNameItems();
		}

		@Restricted(NoExternalUse.class)
		public FormValidation doCheckJob(@QueryParameter("job") final String value,
				@QueryParameter("remoteJenkinsUrl") final String remoteJenkinsUrl,
				@QueryParameter("remoteJenkinsName") final String remoteJenkinsName) {
			RemoteURLCombinationsResult result = FormValidationUtils.checkRemoteURLCombinations(remoteJenkinsUrl,
					remoteJenkinsName, value);
			if (result.isAffected(AffectedField.JOB_NAME_OR_URL))
				return result.formValidation;
			return FormValidation.ok();
		}

		@Restricted(NoExternalUse.class)
		public FormValidation doCheckRemoteJenkinsUrl(@QueryParameter("remoteJenkinsUrl") final String value,
				@QueryParameter("remoteJenkinsName") final String remoteJenkinsName,
				@QueryParameter("job") final String job) {
			RemoteURLCombinationsResult result = FormValidationUtils.checkRemoteURLCombinations(value,
					remoteJenkinsName, job);
			if (result.isAffected(AffectedField.REMOTE_JENKINS_URL))
				return result.formValidation;
			return FormValidation.ok();
		}

		@Restricted(NoExternalUse.class)
		public FormValidation doCheckRemoteJenkinsName(@QueryParameter("remoteJenkinsName") final String value,
				@QueryParameter("remoteJenkinsUrl") final String remoteJenkinsUrl,
				@QueryParameter("job") final String job) {
			RemoteURLCombinationsResult result = FormValidationUtils.checkRemoteURLCombinations(remoteJenkinsUrl, value,
					job);
			if (result.isAffected(AffectedField.REMOTE_JENKINS_NAME))
				return result.formValidation;
			return FormValidation.ok();
		}

		public static List<Auth2Descriptor> getAuth2Descriptors() {
			return Auth2.all();
		}

		public static Auth2Descriptor getDefaultAuth2Descriptor() {
			return NullAuth.DESCRIPTOR;
		}
	}

	public static class Execution extends SynchronousNonBlockingStepExecution<Handle> {

		private static final long serialVersionUID = 5339071667093320735L;

		private final RemoteBuildConfiguration remoteBuildConfig;

		Execution(StepContext context, RemoteBuildConfiguration remoteBuildConfig) {
			super(context);
			this.remoteBuildConfig = remoteBuildConfig;
		}

		@Override
		protected Handle run() throws Exception {
			StepContext stepContext = getContext();
			Run<?, ?> build = stepContext.get(Run.class);
			FilePath workspace = stepContext.get(FilePath.class);
			TaskListener listener = stepContext.get(TaskListener.class);
			RemoteJenkinsServer effectiveRemoteServer = remoteBuildConfig
					.evaluateEffectiveRemoteHost(new BasicBuildContext(build, workspace, listener));
			BuildContext context = new BuildContext(build, workspace, listener, listener.getLogger(),
					effectiveRemoteServer);
			Handle handle = null;
			try {
				if (!remoteBuildConfig.isStepDisabled(listener.getLogger())) {
					handle = remoteBuildConfig.performTriggerAndGetQueueId(context);
					if (remoteBuildConfig.getBlockBuildUntilComplete()) {
						remoteBuildConfig.performWaitForBuild(context, handle);
					}	
				}

			} catch (InterruptedException e) {
				remoteBuildConfig.abortRemoteTask(effectiveRemoteServer, handle, context);
				throw e;
			}
			return handle;
		}
	}

	public String getRemoteJenkinsName() {
		return remoteBuildConfig.getRemoteJenkinsName();
	}

	public String getRemoteJenkinsUrl() {
		return remoteBuildConfig.getRemoteJenkinsUrl();
	}

	public String getJob() {
		return remoteBuildConfig.getJob();
	}

	public boolean getShouldNotFailBuild() {
		return remoteBuildConfig.getShouldNotFailBuild();
	}

	public boolean getPreventRemoteBuildQueue() {
		return remoteBuildConfig.getPreventRemoteBuildQueue();
	}

	public int getPollInterval() {
		return remoteBuildConfig.getPollInterval();
	}

	public boolean getBlockBuildUntilComplete() {
		return remoteBuildConfig.getBlockBuildUntilComplete();
	}

	public String getToken() {
		return remoteBuildConfig.getToken();
	}

	public String getParameters() {
		return remoteBuildConfig.getParameters();
	}

	public boolean getEnhancedLogging() {
		return remoteBuildConfig.getEnhancedLogging();
	}

	public boolean getLoadParamsFromFile() {
		return remoteBuildConfig.getLoadParamsFromFile();
	}

	public String getParameterFile() {
		return remoteBuildConfig.getParameterFile();
	}

	public int getConnectionRetryLimit() {
		return remoteBuildConfig.getConnectionRetryLimit();
	}

	public boolean isUseCrumbCache() {
		return remoteBuildConfig.isUseCrumbCache();
	}

	public boolean isUseJobInfoCache() {
		return remoteBuildConfig.isUseJobInfoCache();
	}

	public boolean isAbortTriggeredJob() {
		return remoteBuildConfig.isAbortTriggeredJob();
	}

	public int getMaxConn() {
		return remoteBuildConfig.getMaxConn();
	}

	public Auth2 getAuth() {
		return remoteBuildConfig.getAuth2();
	}
	
	public boolean isDisabled() {
		return remoteBuildConfig.isDisabled();
	}


}
