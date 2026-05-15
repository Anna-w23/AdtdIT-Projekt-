# SpaceMate – Process Manager User Guide

SpaceMate is a desktop application for managing the onboarding process of space tourists. As a Process Manager (PM), you guide each customer through their medical checks, specialist consultations, space training, and final clearance — scheduling appointments, managing documents, and resolving issues along the way.

---

## Interface Overview

The application window is divided into four main areas:

| Area | Location | Purpose |
|------|----------|---------|
| Toolbar | Top | Time controls, simulation settings, event messages |
| Customer List | Left sidebar | All registered customers with status indicators |
| Week Calendar | Center | Appointment scheduling and availability view |
| Staff & Resource Panel | Bottom | Toggle staff/resource availability overlays |

*(SCREENSHOT: Full application window with all four areas labeled)*

---

## Getting Started

When the application launches, you will see:

- A pre-populated staff roster (physicians, specialists, trainers) with working schedules
- Available rooms and equipment ready for assignment
- The simulated clock set to today's date
- Customers will begin arriving automatically (1–3 per day)

### Advancing Time

The simulation runs on a virtual clock. You control time progression:

- **Simulate Day** (`▶`): Advances exactly one day. All confirmed appointments on that day are automatically conducted and their results generated (simulated by mock doctors/trainers).
- **Auto Mode** (`⏱ Auto`): Continuously advances time at a rate of 1 simulated day per 1 real minute. The button turns green when active. Appointments are conducted and results generated as the timeline passes them.

*(SCREENSHOT: Toolbar showing the time controls — Simulate Day button and Auto toggle)*

### Navigating Weeks

The toolbar displays the currently viewed week (e.g. "14 May 2026 – 20 May 2026"). Use the **←** and **→** arrow buttons to navigate to the previous or next week. This only changes the calendar view — it does not affect the simulated time.

### The Red Timeline

When Auto Mode is active, a red horizontal line moves across the calendar indicating the current simulated time. Appointments are automatically processed as the line crosses them.

---

## Customer Management

### Customer List

The left sidebar displays all customers. Each entry shows:

- **Name** (bold)
- **Language badge** (e.g. DE, ES, JA) — if the customer's preferred language is not English
- **Status badge** (right side):
  - Countdown (e.g. "12d") showing days until takeoff — color indicates urgency
  - "Ready" if the customer is approved for flight
  - "Failed" if onboarding could not be completed
- **Attention indicator** (`!`) — appears when PM action is required
- **Next step** text below the name — summarizes what needs to happen next

*(SCREENSHOT: Customer list showing various customers with different urgency levels and status badges)*

### Urgency Colors

| Color | Meaning |
|-------|---------|
| Red background | Critical — fewer than 7 days per remaining step |
| Orange background | Tight — 7 to 14 days per remaining step |
| Green background | On track — more than 14 days per remaining step |
| Blue background | Approved — customer cleared for flight |
| Gray background | Failed — onboarding ended |

### Selecting a Customer

Click a customer to select them. This:

1. Shows only their appointments in the calendar
2. Displays a **status banner** above the calendar with their name, current stage, and next action
3. Shows an **action button** (if applicable) below the banner
4. Enables drag-to-schedule functionality

Click the same customer again to deselect.

*(SCREENSHOT: Selected customer with status banner showing name, stage, and action guidance)*

---

## Scheduling Appointments

### Step 1: Select the Customer

Click the customer who needs an appointment. The status banner will tell you what type of appointment to schedule next (e.g. "Schedule Initial Medical").

### Step 2: Toggle a Staff Member

In the bottom panel, expand the relevant staff category (Physicians, Specialists, or Training) and click a staff member's name. Their availability appears as colored overlay zones on the calendar:

| Overlay Color | Staff Type |
|---------------|-----------|
| Green (semi-transparent) | Physicians (Chief, Resident, Night) |
| Orange (semi-transparent) | Specialists (Eye, Cardio, Neuro, Ortho, Psychologist) |
| Teal (semi-transparent) | Space Trainers |

