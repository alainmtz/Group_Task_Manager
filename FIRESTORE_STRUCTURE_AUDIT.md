# Firestore Structure Audit - Phase 3 Planning

**Date**: December 9, 2025
**Purpose**: Analyze current data model to prepare for subscription system implementation

## Current Collections Overview

### 1. `/users` Collection
**Purpose**: Store user account information

**Fields**:
- `id` (String): Document ID (Firebase Auth UID)
- `email` (String): User email address
- `name` (String?): Display name
- `phoneNumber` (String?): Phone number for search
- `photoUrl` (String?): Profile picture URL

**Current Limitations for Multi-Company**:
- ❌ No `companyId` field to link users to organizations
- ❌ No role/permission fields
- ❌ No subscription tier tracking
- ❌ No storage usage tracking

**Needed Changes for Subscriptions**:
- Add `companyId: String?` field
- Add `role: String` field (admin, member, owner)
- Add `storageUsedBytes: Long` field
- Add `uploadCount: Int` field (monthly reset)

---

### 2. `/groups` Collection
**Purpose**: Manage team/group collaborations

**Fields**:
- `id` (String): Document ID
- `name` (String): Group name
- `creatorId` (String): Group creator
- `memberIds` (List<String>): Member user IDs
- `code` (String): 6-digit join code

**Current Limitations for Multi-Company**:
- ❌ No group count limits per user/company
- ❌ No member count validation
- ❌ No plan-based restrictions

**Needed Changes for Subscriptions**:
- Add `companyId: String?` field to link groups to companies
- Add `planTier: String` field to enforce group limits
- Add validation logic for max members based on plan

---

### 3. `/tasks` Collection
**Purpose**: Main task management

**Fields**:
- `id` (String): Document ID
- `title` (String): Task title
- `description` (String): Task description
- `dueDate` (Date): Due date
- `creatorId` (String): Task creator
- `priority` (TaskPriority): LOW, MEDIUM, HIGH
- `status` (TaskStatus): PENDING, IN_REVIEW, COMPLETED
- `assignedUserIds` (List<String>): Assigned users
- `groupId` (String?): Associated group
- `totalBudget` (Double?): Total task budget
- `postponementRequests` (List<PostponementRequest>): Postponement requests

**Current Limitations for Multi-Company**:
- ❌ No active task count tracking per company
- ❌ No budget feature restrictions
- ❌ No approval hierarchy support

**Needed Changes for Subscriptions**:
- Add `companyId: String?` field
- Add `requiresApproval: Boolean` field (business plan feature)
- Add `approvalHierarchy: List<String>` field for approval chain
- Track active task count in company billing document

---

### 4. `/subtasks` Collection
**Purpose**: Task breakdown and bidding system

**Fields**:
- `id` (String): Document ID
- `parentTaskId` (String): Parent task reference
- `title` (String): Subtask title
- `createdBy` (String): Creator user ID
- `status` (SubtaskStatus): PENDING, IN_PROGRESS, COMPLETED, IN_REVIEW
- `assignedUserIds` (List<String>): Assigned users
- `assignments` (Map<String, String>): User assignment status
- `dueDate` (Date?): Due date
- `budget` (Double): Budget amount
- `bids` (List<SubtaskBid>): Bid proposals
- `evidences` (List<String>): Proof of work image URLs
- `pendingUploads` (List<PendingUpload>): Upload queue
- `postponementRequests` (List<PostponementRequest>): Date extension requests

**SubtaskBid Structure**:
- `userId` (String): Bidder ID
- `amount` (Double): Bid amount
- `status` (String): pending, accepted, rejected
- `createdAt` (Date): Bid timestamp

**Current Limitations for Multi-Company**:
- ❌ No evidence upload limits enforcement
- ❌ No budget/bidding feature restriction
- ❌ No storage quota tracking

**Needed Changes for Subscriptions**:
- Enforce evidence upload limits in free plan (e.g., 5 photos/month)
- Restrict bidding feature to pro+ plans
- Track storage usage per company when uploading evidence

