import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BASE_URLS = (__ENV.BASE_URLS || BASE_URL)
  .split(',')
  .map((url) => url.trim())
  .filter((url) => url.length > 0);
const ROOM_ID_START = Number(__ENV.ROOM_ID || '1');
const ROOM_COUNT = Number(__ENV.ROOM_COUNT || '1');
const USER_ID_START = Number(__ENV.USER_ID_START || '1');
const USERS_PER_ROOM = Number(__ENV.USERS_PER_ROOM || '100');
const USER_COUNT = Number(__ENV.USER_COUNT || String(ROOM_COUNT * USERS_PER_ROOM));
const SCENARIO = __ENV.SCENARIO || 'request_spike';
const HTTP_REQ_DURATION_P95_THRESHOLD_MS = Number(
  __ENV.HTTP_REQ_DURATION_P95_THRESHOLD_MS || '1000'
);
const HTTP_REQ_DURATION_P99_THRESHOLD_MS = Number(
  __ENV.HTTP_REQ_DURATION_P99_THRESHOLD_MS || '2000'
);
const BATCHED_PREFILL_BATCH_SIZE = Number(__ENV.BATCHED_PREFILL_BATCH_SIZE || '250');
const BATCHED_PREFILL_BATCH_COUNT = Number(__ENV.BATCHED_PREFILL_BATCH_COUNT || '2');
const BATCHED_PREFILL_USERS_PER_ROOM = Number(
  __ENV.BATCHED_PREFILL_USERS_PER_ROOM || String(BATCHED_PREFILL_BATCH_SIZE / ROOM_COUNT)
);
const BATCHED_PREFILL_TOTAL_USERS = BATCHED_PREFILL_BATCH_SIZE * BATCHED_PREFILL_BATCH_COUNT;
const EXPIRATION_ROOM_COUNT = Number(__ENV.EXPIRATION_ROOM_COUNT || '10');
const EXPIRATION_WAITING_PER_ROOM = Number(__ENV.EXPIRATION_WAITING_PER_ROOM || '1');
const EXPIRATION_RUN_VUS = Number(__ENV.EXPIRATION_RUN_VUS || '1');
const EXPIRATION_RUN_ITERATIONS = Number(__ENV.EXPIRATION_RUN_ITERATIONS || '1');
const EXPIRATION_RACE_VUS = Number(__ENV.EXPIRATION_RACE_VUS || '1');
const EXPIRATION_RACE_ITERATIONS = Number(__ENV.EXPIRATION_RACE_ITERATIONS || '100');
const CONTINUOUS_REQUEST_RATE = Number(__ENV.CONTINUOUS_REQUEST_RATE || '25');
const CONTINUOUS_REQUEST_TIME_UNIT = __ENV.CONTINUOUS_REQUEST_TIME_UNIT || '1s';
const CONTINUOUS_DURATION = __ENV.CONTINUOUS_DURATION || '3m';
const CONTINUOUS_REQUEST_PRE_ALLOCATED_VUS = Number(
  __ENV.CONTINUOUS_REQUEST_PRE_ALLOCATED_VUS || '100'
);
const CONTINUOUS_REQUEST_MAX_VUS = Number(__ENV.CONTINUOUS_REQUEST_MAX_VUS || '300');
const CONTINUOUS_ROTATION_VUS = Number(__ENV.CONTINUOUS_ROTATION_VUS || String(ROOM_COUNT));
const CONTINUOUS_ROTATION_INITIAL_DELAY = __ENV.CONTINUOUS_ROTATION_INITIAL_DELAY || '20s';
const CONTINUOUS_ROTATION_INTERVAL_SECONDS = Number(
  __ENV.CONTINUOUS_ROTATION_INTERVAL_SECONDS || '20'
);
const OPENING_BURST_REQUEST_RATE = Number(__ENV.OPENING_BURST_REQUEST_RATE || '100');
const OPENING_BURST_REQUEST_TIME_UNIT = __ENV.OPENING_BURST_REQUEST_TIME_UNIT || '1s';
const OPENING_BURST_DURATION = __ENV.OPENING_BURST_DURATION || '10s';
const OPENING_BURST_PRE_ALLOCATED_VUS = Number(
  __ENV.OPENING_BURST_PRE_ALLOCATED_VUS || '300'
);
const OPENING_BURST_MAX_VUS = Number(__ENV.OPENING_BURST_MAX_VUS || '1000');
const OPENING_OPERATION_DURATION = __ENV.OPENING_OPERATION_DURATION || '3m';
const POSITION_POLLING_VUS = Number(__ENV.POSITION_POLLING_VUS || '20');
const POSITION_POLLING_START_TIME = __ENV.POSITION_POLLING_START_TIME || '10s';
const POSITION_POLLING_SLEEP_MIN_SECONDS = Number(
  __ENV.POSITION_POLLING_SLEEP_MIN_SECONDS || '1'
);
const POSITION_POLLING_SLEEP_MAX_SECONDS = Number(
  __ENV.POSITION_POLLING_SLEEP_MAX_SECONDS || '3'
);