The overlays only appear on time slots where **both** the staff member and the customer are available.

*(SCREENSHOT: Calendar with staff availability overlay visible — green zones showing where appointments can be placed)*

### Step 3: Drag to Schedule

Click and drag on an availability zone to create an appointment:

1. Press and hold the mouse button on the desired start time
2. Drag downward to extend the duration (snaps to 30-minute increments)
3. Release to confirm

The selected staff member's role determines which appointment type is scheduled. Each role maps to exactly one type (e.g. an Eye Specialist always schedules an Eye Specialist consultation, a Space Trainer always schedules Space Training). The appointment appears on the calendar in **orange** (Suggested status).

*(SCREENSHOT: Drag operation in progress — blue preview rectangle visible on the calendar)*

### Step 4: Send the Proposal

Click the orange appointment block to open its detail popover. From here you can:

- Click **Send** (green button) to send the proposal to the customer. The appointment turns **blue** (Sent status).
- Click **Cancel** (red button) to discard the suggestion entirely and try a different time slot.

*(SCREENSHOT: Appointment detail popover with Send and Cancel buttons)*

---

## Managing Appointments

Click any appointment block on the calendar to open its detail popover.

### Popover Contents

- **Appointment type** and **time range**
- **Customer name** and **staff member**
- **Resources** (room, equipment) — with controls to assign or change them
- **Documents** — attached reports and correspondence
- **Action buttons** — vary by status (see below)

### Actions by Appointment Status

| Status | Color | Available Actions |
|--------|-------|-------------------|
| Suggested | Orange | **Send** (green) — send proposal to customer |
| | | **Cancel** (red) — discard and try a different slot |
| Sent | Blue | **Send Reminder** (blue) — nudge the customer to respond |
| | | **PM Cancel** (gray) — cancel from your side |
| Confirmed | Green | **PM Cancel** (gray) — cancel if circumstances change |
| Completed | Gray | Not interactive — appointment blocks cannot be opened |
| Cancelled | Gray | Not interactive — hidden by default (see "Show completed") |

*(SCREENSHOT: Appointment popover for a "Sent" appointment showing Send Reminder and PM Cancel buttons)*

### Assigning Resources

Some appointments require a room or equipment (e.g. VR headset, translation headphone). In the popover:

- If a resource is assigned, its name is shown as a clickable button — click to reassign
- If no resource is assigned, a **+** button appears — click to open a dropdown of available options
- The dropdown only shows resources that are free during the appointment's time slot

**Missing resource warnings:**
- Appointments scheduled for **today** with missing resources show a **red border**
- Appointments scheduled for **future days** with missing resources show an **orange border**

*(SCREENSHOT: Appointment popover showing room dropdown with available options)*

### Attaching Documents

In the Documents section of the popover:

- Attached documents appear as labeled badges (e.g. "Medical Report", "AI Legal Report")
- Click **x** next to a document to detach it
- Click **+** to attach an available document from the customer's file

---

## Customer Journey

Each customer follows this path from registration to flight clearance:

### 1. Registration & Questionnaire

A new customer arrives with status **Registered**. 

**PM Action:** Click the customer, then click **Send Questionnaire** in the action bar. The system sends the questionnaire to the customer. In the simulation, the mock customer fills it out and responds automatically.

After completion, the customer moves to **Questionnaire Completed** and is ready for their first medical.

### 2. Initial Medical Examination

**PM Action:** Schedule an appointment with a physician (Chief Physician, Resident Physician, or Night Physician). Follow the scheduling steps above.

After the appointment is conducted, the system generates a mock medical report on behalf of the attending physician (in a production environment, this report would be submitted by the doctor). The report determines:
- Which specialist consultations are needed (if any)
- Whether extended space training is required

### 3. Specialist Consultations (if required)

Based on the initial medical results, the customer may need one or more of:

