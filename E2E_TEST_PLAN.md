# E2E Test Plan - HIGH PRIORITY Tasks

## Test Environment Setup

**Required:**

- 2 devices/emulators (Device A = Creator, Device B = Bidder)
- 2 different user accounts logged in
- Active internet connection
- FCM notifications enabled

---

## Test 1: Complete Bid Flow ✅

**Objective:** Verify the entire bid placement, acceptance, budget update, and date sync flow

### Prerequisites

- Device A: Creator with an existing task
- Device B: Bidder (team member in the group)

### Test Steps

1. **Create Subtask (Device A - Creator)**
   - Open task detail screen
   - Go to Subtasks tab
   - Click "Add Subtask" button
   - Enter:
     - Title: "Test Subtask E2E"
     - Description: "Testing bid flow"
     - Budget: Leave empty (will be set by bid)
     - Due Date: Tomorrow
   - Click "Create"
   - ✅ **Expected:**

     - Subtask appears in list
     - Chat message: "📼 [Creator Name] created subtask: [Test Subtask E2E](#id)"
     - Reference is clickable

2. **Place Bid (Device B - Bidder)**
   - Open same task
   - Go to Subtasks tab
   - Find "Test Subtask E2E"
   - Click "Place Bid" button (💵)
   - Enter amount: $150.00
   - Click "Submit"
   - ✅ **Expected:**
     - Snackbar: "Bid placed successfully"
     - Bid appears in subtask with "Pending" status
     - Chat message: "💵 [Bidder Name] placed a bid of $150.00 on: [Test Subtask E2E](#id)"
     - Device A receives FCM notification: "New Bid Received"

3. **Verify Creator Notification (Device A)**
   - ✅ **Expected:**
     - Notification appears in notification tray
     - Title: "New Bid Received"
     - Body: "A bid of $150.00 was placed on Test Subtask E2E"
     - Tap notification → Opens task detail screen
     - Subtask shows pending bid with amount $150.00

4. **Accept Bid (Device A - Creator)**
   - In subtask, locate bid entry
   - Click green checkmark (✓) to accept
   - ✅ **Expected:**
     - Snackbar: "Bid accepted successfully"
     - Bid status changes to "Accepted"
     - Subtask budget updates to $150.00
     - Bidder assigned to subtask (visible in AvatarStack)
     - Chat messages:
       - "✅ [Creator Name] accepted [Bidder Name]'s bid of $150.00 for: [Test Subtask E2E](#id)"
       - "👤 [Bidder Name] was assigned to: [Test Subtask E2E](#id)"

5. **Verify Budget Update (Device A)**
   - Check subtask details
   - ✅ **Expected:**
     - Subtask budget: $150.00
     - Task total budget: $150.00 (if only subtask)
     - Budget appears in subtask card

6. **Verify Parent Task Date Sync (Device A)**
   - Check parent task due date
   - ✅ **Expected:**
     - Parent task due date = Tomorrow (matches subtask due date)
     - If multiple subtasks exist, parent date = furthest subtask date

7. **Verify Bidder Notifications (Device B)**
   - ✅ **Expected:**
     - FCM notification: "Bid Accepted"
     - Body: "Your bid of $150.00 was accepted for Test Subtask E2E"
     - Tap notification → Opens task detail screen
     - Subtask shows as assigned to bidder
     - UI updates reactively without manual refresh

8. **Verify Other Bidders (if applicable)**
   - If other users placed bids, they should receive:
   - ✅ **Expected:**
     - Notification: "Bid Not Selected"
     - Body: "Your bid of $[amount] was not selected for Test Subtask E2E"
     - Their bid status changes to "Rejected"

---

## Test 2: Edge Cases Testing ⚠️

### Test 2.1: Null Due Date Handling

**Steps:**

1. Create subtask WITHOUT due date
2. Place and accept bid
3. ✅ **Expected:**
   - Budget updates correctly
   - Parent task date sync handles null gracefully
   - No crashes or errors

