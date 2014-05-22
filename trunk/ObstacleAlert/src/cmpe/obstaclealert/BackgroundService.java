package cmpe.obstaclealert;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class BackgroundService extends Service {
   @Override
   public IBinder onBind(Intent arg0) {
      return null;
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      // Let it continue running until it is stopped.
      return START_STICKY;
   }
   @Override
   public void onDestroy() {
      super.onDestroy();
   }
}