| Specialist | When Required |
|-----------|--------------|
| Eye Specialist | Eye exam not passed |
| Cardiologist | Cardiac screening not passed |
| Neurologist | Neurological exam not passed |
| Orthopedist | Musculoskeletal exam not passed |
| Psychologist | Psychological assessment not passed |

**PM Action:** Schedule each required specialist appointment one at a time. Select the appropriate specialist in the bottom panel, drag to schedule, send the proposal, and wait for the result before scheduling the next one. They can be done in any order, but each must be completed before the next can be scheduled. The status banner shows which specialists are still needed.

After all specialists are completed, the customer is ready for space training. (In the simulation, specialist results are randomly generated — approximately 70% pass rate per consultation.)

### 4. Space Training

**PM Action:** Schedule with a Space Trainer. The recommended duration is 90 minutes (or 120 minutes if extended training was flagged in the initial medical report). Note that the system does not enforce this duration — the PM is free to choose any length when dragging. It is your responsibility to ensure the scheduled duration matches the recommendation from the medical report.

### 5. Final Medical

**PM Action:** Schedule with a physician. This is the last clearance step.

If passed: Customer is set to **Ready** for flight.

### 6. Approval

The customer shows the "Ready" badge and moves to the blue-highlighted section of the customer list. No further action required.

*(SCREENSHOT: Customer list showing an approved customer with the Ready badge)*

### Alternative Paths

> **Note on simulation:** In the current mock environment, customer decisions (accept, refuse timeslot, refuse appointment type) are randomly generated when a proposal is sent. The system logic handles all outcomes identically regardless of whether the customer is simulated or real.

#### Customer Refuses a Timeslot

If a customer declines the proposed time (but not the appointment type itself), the appointment is cancelled and you must reschedule. The customer's available times are updated to exclude the refused slot.

#### Customer Refuses an Appointment Type

If a customer refuses to attend an appointment type entirely (e.g. refuses the specialist consultation), you must offer an **Indemnity Agreement**.

**PM Action:** Click the customer, then click **Offer Indemnity Agreement** in the action bar.

- If the customer **signs**: The refused step is skipped and onboarding continues from the next stage
- If the customer **refuses to sign**: Onboarding ends — customer is marked as failed

#### Appointment Fails

If an appointment result is negative (e.g. specialist consultation not passed, space training failed), the same indemnity flow applies.

---

## Document Management

### Viewing All Documents

With a customer selected, click the **Docs** button (top-right of the status banner) to open the document list.

*(SCREENSHOT: Document list popover showing Reports and Correspondence sections)*

Documents are organized into two sections:

**Reports** (green accent):
- Medical Report — initial/final medical results
- Specialist Report — specialist consultation results  
- Training Report — space training results
- AI Medical Report — automated pre-analysis
- AI Legal Report — automated legal assessment
- AI Trainer Report — automated training assessment

**Correspondence** (gray accent, red for indemnity):
- Questionnaire — initial customer questionnaire
- Appointment Proposal — sent to customer
- Appointment Response — customer's reply
- Indemnity Agreement — legal waiver sent to customer
- Indemnity Response — customer's signature or refusal

Each document shows:
- Direction arrow: **→** (sent to customer) or **←** (received from customer)
- Category name
- Timestamp
- **View** button — opens the full document content

*(SCREENSHOT: Document content dialog showing a medical report with metadata)*

---

## Simulation Controls

### Toolbar Checkboxes

| Control | Default | Effect |
|---------|---------|--------|
| **Show completed** | Off | When checked, completed and cancelled appointments are visible on the calendar overview (no customer selected). When a specific customer is selected, their completed appointments are always shown regardless of this setting. |
| **Auto-spawn** | On | When checked, 1–3 new mock customers arrive each simulated day (with randomized names, languages, and takeoff dates) |
| **Instant response** | On | When checked, mock customers respond to proposals immediately. When unchecked, mock responses take 3–12 simulated hours |

### Delayed Response Mode

When **Instant response** is unchecked:

