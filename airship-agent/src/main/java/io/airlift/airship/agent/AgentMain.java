/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.airship.agent;

import com.proofpoint.bootstrap.Bootstrap;
import com.proofpoint.discovery.client.DiscoveryModule;
import com.proofpoint.event.client.HttpEventModule;
import com.proofpoint.json.JsonModule;
import com.proofpoint.http.server.HttpServerModule;
import com.proofpoint.jaxrs.JaxrsModule;
import com.proofpoint.jmx.JmxModule;
import com.proofpoint.log.Logger;
import com.proofpoint.node.NodeModule;
import org.weakref.jmx.guice.MBeanModule;

public class AgentMain
{
    private static final Logger log = Logger.get(AgentMain.class);

    public static void main(String[] args)
            throws Exception
    {
        try {
            Bootstrap app = new Bootstrap(
                    new DiscoveryModule(),
                    new NodeModule(),
                    new HttpServerModule(),
                    new HttpEventModule(),
                    new JsonModule(),
                    new JaxrsModule(),
                    new MBeanModule(),
                    new JmxModule(),
                    new AgentMainModule());

            app.strictConfig().initialize();
        }
        catch (Exception e) {
            log.error(e, "Startup failed");
            System.exit(1);
        }
    }
}
