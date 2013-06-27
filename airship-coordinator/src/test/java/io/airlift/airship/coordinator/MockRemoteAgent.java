package io.airlift.airship.coordinator;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.airlift.airship.coordinator.job.Stream;
import io.airlift.airship.shared.AgentLifecycleState;
import io.airlift.airship.shared.AgentStatus;
import io.airlift.airship.shared.Installation;
import io.airlift.airship.shared.SlotLifecycleState;
import io.airlift.airship.shared.SlotStatus;
import io.airlift.airship.shared.SlotStatusRepresentation;
import io.airlift.airship.shared.job.KillTask;
import io.airlift.airship.shared.job.RestartTask;
import io.airlift.airship.shared.job.SlotJob;
import io.airlift.airship.shared.job.StartTask;
import io.airlift.airship.shared.job.StopTask;
import io.airlift.airship.shared.job.Task;
import io.airlift.discovery.client.ServiceDescriptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.equalTo;
import static io.airlift.airship.shared.AgentLifecycleState.OFFLINE;
import static io.airlift.airship.shared.HttpUriBuilder.uriBuilderFrom;
import static io.airlift.airship.shared.SlotStatus.createSlotStatus;

public class MockRemoteAgent implements RemoteAgent
{
    private final String instanceId;
    private final Map<String, AgentStatus> agents;

    public MockRemoteAgent(String instanceId, Map<String, AgentStatus> agents)
    {
        this.instanceId = instanceId;
        this.agents = agents;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public synchronized AgentStatus status()
    {
        return getAgentStatus();
    }

    @Override
    public synchronized void setInternalUri(URI internalUri)
    {
        setAgentStatus(getAgentStatus().changeInternalUri(internalUri));
    }

    @Override
    public synchronized List<? extends RemoteSlot> getSlots()
    {
        return ImmutableList.copyOf(Iterables.transform(getAgentStatus().getSlotStatuses(), new Function<SlotStatus, MockRemoteSlot>()
        {
            @Override
            public MockRemoteSlot apply(SlotStatus slotStatus)
            {
                return new MockRemoteSlot(slotStatus, MockRemoteAgent.this);
            }
        }));
    }

    synchronized void setSlotStatus(SlotStatus slotStatus)
    {
        AgentStatus agentStatus = getAgentStatus().changeSlotStatus(slotStatus);
        setAgentStatus(agentStatus);
    }

    @Override
    public synchronized void setServiceInventory(List<ServiceDescriptor> serviceInventory)
    {
    }

    @Override
    public RemoteSlotJob createSlotJob(SlotJob slotJob)
    {
        RemoteSlot slot = Stream.on(getSlots())
                .select(Predicates.compose(equalTo(slotJob.getSlotId()), RemoteSlotFunctions.slotIdGetter()))
                .only();

        for (Task task : slotJob.getTasks()) {
            if (task instanceof StopTask) {
                slot.stop();
            }
            else if (task instanceof StartTask) {
                slot.start();
            }
            else if (task instanceof RestartTask) {
                slot.restart();
            }
            else if (task instanceof KillTask) {
                slot.kill();
            }
            else {
                throw new UnsupportedOperationException("not yet implemented: " + task.getClass().getName());
            }
        }

        // todo: what uri to use?
        return new MockRemoteSlotJob(URI.create("http://foo"), slotJob, SlotStatusRepresentation.from(slot.status()));
    }

    @Override
    public synchronized SlotStatus install(Installation installation)
    {
        checkNotNull(installation, "installation is null");

        AgentStatus agentStatus = getAgentStatus();
        Preconditions.checkState(agentStatus.getState() != OFFLINE, "agent is offline");

        UUID slotId = UUID.randomUUID();
        SlotStatus slotStatus = createSlotStatus(slotId,
                uriBuilderFrom(agentStatus.getInternalUri()).appendPath("slot").appendPath(slotId.toString()).build(),
                uriBuilderFrom(agentStatus.getExternalUri()).appendPath("slot").appendPath(slotId.toString()).build(),
                "instance",
                "/location",
                SlotLifecycleState.STOPPED,
                installation.getAssignment(),
                "/" + slotId,
                installation.getResources());
        agentStatus.changeSlotStatus(slotStatus);

        return slotStatus;
    }

    public synchronized AgentStatus getAgentStatus()
    {
        AgentStatus agentStatus = agents.get(instanceId);
        if (agentStatus != null) {
            return agentStatus;
        } else {
            return new AgentStatus(null,
                    AgentLifecycleState.OFFLINE,
                    instanceId,
                    null,
                    null,
                    null,
                    null,
                    ImmutableList.<SlotStatus>of(),
                    ImmutableMap.<String, Integer>of());
        }
    }

    public synchronized void setAgentStatus(AgentStatus agentStatus)
    {
        Preconditions.checkNotNull(agentStatus, "agentStatus is null");
        agents.put(instanceId, agentStatus);
    }
}