---

### 5. `/earnings` Collection
**Purpose**: Track budget distribution and payments

**Fields**:
- `id` (String): Document ID
- `userId` (String): Earning user
- `subtaskId` (String): Source subtask
- `taskId` (String): Source task
- `subtaskTitle` (String): Subtask name
- `amount` (Double): Earning amount
- `timestamp` (Timestamp): Earning date
- `status` (String): pending, confirmed
- `creatorId` (String): Task creator

**Current Limitations for Multi-Company**:
- ❌ No company-level budget tracking
- ❌ No payment processing integration

**Needed Changes for Subscriptions**:
- Add `companyId: String?` field
- Consider payment gateway integration for business plans
- Add budget reports/analytics (business+ feature)

---

### 6. `/chatThreads` Collection
**Purpose**: Chat conversations (group and direct messages)

**Fields**:
- `id` (String): Document ID
- `title` (String): Chat title
- `memberIds` (List<String>): Participants
- `lastMessage` (String): Last message preview
- `lastMessageTimestamp` (Timestamp): Last activity
- `type` (String): DIRECT, GROUP

**Current Limitations for Multi-Company**:
- ❌ No multimedia message restrictions
- ❌ No advanced chat features gating

**Needed Changes for Subscriptions**:
- Restrict multimedia attachments to pro+ plans
- Add message history limits for free plan
- Add `companyId: String?` for company chats

---

### 7. `/messages` Subcollection (under `/chatThreads/{id}/messages`)
**Purpose**: Store chat messages

**Fields**:
- `id` (String): Message ID
- `senderId` (String): Sender user ID
- `text` (String): Message content
- `createdAt` (Date): Timestamp
- `readBy` (List<String>): Read receipts
- `attachments` (List<Attachment>): Image attachments
- `status` (String): sending, sent, delivered, read

**Attachment Structure**:
- `url` (String): Storage URL
- `type` (String): image, file, etc.

**Current Limitations for Multi-Company**:
- ❌ Unlimited attachments on free plan
- ❌ No storage quota enforcement

**Needed Changes for Subscriptions**:
- Restrict attachments to pro+ plans
- Track attachment storage per company
- Add file size limits based on plan

---

### 8. `/notifications` Collection
**Purpose**: User notifications and FCM

**Fields**:
- `id` (String): Document ID
- `userId` (String): Recipient user ID
- `title` (String): Notification title
- `body` (String): Notification body
- `type` (NotificationType): Enum of 14 event types
- `createdAt` (Timestamp): Creation time
- `isRead` (Boolean): Read status
- `data` (Map<String, String>): Extra data

**NotificationType Enum**:
- TASK_ASSIGNMENT
- MESSAGE
- BID_PLACED
- BID_ACCEPTED
- BID_REJECTED
- BUDGET_PROPOSAL
- POSTPONEMENT_REQUEST
- POSTPONEMENT_ACCEPTED
- POSTPONEMENT_REJECTED
- TASK_UPDATED
- SUBTASK_UPDATED
- SUBTASK_COMPLETED
- TASK_DELETED
- SUBTASK_DELETED

**Current Limitations for Multi-Company**:
- ✅ Already well-structured, minimal changes needed

**Needed Changes for Subscriptions**:
- Add notification preferences per user (business+ feature)
- Add notification history limits for free plan

---

## Multi-Company Support Analysis

### Current State: ❌ **Not Multi-Company Ready**

The current data model is **user-centric** rather than **company-centric**. Each user operates independently without organizational boundaries.

### Identified Issues:

1. **No Company Entity**: No `/companies` collection to group users
2. **No Billing Management**: No subscription tracking
3. **No Feature Gating**: All features available to all users
4. **No Resource Limits**: Unlimited groups, tasks, storage, uploads
5. **No Admin Roles**: No company-level administration

### Required Architecture Changes:

