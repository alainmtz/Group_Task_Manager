const admin = require('firebase-admin');
admin.initializeApp();

admin.firestore().collection('auditLogs')
  .where('action', '==', 'GROUP_DELETE')
  .orderBy('timestamp', 'desc')
  .limit(1)
  .get()
  .then(snapshot => {
    if (!snapshot.empty) {
      const doc = snapshot.docs[0];
      const data = doc.data();
      
      console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
      console.log('📊 AUDIT LOG COMPLETO - GROUP_DELETE');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
      
      console.log('📝 Basic Info:');
      console.log('  Event ID:', data.eventId);
      console.log('  User ID:', data.userId);
      console.log('  Action:', data.action);
      console.log('  Resource:', data.resource);
      console.log('  Resource ID:', data.resourceId);
      console.log('  Status:', data.status);
      console.log('  Timestamp:', data.timestamp?.toDate?.());
      
      console.log('\n⚡ Metrics:');
      if (data.metrics) {
        console.log('  Duration:', data.metrics.durationMs, 'ms');
        console.log('  Resources Affected:', data.metrics.resourcesAffected);
        console.log('  Retry Count:', data.metrics.retryCount);
        console.log('  Data Size:', data.metrics.dataSize || 'N/A');
      } else {
        console.log('  ❌ No metrics found');
      }
      
      console.log('\n📋 Context:');
      if (data.context) {
        console.log('  Company ID:', data.context.companyId || 'unknown');
        console.log('  Company Plan:', data.context.companyPlan || 'unknown');
        console.log('  Source Screen:', data.context.sourceScreen);
        console.log('  Trigger Type:', data.context.triggerType);
        console.log('  Device Info:', data.context.deviceInfo || 'N/A');
        console.log('  App Version:', data.context.appVersion || 'N/A');
      } else {
        console.log('  ❌ No context found');
      }
      
      console.log('\n🏷️  Metadata:');
      console.log('  ', JSON.stringify(data.metadata));
      
      if (data.errorDetails) {
        console.log('\n❌ Error Details:');
        console.log('  Type:', data.errorDetails.errorType);
        console.log('  Message:', data.errorDetails.errorMessage);
        console.log('  Can Retry:', data.errorDetails.canRetry);
      }
      
      console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    } else {
      console.log('❌ No logs found');
    }
    process.exit(0);
  })
  .catch(err => {
    console.error('Error:', err.message);
    process.exit(1);
  });
