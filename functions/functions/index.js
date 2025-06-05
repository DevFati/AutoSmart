const admin = require('firebase-admin');
const cron = require('node-cron');

// Inicializar Firebase Admin
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Función para eliminar usuarios no verificados
async function deleteUnverifiedUsers() {
  const now = Date.now();
  const oneDayMs = 24 * 60 * 60 * 1000;
  let nextPageToken;
  
  try {
    do {
      const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
      const usersToDelete = listUsersResult.users.filter(user =>
        !user.emailVerified && (now - new Date(user.metadata.creationTime).getTime()) > oneDayMs
      );
      
      for (const user of usersToDelete) {
        await admin.auth().deleteUser(user.uid);
        console.log(`Deleted unverified user: ${user.email}`);
      }
      
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);
    
    console.log('Finished checking for unverified users');
  } catch (error) {
    console.error('Error:', error);
  }
}

// Programar la tarea para que se ejecute cada 24 horas
cron.schedule('0 0 * * *', () => {
  console.log('Running scheduled task to delete unverified users');
  deleteUnverifiedUsers();
});

console.log('Scheduler started. Will run every 24 hours.');
