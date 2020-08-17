package example.com.linpphoneproba;

import android.util.Log;

import org.linphone.core.Core;

public class CoreIterateThread extends Thread {
    private Core myCore=null;
    boolean terminated = false; //thread state

    public CoreIterateThread(Core core){

        myCore=core;
    }

    //start thread
    public boolean Start()
    {
        try{
            this.start();
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    //use this function from external code when the sipstack is not needed anymore (when you app is closing).
    public void Stop()
    {
        terminated = true;
    }

    public void run()
    {
        try{

            //continuous blocking read until thread is terminated:
            while (!terminated) {
               myCore.iterate();
               Thread.sleep(20);
               Log.i("MY_THREAD","Thread data: "+Thread.currentThread());
            }
        }
        catch (Exception e)
        {
            if(!terminated) e.printStackTrace();
        }
    }


}

