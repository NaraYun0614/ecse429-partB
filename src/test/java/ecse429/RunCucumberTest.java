package ecse429;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.example.Api;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ecse429")
public class RunCucumberTest {

    @Before
    public void setupEnvironment() {
        Runtime runtime = Runtime.getRuntime();
        try {
            // Launch SUT (expects jar in project root)
            runtime.exec("java -jar runTodoManagerRestAPI-1.5.5.jar");
            // wait briefly or poll until up
            Thread.sleep(1500);
        } catch (Exception e) {
            throw new RuntimeException("Failed starting SUT", e);
        }
    }

//    @After
//    public void shutdownEnvironment() {
//        // Call shutdown endpoint provided by the SUT
//        Api call = new Api();
//        call.getRequest("shutdown", "json");
//    }
}
