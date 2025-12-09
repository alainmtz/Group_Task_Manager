#!/bin/bash

# ============================================
# CREATE ENTERPRISE COMPANY FOR OWNER
# ============================================
# Purpose: Create your personal Enterprise company
# Usage: ./create-my-company.sh YOUR_EMAIL YOUR_UID
# ============================================

set -e

PROJECT_ID="agenda-solar"

# Check arguments
if [ $# -eq 0 ]; then
    echo "Usage: ./create-my-company.sh YOUR_EMAIL [YOUR_UID]"
    echo ""
    echo "Example: ./create-my-company.sh alain@gmail.com abc123xyz"
    echo ""
    echo "To get your UID:"
    echo "  1. Sign up in the app first"
    echo "  2. Go to: https://console.firebase.google.com/project/agenda-solar/authentication/users"
    echo "  3. Find your email and copy the UID"
    exit 1
fi

OWNER_EMAIL=$1
OWNER_UID=${2:-""}

# Prefer a local service account JSON in `./.secrets/` if present. This file must NOT be committed.
if [ -z "$SERVICE_ACCOUNT_KEY_PATH" ]; then
  if [ -f "./.secrets/service-account.json" ]; then
    export SERVICE_ACCOUNT_KEY_PATH="$(pwd)/.secrets/service-account.json"
    echo "Using local service account at $SERVICE_ACCOUNT_KEY_PATH"
  elif [ -f "./.secrets/agenda-solar-989128d1b184.json" ]; then
    export SERVICE_ACCOUNT_KEY_PATH="$(pwd)/.secrets/agenda-solar-989128d1b184.json"
    echo "Using local service account at $SERVICE_ACCOUNT_KEY_PATH"
  fi
fi

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🏢 CREATE ENTERPRISE COMPANY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${NC}"

# Get UID if not provided
if [ -z "$OWNER_UID" ]; then
    echo -e "${YELLOW}⚠️  UID not provided${NC}"
    echo ""
    echo "To get your Firebase Auth UID:"
    echo "  1. Sign up in the app with: ${OWNER_EMAIL}"
    echo "  2. Open: https://console.firebase.google.com/project/${PROJECT_ID}/authentication/users"
    echo "  3. Find your user and copy the UID"
    echo ""
    read -p "Enter your Firebase Auth UID: " OWNER_UID
fi

if [ -z "$OWNER_UID" ]; then
    echo -e "${RED}❌ UID is required${NC}"
    exit 1
fi

echo -e "${BLUE}📋 Configuration:${NC}"
echo "   Email: ${OWNER_EMAIL}"
echo "   UID: ${OWNER_UID}"
echo "   Plan: Enterprise (unlimited)"
echo ""

# Create the script
cat > /tmp/create-enterprise-company.js << 'EOFJS'
const admin = require('firebase-admin');

admin.initializeApp({
  projectId: process.env.PROJECT_ID
});

const db = admin.firestore();

async function createCompany() {
  const ownerId = process.env.OWNER_UID;
  const ownerEmail = process.env.OWNER_EMAIL;
  const companyId = `company_${ownerId}`;
  
  console.log(`\n🏢 Creating Enterprise company...`);
  console.log(`   Owner: ${ownerEmail}`);
  console.log(`   UID: ${ownerId}`);
  console.log(`   Company ID: ${companyId}\n`);
  
  const companyData = {
    id: companyId,
    name: "Mi Empresa (Enterprise)",
    ownerId: ownerId,
    adminIds: [ownerId],
    memberIds: [ownerId],
    planId: "enterprise",
    planTier: "enterprise",
    subscriptionStatus: "ACTIVE",
    subscriptionStartDate: admin.firestore.Timestamp.now(),
    subscriptionEndDate: null,
    autoRenew: true,
    paymentMethod: null,
    activeTasksCount: 0,
    groupsCount: 0,
    storageUsedBytes: 0,
    photosUploadedThisMonth: 0,
    lastPhotoResetDate: admin.firestore.Timestamp.now(),
    createdAt: admin.firestore.Timestamp.now(),
    updatedAt: admin.firestore.Timestamp.now()
  };
  
  try {
    // Check if user exists
    const userDoc = await db.collection('users').doc(ownerId).get();
    if (!userDoc.exists) {
      console.log('⚠️  User document not found, creating it...');
      await db.collection('users').doc(ownerId).set({
        id: ownerId,
        email: ownerEmail,
        companyId: companyId,
        role: 'OWNER',
        storageUsedBytes: 0,
        uploadCountThisMonth: 0,
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now()
      });
      console.log('✅ User document created');
    } else {
      // Update existing user
      await db.collection('users').doc(ownerId).update({
        companyId: companyId,
        role: 'OWNER',
        updatedAt: admin.firestore.Timestamp.now()
      });
      console.log('✅ User document updated');
    }
    
    // Create company
    await db.collection('companies').doc(companyId).set(companyData);
    console.log('✅ Enterprise company created\n');
    
    console.log('🎉 Setup complete!\n');
    console.log('You now have:');
    console.log('  ✓ Enterprise plan (unlimited everything)');
    console.log('  ✓ Owner role with full permissions');
    console.log('  ✓ SSO, API, white-label capabilities');
    console.log('  ✓ Dedicated support and SLA 99.9%');
    console.log('');
    console.log('🔗 View in Firestore:');
    console.log(`   https://console.firebase.google.com/project/${process.env.PROJECT_ID}/firestore/data/~2Fcompanies~2F${companyId}`);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  }
  
  process.exit(0);
}

createCompany();
EOFJS

# Check for firebase-admin
if [ ! -d "node_modules/firebase-admin" ]; then
    echo -e "${YELLOW}📦 Installing firebase-admin...${NC}"
    npm install firebase-admin --no-save
fi

# Run script
export PROJECT_ID="${PROJECT_ID}"
export OWNER_UID="${OWNER_UID}"
export OWNER_EMAIL="${OWNER_EMAIL}"

node /tmp/create-enterprise-company.js

# Cleanup
rm /tmp/create-enterprise-company.js

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Done! Sign in to the app with ${OWNER_EMAIL}${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
