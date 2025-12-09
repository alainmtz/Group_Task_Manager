# Firestore Setup Guide - Subscription System

**Date**: December 9, 2025
**Purpose**: Configure Firestore for Phase 3 subscription system

## 📋 Overview

This guide provides step-by-step instructions to:
1. Initialize subscription plans in Firestore
2. Update Firestore security rules
3. Migrate existing users to company model (optional)
4. Update Firestore indexes

---

## ✅ Prerequisites

- Firebase CLI installed: `npm install -g firebase-tools`
- Firebase project initialized in this directory
- Node.js installed (for running scripts)
- Firebase Admin credentials configured

---

## 🚀 Step 1: Initialize Subscription Plans

This creates the 4 default plans (FREE, PRO, BUSINESS, ENTERPRISE) in Firestore.

### Execute:

```bash
cd functions
node init-plans.js
```

### Expected Output:

```
🚀 Initializing subscription plans in Firestore...

✅ Added plan: Free Plan (free)
✅ Added plan: Pro Plan (pro)
✅ Added plan: Business Plan (business)
✅ Added plan: Enterprise Plan (enterprise)

✨ All plans initialized successfully!

📊 Plans created:
   - FREE: 1 group, 5 members, 10 tasks, 100MB
   - PRO: Unlimited groups, 15 members, 50 tasks, 5GB
   - BUSINESS: Unlimited groups, 50 members, 200 tasks, 50GB
   - ENTERPRISE: Unlimited everything, all features
```

### Verify in Firebase Console:

1. Open Firebase Console → Firestore Database
2. Check `/plans` collection has 4 documents:
   - `free`
   - `pro`
   - `business`
   - `enterprise`

---

## 🔒 Step 2: Update Firestore Security Rules

This adds security rules for the new `/companies` and `/plans` collections.

### Option A: Deploy via Firebase CLI (Recommended)

```bash
# Copy new rules to firestore.rules
cp firestore-rules-subscriptions.conf firestore.rules

# Deploy to Firebase
firebase deploy --only firestore:rules
```

### Option B: Manual Update in Firebase Console

1. Open Firebase Console → Firestore Database → Rules
2. Copy content from `firestore-rules-subscriptions.conf`
3. Paste into rules editor
4. Click "Publish"

### Key Rules Added:

**Companies Collection**:
- Read: Members can read their company
- Create: Users can create their own company
- Update: Owner can update everything, admins can update non-billing fields
- Delete: Only owner can delete

**Plans Collection**:
- Read: All authenticated users
- Write: Blocked (managed by admin only)

---

## �� Step 3: Migrate Existing Users (OPTIONAL)

⚠️ **Only run this if you have existing users in production**

This script:
- Creates personal company for each existing user
- Links all groups/tasks/earnings to companies
- Sets all users to FREE plan
- Zero data loss migration

### Before Running:

**IMPORTANT**: Backup your Firestore data first!

```bash
# Export Firestore data (backup)
firebase firestore:export gs://[YOUR-BUCKET]/backups/$(date +%Y%m%d)
```

### Execute Migration:

```bash
cd functions
node migrate-users-to-companies.js
```

### Expected Output:

```
🚀 Starting migration: Users → Companies

⚠️  This will create personal companies for all existing users

📥 Step 1: Fetching all users...
   Found 15 users

👤 Processing user: user@example.com
   ✅ Created company: John's Workspace
   ✅ Updated user with companyId
   ✅ Linked 3 groups to company
   ✅ Linked 12 tasks to company (8 active)
   ✅ Linked 24 earnings to company
   ✨ Migration complete for user@example.com

[... more users ...]

============================================================
✅ MIGRATION COMPLETED SUCCESSFULLY
============================================================
📊 Summary:
   - Users migrated: 15
   - Users skipped: 0
   - Total users: 15

✨ All users now have personal companies on FREE plan
🔗 All existing groups, tasks, and earnings are linked to companies
```

### Verify Migration:

1. Check `/companies` collection in Firestore
2. Each user should have a company: `company_{userId}`
3. Check a user document - should have `companyId` field
4. Check groups/tasks - should have `companyId` field

