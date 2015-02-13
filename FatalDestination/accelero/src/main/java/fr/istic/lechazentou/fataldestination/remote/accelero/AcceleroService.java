package fr.istic.lechazentou.fataldestination.remote.accelero;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AcceleroService extends Service {
    public AcceleroService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