#### Option A: Add Company Layer (Recommended)
- Create `/companies` collection as parent entity
- Link all resources (users, groups, tasks) to company via `companyId`
- Implement company-level billing and feature flags
- Support multiple users per company with roles

#### Option B: User-Level Subscriptions (Simpler, Limited)
- Each user has individual subscription
- Users can invite others to their "workspace"
- Simpler billing but less enterprise-friendly
- Not suitable for team/business plans

**Recommendation**: **Option A** - Company-centric model for scalability and enterprise features.

---

## Cost Analysis: Firestore Operations

### Current Read Patterns:

1. **Task List Load**: 
   - Query: `whereArrayContains("assignedUserIds", userId)`
   - With filters: Additional `whereEqualTo("priority")`, `orderBy("dueDate")`
   - **Cost**: ~1 read per task per page load

2. **Subtask Load**:
   - Query: `whereEqualTo("parentTaskId", taskId)`
   - **Cost**: ~1 read per subtask per task view

3. **Chat List Load**:
   - Query: `whereArrayContains("memberIds", userId).orderBy("lastMessageTimestamp")`
   - **Cost**: ~1 read per chat thread per load

4. **Message Load**:
   - Query: `chatThreads/{id}/messages.orderBy("createdAt")`
   - **Cost**: ~1 read per message per chat open

5. **Notification Load**:
   - Query: `whereEqualTo("userId", userId).whereEqualTo("isRead", false)`
   - **Cost**: ~1 read per notification check

### Current Write Patterns:

1. **Task Creation**: 
   - 1 write to `/tasks`
   - 1 write to `/chatThreads` (task chat)
   - **Cost**: 2 writes per task

2. **Subtask Completion**:
   - 1 update to `/subtasks` (status)
   - 1 update to `/tasks` (totalBudget recalc)
   - N writes to `/earnings` (per assignee)
   - 1 write to `/chatThreads` (system message)
   - **Cost**: ~4+ writes per completion

3. **Bid Acceptance**:
   - 1 transaction update to `/subtasks`
   - 1 update to `/tasks` (dueDate sync)
   - N notifications writes
   - 2 writes to task chat (system messages)
   - **Cost**: ~5+ writes per bid accept

### Optimization Opportunities:

1. **Reduce Redundant Writes**:
   - ❌ Currently recalculating `totalBudget` on every subtask change
   - ✅ Could batch budget updates or use client-side aggregation

2. **Optimize Chat Messages**:
   - ❌ System messages written for every bid/assignment
   - ✅ Could consolidate or make optional

3. **Notification Batching**:
   - ❌ Individual notification documents per user
   - ✅ Could use FCM topics for group notifications

4. **Index Optimization**:
   - ✅ Already created composite indexes in `firestore.indexes.json`
   - Covers all major query patterns

**Estimated Monthly Cost (per 100 active users)**:
- Reads: ~50,000 reads/month = $0.018
- Writes: ~20,000 writes/month = $0.054
- **Total**: ~$0.07/month per 100 users (very low)

---

## Paywall Opportunities

### Tier 1: FREE Plan (Current Base Features)
**Target**: Individual users, small teams testing the app

**Included Features**:
- ✅ 1 group maximum
- ✅ Up to 5 members per group
- ✅ 10 active tasks maximum
- ✅ Basic task management (create, assign, complete)
- ✅ Basic chat (text-only messages)
- ✅ 5 evidence photos per month
- ✅ 100MB storage quota
- ✅ Email notifications only
- ✅ Standard support

**Restrictions**:
- ❌ No budget/bidding features
- ❌ No multimedia chat attachments
- ❌ No approval hierarchy
- ❌ No advanced analytics
- ❌ No export/reports

---

### Tier 2: PRO Plan ($6.99/month or $69/year)
**Target**: Freelancers, small businesses, power users

