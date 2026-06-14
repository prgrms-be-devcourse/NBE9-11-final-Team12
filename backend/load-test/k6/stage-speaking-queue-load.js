import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ROOM_ID = Number(__ENV.ROOM_ID || '1');
const USER_ID_START = Number(__ENV.USER_ID_START || '1');
const USER_COUNT = Number(__ENV.USER_COUNT || '100');
const SCENARIO = __ENV.SCENARIO || 'request_spike';

const requestFailures = new Counter('stage_request_failures');
const cancelFailures = new Counter('stage_cancel_failures');
const completeFailures = new Counter('stage_complete_failures');
const queueLookupFailures = new Counter('stage_queue_lookup_failures');
const stageFailureRate = new Rate('stage_failure_rate');
const queueResponseBytes = new Trend('stage_queue_response_bytes');

export const options = {
  scenarios: buildScenarios(SCENARIO),
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    stage_failure_rate: ['rate<0.05'],
  },
};

export function requestSpike() {
  const userId = userIdForIteration();

  group('request speaking turn spike', () => {
    const response = requestSpeakingTurn(userId);
    const ok = check(response, {
      'request status is 201 or already active': (res) => res.status === 201 || res.status === 409,
    });

    recordFailure(ok, requestFailures);
  });
}

export function cancelReRequest() {
  const userId = userIdForIteration();

  group('cancel and request again churn', () => {
    const cancelResponse = cancelMyRequest(userId);
    const cancelOk = check(cancelResponse, {
      'cancel status is 200 or not found': (res) => res.status === 200 || res.status === 404,
    });
    recordFailure(cancelOk, cancelFailures);

    sleep(0.1);

    const requestResponse = requestSpeakingTurn(userId);
    const requestOk = check(requestResponse, {
      're-request status is 201 or already active': (res) => (
        res.status === 201 || res.status === 409
      ),
    });
    recordFailure(requestOk, requestFailures);
  });
}

export function speakingTurnCycle() {
  const userId = userIdForIteration();

  group('complete current speaker and auto grant next', () => {
    const completeResponse = completeCurrentSpeaker(userId);
    const completeOk = check(completeResponse, {
      'complete status is 200 or not current speaker': (res) => (
        res.status === 200 || res.status === 403 || res.status === 404
      ),
    });
    recordFailure(completeOk, completeFailures);

    sleep(0.1);

    const currentSpeakerResponse = getCurrentSpeaker();
    check(currentSpeakerResponse, {
      'current speaker status is 200 or empty': (res) => res.status === 200 || res.status === 404,
    });
  });
}

export function queueLookup() {
  group('waiting queue lookup', () => {
    const response = getWaitingQueue();
    const ok = check(response, {
      'queue lookup status is 200': (res) => res.status === 200,
    });

    queueResponseBytes.add(response.body ? response.body.length : 0);
    recordFailure(ok, queueLookupFailures);
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
  };

  if (selectedScenario === 'all') {
    return {
      request_spike: {
        ...scenarioMap.request_spike,
        startTime: '0s',
      },
      cancel_re_request: {
        ...scenarioMap.cancel_re_request,
        startTime: '70s',
      },
      speaking_turn_cycle: {
        ...scenarioMap.speaking_turn_cycle,
        startTime: '260s',
      },
      queue_lookup: {
        ...scenarioMap.queue_lookup,
        startTime: '460s',
      },
    };
  }

  if (!scenarioMap[selectedScenario]) {
    throw new Error(
      `Unknown SCENARIO=${selectedScenario}. `
        + 'Use request_spike, cancel_re_request, speaking_turn_cycle, queue_lookup, or all.'
    );
  }

  return {
    [selectedScenario]: scenarioMap[selectedScenario],
  };
}

function requestSpeakingTurn(userId) {
  return http.post(stageUrl(`/requests?userId=${userId}`), null, requestParams());
}

function cancelMyRequest(userId) {
  return http.del(stageUrl(`/requests/me?userId=${userId}`), null, requestParams());
}

function completeCurrentSpeaker(userId) {
  return http.post(stageUrl(`/complete?userId=${userId}`), null, requestParams());
}

function getCurrentSpeaker() {
  return http.get(stageUrl(''), requestParams());
}

function getWaitingQueue() {
  return http.get(stageUrl('/queue'), requestParams());
}

function stageUrl(path) {
  return `${BASE_URL}/api/v1/rooms/${ROOM_ID}/stage${path}`;
}

function requestParams() {
  return {
    headers: {
      Accept: 'application/json',
    },
    tags: {
      room_id: String(ROOM_ID),
      scenario: SCENARIO,
    },
  };
}

function userIdForIteration() {
  const offset = (__ITER + (__VU - 1)) % USER_COUNT;
  return USER_ID_START + offset;
}

function recordFailure(ok, counter) {
  if (!ok) {
    counter.add(1);
    stageFailureRate.add(true);
    return;
  }

  stageFailureRate.add(false);
}