const requestFailures = new Counter('stage_request_failures');
const requestStatus201 = new Counter('stage_request_status_201');
const requestStatus409 = new Counter('stage_request_status_409');
const requestStatusOther2xx = new Counter('stage_request_status_other_2xx');
const requestStatusOther4xx = new Counter('stage_request_status_other_4xx');
const requestStatus5xx = new Counter('stage_request_status_5xx');
const requestStatusNetworkError = new Counter('stage_request_status_network_error');
const requestStatusOther = new Counter('stage_request_status_other');
const cancelFailures = new Counter('stage_cancel_failures');
const completeFailures = new Counter('stage_complete_failures');
const queueLookupFailures = new Counter('stage_queue_lookup_failures');
const positionLookupFailures = new Counter('stage_position_lookup_failures');
const expirationFailures = new Counter('stage_expiration_failures');
const stageFailureRate = new Rate('stage_failure_rate');
const queueResponseBytes = new Trend('stage_queue_response_bytes');
const positionResponseBytes = new Trend('stage_position_response_bytes');
const expirationCandidateRooms = new Trend('stage_expiration_candidate_rooms');
const expirationExpiredRooms = new Trend('stage_expiration_expired_rooms');
const expirationElapsedMs = new Trend('stage_expiration_elapsed_ms');
const expirationAvgPerRoomMs = new Trend('stage_expiration_avg_per_room_ms');
const expirationRaceVerifyFailures = new Counter('stage_expiration_race_verify_failures');
const expirationRaceTerminalCount = new Trend('stage_expiration_race_terminal_count');
const expirationRaceAssignedCount = new Trend('stage_expiration_race_assigned_count');
const currentSpeakerLookupFailures = new Counter('stage_current_speaker_lookup_failures');
const continuousTurnRotationFailures = new Counter('stage_continuous_turn_rotation_failures');
const continuousTurnCompleteAttempts = new Counter('stage_continuous_turn_complete_attempts');
const continuousTurnCompleteSuccess = new Counter('stage_continuous_turn_complete_success');
const continuousTurnNoCurrentSpeaker = new Counter('stage_continuous_turn_no_current_speaker');
const continuousPositionLookupFailures = new Counter('stage_continuous_position_lookup_failures');
const continuousPositionLookupStatus200 = new Counter('stage_continuous_position_lookup_status_200');
const continuousPositionLookupStatus404 = new Counter('stage_continuous_position_lookup_status_404');

export const options = {
  scenarios: buildScenarios(SCENARIO),
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: [
      `p(95)<${HTTP_REQ_DURATION_P95_THRESHOLD_MS}`,
      `p(99)<${HTTP_REQ_DURATION_P99_THRESHOLD_MS}`,
    ],
    stage_failure_rate: ['rate<0.05'],
  },
};

export function setup() {
  if (usesExpirationPrepare(SCENARIO)) {
    prepareExpirationCandidates();
    return;
  }

  if (usesExpirationRacePrepare(SCENARIO)) {
    prepareExpirationRaceFixtures();
    return;
  }

  if (!usesHotRoomSetupPrefill(SCENARIO)) {
    return;
  }

  prefillBatch(0);
  prefillBatch(BATCHED_PREFILL_BATCH_SIZE);
}

export function requestSpike() {
  const target = targetForIteration();

  group('request speaking turn spike', () => {
    const response = requestSpeakingTurn(target);
    recordRequestStatus(response);
    const ok = check(response, {
      'request status is 201 or already active': (res) => res.status === 201 || res.status === 409,
    });

    recordFailure(ok, requestFailures);
  });
}

export function continuousRequestArrival() {
  const target = targetForContinuousIteration();

  group('continuous request speaking turn arrival', () => {
    const response = requestSpeakingTurn(target);
    recordRequestStatus(response);
    const ok = check(response, {
      'continuous request status is 201 or already active': (res) => (
        res.status === 201 || res.status === 409
      ),
    });

    recordFailure(ok, requestFailures);
  });
}

export function intermittentPositionPolling() {
  const target = targetForPositionPolling();

  group('intermittent my position polling', () => {
    const response = getMyPositionAllowNotFound(target);
    const ok = check(response, {
      'intermittent my position status is 200 or already finished': (res) => (
        res.status === 200 || res.status === 404
      ),
    });

    if (response.status === 200) {
      continuousPositionLookupStatus200.add(1);
    }

    if (response.status === 404) {
      continuousPositionLookupStatus404.add(1);
    }

    positionResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, continuousPositionLookupFailures);
    sleep(randomBetween(POSITION_POLLING_SLEEP_MIN_SECONDS, POSITION_POLLING_SLEEP_MAX_SECONDS));
  });
}

export function cancelReRequest() {
  const target = targetForIteration();

  group('cancel and request again churn', () => {
    const cancelResponse = cancelMyRequest(target);
    const cancelOk = check(cancelResponse, {
      'cancel status is 200 or not found': (res) => res.status === 200 || res.status === 404,
    });
    recordFailure(cancelOk, cancelFailures);

    sleep(0.1);

    const requestResponse = requestSpeakingTurn(target);
    recordRequestStatus(requestResponse);
    const requestOk = check(requestResponse, {
      're-request status is 201 or already active': (res) => (
        res.status === 201 || res.status === 409
      ),
    });
    recordFailure(requestOk, requestFailures);
  });
}

export function speakingTurnCycle() {
  const target = targetForIteration();

  group('complete current speaker and auto grant next', () => {
    const completeResponse = completeCurrentSpeaker(target);
    const completeOk = check(completeResponse, {
      'complete status is 200 or not current speaker': (res) => (
        res.status === 200 || res.status === 403 || res.status === 404
      ),
    });
    recordFailure(completeOk, completeFailures);

    sleep(0.1);

    const currentSpeakerResponse = getCurrentSpeaker(target.roomId);
    check(currentSpeakerResponse, {
      'current speaker status is 200 or empty': (res) => res.status === 200 || res.status === 404,
    });
  });
}

