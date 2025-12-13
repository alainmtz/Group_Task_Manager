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

## ✅ Stage 2: Subscription System - FULLY COMPLETE

### Plan Models & Company Structure
- [x] Plan data models (FREE, PRO, BUSINESS, ENTERPRISE)
- [x] Company model with subscription fields
- [x] PlanFeatures with all feature flags
- [x] FeatureFlags service (14 check methods)
- [x] Unit tests for FeatureFlags (42 tests, all passing)

### CompanyPlanProvider & Integration
- [x] Singleton provider with real-time listeners
- [x] Auto-loads company/plan on Firebase Auth sign-in
- [x] StateFlow integration for reactive UI
- [x] FREE plan default for users without company
- [x] Documentation (PLAN_INTEGRATION_GUIDE.md)

### Firestore Setup
- [x] Plans collection initialized (4 plans)
- [x] Companies collection structure
- [x] Owner company created for testing
- [x] Firebase rules updated

## ✅ Stage 4: Paywall UI & Feature Limits - COMPLETE

### UI Components Created
- [x] PaywallScreen - Full plan comparison with pricing
- [x] UpgradePrompts - 4 reusable components (Dialog, Snackbar, Banner, Chip)
- [x] UsageMetrics - Usage tracking with progress bars
- [x] BillingScreen - Complete subscription management

### Feature Restrictions Integrated
- [x] GroupViewModel - Checks limits before creating groups/adding members
- [x] TaskViewModel - Checks limits before creating tasks
- [x] CreateGroupScreen - Shows upgrade prompts
- [x] CreateTaskScreen - Shows upgrade prompts
- [x] HomeScreen - Usage summary chip in toolbar

### Company Management ✅ NEW
- [x] CompanyService - Auto-create company on upgrade
- [x] Data migration - Migrate user groups/tasks to company
- [x] CompanyManagementScreen - Manage members, roles, settings
- [x] CompanyManagementViewModel - State management
- [x] Member management - Add, remove, change roles
- [x] User.role field - Changed from String to CompanyRole enum
- [x] Navigation integration - Added routes for billing, company_management, paywall
- [x] ProfileScreen - Added navigation buttons to company/billing screens
- [x] PaywallScreen - Auto-create company on upgrade integration
- [x] HomeScreen - Added onNavigateToPaywall callback

## ✅ Phase 2: Polish & Testing (Complete)

### Stage 4 Summary - December 12, 2025

**Major Achievements:**

1. ✅ Complete test suite execution (Tests 1.1-1.5, 7.1)
2. ✅ Photo upload flow fully functional with evidence submission
3. ✅ Budget distribution system working end-to-end
4. ✅ Firestore security rules fixed for production use
5. ✅ Material 3 UI components with modern design
6. ✅ CompanyPlanProvider integration issues resolved

**Technical Fixes Applied:**

- Company lookup via task/group hierarchy (bypasses provider cache issues)
- Checkbox state management with local `isPendingPhotoUpload` flag
- Three new Firestore rule sets (earnings, comments, system messages)
- Enhanced logging throughout photo upload pipeline

**Next Phase:** Security hardening (Firestore rules audit) and Stage 3 (Google Play Billing)

---

## 🚧 Phase 2: Additional Polish (Optional)

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

### HIGH PRIORITY: Stage 4 Manual Testing ✅ COMPLETE

#### All Tests Completed ✅
- [x] **Test 1.1 - Group Creation Limit (FREE)**: Fully working
  - Backend enforcement with "effective company" pattern
  - Modern Material 3 Snackbar design (Card-based, elevated)
  - Positioned at bottom center of screen
  - Error message with upgrade prompt and star icon
  - Enhanced logging for debugging
- [x] **Test 1.2 - Task Creation Limit (FREE)**: Working with effective company pattern
- [x] **Test 1.3 - Member Addition Limit (FREE)**: 3 members max enforced
- [x] **Test 1.4 - Storage Limit Display (FREE)**: 100 MB quota displayed
- [x] **Test 1.5 - Photo Upload Limit (FREE)**: 5 photos/month enforced
  - Photo upload working correctly
  - Limit check implemented via task/group company lookup
  - Fixed CompanyPlanProvider dependency issue
- [x] **Test 7.1 - No Company Handling**: Complete solution
  - Temporary "Personal Account" for users without company
  - Billing & Subscription screen accessible
  - Limits enforced correctly
  - No crashes in any navigation
  - FREE plan default applied

#### Critical Bug Fixes ✅

- [x] **Photo Upload Issue**: Fixed "No company found" error
  - Modified `completeSubtaskWithProof()` to fetch company from parent task/group
  - Removed dependency on CompanyPlanProvider.currentCompany
  - Added detailed logging for debugging
