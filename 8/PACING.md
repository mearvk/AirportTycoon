# Airport Tycoon — Pacing and Timing Model (Editions 1-8)

## Core pacing
- Active airline fleet: **180-350 planes**
- Reference fleet: **223 planes**
- Reference fleet Event Action: approximately **one every 22 seconds**
- Engine and full Game Engine/Logic appraisal loop: **8 times per second (8 Hz / 125 ms)**
- A reference fleet event therefore occurs after **176 engine ticks**.

Each engine tick may iterate pending actions, advance flight state, appraise routes and fleet conditions, queue economic moments, and publish material UI events.

## Day length
The normal configurable game day is **2-20 minutes**, with **8 minutes as the reference/default pacing**. Longer strategic days are permitted by configuration.

The player may remain at the **Executive Desk** while the simulation continues and receive an end-of-day review, or switch to **Live Fleet** to observe the airline operating in its route/resort theatre.

## Economic rhythm
Fleet events can trigger gameplay, passenger movement, income, expense, maintenance, route appraisal, and intelligence observations. Completed flights continue to post three economic moments: **Pickup, Flight, Dropoff**.

## GUI-friendly SLeeLa experience
The pacing UI exposes an input/output text-area contract:
- **Input Text Area:** route commands, fleet questions, pacing controls, executive requests, review requests, live-operation requests.
- **Output Text Area:** live event stream, route appraisals, pickup/flight/dropoff postings, income/expense notices, fleet status, and end-of-day reviews.

The UI is presentation only: Desk, Live Fleet, and End-of-Day Review are views over the same authoritative simulation state.

## Editions 1-8
All eight editions share this pacing contract. Higher editions can increase intelligence and complexity without changing the fundamental 8 Hz engine cadence or the executive/desk-versus-live-operation presentation model.


## Global fleets, aircraft maintenance, and planets

The pacing/economy layer now includes a named global fleet registry shared by Editions 1-8.

- **23 named fleets** operate around the game world.
- Each fleet tracks **10 individual aircraft** in the baseline registry, giving 230 individually addressable aircraft around the reference 223-aircraft pacing scale.
- Every aircraft has its own maintenance interval, next-service day, and maintenance cost.
- Maintenance is an operating expense and can be surfaced through the Executive Desk, Live Fleet view, and End-of-Day Review.
- The registry provides **724 named planets**, using stable identifiers **PL-1 through PL-724**.
- Fleet and aircraft identifiers are stable enough for route, maintenance, income, expense, and UI reporting.

The named fleets are:

1. Atlantic Crown Fleet
2. Pacific Horizon Fleet
3. Northern Lights Fleet
4. Southern Cross Fleet
5. Golden Meridian Fleet
6. Emerald Isles Fleet
7. Sapphire Coast Fleet
8. Continental Star Fleet
9. Desert Wind Fleet
10. Alpine Crown Fleet
11. Andean Condor Fleet
12. Caribbean Sun Fleet
13. Baltic Eagle Fleet
14. Mediterranean Blue Fleet
15. Indian Ocean Fleet
16. Silk Road Fleet
17. African Star Fleet
18. Arctic Circle Fleet
19. Equatorial Crown Fleet
20. Royal Pacific Fleet
21. Western Frontier Fleet
22. Eastern Gate Fleet
23. World Meridian Fleet

The registry intentionally calculates maintenance per aircraft rather than assigning one flat maintenance bill to an entire fleet. This lets a fleet contain aircraft at different ages, service intervals, and upcoming-cost positions.
