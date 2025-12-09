/**
 * Script to initialize subscription plans in Firestore
 * Run once to populate /plans collection with default configurations
 * 
 * Usage: node init-plans.js
 */

const admin = require('firebase-admin');
const fs = require('fs');

// Helper to initialize admin with flexible credential sources.
function initFirebaseAdmin() {
  const projectId = process.env.GCLOUD_PROJECT || process.env.GCLOUD_PROJECT_ID || 'agenda-solar';

  // 1) Explicit service account JSON path
  if (process.env.SERVICE_ACCOUNT_KEY_PATH) {
    const keyPath = process.env.SERVICE_ACCOUNT_KEY_PATH;
    if (!fs.existsSync(keyPath)) {
      throw new Error(`Service account key file not found at ${keyPath}`);
    }
    const key = require(keyPath);
    return admin.initializeApp({
      credential: admin.credential.cert(key),
      projectId
    });
  }

  // 2) Base64 encoded JSON in env var
  if (process.env.SERVICE_ACCOUNT_KEY_JSON) {
    const raw = Buffer.from(process.env.SERVICE_ACCOUNT_KEY_JSON, 'base64').toString();
    const key = JSON.parse(raw);
    return admin.initializeApp({
      credential: admin.credential.cert(key),
      projectId
    });
  }

  // 3) GOOGLE_APPLICATION_CREDENTIALS (ADC)
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    // applicationDefault will respect GOOGLE_APPLICATION_CREDENTIALS
    return admin.initializeApp({
      credential: admin.credential.applicationDefault(),
      projectId
    });
  }

  // 4) Fallback to Application Default (gcloud auth application-default login)
  return admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId
  });
}

try {
  initFirebaseAdmin();
} catch (e) {
  console.error('❌ Failed to initialize Firebase Admin SDK:', e.message);
  console.error('\nPlease provide credentials by one of these options:');
  console.error('  1) Set SERVICE_ACCOUNT_KEY_PATH=/path/to/key.json');
  console.error('  2) Set SERVICE_ACCOUNT_KEY_JSON=<base64-of-json>');
  console.error('  3) Set GOOGLE_APPLICATION_CREDENTIALS to a service account key path');
  console.error('  4) Run: gcloud auth application-default login');
  process.exit(1);
}

const db = admin.firestore();

// Plan configurations matching PlanDefaults.kt
const plans = [
  {
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
  {
    id: 'pro',
    name: 'Pro Plan',
    tier: 'pro',
    priceMonthly: 6.99,
    priceYearly: 69.0,
    features: {
      maxGroups: -1, // Unlimited
      maxMembersPerGroup: 15,
      maxActiveTasks: 50,
      maxStorageGB: 5,
      maxStorageMB: 0,
      maxPhotosPerMonth: -1, // Unlimited
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
  {
    id: 'business',
    name: 'Business Plan',
    tier: 'business',
    priceMonthly: 39.0,
    priceYearly: 399.0,
    features: {
      maxGroups: -1, // Unlimited
      maxMembersPerGroup: 50,
      maxActiveTasks: 200,
      maxStorageGB: 50,
      maxStorageMB: 0,
      maxPhotosPerMonth: -1, // Unlimited
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
  {
    id: 'enterprise',
    name: 'Enterprise Plan',
    tier: 'enterprise',
    priceMonthly: 0.0, // Custom pricing
    priceYearly: 0.0,
    features: {
      maxGroups: -1, // Unlimited
      maxMembersPerGroup: -1, // Unlimited
      maxActiveTasks: -1, // Unlimited
      maxStorageGB: -1, // Unlimited
      maxStorageMB: 0,
      maxPhotosPerMonth: -1, // Unlimited
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
];

async function initializePlans() {
  console.log('🚀 Initializing subscription plans in Firestore...\n');

  try {
    const batch = db.batch();
    
    for (const plan of plans) {
      const planRef = db.collection('plans').doc(plan.id);
      batch.set(planRef, plan);
      console.log(`✅ Added plan: ${plan.name} (${plan.tier})`);
    }

    await batch.commit();
    console.log('\n✨ All plans initialized successfully!');
    console.log('\n📊 Plans created:');
    console.log('   - FREE: 1 group, 5 members, 10 tasks, 100MB');
    console.log('   - PRO: Unlimited groups, 15 members, 50 tasks, 5GB');
    console.log('   - BUSINESS: Unlimited groups, 50 members, 200 tasks, 50GB');
    console.log('   - ENTERPRISE: Unlimited everything, all features');

  } catch (error) {
    console.error('❌ Error initializing plans:', error);
    process.exit(1);
  }

  process.exit(0);
}

initializePlans();