export function queueLookup() {
  const target = targetForIteration();

  group('waiting queue lookup', () => {
    const response = getWaitingQueue(target.roomId);
    const ok = check(response, {
      'queue lookup status is 200': (res) => res.status === 200,
    });

    queueResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, queueLookupFailures);
  });
}

export function myPositionLookup() {
  const target = targetForIteration();

  group('my position lookup', () => {
    const response = getMyPosition(target);
    const ok = check(response, {
      'my position lookup status is 200': (res) => res.status === 200,
    });

    positionResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, positionLookupFailures);
  });
}

export function batchedPrefillBatch1() {
  requestBatchedPrefill(0);
}

export function batchedPrefillBatch2() {
  requestBatchedPrefill(BATCHED_PREFILL_BATCH_SIZE);
}

export function batchedQueueLookup() {
  const roomId = roomIdForIteration();

  group('batched prefill waiting queue lookup', () => {
    const response = getWaitingQueue(roomId);
    const ok = check(response, {
      'batched queue lookup status is 200': (res) => res.status === 200,
    });

    queueResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, queueLookupFailures);
  });
}

export function batchedMyPositionLookup() {
  const target = targetForBatchedPrefillTotal();

  group('batched prefill my position lookup', () => {
    const response = getMyPosition(target);
    const ok = check(response, {
      'batched my position lookup status is 200': (res) => res.status === 200,
    });

    positionResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, positionLookupFailures);
  });
}

export function batchedSpeakingTurnCycle() {
  const target = currentSpeakerForBatchedPrefill();

  group('batched prefill complete current speaker and auto grant next', () => {
    const completeResponse = completeCurrentSpeaker(target);
    const completeOk = check(completeResponse, {
      'batched complete status is 200 or not current speaker': (res) => (
        res.status === 200 || res.status === 403 || res.status === 404
      ),
    });
    recordFailure(completeOk, completeFailures);

    sleep(0.1);

    const currentSpeakerResponse = getCurrentSpeaker(target.roomId);
    check(currentSpeakerResponse, {
      'batched current speaker status is 200 or empty': (res) => (
        res.status === 200 || res.status === 404
      ),
    });
  });
}

export function hotRoomQueueLookupAfterSetup() {
  const roomId = ROOM_ID_START;

  group('hot room waiting queue lookup after setup prefill', () => {
    const response = getWaitingQueue(roomId);
    const ok = check(response, {
      'hot room queue lookup status is 200': (res) => res.status === 200,
    });

    queueResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, queueLookupFailures);
  });
}

export function hotRoomMyPositionLookupAfterSetup() {
  const target = targetForSetupPrefillTotal();

  group('hot room my position lookup after setup prefill', () => {
    const response = getMyPosition(target);
    const ok = check(response, {
      'hot room my position lookup status is 200': (res) => res.status === 200,
    });

    positionResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, positionLookupFailures);
  });
}

export function hotRoomSpeakingTurnCycleAfterSetup() {
  const target = {
    roomId: ROOM_ID_START,
    userId: USER_ID_START,
  };

  group('hot room complete current speaker and auto grant next after setup prefill', () => {
    const completeResponse = completeCurrentSpeaker(target);
    const completeOk = check(completeResponse, {
      'hot room complete status is 200 or not current speaker': (res) => (
        res.status === 200 || res.status === 403 || res.status === 404
      ),
    });
    recordFailure(completeOk, completeFailures);

    sleep(0.1);

    const currentSpeakerResponse = getCurrentSpeaker(target.roomId);
    check(currentSpeakerResponse, {
      'hot room current speaker status is 200 or empty': (res) => (
        res.status === 200 || res.status === 404
      ),
    });
  });
}

export function expirationCandidatesRun() {
  group('expiration candidates run', () => {
    const response = runExpiration();
    const ok = check(response, {
      'expiration run status is 200': (res) => res.status === 200,
      'expiration run has no failed room': (res) => {
        const failureCount = Number(res.json('data.failureCount') || 0);
        return failureCount === 0;
      },
      'expiration run expires all candidates': (res) => {
        const candidateRoomCount = Number(res.json('data.candidateRoomCount') || 0);
        const expiredCount = Number(res.json('data.expiredCount') || 0);
        return candidateRoomCount === expiredCount;
      },
    });

    recordExpirationMetrics(response, ok);
  });
}

export function expirationRaceCompleteVsRun() {
  const target = targetForExpirationRaceIteration();

  group('complete current speaker vs expiration race', () => {
    const [completeResponse, expirationResponse] = http.batch([
      [
        'POST',
        stageUrl(target.roomId, `/complete?userId=${target.currentSpeakerUserId}`),
        null,
        requestParams(target.roomId, [200, 403, 404]),
      ],
      [
        'POST',
        loadTestUrl('/stage/expiration/run'),
        null,
        loadTestParams([200]),
      ],
    ]);

    const raceOk = check(null, {
      'expiration race complete is terminal-safe': () => (
        completeResponse.status === 200
        || completeResponse.status === 403
        || completeResponse.status === 404
      ),
      'expiration race run status is 200': () => expirationResponse.status === 200,
    });
    recordFailure(raceOk, expirationFailures);

    const verifyResponse = verifyExpirationRace(target.roomId);
    const verifyOk = check(verifyResponse, {
      'expiration race verify status is 200': (res) => res.status === 200,
      'expiration race final state is valid': (res) => res.json('data.valid') === true,
    });
    recordExpirationRaceVerifyMetrics(verifyResponse, verifyOk);
  });
}

