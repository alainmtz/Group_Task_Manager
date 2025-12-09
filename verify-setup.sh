#!/bin/bash

# Firestore Setup Verification Script
# Checks that all required files are present before deployment

echo "🔍 Verifying Firestore Setup Files..."
echo ""

EXIT_CODE=0

# Check deployment scripts
echo "📋 Checking Deployment Scripts:"
if [ -f "functions/init-plans.js" ]; then
    echo "   ✅ init-plans.js found"
else
    echo "   ❌ init-plans.js missing"
    EXIT_CODE=1
fi

if [ -f "functions/migrate-users-to-companies.js" ]; then
    echo "   ✅ migrate-users-to-companies.js found"
else
    echo "   ❌ migrate-users-to-companies.js missing"
    EXIT_CODE=1
fi

echo ""

# Check configuration files
echo "⚙️  Checking Configuration Files:"
if [ -f "firestore-rules-subscriptions.conf" ]; then
    echo "   ✅ firestore-rules-subscriptions.conf found"
else
    echo "   ❌ firestore-rules-subscriptions.conf missing"
    EXIT_CODE=1
fi

if [ -f "firestore.indexes.json" ]; then
    echo "   ✅ firestore.indexes.json found"
    # Count indexes
    INDEX_COUNT=$(grep -c '"collectionGroup"' firestore.indexes.json)
    echo "      → Contains $INDEX_COUNT composite indexes"
else
    echo "   ❌ firestore.indexes.json missing"
    EXIT_CODE=1
fi

echo ""

# Check documentation
echo "📚 Checking Documentation:"
if [ -f "FIRESTORE_SETUP_GUIDE.md" ]; then
    echo "   ✅ FIRESTORE_SETUP_GUIDE.md found"
else
    echo "   ❌ FIRESTORE_SETUP_GUIDE.md missing"
    EXIT_CODE=1
fi

if [ -f "FIRESTORE_DEPLOYMENT_SUMMARY.md" ]; then
    echo "   ✅ FIRESTORE_DEPLOYMENT_SUMMARY.md found"
else
    echo "   ❌ FIRESTORE_DEPLOYMENT_SUMMARY.md missing"
    EXIT_CODE=1
fi

if [ -f "STAGE2_IMPLEMENTATION.md" ]; then
    echo "   ✅ STAGE2_IMPLEMENTATION.md found"
else
    echo "   ❌ STAGE2_IMPLEMENTATION.md missing"
    EXIT_CODE=1
fi

echo ""

# Check Kotlin models
echo "🔧 Checking Kotlin Models:"
MODELS_DIR="app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/models"
SERVICES_DIR="app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/services"

if [ -f "$MODELS_DIR/Plan.kt" ]; then
    echo "   ✅ Plan.kt found"
else
    echo "   ❌ Plan.kt missing"
    EXIT_CODE=1
fi

if [ -f "$MODELS_DIR/Company.kt" ]; then
    echo "   ✅ Company.kt found"
else
    echo "   ❌ Company.kt missing"
    EXIT_CODE=1
fi

if [ -f "$MODELS_DIR/CompanyRole.kt" ]; then
    echo "   ✅ CompanyRole.kt found"
else
    echo "   ❌ CompanyRole.kt missing"
    EXIT_CODE=1
fi

if [ -f "$MODELS_DIR/PlanDefaults.kt" ]; then
    echo "   ✅ PlanDefaults.kt found"
else
    echo "   ❌ PlanDefaults.kt missing"
    EXIT_CODE=1
fi

if [ -f "$SERVICES_DIR/FeatureFlags.kt" ]; then
    echo "   ✅ FeatureFlags.kt found"
else
    echo "   ❌ FeatureFlags.kt missing"
    EXIT_CODE=1
fi

echo ""

# Check Firebase CLI
echo "🔥 Checking Firebase CLI:"
if command -v firebase &> /dev/null; then
    FIREBASE_VERSION=$(firebase --version)
    echo "   ✅ Firebase CLI installed: $FIREBASE_VERSION"
else
    echo "   ⚠️  Firebase CLI not found (install: npm install -g firebase-tools)"
    EXIT_CODE=1
fi

echo ""

# Check Node.js
echo "🟢 Checking Node.js:"
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo "   ✅ Node.js installed: $NODE_VERSION"
else
    echo "   ⚠️  Node.js not found (required for running scripts)"
    EXIT_CODE=1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ ALL CHECKS PASSED!"
    echo ""
    echo "📋 Ready to deploy. Follow these steps:"
    echo ""
    echo "   1. Initialize plans:"
    echo "      cd functions && node init-plans.js"
    echo ""
    echo "   2. Deploy security rules:"
    echo "      cp firestore-rules-subscriptions.conf firestore.rules"
    echo "      firebase deploy --only firestore:rules"
    echo ""
    echo "   3. Deploy indexes:"
    echo "      firebase deploy --only firestore:indexes"
    echo ""
    echo "   4. (Optional) Migrate existing users:"
    echo "      cd functions && node migrate-users-to-companies.js"
    echo ""
    echo "📖 See FIRESTORE_SETUP_GUIDE.md for detailed instructions"
else
    echo "❌ SOME CHECKS FAILED!"
    echo ""
    echo "⚠️  Fix the issues above before deploying"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

exit $EXIT_CODE