- [x] **Checkbox State Issue**: Fixed checkbox reverting to unchecked
  - Added `isPendingPhotoUpload` local state
  - Checkbox stays checked during photo selection/upload
  - Resets on cancel, permission denial, or successful upload
- [x] **Firestore Security Rules**: Complete rule setup
  - ✅ `/users/{userId}/earnings` subcollection rules added
  - ✅ `/tasks/{taskId}/comments` subcollection rules added
  - ✅ System messages (`authorId: "system"`) now allowed
  - ✅ Budget receipt confirmation working without permission errors
- [x] **Budget Distribution Flow**: End-to-end tested and working
  - Photo upload → Pending state → Creator approval → Budget distributed
  - Earnings created in `/users/{userId}/earnings` subcollection
  - User can confirm receipt → System message posted to task chat
  - ProfileScreen updates with new earnings statistics

#### UI/UX Improvements ✅

- [x] **UpgradeSnackbar Redesign**: Modern Material 3 design
  - Card-based layout with primary container colors
  - Circular icon background (40dp) with primary color
  - Prominent button with star icon
  - 6dp elevation for depth
  - Rounded corners (12dp)
  - Bottom center positioning with 16dp padding
- [x] **Effective Company Pattern**: Applied to:
  - GroupViewModel.createGroup() - For limit enforcement
  - TaskViewModel.completeSubtaskWithProof() - Photo limit checks
  - BillingScreen navigation - Shows "Personal Account"

#### Known Issues & Improvements Needed

- [ ] **Firestore Rules Review**: Some rules too permissive (see audit below)
  - 🔴 CRITICAL: `/users/{userId}` read access (any authenticated user can read all profiles)
  - 🔴 CRITICAL: `/bids` collection (anyone can modify/delete any bid)
  - 🔴 CRITICAL: `/earnings` global collection (legacy, should be removed if unused)
  - 🟡 MODERATE: `/users/{userId}/earnings` create (should validate creatorId)
  - 🟡 MODERATE: `/groups` update (too permissive for members)
  - 🟡 MODERATE: `/tasks` create (should validate groupId membership)

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

## 🔒 URGENT: Security Hardening (Before Production Launch)

### Firestore Rules Audit - HIGH PRIORITY

#### 🔴 CRITICAL Security Issues (Fix Immediately)

1. **`/users/{userId}` - Overly Permissive Read Access**

   ```firerules
   // CURRENT (INSECURE):
   allow read: if isAuthenticated();

   // SHOULD BE:
   allow read: if isAuthenticated() && (
     userId == request.auth.uid ||
     // Add logic to check if users share a company/group
   );
   ``` text
   **Risk:** Any authenticated user can read all user profiles (emails, names, phone numbers)

2. **`/bids` Collection - No Access Control**

   ```firerules
   // CURRENT (INSECURE):
   allow read: if isAuthenticated();
   allow create: if isAuthenticated();
   allow update: if isAuthenticated();
   allow delete: if isAuthenticated();

   // SHOULD BE:
   allow read: if isAuthenticated() && (
     resource.data.bidderId == request.auth.uid ||
     // Check if user is creator of parent subtask/task
   );
   allow create: if isAuthenticated() && request.resource.data.bidderId == request.auth.uid;
   allow update: if isAuthenticated() && (
     resource.data.bidderId == request.auth.uid ||
     // Only task creator can accept/reject
   );
   allow delete: if isAuthenticated() && resource.data.bidderId == request.auth.uid;
   ```text
   **Risk:** Users can view, modify, or delete ANY bid from ANY user

3. **`/earnings` Global Collection - Legacy?**

   ```firerules
   // CURRENT:
   allow create: if isAuthenticated();
   ```

   **Action:** Verify if this collection is still used. If not, remove rules entirely.
   **Risk:** Any user can create fake earnings documents

#### 🟡 MODERATE Security Issues (Fix Soon)

4. **`/users/{userId}/earnings` - Weak Create Validation**

   - Should validate that `creatorId` matches `request.auth.uid`
   - Should validate that earning is linked to valid task/subtask

5. **`/groups` - Permissive Update Rules**
   - Members can update any field
   - Should restrict based on field changes (e.g., only creator can change `name`)

6. **`/tasks` - No Group Membership Validation on Create**
   - Should verify user belongs to group if `groupId` is set
   - Currently allows creating tasks in any group

#### ✅ Well-Secured Collections

- `/companies` - Proper owner/admin/member checks
- `/plans` - Read-only for users (correct)
- `/subtasks` - Complete parent task validation
- `/chatThreads` - Member-only access
- `/notifications` - User-scoped correctly
- `/auditLogs` - Admin-only, immutable