export function continuousSpeakingTurnRotation() {
  const roomId = roomIdForVu();

  group('continuous speaking turn rotation', () => {
    const currentSpeakerResponse = getCurrentSpeaker(roomId);
    const lookupOk = check(currentSpeakerResponse, {
      'continuous current speaker status is 200 or empty': (res) => (
        res.status === 200 || res.status === 404
      ),
    });
    recordFailure(lookupOk, currentSpeakerLookupFailures);

    if (!lookupOk) {
      sleep(CONTINUOUS_ROTATION_INTERVAL_SECONDS);
      return;
    }

    if (currentSpeakerResponse.status === 404) {
      continuousTurnNoCurrentSpeaker.add(1);
      sleep(CONTINUOUS_ROTATION_INTERVAL_SECONDS);
      return;
    }

    const currentSpeakerUserId = currentSpeakerUserIdFrom(currentSpeakerResponse);
    if (currentSpeakerUserId === 0) {
      recordFailure(false, continuousTurnRotationFailures);
      sleep(CONTINUOUS_ROTATION_INTERVAL_SECONDS);
      return;
    }

    continuousTurnCompleteAttempts.add(1);

    const completeResponse = completeCurrentSpeaker({
      roomId,
      userId: currentSpeakerUserId,
    });
    const completeOk = check(completeResponse, {
      'continuous complete status is 200 or already rotated': (res) => (
        res.status === 200 || res.status === 403 || res.status === 404
      ),
    });

    if (completeResponse.status === 200) {
      continuousTurnCompleteSuccess.add(1);
    }

    recordFailure(completeOk, continuousTurnRotationFailures);
    sleep(CONTINUOUS_ROTATION_INTERVAL_SECONDS);
  });
}

function buildScenarios(selectedScenario) {
  const scenarioMap = {
    request_spike: {
      executor: 'shared-iterations',
      exec: 'requestSpike',
      vus: Number(__ENV.REQUEST_SPIKE_VUS || USER_COUNT),
      iterations: Number(__ENV.REQUEST_SPIKE_ITERATIONS || USER_COUNT),
      maxDuration: __ENV.REQUEST_SPIKE_MAX_DURATION || '1m',
    },
    service_capacity_request_spike: {
      executor: 'shared-iterations',
      exec: 'requestSpike',
      vus: Number(__ENV.SERVICE_CAPACITY_VUS || USER_COUNT),
      iterations: Number(__ENV.SERVICE_CAPACITY_ITERATIONS || USER_COUNT),
      maxDuration: __ENV.SERVICE_CAPACITY_MAX_DURATION || '2m',
    },
    request_arrival_rate: {
      executor: 'constant-arrival-rate',
      exec: 'requestSpike',
      rate: Number(__ENV.REQUEST_ARRIVAL_RATE || '50'),
      timeUnit: __ENV.REQUEST_ARRIVAL_TIME_UNIT || '1s',
      duration: __ENV.REQUEST_ARRIVAL_DURATION || '10s',
      preAllocatedVUs: Number(__ENV.REQUEST_ARRIVAL_PRE_ALLOCATED_VUS || '100'),
      maxVUs: Number(__ENV.REQUEST_ARRIVAL_MAX_VUS || '500'),
    },
    cancel_re_request: {
      executor: 'shared-iterations',
      exec: 'cancelReRequest',
      vus: Number(__ENV.CHURN_VUS || '20'),
      iterations: Number(__ENV.CHURN_ITERATIONS || USER_COUNT),
      maxDuration: __ENV.CHURN_MAX_DURATION || '3m',
    },
    speaking_turn_cycle: {
      executor: 'shared-iterations',
      exec: 'speakingTurnCycle',
      vus: Number(__ENV.CYCLE_VUS || '1'),
      iterations: Number(__ENV.CYCLE_ITERATIONS || USER_COUNT),
      maxDuration: __ENV.CYCLE_MAX_DURATION || '3m',
    },
    queue_lookup: {
      executor: 'constant-vus',
      exec: 'queueLookup',
      vus: Number(__ENV.QUEUE_LOOKUP_VUS || '10'),
      duration: __ENV.QUEUE_LOOKUP_DURATION || '30s',
    },
    my_position_lookup: {
      executor: 'constant-vus',
      exec: 'myPositionLookup',
      vus: Number(__ENV.POSITION_LOOKUP_VUS || '10'),
      duration: __ENV.POSITION_LOOKUP_DURATION || '30s',
    },
    batched_prefill_500_then_operate: buildBatchedPrefillScenario(),
    hot_room_batched_prefill_500_then_operate: buildBatchedPrefillScenario(),
    hot_room_setup_prefill_500_then_operate: buildHotRoomSetupPrefillScenario(),
    hot_room_position_polling_500: buildHotRoomPositionPollingScenario(),
    hot_room_queue_polling_500: buildHotRoomQueuePollingScenario(),
    expiration_candidates_run: buildExpirationCandidatesRunScenario(),
    expiration_race_complete_vs_run: buildExpirationRaceScenario(),
    continuous_stage_operation: buildContinuousStageOperationScenario(),
    room_opening_burst_then_operate: buildRoomOpeningBurstThenOperateScenario(),
  };

  if (selectedScenario === 'all') {
    return {
      request_spike: {
        ...scenarioMap.request_spike,
        startTime: '0s',
      },
      service_capacity_request_spike: {
        ...scenarioMap.service_capacity_request_spike,
        startTime: '70s',
      },
      cancel_re_request: {
        ...scenarioMap.cancel_re_request,
        startTime: '200s',
      },
      speaking_turn_cycle: {
        ...scenarioMap.speaking_turn_cycle,
        startTime: '390s',
      },
      queue_lookup: {
        ...scenarioMap.queue_lookup,
        startTime: '590s',
      },
      my_position_lookup: {
        ...scenarioMap.my_position_lookup,
        startTime: '630s',
      },
    };
  }

  if (!scenarioMap[selectedScenario]) {
    throw new Error(
      `Unknown SCENARIO=${selectedScenario}. `
        + 'Use request_spike, service_capacity_request_spike, request_arrival_rate, '
        + 'cancel_re_request, '
        + 'speaking_turn_cycle, queue_lookup, my_position_lookup, '
        + 'batched_prefill_500_then_operate, hot_room_batched_prefill_500_then_operate, '
        + 'hot_room_setup_prefill_500_then_operate, hot_room_position_polling_500, '
        + 'hot_room_queue_polling_500, expiration_candidates_run, '
        + 'expiration_race_complete_vs_run, continuous_stage_operation, '
        + 'room_opening_burst_then_operate, '
        + 'or all.'
    );
  }

  if (
    selectedScenario === 'batched_prefill_500_then_operate'
    || selectedScenario === 'hot_room_batched_prefill_500_then_operate'
  ) {
    return scenarioMap[selectedScenario];
  }

  if (selectedScenario === 'hot_room_setup_prefill_500_then_operate') {
    return scenarioMap[selectedScenario];
  }

  if (
    selectedScenario === 'hot_room_position_polling_500'
    || selectedScenario === 'hot_room_queue_polling_500'
    || selectedScenario === 'expiration_candidates_run'
    || selectedScenario === 'expiration_race_complete_vs_run'
    || selectedScenario === 'continuous_stage_operation'
    || selectedScenario === 'room_opening_burst_then_operate'
  ) {
    return scenarioMap[selectedScenario];
  }

  return {
    [selectedScenario]: scenarioMap[selectedScenario],
  };
}

