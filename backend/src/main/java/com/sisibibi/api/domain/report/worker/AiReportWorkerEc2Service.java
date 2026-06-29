package com.sisibibi.api.domain.report.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportWorkerEc2Service {

    private final Ec2Client ec2Client;
    private final AiReportWorkerEc2Properties properties;

    public void startWorkerIfEnabled() {
        if (!isReady()) {
            return;
        }

        try {
            InstanceStateName state = getCurrentState();
            if (state == InstanceStateName.STOPPED) {
                ec2Client.startInstances(StartInstancesRequest.builder()
                        .instanceIds(properties.getInstanceId())
                        .build());
                log.info("Requested AI Worker EC2 start. instanceId={}", properties.getInstanceId());
                return;
            }

            if (state == InstanceStateName.RUNNING || state == InstanceStateName.PENDING) {
                log.debug("AI Worker EC2 start skipped because it is already active. instanceId={}, state={}",
                        properties.getInstanceId(),
                        state);
                return;
            }

            log.info("AI Worker EC2 start skipped. instanceId={}, state={}", properties.getInstanceId(), state);
        } catch (RuntimeException e) {
            log.warn("Failed to start AI Worker EC2. instanceId={}", properties.getInstanceId(), e);
        }
    }

    public void stopWorkerIfEnabled() {
        if (!isReady()) {
            return;
        }

        try {
            InstanceStateName state = getCurrentState();
            if (state == InstanceStateName.RUNNING) {
                ec2Client.stopInstances(StopInstancesRequest.builder()
                        .instanceIds(properties.getInstanceId())
                        .build());
                log.info("Requested AI Worker EC2 stop. instanceId={}", properties.getInstanceId());
                return;
            }

            if (state == InstanceStateName.STOPPED || state == InstanceStateName.STOPPING) {
                log.debug("AI Worker EC2 stop skipped because it is already inactive. instanceId={}, state={}",
                        properties.getInstanceId(),
                        state);
                return;
            }

            log.info("AI Worker EC2 stop skipped. instanceId={}, state={}", properties.getInstanceId(), state);
        } catch (RuntimeException e) {
            log.warn("Failed to stop AI Worker EC2. instanceId={}", properties.getInstanceId(), e);
        }
    }

    private boolean isReady() {
        if (!properties.isEnabled()) {
            return false;
        }

        if (!StringUtils.hasText(properties.getInstanceId())) {
            log.warn("AI Worker EC2 control is enabled, but instance id is missing.");
            return false;
        }

        return true;
    }

    private InstanceStateName getCurrentState() {
        var response = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(properties.getInstanceId())
                .build());

        return response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "AI Worker EC2 instance was not found. instanceId=" + properties.getInstanceId()))
                .state()
                .name();
    }
}
