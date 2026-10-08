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
