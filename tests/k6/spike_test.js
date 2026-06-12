import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// Load test users
const users = new SharedArray('users', function () {
    return JSON.parse(open('./users.json'));
});

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            start: 0,
            stages: [
                { duration: '30s', target: 100 },   // Ramp up to 100 VUs
                { duration: '1m', target: 100 },    // Stay at 100 VUs
                { duration: '10s', target: 10000 }, // Spike to 10,000 VUs
                { duration: '1m', target: 10000 },   // Stay at 10,000 VUs
                { duration: '30s', target: 0 },     // Ramp down
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
        http_req_failed: ['rate<0.01'],   // Less than 1% error rate
    },
};

const BASE_URL = 'http://ticket-api.ticket-platform.svc.cluster.local:8080/api/v1';

export default function () {
    const user = users[Math.floor(Math.random() * users.length)];

    // 1. Login
    const loginParams = {
        headers: { 'Content-Type': 'application/json' },
        json: {
            email: user.username,
            password: user.password || 'password',
        },
    };
    const loginRes = http.post(`${BASE_URL}/auth/login`, loginParams);

    if (!check(loginRes, { 'login successful': (r) => r.status === 200 })) {
        return;
    }

    const token = loginRes.json().accessToken;
    const params = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // 2. Join Queue
    const joinRes = http.post(`${BASE_URL}/queue/join`, params);
    check(joinRes, { 'join queue successful': (r) => r.status === 200 });
    sleep(1);

    // 3. Poll Status (Simulate waiting in queue)
    let allowed = false;
    for (let i = 0; i < 5; i++) {
        const statusRes = http.get(`${BASE_URL}/queue/status`, params);
        if (statusRes.json().status === 'ACTIVE' || statusRes.json().canEnter === true) {
            allowed = true;
            break;
        }
        sleep(2);
    }

    if (!allowed) return;

    // 4. Hold Seat (Assume seatId 1 for simulation)
    const seatId = 1;
    const holdRes = http.post(`${BASE_URL}/seats/${seatId}/hold`, params);
    if (!check(holdRes, { 'hold seat successful': (r) => r.status === 200 })) {
        return;
    }

    // 5. Confirm Booking
    const bookingRes = http.post(`${BASE_URL}/bookings?seatId=${seatId}`, params);
    check(bookingRes, { 'booking requested successful': (r) => r.status === 202 });
}
