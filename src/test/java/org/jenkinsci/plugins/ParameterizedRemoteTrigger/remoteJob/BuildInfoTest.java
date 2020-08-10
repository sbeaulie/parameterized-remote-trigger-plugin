package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import hudson.model.Result;


public class BuildInfoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void buildStatusTest() {

        RemoteBuildInfo buildInfo = new RemoteBuildInfo();

        assert(buildInfo.isNotTriggered());
        assert(buildInfo.getResult() == Result.NOT_BUILT);
    }

    @Test
    public void illegalBuildStatusTest() {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("It is not possible to set the status to finished without setting the build result. "
                + "Please use BuildInfo(Result result) or BuildInfo(String result) in order to set the status to finished.");

        RemoteBuildInfo buildInfo = new RemoteBuildInfo();
        buildInfo.setBuildStatus(RemoteBuildStatus.FINISHED);
    }

    @Test
    public void buildResultTest() {

        RemoteBuildInfo buildInfo = new RemoteBuildInfo();
        buildInfo.setBuildResult(Result.SUCCESS);

        assert(buildInfo.isFinished());
        assert(buildInfo.getResult() == Result.SUCCESS);
    }

    @Test
    public void stringBuildResultTest() {

        RemoteBuildInfo buildInfo = new RemoteBuildInfo();
        buildInfo.setBuildResult(Result.SUCCESS);

        assert(buildInfo.isFinished());
        assert(buildInfo.getResult() == Result.SUCCESS);
    }

    @Test
    public void buildInfoToStringTest() {

        RemoteBuildInfo buildInfo = new RemoteBuildInfo();

        assert(buildInfo.toString().equals("status=NOT_TRIGGERED"));

        buildInfo = new RemoteBuildInfo();
        buildInfo.setBuildResult(Result.SUCCESS);

        assert(buildInfo.toString().equals("status=FINISHED, result=SUCCESS"));
    }
}