**Everything in FREE, plus**:
- ✅ Unlimited groups
- ✅ Up to 15 members per group
- ✅ 50 active tasks
- ✅ **Budget & Bidding System** 🎯 (Major selling point)
- ✅ Multimedia chat (images, files)
- ✅ Unlimited evidence photos
- ✅ 5GB storage quota
- ✅ Push notifications (FCM)
- ✅ Postponement requests
- ✅ Priority support
- ✅ Basic analytics dashboard

**Premium Features**:
- 💰 Bid on subtasks
- 💰 Automatic budget distribution
- 💰 Earnings tracking
- 📊 Task completion reports

---

### Tier 3: BUSINESS Plan ($39/month or $399/year)
**Target**: Teams, departments, small companies

**Everything in PRO, plus**:
- ✅ Up to 50 members per group
- ✅ 200 active tasks
- ✅ **Approval Hierarchy** 🎯 (Business feature)
- ✅ Multi-level task approval workflow
- ✅ Advanced analytics & reports
- ✅ Export data (CSV, PDF)
- ✅ 50GB storage quota
- ✅ Audit logs (track all changes)
- ✅ Custom notifications preferences
- ✅ Dedicated support

**Business Features**:
- 🔒 Task approval chains
- 📈 Advanced analytics (completion rates, time tracking)
- 📄 Custom reports and exports
- 🔍 Full audit trail

---

### Tier 4: ENTERPRISE Plan (Custom Pricing)
**Target**: Large organizations, corporations

**Everything in BUSINESS, plus**:
- ✅ Unlimited members
- ✅ Unlimited tasks
- ✅ Unlimited storage
- ✅ **Google Workspace Integration** 🎯
- ✅ SSO/SAML authentication
- ✅ Custom branding
- ✅ API access
- ✅ On-premise deployment option
- ✅ SLA guarantees
- ✅ Dedicated account manager

**Enterprise Features**:
- 🏢 Google Workspace sync
- 🔐 Advanced security (SSO, 2FA)
- 🎨 White-label options
- 🔌 REST API access

---

## Task/Subtask Flow Documentation

### 1. Task Creation Flow

**Steps**:
1. User creates task via `CreateTaskScreen`
2. `TaskViewModel.createTask()` called
3. Task document written to `/tasks`
4. Task chat thread created in `/chatThreads`
5. If group task: Members notified via FCM (TASK_ASSIGNMENT)
6. Navigation to TaskDetailScreen

**Firestore Operations**:
- 1 write to `/tasks`
- 1 write to `/chatThreads`
- N writes to `/notifications` (per assigned user)

**Current Permissions**: ✅ Any user can create tasks

**Subscription Impact**:
- FREE: Max 10 active tasks
- PRO: Max 50 active tasks
- BUSINESS: Max 200 active tasks
- ENTERPRISE: Unlimited

---

### 2. Subtask Creation & Bidding Flow

**Steps**:
1. Creator adds subtask to task
2. `TaskViewModel.createSubtask()` writes to `/subtasks`
3. Subtask appears in TaskDetailScreen
4. Other users can place bids via `TaskViewModel.placeBid()`
5. Bids written to subtask's `bids` array
6. Creator sees bids and can accept/reject
7. On accept: Budget assigned, user assigned, notifications sent

**Firestore Operations (Per Bid)**:
- 1 update to `/subtasks` (add bid)
- 1 write to `/notifications` (notify creator)
- On accept: 1 transaction to `/subtasks`, N notifications, 2 chat messages

**Current Permissions**: 
- ✅ Any user can bid
- ✅ Only creator can accept/reject bids

**Subscription Impact**:
- FREE: ❌ Budget/bidding disabled
- PRO+: ✅ Full bidding system

---

### 3. Evidence Upload & Completion Flow

**Steps**:
1. Assigned user completes work
2. User uploads evidence images via camera/gallery
3. Images stored in Firebase Storage: `/subtasks/{id}/evidence_{timestamp}.jpg`
4. `TaskViewModel.uploadEvidence()` adds URL to subtask
5. Subtask status → IN_REVIEW
6. Creator reviews evidence in TaskDetailScreen
7. Creator accepts/rejects completion
8. On accept: Budget distributed to `/earnings`, status → COMPLETED