function usesHotRoomSetupPrefill(selectedScenario) {
  return (
    selectedScenario === 'hot_room_setup_prefill_500_then_operate'
    || selectedScenario === 'hot_room_position_polling_500'
    || selectedScenario === 'hot_room_queue_polling_500'
  );
}

function usesExpirationPrepare(selectedScenario) {
  return selectedScenario === 'expiration_candidates_run';
}

function usesExpirationRacePrepare(selectedScenario) {
  return selectedScenario === 'expiration_race_complete_vs_run';
}

function buildHotRoomSetupPrefillScenario() {
  return {
    hot_room_queue_lookup_after_setup: {
      executor: 'shared-iterations',
      exec: 'hotRoomQueueLookupAfterSetup',
      vus: Number(__ENV.BATCHED_QUEUE_LOOKUP_VUS || '10'),
      iterations: Number(__ENV.BATCHED_QUEUE_LOOKUP_ITERATIONS || '10'),
      maxDuration: __ENV.BATCHED_QUEUE_LOOKUP_MAX_DURATION || '1m',
      startTime: '0s',
    },
    hot_room_my_position_lookup_after_setup: {
      executor: 'shared-iterations',
      exec: 'hotRoomMyPositionLookupAfterSetup',
      vus: Number(__ENV.BATCHED_POSITION_LOOKUP_VUS || '10'),
      iterations: Number(__ENV.BATCHED_POSITION_LOOKUP_ITERATIONS || String(BATCHED_PREFILL_TOTAL_USERS)),
      maxDuration: __ENV.BATCHED_POSITION_LOOKUP_MAX_DURATION || '2m',
      startTime: '1s',
    },
    hot_room_speaking_turn_cycle_after_setup: {
      executor: 'shared-iterations',
      exec: 'hotRoomSpeakingTurnCycleAfterSetup',
      vus: Number(__ENV.BATCHED_CYCLE_VUS || '1'),
      iterations: Number(__ENV.BATCHED_CYCLE_ITERATIONS || '1'),
      maxDuration: __ENV.BATCHED_CYCLE_MAX_DURATION || '1m',
      startTime: '3s',
    },
  };
}

function buildHotRoomPositionPollingScenario() {
  return {
    hot_room_position_polling_500: {
      executor: 'constant-vus',
      exec: 'hotRoomMyPositionLookupAfterSetup',
      vus: Number(__ENV.HOT_ROOM_POSITION_POLLING_VUS || '50'),
      duration: __ENV.HOT_ROOM_POSITION_POLLING_DURATION || '30s',
      startTime: '0s',
    },
  };
}

function buildHotRoomQueuePollingScenario() {
  return {
    hot_room_queue_polling_500: {
      executor: 'constant-vus',
      exec: 'hotRoomQueueLookupAfterSetup',
      vus: Number(__ENV.HOT_ROOM_QUEUE_POLLING_VUS || '10'),
      duration: __ENV.HOT_ROOM_QUEUE_POLLING_DURATION || '30s',
      startTime: '0s',
    },
  };
}

function buildExpirationCandidatesRunScenario() {
  return {
    expiration_candidates_run: {
      executor: 'shared-iterations',
      exec: 'expirationCandidatesRun',
      vus: EXPIRATION_RUN_VUS,
      iterations: EXPIRATION_RUN_ITERATIONS,
      maxDuration: __ENV.EXPIRATION_RUN_MAX_DURATION || '2m',
      startTime: '0s',
    },
  };
}

