# Project Status & TODOs

## ✅ Phase 1: Core Features Complete

### Communication & Groups

- [x] **Messaging System**: Chat Threads, Messages, Image Attachments, Unread Badges
- [x] **Group Management**: 6-digit codes, "Join by Code" flow, Group Chat
- [x] **Group Sharing**: Android sharing intent (creator only)
- [x] **User Search**: Email/phone search for direct messages

### Collaboration Features

- [x] **Task Filtering**: Priority and date range with BottomSheet
- [x] **Assignments**: Assign any user (in/out of group)
- [x] **Assignment UI**: Reusable AvatarStack component
- [x] **Proof of Work**: Image upload (camera/gallery)
- [x] **Review Flow**: Pending → Review → Completed

### Budgeting & Bidding

- [x] **Task Budget**: Automatic totalBudget calculation
- [x] **Bidding System**: Users bid on subtasks
- [x] **Bid UI**: Icon buttons (✓ accept, ✗ reject) for creators
- [x] **Bid Block**: Only one accepted bid per subtask
- [x] **Budget Sync**: Accepted bid updates subtask budget

### Dates & Postponement

- [x] **Subtask Due Date**: Owner can set/edit anytime
- [x] **Parent Task Sync**: Auto-updates to max subtask date
- [x] **Postponement Logic**: Backend request/response flow
- [x] **Postponement Notifications**: Creator notified

### Infrastructure & UX

- [x] **Firebase Storage**: Error handling + rules
- [x] **Snackbar Feedback**: Success/error messages
- [x] **Camera Access**: TaskDetail, Chat, Profile screens
- [x] **FileProvider**: No more camera crashes
- [x] **Permissions**: CAMERA + storage declared
- [x] **Profile Pictures**: Upload + crop (100x100)
- [x] **Notifications**: Firestore triggers (messages, assignments, bids, dates)
- [x] **Theme System**: Color schemes + status bar
- [x] **UI Icons**: In-app + notification badges

## 🚧 Phase 2: Polish & Testing (In Progress)

### HIGH PRIORITY: Postponement UI ✅ COMPLETE

- [x] **Postponement Display**: Add UI in SubtaskItem to show postponement requests
- [x] **Icon Buttons**: Accept (✓) and Reject (✗) buttons (match bid UI pattern)
- [x] **Block if Complete**: Prevent postponement requests on completed subtasks
- [x] **Verify Date Sync**: Parent updates to max subtask date after acceptance

### HIGH PRIORITY: FCM Notifications System ✅ COMPLETE

- [x] **Cloud Functions v2 Migration**: Migrated to firebase-functions 7.0.1 with v2 API
- [x] **FCM Message Delivery**: Using sendEach() for reliable delivery
- [x] **Background Notifications**: Notification payload for closed/background apps
- [x] **Deep Linking**: Tap notification → opens specific task
- [x] **Bid Notifications**: Real-time FCM events when bids placed (BID_PLACED)
- [x] **Postponement Notifications**: FCM for requests, accepts, and rejects
- [x] **Reactive UI Updates**: UpdateEventBus → ViewModel → UI recomposition
- [x] **14 Event Types**: Complete notification support for all events

### HIGH PRIORITY: Testing & Validation

- [x] **Recursive Chat Deletion**: Tasks and subtasks now delete associated chats automatically ✅
- [ ] **E2E Bid Flow**: Comprehensive test plan created in `E2E_TEST_PLAN.md` - Ready for manual testing
  - Test 1: Complete bid flow (place → accept → budget update → date sync → notifications)
  - Test 3: Reactive UI updates via UpdateEventBus
  - Test 4: Postponement integration with bid system
  - Test 5: Completion flow with budget distribution
  - Test 6: Deletion cascade verification
- [ ] **Edge Cases**: Test plan includes 8 edge case scenarios
  - Null dates handling
  - Zero budget handling
  - Multiple subtasks date sync
  - Simultaneous bid acceptance (transaction safety)
  - Offline bid placement
  - Invalid FCM token cleanup
  - Long subtask titles in references
  - Special characters in titles