---

## 📊 Step 4: Deploy Firestore Indexes

Deploy the composite indexes for optimal query performance.

```bash
firebase deploy --only firestore:indexes
```

### Expected Output:

```
=== Deploying to 'your-project'...

i  firestore: deploying indexes
✔  firestore: deployed indexes successfully

✔  Deploy complete!
```

---

## �� Step 5: Verify Setup

### Check Plans:

```bash
# In Firebase Console Firestore
# Navigate to /plans collection
# Should see: free, pro, business, enterprise
```

### Check Companies (if migrated):

```bash
# Navigate to /companies collection
# Should see: company_{userId} for each user
```

### Test Security Rules:

1. Try reading plans as authenticated user → ✅ Should work
2. Try writing to plans as authenticated user → ❌ Should fail
3. Try reading your company → ✅ Should work
4. Try reading someone else's company → ❌ Should fail

---

## �� New Collections Structure

### `/plans` Collection

```javascript
{
  "free": {
    id: "free",
    name: "Free Plan",
    tier: "free",
    priceMonthly: 0.0,
    priceYearly: 0.0,
    features: {
      maxGroups: 1,
      maxMembersPerGroup: 5,
      maxActiveTasks: 10,
      maxStorageGB: 0,
      maxStorageMB: 100,
      maxPhotosPerMonth: 5,
      canUseBudgets: false,
      canUseBidding: false,
      // ... 10 more feature flags
    }
  },
  // ... pro, business, enterprise
}
```

### `/companies` Collection

```javascript
{
  "company_{userId}": {
    name: "John's Workspace",
    ownerId: "{userId}",
    adminIds: [],
    memberIds: ["{userId}"],
    planId: "free",
    planTier: "free",
    subscriptionStatus: "active",
    googlePlayPurchaseToken: null,
    subscriptionStartDate: Timestamp,
    nextBillingDate: null,
    activeTasksCount: 8,
    groupsCount: 3,
    storageUsedBytes: 15728640, // 15MB
    photosUploadedThisMonth: 2,
    lastPhotoResetDate: Timestamp,
    createdAt: Timestamp
  }
}
```

---

## 🔧 Troubleshooting

### Error: "Firebase app not initialized"

**Solution**: Make sure you're in the `functions/` directory and have proper credentials:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="path/to/serviceAccountKey.json"
```

### Error: "Permission denied"

**Solution**: Update Firestore rules first (Step 2) before running migrations.

### Migration runs but data not linked

**Solution**: Check that user authentication is working and userId matches document IDs.

### Plans collection empty after init-plans.js

**Solution**: Check Firebase Admin credentials and project configuration.

---

## �� Next Steps

After completing this setup:

1. ✅ Plans are ready in Firestore
2. ✅ Security rules updated
3. ✅ Existing users migrated (if applicable)
4. ✅ Indexes deployed

**Ready for**: 
- STAGE 3: Google Play Billing integration
- Testing feature flags in app
- Building paywall UI

---

## 📚 Related Documentation

- `STAGE2_IMPLEMENTATION.md`: Data model details
- `FIRESTORE_STRUCTURE_AUDIT.md`: Complete Firestore analysis
- `TODO.md`: Project roadmap

---

## ⚠️ Important Notes

1. **Backup First**: Always backup Firestore before migrations
2. **Test in Development**: Test all scripts in dev environment first
3. **One-Time Migration**: Only run migration scripts once
4. **Security Rules**: Update rules before migrations
5. **Indexes**: Deploy indexes to avoid slow queries

---

## 🆘 Support Commands

**List all plans:**
```bash
firebase firestore:read /plans
```

**List all companies:**
```bash
firebase firestore:read /companies
```

**Check specific user:**
```bash
firebase firestore:read /users/{userId}
```

**Rollback rules (if needed):**
```bash
# Restore from backup
cp firestore.rules.backup firestore.rules
firebase deploy --only firestore:rules
```

---

**Last Updated**: December 9, 2025
**Author**: Subscription System Implementation Team