function buildExpirationRaceScenario() {
  return {
    expiration_race_complete_vs_run: {
      executor: 'shared-iterations',
      exec: 'expirationRaceCompleteVsRun',
      vus: EXPIRATION_RACE_VUS,
      iterations: EXPIRATION_RACE_ITERATIONS,
      maxDuration: __ENV.EXPIRATION_RACE_MAX_DURATION || '3m',
      startTime: '0s',
    },
  };
}

function buildContinuousStageOperationScenario() {
  return {
    continuous_request_arrival: {
      executor: 'constant-arrival-rate',
      exec: 'continuousRequestArrival',
      rate: CONTINUOUS_REQUEST_RATE,
      timeUnit: CONTINUOUS_REQUEST_TIME_UNIT,
      duration: CONTINUOUS_DURATION,
      preAllocatedVUs: CONTINUOUS_REQUEST_PRE_ALLOCATED_VUS,
      maxVUs: CONTINUOUS_REQUEST_MAX_VUS,
      startTime: '0s',
    },
    continuous_speaking_turn_rotation: {
      executor: 'constant-vus',
      exec: 'continuousSpeakingTurnRotation',
      vus: CONTINUOUS_ROTATION_VUS,
      duration: CONTINUOUS_DURATION,
      startTime: CONTINUOUS_ROTATION_INITIAL_DELAY,
    },
  };
}

function buildRoomOpeningBurstThenOperateScenario() {
  return {
    opening_request_burst: {
      executor: 'constant-arrival-rate',
      exec: 'continuousRequestArrival',
      rate: OPENING_BURST_REQUEST_RATE,
      timeUnit: OPENING_BURST_REQUEST_TIME_UNIT,
      duration: OPENING_BURST_DURATION,
      preAllocatedVUs: OPENING_BURST_PRE_ALLOCATED_VUS,
      maxVUs: OPENING_BURST_MAX_VUS,
      startTime: '0s',
    },
    opening_position_polling: {
      executor: 'constant-vus',
      exec: 'intermittentPositionPolling',
      vus: POSITION_POLLING_VUS,
      duration: OPENING_OPERATION_DURATION,
      startTime: POSITION_POLLING_START_TIME,
    },
    opening_speaking_turn_rotation: {
      executor: 'constant-vus',
      exec: 'continuousSpeakingTurnRotation',
      vus: CONTINUOUS_ROTATION_VUS,
      duration: OPENING_OPERATION_DURATION,
      startTime: CONTINUOUS_ROTATION_INITIAL_DELAY,
    },
  };
}

function buildBatchedPrefillScenario() {
  return {
    batched_prefill_batch_1: {
      executor: 'shared-iterations',
      exec: 'batchedPrefillBatch1',
      vus: Number(__ENV.BATCHED_PREFILL_VUS || BATCHED_PREFILL_BATCH_SIZE),
      iterations: BATCHED_PREFILL_BATCH_SIZE,
      maxDuration: __ENV.BATCHED_PREFILL_MAX_DURATION || '2m',
      startTime: '0s',
    },
    batched_prefill_batch_2: {
      executor: 'shared-iterations',
      exec: 'batchedPrefillBatch2',
      vus: Number(__ENV.BATCHED_PREFILL_VUS || BATCHED_PREFILL_BATCH_SIZE),
      iterations: BATCHED_PREFILL_BATCH_SIZE,
      maxDuration: __ENV.BATCHED_PREFILL_MAX_DURATION || '2m',
      startTime: __ENV.BATCHED_PREFILL_BATCH_2_START_TIME || '10s',
    },
    batched_queue_lookup: {
      executor: 'shared-iterations',
      exec: 'batchedQueueLookup',
      vus: Number(__ENV.BATCHED_QUEUE_LOOKUP_VUS || '10'),
      iterations: Number(__ENV.BATCHED_QUEUE_LOOKUP_ITERATIONS || String(ROOM_COUNT)),
      maxDuration: __ENV.BATCHED_QUEUE_LOOKUP_MAX_DURATION || '1m',
      startTime: __ENV.BATCHED_QUEUE_LOOKUP_START_TIME || '25s',
    },
    batched_my_position_lookup: {
      executor: 'shared-iterations',
      exec: 'batchedMyPositionLookup',
      vus: Number(__ENV.BATCHED_POSITION_LOOKUP_VUS || '10'),
      iterations: Number(__ENV.BATCHED_POSITION_LOOKUP_ITERATIONS || String(BATCHED_PREFILL_TOTAL_USERS)),
      maxDuration: __ENV.BATCHED_POSITION_LOOKUP_MAX_DURATION || '2m',
      startTime: __ENV.BATCHED_POSITION_LOOKUP_START_TIME || '50s',
    },
    batched_speaking_turn_cycle: {
      executor: 'shared-iterations',
      exec: 'batchedSpeakingTurnCycle',
      vus: Number(__ENV.BATCHED_CYCLE_VUS || String(ROOM_COUNT)),
      iterations: Number(__ENV.BATCHED_CYCLE_ITERATIONS || String(ROOM_COUNT)),
      maxDuration: __ENV.BATCHED_CYCLE_MAX_DURATION || '1m',
      startTime: __ENV.BATCHED_CYCLE_START_TIME || '75s',
    },
  };
}

