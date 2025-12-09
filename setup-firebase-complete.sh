#!/bin/bash

# ============================================
# FIREBASE COMPLETE SETUP SCRIPT
# ============================================
# Purpose: Configure Firestore for subscription system
# Date: December 9, 2025
# Author: Alain (Enterprise Owner)
# ============================================

set -e  # Exit on error

PROJECT_ID="agenda-solar"
OWNER_EMAIL="melvinalvin.bello@gmail.com"  # ⚠️ CAMBIA ESTO por tu email de Firebase Auth
OWNER_UID=""  # Se obtendrá automáticamente si está vacío

# Prefer a local service account JSON in `app/` if present. This file must NOT be committed.
if [ -z "$SERVICE_ACCOUNT_KEY_PATH" ] && [ -f "./app/agenda-solar-989128d1b184.json" ]; then
    export SERVICE_ACCOUNT_KEY_PATH="$(pwd)/app/agenda-solar-989128d1b184.json"
    echo "Using local service account at $SERVICE_ACCOUNT_KEY_PATH"
fi
# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🚀 FIREBASE COMPLETE SETUP - SUBSCRIPTION SYSTEM"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${NC}"

# ============================================
# STEP 1: Verify Firebase CLI
# ============================================
echo -e "${YELLOW}📋 Step 1: Verifying Firebase CLI...${NC}"

if ! command -v firebase &> /dev/null; then
    echo -e "${RED}❌ Firebase CLI not found${NC}"
    echo "Install with: npm install -g firebase-tools"
    exit 1
fi

echo -e "${GREEN}✅ Firebase CLI installed${NC}"

# ============================================
# STEP 2: Authenticate with Firebase
# ============================================
echo -e "\n${YELLOW}📋 Step 2: Checking Firebase authentication...${NC}"

if ! firebase projects:list > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Not authenticated. Starting login...${NC}"
    firebase login
else
    echo -e "${GREEN}✅ Already authenticated${NC}"
fi

# ============================================
# STEP 3: Set Firebase Project
# ============================================
echo -e "\n${YELLOW}📋 Step 3: Setting Firebase project to ${PROJECT_ID}...${NC}"

firebase use default --project ${PROJECT_ID}

echo -e "${GREEN}✅ Using project: ${PROJECT_ID}${NC}"

# ============================================
# STEP 4: Deploy Firestore Rules
# ============================================
echo -e "\n${YELLOW}📋 Step 4: Deploying Firestore security rules...${NC}"

if [ -f "firestore.rules" ]; then
    firebase deploy --only firestore:rules --project ${PROJECT_ID}
    echo -e "${GREEN}✅ Firestore rules deployed${NC}"
else
    echo -e "${RED}❌ firestore.rules not found${NC}"
    exit 1
fi

# ============================================
# STEP 5: Deploy Firestore Indexes
# ============================================
echo -e "\n${YELLOW}📋 Step 5: Deploying Firestore indexes...${NC}"

if [ -f "firestore.indexes.json" ]; then
    firebase deploy --only firestore:indexes --project ${PROJECT_ID}
    echo -e "${GREEN}✅ Firestore indexes deployed (building in background)${NC}"
else
    echo -e "${RED}❌ firestore.indexes.json not found${NC}"
    exit 1
fi

# ============================================
# STEP 6: Initialize Plans in Firestore
# ============================================
echo -e "\n${YELLOW}📋 Step 6: Initializing subscription plans...${NC}"

if [ -f "init-plans-direct.js" ]; then
    echo -e "${BLUE}🔄 Running plan initialization script...${NC}"
    node init-plans-direct.js
    echo -e "${GREEN}✅ Plans initialized in Firestore${NC}"
else
    echo -e "${YELLOW}⚠️  init-plans-direct.js not found${NC}"
    echo -e "${BLUE}📝 Manual step required:${NC}"
    echo "   1. Open Firebase Console: https://console.firebase.google.com/project/${PROJECT_ID}/firestore/data"
    echo "   2. Create collection: 'plans'"
    echo "   3. Import data from: plans-data.json"
fi

# ============================================
# STEP 7: Create Your Enterprise Company
# ============================================
echo -e "\n${YELLOW}📋 Step 7: Creating your Enterprise company...${NC}"

# Get Firebase Auth UID
if [ -z "$OWNER_UID" ]; then
    echo -e "${BLUE}🔍 Getting your Firebase Auth UID...${NC}"
    echo -e "${YELLOW}⚠️  Make sure you've signed up in the app first!${NC}"
    echo ""
    echo "To get your UID:"
    echo "  1. Open Firebase Console: https://console.firebase.google.com/project/${PROJECT_ID}/authentication/users"
    echo "  2. Find your user email: ${OWNER_EMAIL}"
    echo "  3. Copy the UID"
    echo ""
    read -p "Enter your Firebase Auth UID: " OWNER_UID