### Action Items

- [ ] **Fix `/users` read permissions** (Block 1 - 30 min)
- [ ] **Fix `/bids` collection rules** (Block 2 - 1 hour)
- [ ] **Remove or fix `/earnings` global collection** (Block 3 - 15 min)
- [ ] **Strengthen `/users/{userId}/earnings` validation** (Block 4 - 30 min)
- [ ] **Review and restrict `/groups` updates** (Block 5 - 45 min)
- [ ] **Add group membership check to `/tasks` create** (Block 6 - 30 min)
- [ ] **Deploy updated rules** (Block 7 - 5 min)
- [ ] **Test with non-owner accounts** (Block 8 - 1 hour)

**Total Estimated Time:** 4-5 hours

---

## 📚 Phase 3: Subscription System & Enterprise Features combinations for filtered queries

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
- [x] **Feature Check Methods**: 14 methods returning Pair<Boolean, String?> with upgrade prompts
  - `canCreateGroup`, `canCreateTask`, `canAddMember`
  - `canUploadPhoto`, `canUploadFile` (storage quota)
  - `canUseBudgets`, `canUseApprovalHierarchy`
  - `canSendMultimedia`, `canUseAnalytics`, `canExportData`
  - `canUseSSO`, `canUseAPI`, `canWhiteLabel`, `canUseAuditLogs` (Enterprise features)
- [x] **Usage Tracking Utilities**: `getStorageUsagePercentage`, `getPhotoUsagePercentage`, `getSupportLevel`
- [x] **Upgrade Suggestions**: Automatic next-tier recommendations in error messages

#### 2.4 Company & Plan Provider ✅

- [x] **Create `CompanyPlanProvider` Singleton**: Auto-loads company and plan on sign-in
- [x] **Real-time Listeners**: Firestore listeners for company/plan updates
- [x] **StateFlow Integration**: Expose `currentCompany`, `currentPlan`, `isLoading` flows
- [x] **Application Initialization**: Integrated into `CollaborativeTasksApplication.onCreate()`
- [x] **Example ViewModel**: `PlanAwareViewModel` demonstrates usage patterns

#### 2.5 Unit Tests ✅

- [x] **FeatureFlags Test Suite**: 42 comprehensive unit tests (all passing)
  - 16 quantity limit tests (groups, tasks, members, photos, storage)
  - 14 boolean feature tests (budgets, analytics, approval, export, etc.)
  - 8 Enterprise feature tests (SSO, API, white-label, audit logs)
  - 4 utility method tests (usage percentages, support levels)
- [x] **Test Dependencies**: Added JUnit, Mockito, Coroutines test utilities
- [x] **Test Coverage**: All plan tiers validated (FREE, PRO, BUSINESS, ENTERPRISE)

#### 2.6 Firestore Plans Initialization ✅

- [x] **Init Plans Script**: Node.js script to create plan documents in Firestore
- [x] **Service Account Permissions**: Granted `datastore.user` role to bypass security rules
- [x] **Plans Created**: All 4 plans (FREE, PRO, BUSINESS, ENTERPRISE) initialized in Firestore
- [x] **Enterprise Company**: Created company document for owner with Enterprise plan

#### 2.7 Documentation & Deployment ✅

- [x] **Integration Guide**: `PLAN_INTEGRATION_GUIDE.md` with complete usage examples
- [x] **Test Summary**: `FEATURE_FLAGS_TEST_SUMMARY.md` with 42 test breakdowns
- [x] **Security Setup**: Service account keys rotated, uploaded to Secret Manager
- [x] **Secrets Management**: Local `.secrets/` folder convention established
- [x] **Firestore Rules**: Deployed rules with plan read access
- [x] **Build Verification**: All code compiles successfully

#### 📄 Documentation**

- `STAGE2_IMPLEMENTATION.md` - Complete technical details
- `PLAN_INTEGRATION_GUIDE.md` - Usage guide for ViewModels/Composables
- `FEATURE_FLAGS_TEST_SUMMARY.md` - Test coverage breakdown

**✅ Stage 2 Status**: FULLY COMPLETE - All infrastructure ready for UI implementation

**🎯 Next Steps**: Stage 3 (Google Play Billing) or Stage 4 (Paywall UI & Feature Limits)

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

### STAGE 4: Paywall & Feature Limits (3-6 days) ✅ COMPLETE

#### 4.1 Paywall UI Components ✅

- [x] **Create `PaywallScreen` Composable**: Display plans and pricing ✅
  - Full plan comparison with 4 tiers (FREE, PRO, BUSINESS, ENTERPRISE)
  - Feature matrix showing capabilities per plan
  - Pricing display with monthly/annual options
  - "Current Plan" badge indicator
  - Upgrade buttons with navigation to billing
