package com.sisibibi.api.domain.report.worker;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AiReportWorkerEc2ServiceTest {

    private final Ec2Client ec2Client = mock(Ec2Client.class);
    private final AiReportWorkerEc2Properties properties = new AiReportWorkerEc2Properties();
    private final AiReportWorkerEc2Service service = new AiReportWorkerEc2Service(ec2Client, properties);

    @Test
    void startWorkerIfEnabled_doesNothing_whenDisabled() {
        properties.setEnabled(false);

        service.startWorkerIfEnabled();

        verifyNoInteractions(ec2Client);
    }

    @Test
    void startWorkerIfEnabled_startsStoppedInstance() {
        properties.setEnabled(true);
        properties.setInstanceId("i-worker");
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .willReturn(describeResponse(InstanceStateName.STOPPED));

        service.startWorkerIfEnabled();

        verify(ec2Client).startInstances(StartInstancesRequest.builder()
                .instanceIds("i-worker")
                .build());
    }

    @Test
    void startWorkerIfEnabled_skipsRunningInstance() {
        properties.setEnabled(true);
        properties.setInstanceId("i-worker");
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .willReturn(describeResponse(InstanceStateName.RUNNING));

        service.startWorkerIfEnabled();

        verify(ec2Client).describeInstances(any(DescribeInstancesRequest.class));
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    void startWorkerIfEnabled_doesNotPropagateEc2Failure() {
        properties.setEnabled(true);
        properties.setInstanceId("i-worker");
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .willThrow(new IllegalStateException("instance not found"));

        assertThatCode(service::startWorkerIfEnabled)
                .doesNotThrowAnyException();
    }

    @Test
    void stopWorkerIfEnabled_stopsRunningInstance() {
        properties.setEnabled(true);
        properties.setInstanceId("i-worker");
        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .willReturn(describeResponse(InstanceStateName.RUNNING));

        service.stopWorkerIfEnabled();

        verify(ec2Client).stopInstances(StopInstancesRequest.builder()
                .instanceIds("i-worker")
                .build());
    }

    private DescribeInstancesResponse describeResponse(InstanceStateName stateName) {
        return DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .state(InstanceState.builder()
                                        .name(stateName)
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
