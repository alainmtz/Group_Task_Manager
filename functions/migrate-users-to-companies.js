/**
 * Migration Script: Convert existing users to company-centric model
 * Creates personal company for each user and links existing data
 * 
 * ⚠️ RUN ONCE ONLY - This is a one-time migration
 * Usage: node migrate-users-to-companies.js
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin with project ID
admin.initializeApp({
  projectId: 'agenda-solar'
});
const db = admin.firestore();

async function migrateUsersToCompanies() {
  console.log('🚀 Starting migration: Users → Companies\n');
  console.log('⚠️  This will create personal companies for all existing users\n');

  try {
    // Step 1: Get all users
    console.log('📥 Step 1: Fetching all users...');
    const usersSnapshot = await db.collection('users').get();
    console.log(`   Found ${usersSnapshot.size} users\n`);

    if (usersSnapshot.empty) {
      console.log('✅ No users found. Migration not needed.');
      process.exit(0);
    }

    let migratedCount = 0;
    let skippedCount = 0;

    // Step 2: Process each user
    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const userData = userDoc.data();

      // Skip if user already has a company
      if (userData.companyId) {
        console.log(`⏭️  Skipping user ${userData.email} - already has company`);
        skippedCount++;
        continue;
      }

      console.log(`\n👤 Processing user: ${userData.email || userId}`);

      // Step 2a: Create personal company
      const companyId = `company_${userId}`;
      const companyName = userData.name 
        ? `${userData.name}'s Workspace` 
        : `${userData.email.split('@')[0]}'s Workspace`;

      const companyData = {
        name: companyName,
        ownerId: userId,
        adminIds: [],
        memberIds: [userId],
        planId: 'free',
        planTier: 'free',
        subscriptionStatus: 'active',
        googlePlayPurchaseToken: null,
        subscriptionStartDate: admin.firestore.FieldValue.serverTimestamp(),
        nextBillingDate: null,
        activeTasksCount: 0,
        groupsCount: 0,
        storageUsedBytes: 0,
        photosUploadedThisMonth: 0,
        lastPhotoResetDate: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      };

      await db.collection('companies').doc(companyId).set(companyData);
      console.log(`   ✅ Created company: ${companyName}`);

      // Step 2b: Update user with companyId
      await db.collection('users').doc(userId).update({
        companyId: companyId,
        role: 'owner',
        storageUsedBytes: 0,
        uploadCountThisMonth: 0
      });
      console.log(`   ✅ Updated user with companyId`);

      // Step 2c: Link user's groups to company
      const groupsSnapshot = await db.collection('groups')
        .where('creatorId', '==', userId)
        .get();

      if (!groupsSnapshot.empty) {
        const batch = db.batch();
        groupsSnapshot.docs.forEach(groupDoc => {
          batch.update(groupDoc.ref, { companyId: companyId });
        });
        await batch.commit();
        console.log(`   ✅ Linked ${groupsSnapshot.size} groups to company`);

        // Update company group count
        await db.collection('companies').doc(companyId).update({
          groupsCount: groupsSnapshot.size
        });
      }

      // Step 2d: Link user's tasks to company
      const tasksSnapshot = await db.collection('tasks')
        .where('creatorId', '==', userId)
        .get();

      if (!tasksSnapshot.empty) {
        const batch = db.batch();
        let activeTasksCount = 0;

        tasksSnapshot.docs.forEach(taskDoc => {
          const taskData = taskDoc.data();
          batch.update(taskDoc.ref, { 
            companyId: companyId,
            requiresApproval: false,
            approvalHierarchy: [],
            approvalStatus: 'none'
          });
          
          if (taskData.status === 'pending' || taskData.status === 'in_review') {
            activeTasksCount++;
          }
        });
        
        await batch.commit();
        console.log(`   ✅ Linked ${tasksSnapshot.size} tasks to company (${activeTasksCount} active)`);

        // Update company active tasks count
        await db.collection('companies').doc(companyId).update({
          activeTasksCount: activeTasksCount
        });
      }

      // Step 2e: Link user's earnings to company
      const earningsSnapshot = await db.collection('earnings')
        .where('userId', '==', userId)
        .get();

      if (!earningsSnapshot.empty) {
        const batch = db.batch();
        earningsSnapshot.docs.forEach(earningDoc => {
          batch.update(earningDoc.ref, { companyId: companyId });
        });
        await batch.commit();
        console.log(`   ✅ Linked ${earningsSnapshot.size} earnings to company`);
      }

      migratedCount++;
      console.log(`   ✨ Migration complete for ${userData.email || userId}`);
    }

    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('✅ MIGRATION COMPLETED SUCCESSFULLY');
    console.log('='.repeat(60));
    console.log(`📊 Summary:`);
    console.log(`   - Users migrated: ${migratedCount}`);
    console.log(`   - Users skipped: ${skippedCount}`);
    console.log(`   - Total users: ${usersSnapshot.size}`);
    console.log('\n✨ All users now have personal companies on FREE plan');
    console.log('🔗 All existing groups, tasks, and earnings are linked to companies\n');

  } catch (error) {
    console.error('\n❌ Migration failed:', error);
    process.exit(1);
  }

  process.exit(0);
}

// Run migration
migrateUsersToCompanies();
