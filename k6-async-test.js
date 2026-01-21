import http from "k6/http";
import { check, group, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

// Get token from environment variable
const TOKEN = __ENV.TOKEN;

// Custom metrics
const errorRate = new Rate("errors");
const latency = new Trend("latency");
const successRate = new Rate("success");

export const options = {
  vus: 50,
  duration: "30s",
};

export default function () {
  group("async_watchlist_add", () => {
    const playersToAdd = [
      "LeBron James",
      "Kevin Durant",
      "Giannis Antetokounmpo",
      "Luka Doncic",
      "Stephen Curry",
    ];

    const randomPlayer =
      playersToAdd[Math.floor(Math.random() * playersToAdd.length)];

    const url = "http://localhost:8080/api/watchlists";
    const payload = JSON.stringify({
      playerName: randomPlayer,
    });

    const params = {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${TOKEN}`,
      },
    };

    // Make the async request
    const res = http.post(url, payload, params);

    // Record latency
    latency.add(res.timings.duration);

    // Check the response
    const success = check(res, {
      "status is 201": (r) => r.status === 201,
      "response time < 200ms": (r) => r.timings.duration < 200,
    });

    if (!success) {
      errorRate.add(1);
    } else {
      successRate.add(1);
    }

    sleep(0.1);
  });
}
