# Airport Tycoon — Pacing and Timing Model (Editions 1-8)

## Purpose

Editions 1 through 8 use a common pacing model so the airline feels alive without requiring the player to watch every aircraft continuously.

### Fleet scale

- Normal active fleet range: **180-350 planes**
- Reference fleet: **223 planes**
- Reference fleet action cadence: approximately **one fleet Event Action every 22 seconds**
- The 223-plane figure is a pacing reference, not a hard fleet limit.

A fleet Event Action is allowed to trigger gameplay, passenger activity, route appraisals, income, expenses, maintenance consequences, or other engine events.

## Engine cadence

The game engine and logic appraisal loop runs at **8 times per second (8 Hz)**.

Each 125 ms engine tick may:

1. iterate pending actions;
2. advance flight and airport state;
3. inspect the active fleet;
4. run route and system appraisals;
5. queue income and expense moments;
6. update the executive observation model;
7. publish UI text-area output when there is something material to report.

The 22-second fleet event is therefore scheduled from the same authoritative 8 Hz clock: 176 engine ticks per reference fleet event.

## Day pacing

A game day is configurable between:

- **2 minutes minimum**
- **20 minutes maximum**
- **8 minutes default/reference**

Longer days may be supported by configuration for slower strategic sessions.

The purpose is to make a day long enough for a player to operate from an executive desk while still being short enough to reach an end-of-day review during a normal play session.

## Executive play

The player can remain at the **Desk** and receive:

- live event notices;
- route profitability changes;
- income/expense postings;
- fleet alerts;
- end-of-day review;
- recommendations from the management/intelligence layer.

The player can instead open **Live Fleet** and watch aircraft operations in their active route/resort theatre.

The two views are different presentations of the same game state. They are not separate simulations.

## GUI-friendly Text Area contract

The SLeeLa GUI layer provides an input/output text-area contract:

**Input Text Area**
- route commands;
- fleet questions;
- pacing controls;
- executive requests;
- review requests;
- live-operation requests.

**Output Text Area**
- event stream;
- route appraisals;
- pickup/flight/dropoff economic postings;
- income and expense notices;
- fleet status;
- end-of-day reports.

The UI is intentionally text-area aware so a future native text-area widget can bind directly to the SLeeLa input/output strings without changing the game engine.

## Relationship to the economic model

The pacing engine does not replace the Route Profitability Model. It schedules the moments at which that model and the rest of the game can act.

For every completed flight, the economic model continues to recognize three moments:

1. Pickup
2. Flight
3. Dropoff

Thus the fast 8 Hz simulation clock, the approximately 22-second fleet-action cadence, and the slower 2-20 minute day clock all coexist.

## Editions 1-8

Each edition contains the same core pacing contract so later editions can increase intelligence, route complexity, fleet size, and executive detail without changing the fundamental rhythm of the game.