### Test 2.2: Zero Budget

**Steps:**

1. Place bid with amount $0.00
2. Accept bid
3. ✅ **Expected:**
   - Accepts $0 budget
   - Displays as $0.00
   - Total budget calculation correct

### Test 2.3: Multiple Subtasks - Date Sync

**Steps:**

1. Create 3 subtasks with different due dates:
   - Subtask A: Today
   - Subtask B: Tomorrow
   - Subtask C: Next week
2. Accept bids for all
3. ✅ **Expected:**
   - Parent task due date = Next week (furthest date)
   - When Subtask C deleted, parent date updates to Tomorrow
   - When all have null dates, parent date becomes null

### Test 2.4: Simultaneous Bid Acceptance

**⚠️ IMPORTANT:** This tests transaction safety

**Setup:**

- Device A and Device B both creators
- Device C places bid on subtask
- Device A and Device B both try to accept bid at same time

**Steps:**

1. Device C places bid
2. Device A clicks accept (✓)
3. Device B clicks accept (✓) immediately after
4. ✅ **Expected:**
   - Only ONE acceptance succeeds (transaction blocks second)
   - One device shows "Bid accepted successfully"
   - Other device shows error or "Bid already accepted"
   - NO duplicate assignments
   - Budget updated only once

### Test 2.5: Offline Bid Placement

**Steps:**

1. Turn off WiFi/data on Device B
2. Place bid on subtask
3. Turn WiFi/data back on
4. ✅ **Expected:**
   - Bid queues while offline
   - Syncs when connection restored
   - Creator receives notification after sync
   - UI shows "Pending sync" or similar

### Test 2.6: Invalid Token Cleanup

**Steps:**

1. User uninstalls app (Device B)
2. Device A accepts bid for user who uninstalled
3. ✅ **Expected:**
   - FCM notification fails gracefully
   - Error logged but doesn't crash
   - Invalid token cleaned up automatically
   - Other users still receive notifications

### Test 2.7: Long Subtask Titles in Chat References

**Steps:**

1. Create subtask with very long title (100+ characters)
2. Place bid
3. Check chat message display
4. ✅ **Expected:**
   - Chat reference displays correctly
   - No UI overflow
   - Clickable reference works
   - Title truncated if needed with "..."

### Test 2.8: Special Characters in Titles

**Steps:**

1. Create subtask with title: "Test [#special] (chars) & symbols"
2. Place bid
3. Check chat reference parsing
4. ✅ **Expected:**
   - Regex parses correctly
   - Reference displays: `[Test [#special] (chars) & symbols](#id)`
   - Clicking works correctly
   - No parsing errors

---

## Test 3: Reactive UI Updates (UpdateEventBus)

**Objective:** Verify UI updates without manual refresh

**Steps:**

1. Device A and Device B both viewing same task
2. Device A accepts bid
3. ✅ **Expected on Device B:**
   - Subtask list updates immediately
   - Budget changes reflect instantly
   - Assignment avatar appears
   - No need to pull-to-refresh
   - Smooth transition (no flicker)

---

## Test 4: Postponement Flow Integration

**Objective:** Verify postponement works with bid system

**Steps:**

1. Accept bid with due date = Tomorrow
2. Assigned user requests postponement to Next Week
3. Creator accepts postponement
4. ✅ **Expected:**
   - Subtask due date updates to Next Week
   - Parent task date syncs to Next Week
   - Budget remains $150.00
   - FCM notifications sent
   - Chat message: "📅 [User] changed due date to [Next Week] for: [Subtask](#id)"

---

## Test 5: Completion Flow with Budget Distribution

**Objective:** Verify completed subtasks distribute budget

**Steps:**

1. Subtask with budget $150.00 assigned to User B
2. User B submits completion with proof image
3. Creator approves completion
4. ✅ **Expected:**
   - Subtask status → COMPLETED
   - Budget distributed to User B
   - User B's balance increases by $150.00
   - Parent task status updates if all subtasks complete
   - FCM notification: "Completion approved"
   - Chat message: "✅ [Creator] approved completion of: [Subtask](#id)"

