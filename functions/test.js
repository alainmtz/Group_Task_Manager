// Test para las Cloud Functions
// Ejecutar con: npm test

const test = require('firebase-functions-test')();
const admin = require('firebase-admin');

// Inicializar para testing
admin.initializeApp();

// Mock de datos de prueba
const mockUserIds = ['user1', 'user2', 'user3'];
const mockEventType = 'TASK_UPDATED';
const mockEventData = {
  taskId: 'task123',
  status: 'completed'
};

// Mock de contexto autenticado
const mockContext = {
  auth: {
    uid: 'testuser',
    token: {}
  }
};

// Importar las funciones
const myFunctions = require('./index');

describe('Cloud Functions Tests', () => {
  
  describe('sendUpdateEvent', () => {
    it('should reject unauthenticated requests', async () => {
      const wrapped = test.wrap(myFunctions.sendUpdateEvent);
      
      try {
        await wrapped({
          userIds: mockUserIds,
          eventType: mockEventType,
          data: mockEventData
        }, {});
        
        // Si no lanza error, falla el test
        throw new Error('Should have thrown unauthenticated error');
      } catch (error) {
        expect(error.code).toBe('unauthenticated');
      }
    });

    it('should reject invalid userIds', async () => {
      const wrapped = test.wrap(myFunctions.sendUpdateEvent);
      
      try {
        await wrapped({
          userIds: [], // Array vacío
          eventType: mockEventType,
          data: mockEventData
        }, mockContext);
        
        throw new Error('Should have thrown invalid-argument error');
      } catch (error) {
        expect(error.code).toBe('invalid-argument');
      }
    });

    it('should reject missing eventType', async () => {
      const wrapped = test.wrap(myFunctions.sendUpdateEvent);
      
      try {
        await wrapped({
          userIds: mockUserIds,
          data: mockEventData
        }, mockContext);
        
        throw new Error('Should have thrown invalid-argument error');
      } catch (error) {
        expect(error.code).toBe('invalid-argument');
      }
    });
  });

  // Limpiar después de los tests
  after(() => {
    test.cleanup();
  });
});

console.log('✅ Tests configurados. Para ejecutar:');
console.log('   npm install --save-dev firebase-functions-test jest');
console.log('   npm test');
