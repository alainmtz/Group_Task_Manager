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

### STAGE 1: Current State Audit (1-2 days) ✅ COMPLETE

- [x] **Firestore Structure Review**: Analyzed 8 collections with complete schema documentation ✅
  - `/users`: User accounts (needs companyId, role, storage tracking)
  - `/groups`: Team management (needs plan-based limits)
  - `/tasks`: Task management (needs company linking, approval hierarchy)
  - `/subtasks`: Subtask & bidding (needs evidence limits, storage quota)
  - `/earnings`: Budget distribution (needs company tracking)
  - `/chatThreads`: Chat conversations (needs multimedia restrictions)
  - `/messages`: Chat messages (needs attachment limits)
  - `/notifications`: FCM notifications (well-structured, minimal changes)
  
- [x] **Multi-company Data Model**: Current state is user-centric, NOT multi-company ready ✅
  - ❌ No `/companies` collection
  - ❌ No billing management
  - ❌ No feature gating
  - ❌ No resource limits
  - ✅ **Recommendation**: Implement company-centric model (Option A)
  
- [x] **Cost Analysis**: Optimized Firestore operations documented ✅
  - Current cost: ~$0.07/month per 100 active users
  - 11 composite indexes already configured
  - Identified optimization opportunities (batch updates, notification consolidation)
  
- [x] **Task/Subtask Flow Review**: Complete workflow documentation ✅
  - Task creation: 2 writes per task
  - Bidding flow: 5+ writes per bid acceptance (transaction-safe)
  - Evidence upload: Storage + Firestore integration
  - Postponement: Request/approval workflow
  - Deletion: Cascade delete with notifications
  
- [x] **Identify Paywall Opportunities**: 4-tier pricing model defined ✅
  - **FREE**: 1 group, 5 members, 10 tasks, 5 photos/month, 100MB
  - **PRO** ($6.99/mo): Unlimited groups, 15 members, 50 tasks, budgeting/bidding, 5GB
  - **BUSINESS** ($39/mo): 50 members, 200 tasks, approval hierarchy, analytics, 50GB
  - **ENTERPRISE** (custom): Unlimited everything, Google Workspace integration, SSO

**📄 Documentation**: See `FIRESTORE_STRUCTURE_AUDIT.md` for complete analysis

### STAGE 2: Technical Infrastructure for Subscriptions (4-7 days) ✅ COMPLETE

#### 2.1 Plans Data Model ✅

- [x] **Create `/plans` Collection**: Complete data model with PlanTier enum
- [x] **Plan Features Schema**: 18 feature flags implemented (budgets, analytics, storage, etc.)
- [x] **Dynamic Plan Rules**: PlanDefaults object with FREE, PRO, BUSINESS, ENTERPRISE configurations

#### 2.2 Company/Team Billing Structure ✅

- [x] **Create `/companies` Collection**: Company-level data model with billing
- [x] **Create Company Billing Fields**:
  - `planId` (free/pro/business/enterprise)
  - `nextBillingDate`
  - `storageUsedBytes`
  - `googlePlayPurchaseToken`
  - `subscriptionStatus` (active/canceled/expired/grace_period/on_hold)
  - Usage tracking: `activeTasksCount`, `groupsCount`, `photosUploadedThisMonth`
- [x] **Link Users to Companies**: User model updated with `companyId` field
- [x] **Company Admin Roles**: CompanyRole enum (OWNER, ADMIN, MEMBER) with permissions

#### 2.3 Feature Flags System ✅

- [x] **Create `FeatureFlags` Object**: Runtime feature checking system
- [x] **Feature Check Methods**: 10 methods returning Pair<Boolean, String?> with upgrade prompts
  - `canCreateGroup`, `canCreateTask`, `canAddMember`
  - `canUploadPhoto`, `canUploadFile` (storage quota)
  - `canUseBudgets`, `canUseApprovalHierarchy`
  - `canSendMultimedia`, `canUseAnalytics`, `canExportData`
- [x] **Usage Tracking Utilities**: `getStorageUsagePercentage`, `getPhotoUsagePercentage`
- [x] **Upgrade Suggestions**: Automatic next-tier recommendations in error messages

**📄 Documentation**: See `STAGE2_IMPLEMENTATION.md` for complete details

**✅ Compilation Status**: BUILD SUCCESSFUL - All models compile without errors

---

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

- [x] **sync with github repository** https://github.com/alainmtz/Group_Task_Manager.git ✅
  - Initial commit: 136 files, 22,295 lines
  - Optimized .gitignore: ~210MB of build files excluded
  - Pushed to main branch successfully