- [x] **Completion Flow**: creator has to check subtask images of completion and accept/reject the completion.
- [x] **budget assignment** when task or subtask is completed assign the amount of subtask to each user who this completion has been accepted
- [x] **Creator deletion capabilities**: Tasks, subtasks, and chats with FCM notifications
- [x] **Warning dialogs**: Already implemented for task and subtask deletions

**📋 Testing Documentation:**

- See `E2E_TEST_PLAN.md` for detailed test cases, steps, and expected results
- Includes test results template for tracking execution
- Performance metrics and automation recommendations included

### MEDIUM PRIORITY: Code Quality

- [x] **Deprecated Warnings**: Replace `Divider()` with `HorizontalDivider()`
- [x] **Date Format**: Standardized to "MMM dd, yyyy" format ✅
  - Chat list: "MMM dd, yyyy HH:mm" (full date with time)
  - Task/Subtask dates: "MMM dd, yyyy" (full date)
  - Filter dates: "MMM dd, yyyy" (full year for context)
  - Message timestamps: "HH:mm" (time within chat)
- [x] **Snackbar Layout**: Added 80dp bottom padding to avoid overlap with navigation bar ✅
  - ChatScreen: Bottom padding applied
  - TaskDetailScreen: Bottom padding applied
  - ProfileScreen: Bottom padding applied
- [x] **Loading States**: Progress indicators for long operations ✅
  - TaskDetailScreen: Added full-screen loading overlay with semi-transparent background
  - Displays "Processing..." card with CircularProgressIndicator during operations
  - Shows when isLoading state is true (bid operations, deletions, updates)
  - PendingUploadCard: Already has progress indicators for image uploads
- [x] **messages status handeling** handle messages status (pending sended recived) ✅
  - Added `status` field to TaskMessage and ChatMessage models ("sending", "sent", "delivered", "read")
  - Visual indicators with icons:
    - ⏰ Schedule icon: "sending" state
    - ✓ Done icon: "sent" state
    - ✓✓ DoneAll icon (gray): "delivered" state
    - ✓✓ DoneAll icon (colored): "read" state
  - Icons only shown for current user's messages
  - Positioned next to timestamp in message bubble
- [x] **pictures status handeling** upload progress , error uploading, retry option ✅
  - Already implemented with PendingUploadCard component
  - Shows upload progress with CircularProgressIndicator
  - Error states with retry button
  - Cancel option for failed/pending uploads
  - Displays attempt counter (1/3, 2/3, 3/3)

### MEDIUM PRIORITY: Performance

- [x] **Firestore Indexes**: Created comprehensive index configuration file with 11 composite indexes ✅
  - Tasks collection: assignedUserIds + priority/dueDate combinations for filtered queries
  - Subtasks collection: parentTaskId + createdAt for efficient subtask loading
  - ChatThreads collection: memberIds + lastMessageTimestamp for sorted chat lists
  - Messages collection: chatThreadId + createdAt for chronological message display
  - Notifications collection: userId + isRead + createdAt for unread badge queries
  - Groups collection: code lookup and memberIds + createdAt queries
  - Users collection: email search optimization
  - Bids collection: subtaskId + status + createdAt for bid management
  - Tasks collection: creatorId + status for task filtering
  - Configuration: `firestore.indexes.json` ready for deployment
- [x] **Transaction Safety**: Bid accept operations already use Firestore transactions ✅
  - `acceptBid()` function wraps all bid updates in `db.runTransaction`
  - Ensures atomic updates: bid status, assignments, assignedUserIds, and budget
  - Prevents race conditions when multiple users attempt simultaneous bid acceptance
  - Transaction guarantees consistency across all related fields
- [x] **Offline Resilience**: Firestore offline persistence enabled with graceful error handling ✅
  - Enabled unlimited cache size in CollaborativeTasksApplication
  - Added CancellationException handling to preserve coroutine cancellation semantics
  - Wrapped critical operations (calculateTotalBudget, updateParentTaskDueDate) with offline-aware error handling
  - Operations automatically sync when connection is restored
  - User feedback maintained through existing error handling (_error.value)

## 📚 Phase 3: Subscription System & Enterprise Features

### STAGE 1: Current State Audit (1-2 days)