- [x] **Plan Comparison Table**: Show feature differences between plans ✅
  - Organized by categories (Collaboration, Budgeting, Storage, Support, Enterprise)
  - Visual checkmarks and X marks for feature availability
  - Highlighted differences between tiers
- [x] **Upgrade Prompts**: Show when users hit limits ✅
  - UpgradeDialog - Full-screen modal with plan selection
  - UpgradeSnackbar - Modern Material 3 card-based design
  - UpgradeBanner - Persistent top banner
  - UpgradeChip - Compact inline prompt
- [x] **Billing Management Screen**: View current plan, usage, and change subscription ✅
  - BillingScreen shows current plan and subscription status
  - Usage metrics with progress bars
  - Navigation to PaywallScreen for upgrades
  - "Personal Account" support for users without company
  - Subscription status display (active/canceled/expired/grace_period)
- [x] **Success/Failure Dialogs**: Handle purchase outcomes ✅
  - Success/error states in BillingScreen
  - Toast/Snackbar feedback for operations

#### 4.2 Feature Restrictions ✅

- [x] **Evidence Upload Limits**: Block after monthly quota (free plan) ✅
  - 5 photos/month on FREE plan enforced
  - Photo limit check in `completeSubtaskWithProof()`
  - Fetches company from task/group hierarchy
  - Shows upgrade prompt when limit reached
  - Tested end-to-end (Test 1.5)
- [x] **Group Creation Limits**: Restrict to 1 group on free plan ✅
  - Enforced in GroupViewModel.createGroup()
  - Uses "effective company" pattern for users without company
  - Shows UpgradeSnackbar with modern Material 3 design
  - Tested successfully (Test 1.1)
- [x] **User Limits**: Max users per group based on plan ✅
  - 5 members on FREE, 15 on PRO, 50 on BUSINESS, unlimited on ENTERPRISE
  - Enforced in GroupViewModel.addMemberToGroup()
  - canAddMember() check before adding users
  - Tested (Test 1.3)
- [x] **Budget/Bidding Access**: Require pro plan or higher ✅
  - FeatureFlags.canUseBudgets() check implemented
  - UI shows upgrade prompts for FREE users
  - PRO+ plans have full budgeting/bidding access
- [x] **Approval Hierarchy**: Business plan feature ✅
  - FeatureFlags.canUseApprovalHierarchy() implemented
  - Feature gated to BUSINESS and ENTERPRISE tiers
- [x] **Advanced Chat**: Multimedia messages (pro+) ✅
  - FeatureFlags.canSendMultimedia() check
  - Image attachments working for PRO+ plans
- [x] **Storage Limits**: Enforce storage quota per plan ✅
  - 100MB on FREE, 5GB on PRO, 50GB on BUSINESS, unlimited on ENTERPRISE
  - FeatureFlags.canUploadFile() validates file size against quota
  - Storage usage display in BillingScreen (Test 1.4)
  - getStorageUsagePercentage() shows usage with progress bar

#### 4.3 Usage Tracking ✅

- [x] **Track Photo Uploads**: Count monthly uploads per company ✅
  - `photosUploadedThisMonth` field in Company model
  - Incremented on each successful photo upload
  - Checked before allowing new uploads
  - Reset logic planned for Cloud Function
- [x] **Track Storage Usage**: Calculate total storage used ✅
  - `storageUsedBytes` field in Company model
  - Updated on file uploads to Firebase Storage
  - Displayed with progress bar in BillingScreen
  - getStorageUsagePercentage() utility function
- [x] **Track Active Tasks**: Count concurrent active tasks ✅
  - `activeTasksCount` field in Company model
  - Incremented on task creation
  - Checked against plan limits (10 on FREE, 50 on PRO, 200 on BUSINESS)
  - Enforced in TaskViewModel
- [x] **Track Group Count**: Monitor number of groups per company ✅
  - `groupsCount` field in Company model
  - Incremented on group creation
  - Checked against plan limits (1 on FREE, unlimited on PRO+)
  - Tested in Test 1.1
- [ ] **Reset Monthly Counters**: Cloud Function to reset usage each billing cycle
  - Planned for `photosUploadedThisMonth` field
  - Should run on 1st of each month
  - Will reset counters per company based on billing cycle

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

- [x] **sync with github repository** <https://github.com/alainmtz/Group_Task_Manager.git> ✅
  - Initial commit: 136 files, 22,295 lines
  - Optimized .gitignore: ~210MB of build files excluded
  - Pushed to main branch successfully