1. Sent proposals remain in **blue** (Sent) status until the customer responds
2. In the simulation, mock customers respond after 3–12 simulated hours (requires Auto Mode or Simulate Day to advance time)
3. Use **Send Reminder** on a blue appointment to trigger an immediate response from the mock customer

This simulates realistic customer communication delays. In a production environment, the system would wait for the real customer to respond via their communication channel.

### Auto Mode Details

When **Auto** is toggled on:
- Time advances continuously (1 real minute = 1 simulated day)
- The red timeline moves across the calendar
- Appointments are processed automatically as they are reached
- New customers arrive daily (if Auto-spawn is on)
- Pending customer responses are collected as they become due

---

## Staff & Resource Panel

The bottom panel contains expandable groups:

### Staff Groups

| Group | Roles |
|-------|-------|
| Physicians | Chief Physician, Resident Physician, Night Physician |
| Specialists | Eye Specialist, Cardiologist, Neurologist, Orthopedist, Psychologist |
| Training | Space Trainer |

Click any staff member to toggle their availability overlay on the calendar. Only one can be active at a time across all groups.

### Resource Groups

| Group | Types |
|-------|-------|
| Rooms | Medical rooms (M2.01–M2.11), Shuttle simulation rooms (S1.01–S1.03) |
| Equipment | VR Headsets, Translation Headphones |

Click any resource to toggle its availability overlay on the calendar. Only one staff member or resource can be active at a time (selecting one deselects any other).

*(SCREENSHOT: Bottom panel showing expanded Specialists group with one staff member toggled on)*

---

## Status Color Reference

### Appointment Colors (Calendar)

| Color | Status | Meaning |
|-------|--------|---------|
| Orange | Suggested | Draft — not yet sent to customer |
| Blue | Sent | Proposal sent — awaiting customer response |
| Green | Confirmed | Customer accepted — appointment is booked |
| Gray | Completed / Cancelled | No further action needed |

### Availability Overlay Colors (Calendar)

| Color | Meaning |
|-------|---------|
| Green (semi-transparent) | Physician availability |
| Orange (semi-transparent) | Specialist availability |
| Teal (semi-transparent) | Trainer availability |
| Light blue (semi-transparent) | Room availability |
| Light purple (semi-transparent) | Equipment availability |

### Calendar Background

| Color | Meaning |
|-------|---------|
| White | Customer is available at this time |
| Light gray | Customer is unavailable (or time is in the past) |

### Customer List Urgency

| Color | Meaning |
|-------|---------|
| Red | Critical — customer at risk of missing takeoff |
| Orange | Tight timeline — needs priority attention |
| Green | On track — sufficient time remaining |
| Blue | Approved — cleared for flight |
| Gray | Failed — onboarding ended |

---

## Quick Reference: Common Workflows

### Onboard a New Customer (Full Happy Path)

1. Customer appears in list with `!` indicator → click them
2. Click **Send Questionnaire** → wait for completion
3. Toggle a physician → drag to schedule Initial Medical → click appointment → **Send**
4. Wait for confirmation → wait for appointment to be conducted (auto or simulate day)
5. Review medical report (Docs button) → note required specialists
6. For each specialist: toggle specialist → drag to schedule → **Send** → wait for result
7. Toggle a trainer → drag to schedule Space Training (check medical report for extended duration recommendation) → **Send** → wait for result
8. Toggle a physician → drag to schedule Final Medical → **Send** → wait for result
9. Customer is ready — done

### Handle a Refused Appointment

1. Customer shows `!` with "refused [type]" message
2. Click customer → click **Offer Indemnity Agreement**
3. If signed: continue with next step in journey
4. If refused: customer is marked failed

### Reschedule After Timeslot Refusal

1. Customer shows `!` with "reschedule required" message
2. Toggle appropriate staff member
3. Drag to schedule a new time slot → **Send**

### Send a Reminder

1. Click the blue (Sent) appointment on the calendar
2. Click **Send Reminder** in the popover
3. In the simulation, the mock customer responds immediately after receiving a reminder
