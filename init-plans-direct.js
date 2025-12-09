// Direct script to add plans to Firestore
// Run with: firebase firestore:import
const plans = {
  free: {
    id: 'free',
    name: 'Free Plan',
    tier: 'free',
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
      canUseApprovalHierarchy: false,
      canUseMultimediaChat: false,
      canExportData: false,
      canUseAnalytics: false,
      canUseAuditLogs: false,
      canCustomizeNotifications: false,
      supportLevel: 'standard',
      canUseSSO: false,
      canUseAPI: false,
      canWhiteLabel: false
    }
  },
  pro: {
    id: 'pro',
    name: 'Pro Plan',
    tier: 'pro',
    priceMonthly: 6.99,
    priceYearly: 69.0,
    features: {
      maxGroups: -1,
      maxMembersPerGroup: 15,
      maxActiveTasks: 50,
      maxStorageGB: 5,
      maxStorageMB: 0,
      maxPhotosPerMonth: -1,
      canUseBudgets: true,
      canUseBidding: true,
      canUseApprovalHierarchy: false,
      canUseMultimediaChat: true,
      canExportData: false,
      canUseAnalytics: true,
      canUseAuditLogs: false,
      canCustomizeNotifications: false,
      supportLevel: 'priority',
      canUseSSO: false,
      canUseAPI: false,
      canWhiteLabel: false
    }
  },
  business: {
    id: 'business',
    name: 'Business Plan',
    tier: 'business',
    priceMonthly: 39.0,
    priceYearly: 399.0,
    features: {
      maxGroups: -1,
      maxMembersPerGroup: 50,
      maxActiveTasks: 200,
      maxStorageGB: 50,
      maxStorageMB: 0,
      maxPhotosPerMonth: -1,
      canUseBudgets: true,
      canUseBidding: true,
      canUseApprovalHierarchy: true,
      canUseMultimediaChat: true,
      canExportData: true,
      canUseAnalytics: true,
      canUseAuditLogs: true,
      canCustomizeNotifications: true,
      supportLevel: 'dedicated',
      canUseSSO: false,
      canUseAPI: false,
      canWhiteLabel: false
    }
  },
  enterprise: {
    id: 'enterprise',
    name: 'Enterprise Plan',
    tier: 'enterprise',
    priceMonthly: 0.0,
    priceYearly: 0.0,
    features: {
      maxGroups: -1,
      maxMembersPerGroup: -1,
      maxActiveTasks: -1,
      maxStorageGB: -1,
      maxStorageMB: 0,
      maxPhotosPerMonth: -1,
      canUseBudgets: true,
      canUseBidding: true,
      canUseApprovalHierarchy: true,
      canUseMultimediaChat: true,
      canExportData: true,
      canUseAnalytics: true,
      canUseAuditLogs: true,
      canCustomizeNotifications: true,
      supportLevel: 'dedicated_manager',
      canUseSSO: true,
      canUseAPI: true,
      canWhiteLabel: true
    }
  }
};

// If a FIRESTORE_IMPORT env is set, try to write directly using Admin SDK.
if (process.env.WRITE_DIRECT === '1') {
  const admin = require('firebase-admin');
  const fs = require('fs');

  function initFirebaseAdmin() {
    const projectId = process.env.GCLOUD_PROJECT || process.env.GCLOUD_PROJECT_ID || 'agenda-solar';
    if (process.env.SERVICE_ACCOUNT_KEY_PATH) {
      const keyPath = process.env.SERVICE_ACCOUNT_KEY_PATH;
      if (!fs.existsSync(keyPath)) throw new Error(`Service account file not found: ${keyPath}`);
      const key = require(keyPath);
      return admin.initializeApp({ credential: admin.credential.cert(key), projectId });
    }
    if (process.env.SERVICE_ACCOUNT_KEY_JSON) {
      const raw = Buffer.from(process.env.SERVICE_ACCOUNT_KEY_JSON, 'base64').toString();
      const key = JSON.parse(raw);
      return admin.initializeApp({ credential: admin.credential.cert(key), projectId });
    }
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      return admin.initializeApp({ credential: admin.credential.applicationDefault(), projectId });
    }
    return admin.initializeApp({ credential: admin.credential.applicationDefault(), projectId });
  }

  try {
    initFirebaseAdmin();
    const db = admin.firestore();
    (async () => {
      const batch = db.batch();
      for (const id of Object.keys(plans)) {
        batch.set(db.collection('plans').doc(id), plans[id]);
        console.log(`✅ Queued plan: ${id}`);
      }
      await batch.commit();
      console.log('✨ Plans written to Firestore');
      process.exit(0);
    })();
  } catch (err) {
    console.error('❌ Failed to write plans directly:', err.message);
    process.exit(1);
  }

} else {
  console.log(JSON.stringify(plans, null, 2));
}