function requestSpeakingTurn(target) {
  return http.post(
    stageUrl(target.roomId, `/requests?userId=${target.userId}`),
    null,
    requestParams(target.roomId, [{ min: 200, max: 399 }, 409])
  );
}

function requestBatchedPrefill(userOffset) {
  const target = targetForBatchedPrefillBatch(userOffset);

  group('batched prefill request speaking turn', () => {
    const response = requestSpeakingTurn(target);
    recordRequestStatus(response);
    const ok = check(response, {
      'batched prefill request status is 201 or already active': (res) => (
        res.status === 201 || res.status === 409
      ),
    });

    recordFailure(ok, requestFailures);
  });
}

function prefillBatch(userOffset) {
  const requests = [];

  for (let index = 0; index < BATCHED_PREFILL_BATCH_SIZE; index += 1) {
    const userId = USER_ID_START + userOffset + index;
    const baseUrl = baseUrlForIndex(index);
    requests.push([
      'POST',
      stageUrl(ROOM_ID_START, `/requests?userId=${userId}`, baseUrl),
      null,
      requestParams(ROOM_ID_START, [{ min: 200, max: 399 }, 409], baseUrl),
    ]);
  }

  const responses = http.batch(requests);
  for (const response of responses) {
    recordRequestStatus(response);
    const ok = response.status === 201 || response.status === 409;
    recordFailure(ok, requestFailures);
  }
}

function cancelMyRequest(target) {
  return http.del(
    stageUrl(target.roomId, `/requests/me?userId=${target.userId}`),
    null,
    requestParams(target.roomId, [200, 404])
  );
}

function completeCurrentSpeaker(target) {
  return http.post(
    stageUrl(target.roomId, `/complete?userId=${target.userId}`),
    null,
    requestParams(target.roomId, [200, 403, 404])
  );
}

function getCurrentSpeaker(roomId) {
  return http.get(stageUrl(roomId, ''), requestParams(roomId, [200, 404]));
}

function getWaitingQueue(roomId) {
  return http.get(stageUrl(roomId, '/queue'), requestParams(roomId, [200]));
}

function getMyPosition(target) {
  return http.get(
    stageUrl(target.roomId, `/requests/me/position?userId=${target.userId}`),
    requestParams(target.roomId, [200])
  );
}

function getMyPositionAllowNotFound(target) {
  return http.get(
    stageUrl(target.roomId, `/requests/me/position?userId=${target.userId}`),
    requestParams(target.roomId, [200, 404])
  );
}

function prepareExpirationCandidates() {
  const response = http.post(
    loadTestUrl(
      '/stage/expiration/prepare'
        + `?roomCount=${EXPIRATION_ROOM_COUNT}`
        + `&waitingPerRoom=${EXPIRATION_WAITING_PER_ROOM}`
        + `&roomIdStart=${ROOM_ID_START}`
        + `&userIdStart=${USER_ID_START}`
    ),
    null,
    loadTestParams([201])
  );

  const ok = check(response, {
    'expiration prepare status is 201': (res) => res.status === 201,
  });
  recordFailure(ok, expirationFailures);
}

function prepareExpirationRaceFixtures() {
  for (let offset = 0; offset < EXPIRATION_RACE_ITERATIONS; offset += 1) {
    const target = targetForExpirationRaceOffset(offset);
    const response = prepareExpirationRace(target);
    const ok = check(response, {
      'expiration race prepare status is 201': (res) => res.status === 201,
    });
    recordFailure(ok, expirationFailures);
  }
}

function runExpiration() {
  return http.post(
    loadTestUrl('/stage/expiration/run'),
    null,
    loadTestParams([200])
  );
}

function prepareExpirationRace(target) {
  return http.post(
    loadTestUrl(
      '/stage/expiration/race/prepare'
        + `?roomId=${target.roomId}`
        + `&currentSpeakerUserId=${target.currentSpeakerUserId}`
        + `&nextSpeakerUserId=${target.nextSpeakerUserId}`
    ),
    null,
    loadTestParams([201])
  );
}

function verifyExpirationRace(roomId) {
  return http.post(
    loadTestUrl(`/stage/expiration/race/verify?roomId=${roomId}`),
    null,
    loadTestParams([200])
  );
}

function stageUrl(roomId, path, baseUrl = baseUrlForCurrentIteration()) {
  return stageUrlForBase(baseUrl, roomId, path);
}

function loadTestUrl(path) {
  return `${baseUrlForCurrentIteration()}/api/load-test${path}`;
}

function stageUrlForBase(baseUrl, roomId, path) {
  return `${baseUrl}/api/v1/rooms/${roomId}/stage${path}`;
}

function baseUrlForCurrentIteration() {
  return baseUrlForIndex(iterationIndexOrZero());
}

function baseUrlForIndex(index) {
  return BASE_URLS[index % BASE_URLS.length];
}

function iterationIndexOrZero() {
  try {
    return exec.scenario.iterationInTest || 0;
  } catch (error) {
    return 0;
  }
}

function requestParams(
  roomId,
  expectedStatuses = [{ min: 200, max: 399 }],
  baseUrl = baseUrlForCurrentIteration()
) {
  return {
    headers: {
      Accept: 'application/json',
    },
    responseCallback: http.expectedStatuses(...expectedStatuses),
    tags: {
      room_id: String(roomId),
      scenario: SCENARIO,
      target_base_url: baseUrl,
    },
  };
}

