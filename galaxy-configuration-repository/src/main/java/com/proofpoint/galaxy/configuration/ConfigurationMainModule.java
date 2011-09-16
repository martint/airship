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
package com.proofpoint.galaxy.configuration;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.galaxy.shared.SlotStatusRepresentation;
import com.proofpoint.json.JsonCodecBinder;

import javax.inject.Scope;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;
import static com.proofpoint.json.JsonCodecBinder.jsonCodecBinder;

public class ConfigurationMainModule
        implements Module
{
    public void configure(Binder binder)
    {
        binder.disableCircularProxies();
        binder.requireExplicitBindings();

        binder.bind(ConfigurationResource.class).in(Scopes.SINGLETON);
        binder.bind(ConfigurationRepository.class).to(GitConfigurationRepository.class).in(Scopes.SINGLETON);
        bindConfig(binder).to(GitConfigurationRepositoryConfig.class);

        binder.bind(DiscoverDiscovery.class).in(Scopes.SINGLETON);
        bindConfig(binder).to(ConfigurationRepositoryConfig.class);
        jsonCodecBinder(binder).bindListJsonCodec(SlotStatusRepresentation.class);
    }
}