**Firestore Operations**:
- 1 Storage upload per image
- 1 update to `/subtasks` (add evidence URL)
- 1 update to `/subtasks` (status change)
- N writes to `/earnings` (per assignee)
- 1 update to `/tasks` (totalBudget recalc)

**Current Permissions**:
- ✅ Only assigned users can upload evidence
- ✅ Only creator can approve/reject completion

**Subscription Impact**:
- FREE: Max 5 evidence photos/month, 100MB storage
- PRO: Unlimited photos, 5GB storage
- BUSINESS: 50GB storage
- ENTERPRISE: Unlimited storage

---

### 4. Postponement Request Flow

**Steps**:
1. Assigned user requests date extension
2. `TaskViewModel.requestPostponement()` adds request to subtask
3. Creator notified via FCM (POSTPONEMENT_REQUEST)
4. Creator sees request in SubtaskItem UI
5. Creator accepts/rejects via icon buttons
6. On accept: `dueDate` updated, parent task date synced
7. Notifications sent to all parties

**Firestore Operations**:
- 1 update to `/subtasks` (add request)
- 1 write to `/notifications` (creator)
- On accept: 1 update to `/subtasks` (date), 1 update to `/tasks` (sync date), N notifications

**Current Permissions**:
- ✅ Only assigned users can request postponement
- ✅ Only creator can approve/reject

**Subscription Impact**:
- FREE: ✅ Included (basic feature)
- All tiers: ✅ Full access

---

### 5. Deletion & Cascade Flow

**Steps**:
1. Creator deletes task/subtask
2. Warning dialog shown
3. `TaskViewModel.deleteTask()` or `deleteSubtask()`
4. Document deleted from Firestore
5. Associated chat thread deleted (recursive)
6. All members notified via FCM
7. UI updated via UpdateEventBus

**Firestore Operations**:
- 1 delete from `/tasks` or `/subtasks`
- 1 delete from `/chatThreads`
- N deletes from `/chatThreads/{id}/messages`
- N writes to `/notifications`

**Current Permissions**:
- ✅ Only creator can delete tasks
- ✅ Only subtask creator can delete subtasks

**Subscription Impact**: No restrictions (safety feature)

---

## Summary & Recommendations

### Current State Assessment:

✅ **Strengths**:
- Well-structured data model for current features
- Efficient Firestore queries with indexes
- Comprehensive notification system
- Real-time updates via UpdateEventBus
- Offline persistence enabled

❌ **Gaps for Subscriptions**:
- No company/organization layer
- No billing management
- No feature gating logic
- No usage tracking (storage, uploads, tasks)
- No admin roles or permissions

### Phase 3 Implementation Priority:

**STAGE 1 (This Document)**: ✅ COMPLETE
- Firestore structure analysis
- Multi-company evaluation
- Cost analysis
- Paywall identification

**STAGE 2 (Next)**: Create Subscription Data Model
- Design `/companies` collection
- Design `/plans` collection
- Add `companyId` to existing models
- Implement feature flags system

**STAGE 3**: Google Play Billing Integration
- Set up billing products in Play Console
- Implement BillingClient in app
- Backend receipt verification

**STAGE 4**: Paywall UI & Feature Restrictions
- Create PaywallScreen
- Implement usage limit checks
- Add upgrade prompts

**STAGE 5**: Testing & Launch
- E2E subscription testing
- Security audit
- Legal compliance

### Estimated Timeline:
- **STAGE 2**: 4-7 days
- **STAGE 3**: 5-7 days  
- **STAGE 4**: 3-6 days
- **STAGE 5**: 2-3 days
- **Total**: ~3-4 weeks for full subscription system

---

**Next Steps**: Begin STAGE 2 - Design and implement company/billing data model.
