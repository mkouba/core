package org.jboss.weld.environment.servlet.test.session;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.environment.servlet.test.util.Deployments;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;

@RunAsClient
@RunWith(Arquillian.class)
public class SessionInvalidationTest {

    @Deployment
    public static WebArchive createTestArchive() {
        return Deployments.baseDeployment().addPackage(SessionInvalidationTest.class.getPackage());
    }

    @ArquillianResource
    URL contextPath;

    @Test
    public void testInvalidation() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        WebClient client = new WebClient();
        client.setThrowExceptionOnFailingStatusCode(true);
        // Init session
        client.getPage(contextPath+"test?action=init");
        // Invalidate session
        client.getPage(contextPath+"test");
    }

}