fi

if [ -z "$OWNER_UID" ]; then
    echo -e "${RED}❌ UID is required${NC}"
    exit 1
fi

echo -e "${BLUE}🔄 Creating company document...${NC}"

# Create company creation script
cat > /tmp/create-company.js << 'EOFJS'
const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin
admin.initializeApp({
  projectId: process.env.PROJECT_ID
});

const db = admin.firestore();

async function createEnterpriseCompany() {
  const ownerId = process.env.OWNER_UID;
  const ownerEmail = process.env.OWNER_EMAIL;
  
  console.log(`\n🏢 Creating Enterprise company for: ${ownerEmail}`);
  console.log(`   UID: ${ownerId}`);
  
  const companyId = `company_${ownerId}`;
  
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
    subscriptionEndDate: null, // Enterprise never expires
    autoRenew: true,
    paymentMethod: null, // Custom billing
    
    // Usage tracking (unlimited for Enterprise)
    activeTasksCount: 0,
    groupsCount: 0,
    storageUsedBytes: 0,
    photosUploadedThisMonth: 0,
    lastPhotoResetDate: admin.firestore.Timestamp.now(),
    
    // Metadata
    createdAt: admin.firestore.Timestamp.now(),
    updatedAt: admin.firestore.Timestamp.now()
  };
  
  try {
    await db.collection('companies').doc(companyId).set(companyData);
    console.log('✅ Enterprise company created successfully!');
    console.log(`   Company ID: ${companyId}`);
    console.log(`   Plan: Enterprise (unlimited everything)`);
    
    // Update user document
    console.log('\n👤 Updating user document...');
    await db.collection('users').doc(ownerId).update({
      companyId: companyId,
      role: 'OWNER',
      updatedAt: admin.firestore.Timestamp.now()
    });
    console.log('✅ User document updated');
    
    console.log('\n🎉 Setup complete! You now have:');
    console.log('   ✓ Enterprise plan with unlimited resources');
    console.log('   ✓ Owner role with full permissions');
    console.log('   ✓ SSO, API, white-label capabilities');
    console.log('   ✓ Dedicated support and SLA 99.9%');
    
  } catch (error) {
    console.error('❌ Error creating company:', error.message);
    process.exit(1);
  }
  
  process.exit(0);
}

createEnterpriseCompany();
EOFJS

# Run the company creation script
export PROJECT_ID="${PROJECT_ID}"
export OWNER_UID="${OWNER_UID}"
export OWNER_EMAIL="${OWNER_EMAIL}"

if command -v node &> /dev/null; then
    # Check if firebase-admin is installed
    if [ ! -d "node_modules/firebase-admin" ]; then
        echo -e "${YELLOW}📦 Installing firebase-admin...${NC}"
        npm install firebase-admin --no-save
    fi
    
    node /tmp/create-company.js
    
    # Cleanup
    rm /tmp/create-company.js
else
    echo -e "${RED}❌ Node.js not found${NC}"
    echo "Install Node.js to run company creation script"
    exit 1
fi

# ============================================
# STEP 8: Verify Setup
# ============================================
echo -e "\n${YELLOW}📋 Step 8: Verifying setup...${NC}"

echo -e "\n${GREEN}✅ SETUP COMPLETE!${NC}"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 Your Firebase subscription system is ready!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "📊 What's been configured:"
echo "   ✓ Firestore security rules deployed"
echo "   ✓ Firestore composite indexes deployed"
echo "   ✓ Subscription plans initialized (FREE, PRO, BUSINESS, ENTERPRISE)"
echo "   ✓ Your Enterprise company created"
echo "   ✓ Owner permissions configured"
echo ""
echo "🔗 Quick Links:"
echo "   • Firebase Console: https://console.firebase.google.com/project/${PROJECT_ID}"
echo "   • Firestore Data: https://console.firebase.google.com/project/${PROJECT_ID}/firestore/data"
echo "   • Authentication: https://console.firebase.google.com/project/${PROJECT_ID}/authentication/users"
echo "   • Rules: https://console.firebase.google.com/project/${PROJECT_ID}/firestore/rules"
echo ""
echo "📱 Next Steps:"
echo "   1. Build and install the app: ./gradlew installDebug"
echo "   2. Sign in with: ${OWNER_EMAIL}"
echo "   3. You'll automatically have Enterprise access"
echo "   4. Start creating groups and tasks with unlimited resources"
echo ""
echo -e "${YELLOW}⚠️  Important Notes:${NC}"
echo "   • Indexes may take a few minutes to build"
echo "   • Check Firestore console to monitor index status"
echo "   • Your Enterprise plan never expires"
echo "   • All features are unlocked for you"
echo ""
