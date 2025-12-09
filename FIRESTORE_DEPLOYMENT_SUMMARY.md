# Firestore Deployment Summary

**Date**: December 9, 2025
**Status**: ✅ Ready to Deploy

## 📦 Files Created

### 1. Configuration Scripts

✅ **functions/init-plans.js**
- Purpose: Initialize 4 subscription plans in Firestore
- Creates: `/plans` collection with FREE, PRO, BUSINESS, ENTERPRISE
- Run: `node functions/init-plans.js`

✅ **functions/migrate-users-to-companies.js**
- Purpose: Migrate existing users to company-centric model
- Creates: Personal company for each user
- Links: All groups, tasks, earnings to companies
- Run: `node functions/migrate-users-to-companies.js`

✅ **firestore-rules-subscriptions.conf**
- Purpose: Updated security rules with companies/plans collections
- Includes: Read/write permissions for new data structure
- Deploy: `firebase deploy --only firestore:rules`

✅ **firestore.indexes.json** (Updated)
- Added 5 new composite indexes:
  - companies: ownerId + createdAt
  - companies: memberIds + planTier
  - tasks: companyId + status + createdAt
  - groups: companyId + createdAt
  - earnings: companyId + userId + timestamp
- Deploy: `firebase deploy --only firestore:indexes`

### 2. Documentation

✅ **FIRESTORE_SETUP_GUIDE.md**
- Complete step-by-step deployment guide
- Includes verification steps and troubleshooting

✅ **STAGE2_IMPLEMENTATION.md**
- Technical implementation details
- Data model documentation

---

## 🚀 Deployment Checklist

### Phase 1: Initialize Plans (Required)

```bash
cd functions
node init-plans.js
```

**Result**: `/plans` collection created with 4 documents

### Phase 2: Update Security Rules (Required)

```bash
cp firestore-rules-subscriptions.conf firestore.rules
firebase deploy --only firestore:rules
```

**Result**: Companies and plans collections secured

### Phase 3: Deploy Indexes (Required)

```bash
firebase deploy --only firestore:indexes
```

**Result**: 16 composite indexes deployed (11 existing + 5 new)

### Phase 4: Migrate Existing Data (Optional - Only if you have users)

⚠️ **Backup first!**

```bash
# Backup
firebase firestore:export gs://[YOUR-BUCKET]/backups/$(date +%Y%m%d)

# Migrate
cd functions
node migrate-users-to-companies.js
```

**Result**: All users have companies, data linked

---

## 📊 New Firestore Structure

### Collections Added:

1. **`/plans`** (4 documents)
   - free, pro, business, enterprise
   - Read-only for users
   - Managed by admin

2. **`/companies`** (1 per user/organization)
   - Company profiles
   - Billing information
   - Usage tracking
   - Member management

### Collections Updated:

3. **`/users`** 
   - Added: companyId, role, storageUsedBytes, uploadCountThisMonth

4. **`/groups`**
   - Added: companyId

5. **`/tasks`**
   - Added: companyId, requiresApproval, approvalHierarchy, approvalStatus

6. **`/earnings`**
   - Added: companyId

---

## ✅ Verification Steps

After deployment, verify:

### 1. Check Plans Collection
```bash
firebase firestore:read /plans
```
Expected: 4 documents (free, pro, business, enterprise)

### 2. Check Security Rules
- Try reading `/plans` as authenticated user → ✅ Should work
- Try writing to `/plans` as user → ❌ Should fail
- Try reading your company → ✅ Should work

### 3. Check Indexes
```bash
firebase firestore:indexes
```
Expected: 16 indexes (status: READY)

### 4. Check Migration (if run)
- Navigate to `/companies` in Firebase Console
- Each user should have `company_{userId}`
- User documents should have `companyId` field

---

## 🔄 Rollback Instructions

If you need to rollback:

### Rollback Rules:
```bash
# Restore old rules
git checkout HEAD -- firestore.rules
firebase deploy --only firestore:rules
```

### Rollback Indexes:
```bash
# Restore old indexes
git checkout HEAD -- firestore.indexes.json
firebase deploy --only firestore:indexes
```

### Rollback Migration:
```bash
# Restore from backup
firebase firestore:import gs://[YOUR-BUCKET]/backups/[BACKUP-DATE]
```

---

## 📈 Expected Performance

### Before (11 indexes):
- Task queries: Optimized
- Chat queries: Optimized
- Notification queries: Optimized

### After (16 indexes):
- ✅ All previous queries still optimized
- ✅ Company queries optimized
- ✅ Company-filtered task/group queries optimized
- ✅ Earnings by company optimized

### Storage Impact:
- Plans collection: ~20KB (4 documents)
- Companies collection: ~2KB per company
- Index overhead: ~15% increase

---

## 🎯 Next Steps After Deployment

1. ✅ Verify all scripts ran successfully
2. ✅ Test security rules with authenticated user
3. ✅ Check Firebase Console for new collections
4. ⏳ Build CompanyService.kt (load company for user)
5. ⏳ Build SubscriptionService.kt (billing management)
6. ⏳ Integrate FeatureFlags in ViewModels
7. ⏳ Create PaywallScreen UI
8. ⏳ Google Play Billing integration

---

## 🆘 Support

**Issues with deployment?**
- See `FIRESTORE_SETUP_GUIDE.md` for detailed troubleshooting
- Check Firebase Console → Firestore for error messages
- Verify Firebase CLI version: `firebase --version` (needs 12.0+)

**Need help with migration?**
- Test in development environment first
- Always backup before running migrations
- Check logs for detailed error messages

---

## 📝 Summary

**Created**:
- 2 deployment scripts (init-plans, migrate)
- 1 security rules file
- 5 new Firestore indexes
- 2 documentation files

**Status**: ✅ Ready for deployment
**Backward Compatible**: ✅ Yes (all new fields nullable)
**Data Loss Risk**: ❌ None (if backups taken)

**Estimated Deployment Time**: 10-15 minutes
**Estimated Migration Time**: 5-10 minutes (for 100 users)

---

**Last Updated**: December 9, 2025