- [ ] **Firestore Structure Review**: Analyze current collections, subcollections, and data schema
- [ ] **Multi-company Data Model**: Evaluate if current structure supports multiple companies/teams
- [ ] **Cost Analysis**: Identify redundant writes and optimize read patterns
- [ ] **Task/Subtask Flow Review**: Document evidence fields, status, permissions, budget/bidding flows
- [ ] **Identify Paywall Opportunities**: List features suitable for premium tiers (unlimited evidence, groups, reports, approval hierarchy)

### STAGE 2: Technical Infrastructure for Subscriptions (4-7 days)

#### 2.1 Plans Data Model

- [ ] **Create `/plans` Collection**:

- [ ]  Define free, pro, business, enterprise plans in Firestore

- [ ] **Plan Features Schema**:
  - Max photos per month
  - Max groups
  - Max users per group
  - Max active tasks
  - Access to budgets/bidding
  - Access to approval hierarchy
  - Extended chat features
  - Storage quota
- [ ] **Dynamic Plan Rules**: Ensure plans can be updated without app redeployment

#### 2.2 Company/Team Billing Structure

- [ ] **Create `/companies` Collection**: Company-level data model
- [ ] **Create `/companies/{id}/billing` Subcollection**:
  - `planId` (free/pro/business/enterprise)
  - `nextBillingDate`
  - `storageUsed`
  - `maxStorage`
  - `usersAllowed`
  - `subscriptionStatus` (active/canceled/expired)
- [ ] **Link Users to Companies**: Update user model with `companyId` field
- [ ] **Company Admin Roles**: Define who can manage billing and subscriptions

#### 2.3 Feature Flags System

- [ ] **Create `FeatureFlags` Data Class**:
  - `canUploadUnlimitedPhotos: Boolean`
  - `maxTeams: Int`
  - `maxTasksActive: Int`
  - `canUseBudgets: Boolean`
  - `canApproveTasks: Boolean`
  - `canUseAdvancedChat: Boolean`
  - `maxStorageGB: Int`
- [ ] **Feature Loading Function**: Load plan features at app startup based on company plan
- [ ] **Cache Feature Flags**: Store in memory to avoid repeated Firestore reads
- [ ] **Feature Check Utilities**: Helper functions to validate feature access throughout app

### STAGE 3: Google Play Billing Integration (5-7 days)

#### 3.1 Google Play Console Setup

- [ ] **Create Subscription Products**:
  - Plan PRO Monthly ($6.99)
  - Plan PRO Annual ($69)
  - Plan Business Monthly ($39)
  - Plan Business Annual ($399)
- [ ] **Create Add-on Products** (Consumables):
  - 100GB Storage Add-on ($4.99)
  - 1TB Storage Add-on ($19.99)
- [ ] **Configure Billing Period**: Set renewal cycles and grace periods
- [ ] **Set up Proration Rules**: Define upgrade/downgrade behavior

#### 3.2 BillingClient Implementation (Kotlin)

- [ ] **Add Google Play Billing Dependency**: `implementation 'com.android.billingclient:billing-ktx:6.x.x'`
- [ ] **Initialize BillingClient**: Set up connection and listeners
- [ ] **Query Available Products**: Fetch subscription and consumable products
- [ ] **Launch Purchase Flow**: Implement UI to start subscription purchase
- [ ] **Handle Purchase Updates**: Process successful/failed/canceled purchases
- [ ] **Restore Purchases**: Handle subscription restoration on new devices
- [ ] **Acknowledge Purchases**: Confirm purchase completion to Google Play

#### 3.3 Backend Verification (Firebase Functions)

- [ ] **Create `verifyPurchase` Cloud Function**: Verify receipts with Google Play API
- [ ] **Create `updateSubscription` Function**: Update company billing in Firestore after verification
- [ ] **Create `handleWebhook` Function**: Process Google Play real-time developer notifications
- [ ] **Implement Receipt Validation**: Use Google Play Developer API for security
- [ ] **Log Billing Events**: Create audit trail for all subscription changes
- [ ] **Handle Subscription Lifecycle**:
  - New subscription
  - Renewal
  - Upgrade/Downgrade
  - Cancellation
  - Expiration
  - Refund

### STAGE 4: Paywall & Feature Limits (3-6 days)

#### 4.1 Paywall UI Components