---

## Test 6: Deletion Cascade

**Objective:** Verify recursive deletion of chats

### Test 6.1: Delete Task

**Steps:**

1. Create task with 2 subtasks
2. Create chat for task (taskId field set)
3. Create chat for subtask A (subtaskId field set)
4. Delete task
5. ✅ **Expected:**
   - Task deleted
   - Both subtasks deleted
   - Task chat deleted (all messages)
   - Subtask A chat deleted (all messages)
   - No orphaned chats in Firestore
   - All users notified

### Test 6.2: Delete Subtask

**Steps:**

1. Create subtask with chat (subtaskId field set)
2. Delete subtask
3. ✅ **Expected:**
   - Subtask deleted
   - Subtask chat deleted (all messages)
   - Parent task remains
   - Parent task chat remains
   - Budget recalculated
   - Date sync updated

---

## Performance Metrics

### Expected Response Times:

- Bid placement: < 2 seconds
- Bid acceptance: < 3 seconds (includes transaction, notifications, budget calc)
- FCM notification delivery: < 5 seconds
- UI reactive update: < 2 seconds after FCM event

### Firestore Operations Count (per bid acceptance):

- Transaction: 1 (subtask update)
- Reads: 3-4 (task, users, parent task)
- Writes: 2-3 (subtask, parent task budget/date)
- Notifications: 3+ (winner, rejected bidders, team members)

---

## Known Issues & Limitations

1. **Offline Handling**: Bids placed offline may not show in real-time
2. **Race Conditions**: Multiple creators accepting same bid - needs transaction testing
3. **Notification Delays**: FCM can take 5-30 seconds in background
4. **Token Cleanup**: Invalid tokens need manual cleanup (should auto-cleanup)

---

## Test Results Template

```
## Test Execution Results
Date: [YYYY-MM-DD]
Tester: [Name]
Devices:
- Device A: [Model/Emulator]
- Device B: [Model/Emulator]

| Test Case | Status | Notes | Issues |
|-----------|--------|-------|--------|
| Test 1: Complete Bid Flow | ☐ PASS ☐ FAIL | | |
| Test 2.1: Null Due Date | ☐ PASS ☐ FAIL | | |
| Test 2.2: Zero Budget | ☐ PASS ☐ FAIL | | |
| Test 2.3: Multiple Subtasks Date | ☐ PASS ☐ FAIL | | |
| Test 2.4: Simultaneous Accept | ☐ PASS ☐ FAIL | | |
| Test 2.5: Offline Bid | ☐ PASS ☐ FAIL | | |
| Test 2.6: Invalid Token | ☐ PASS ☐ FAIL | | |
| Test 2.7: Long Titles | ☐ PASS ☐ FAIL | | |
| Test 2.8: Special Characters | ☐ PASS ☐ FAIL | | |
| Test 3: Reactive UI | ☐ PASS ☐ FAIL | | |
| Test 4: Postponement | ☐ PASS ☐ FAIL | | |
| Test 5: Budget Distribution | ☐ PASS ☐ FAIL | | |
| Test 6.1: Delete Task | ☐ PASS ☐ FAIL | | |
| Test 6.2: Delete Subtask | ☐ PASS ☐ FAIL | | |

**Overall Result:** ☐ ALL PASS ☐ SOME FAILURES

**Critical Issues Found:**
1. [Issue description]
2. [Issue description]

**Recommendations:**
1. [Recommendation]
2. [Recommendation]
```

---

## Automation Considerations

For future CI/CD integration, these tests should be automated using:

- **Espresso** for UI testing
- **Firebase Test Lab** for multi-device testing
- **Mockito** for unit testing ViewModel logic
- **Robolectric** for local JVM testing

Priority automation targets:

1. Bid acceptance transaction safety
2. Budget calculation accuracy
3. Date sync logic
4. Notification delivery
5. Chat deletion cascade
