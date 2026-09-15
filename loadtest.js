import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '10s',
};

export default function () {
  const params = {
    headers: {
      'X-API-KEY': 'pro_team_demo_key',
    },
  };
  const res = http.get('http://localhost:8080/api/resource', params);
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });
  sleep(0.1);
}