- [ ] **Create `PaywallScreen` Composable**: Display plans and pricing
- [ ] **Plan Comparison Table**: Show feature differences between plans
- [ ] **Upgrade Prompts**: Show when users hit limits
- [ ] **Billing Management Screen**: View current plan, usage, and change subscription
- [ ] **Success/Failure Dialogs**: Handle purchase outcomes

#### 4.2 Feature Restrictions

- [ ] **Evidence Upload Limits**: Block after monthly quota (free plan)
- [ ] **Group Creation Limits**: Restrict to 1 group on free plan
- [ ] **User Limits**: Max users per group based on plan
- [ ] **Budget/Bidding Access**: Require pro plan or higher
- [ ] **Approval Hierarchy**: Business plan feature
- [ ] **Advanced Chat**: Multimedia messages (pro+)
- [ ] **Storage Limits**: Enforce storage quota per plan

#### 4.3 Usage Tracking

- [ ] **Track Photo Uploads**: Count monthly uploads per company
- [ ] **Track Storage Usage**: Calculate total storage used
- [ ] **Track Active Tasks**: Count concurrent active tasks
- [ ] **Track Group Count**: Monitor number of groups per company
- [ ] **Reset Monthly Counters**: Cloud Function to reset usage each billing cycle

### STAGE 5: Testing & Launch (2-3 days)

- [ ] **Test Purchase Flow**: End-to-end subscription purchase
- [ ] **Test Upgrade/Downgrade**: Verify proration and feature access changes
- [ ] **Test Cancellation**: Ensure access continues until period end
- [ ] **Test Restoration**: Verify purchases restore on reinstall
- [ ] **Test Feature Restrictions**: Confirm paywalls trigger correctly
- [ ] **Load Testing**: Simulate multiple companies with different plans
- [ ] **Security Audit**: Review receipt verification and API security
- [ ] **Legal Compliance**: Terms of service, privacy policy updates

## 📚 Phase 4: Backlog & Future Features

### Nice-to-Have Enhancements

- [ ] **Internationalization (i18n)**: Multi-language support
- [ ] **Analytics**: Track user engagement, feature usage
- [ ] **Payment Integration**: Process bids/budgets (if needed)
- [ ] **Recurring Tasks**: Template tasks that repeat
- [ ] **Task Dependencies**: Block tasks based on other completions
- [ ] **Audit Log**: Track all changes to tasks/budgets
- [ ] **Export Reports**: Generate task completion reports

## Avanced Features for Enterprice PLANS

- [ ] **Google Workspace integration**

## 📋 Reference: Messaging & Legacy Features (Complete)

### General Messaging System (Chat Threads) ✅

- [x] `ChatThread` data model with id, title, memberIds, lastMessage, lastUpdated, type (DIRECT/GROUP)
- [x] `ChatMessage` data model with id, senderId, text, createdAt, readBy
- [x] `ChatViewModel` for fetching threads, messages, sending, creating threads
- [x] Image attachments in messages with camera/gallery support
- [x] Picture caching (Coil default)
- [x] Direct upload to Firebase Storage

### Chat UI/Presentation ✅

- [x] **Chat List Screen**: View conversations with last message preview and timestamp
- [x] **Chat Detail Screen**: Full conversation view with message input
- [x] **New Chat Flow**: Dialog to select users for new conversation
- [x] **Navigation**: Messages added to main navigation (Bottom Bar)

### Group Features ✅

- [x] **Group Chat Integration**: Auto-create ChatThread when group created
- [x] **Group Chat Button**: Added to GroupDetailScreen
- [x] **Group Sharing**: Android share intent with deep link (creator only)

### User Profile & Settings ✅

- [x] **User Profile Screen**: Profile view + sign out
- [x] **Avatar Upload**: Select from device
- [x] **Avatar Crop**: 100x100px crop functionality
- [x] **User Search by Phone**: Find users via phone number for DM

### Theming ✅

- [x] **Color Schemes**: Implemented theme system
- [x] **Status Bar Theming**: Theme-aware status bar coloring
- [x] **In-App Icons**: Custom icon set
- [x] **Notification Badges**: Message count indicators

### Internationalization (i18n)

- [ ] Multi-language implementation

### Git Sync

- [] **sync with github repository** <https://github.com/alainmtz/Group_Task_Manager.git>