function loadTestParams(expectedStatuses = [{ min: 200, max: 399 }]) {
  const baseUrl = baseUrlForCurrentIteration();
  return {
    headers: {
      Accept: 'application/json',
    },
    responseCallback: http.expectedStatuses(...expectedStatuses),
    tags: {
      scenario: SCENARIO,
      target_base_url: baseUrl,
    },
  };
}

function targetForIteration() {
  const offset = exec.scenario.iterationInTest % USER_COUNT;
  const roomIndex = Math.min(Math.floor(offset / USERS_PER_ROOM), ROOM_COUNT - 1);

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + offset,
  };
}

function targetForContinuousIteration() {
  const iteration = exec.scenario.iterationInTest;
  const roomIndex = iteration % ROOM_COUNT;
  const userSequenceInRoom = Math.floor(iteration / ROOM_COUNT) % USERS_PER_ROOM;

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + (roomIndex * USERS_PER_ROOM) + userSequenceInRoom,
  };
}

function targetForPositionPolling() {
  const offset = (exec.scenario.iterationInTest + exec.vu.idInTest) % USER_COUNT;
  const roomIndex = Math.min(Math.floor(offset / USERS_PER_ROOM), ROOM_COUNT - 1);

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + offset,
  };
}

function targetForBatchedPrefillBatch(userOffset) {
  const inBatchOffset = exec.scenario.iterationInTest % BATCHED_PREFILL_BATCH_SIZE;
  const roomIndex = Math.min(
    Math.floor(inBatchOffset / BATCHED_PREFILL_USERS_PER_ROOM),
    ROOM_COUNT - 1
  );

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + userOffset + inBatchOffset,
  };
}

function targetForBatchedPrefillTotal() {
  const totalOffset = exec.scenario.iterationInTest % BATCHED_PREFILL_TOTAL_USERS;
  const inBatchOffset = totalOffset % BATCHED_PREFILL_BATCH_SIZE;
  const roomIndex = Math.min(
    Math.floor(inBatchOffset / BATCHED_PREFILL_USERS_PER_ROOM),
    ROOM_COUNT - 1
  );

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + totalOffset,
  };
}

function targetForSetupPrefillTotal() {
  const totalOffset = exec.scenario.iterationInTest % BATCHED_PREFILL_TOTAL_USERS;

  return {
    roomId: ROOM_ID_START,
    userId: USER_ID_START + totalOffset,
  };
}

function currentSpeakerForBatchedPrefill() {
  const roomIndex = exec.scenario.iterationInTest % ROOM_COUNT;

  return {
    roomId: ROOM_ID_START + roomIndex,
    userId: USER_ID_START + (roomIndex * BATCHED_PREFILL_USERS_PER_ROOM),
  };
}

function roomIdForIteration() {
  return ROOM_ID_START + (exec.scenario.iterationInTest % ROOM_COUNT);
}

function roomIdForVu() {
  return ROOM_ID_START + ((exec.vu.idInTest - 1) % ROOM_COUNT);
}

function targetForExpirationRaceIteration() {
  const offset = exec.scenario.iterationInTest % EXPIRATION_RACE_ITERATIONS;

  return targetForExpirationRaceOffset(offset);
}

function targetForExpirationRaceOffset(offset) {
  const roomId = ROOM_ID_START + offset;
  const currentSpeakerUserId = USER_ID_START + (offset * 2);

  return {
    roomId,
    currentSpeakerUserId,
    nextSpeakerUserId: currentSpeakerUserId + 1,
  };
}

function recordFailure(ok, counter) {
  if (!ok) {
    counter.add(1);
    stageFailureRate.add(true);
    return;
  }

  stageFailureRate.add(false);
}

function recordRequestStatus(response) {
  if (!response || response.status === 0) {
    requestStatusNetworkError.add(1);
    return;
  }

  if (response.status === 201) {
    requestStatus201.add(1);
    return;
  }

  if (response.status === 409) {
    requestStatus409.add(1);
    return;
  }

  if (response.status >= 200 && response.status < 300) {
    requestStatusOther2xx.add(1);
    return;
  }

  if (response.status >= 400 && response.status < 500) {
    requestStatusOther4xx.add(1);
    return;
  }

  if (response.status >= 500 && response.status < 600) {
    requestStatus5xx.add(1);
    return;
  }

  requestStatusOther.add(1);
}

function currentSpeakerUserIdFrom(response) {
  try {
    return Number(response.json('data.userId') || 0);
  } catch (error) {
    return 0;
  }
}

function randomBetween(min, max) {
  const lowerBound = Math.max(0, min);
  const upperBound = Math.max(lowerBound, max);

  return lowerBound + (Math.random() * (upperBound - lowerBound));
}

function recordExpirationMetrics(response, ok) {
  if (response.status === 200) {
    expirationCandidateRooms.add(Number(response.json('data.candidateRoomCount') || 0));
    expirationExpiredRooms.add(Number(response.json('data.expiredCount') || 0));
    expirationElapsedMs.add(Number(response.json('data.elapsedMs') || 0));
    expirationAvgPerRoomMs.add(Number(response.json('data.avgPerRoomMs') || 0));
  }

  recordFailure(ok, expirationFailures);
}

function recordExpirationRaceVerifyMetrics(response, ok) {
  if (response.status === 200) {
    expirationRaceTerminalCount.add(Number(response.json('data.terminalCount') || 0));
    expirationRaceAssignedCount.add(Number(response.json('data.assignedCount') || 0));
  }

  recordFailure(ok, expirationRaceVerifyFailures);
}
