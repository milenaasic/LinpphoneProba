package example.com.linpphoneproba

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private val MLOG="MY_MAIN"
    private val LINPHONE_CORE_RANDOM_PORT = -1
    private val LINPHONE_DEFAULT_RC = "/.linphonerc"
    private val LINPHONE_FACTORY_RC = "/linphonerc"
    private val LINPHONE_LPCONFIG_XSD = "/lpconfig.xsd"
    private val DEFAULT_ASSISTANT_RC = "/default_assistant_create.rc"
    private val LINPHONE_ASSISTANT_RC = "/linphone_assistant_create.rc"
    private lateinit var mBasePath:String

    private lateinit var coreIterateThread: CoreIterateThread

    private lateinit var mCore:Core
    private var mListener: CoreListenerStub? = null
    var mCall: Call? = null
    var mIsMicMuted = false;
    var mIsSpeakerEnabled = false;

    private val sHandler: Handler = Handler(Looper.getMainLooper())

    private val myLoggingServiceListener =
        LoggingServiceListener { logService, domain, lev, message ->
            when (lev) {
                LogLevel.Debug -> {Log.d(domain, message)
                                    Log.d("MY_MAIN","$domain, $message") }

                LogLevel.Message -> {Log.i(domain, message)
                                    Log.i("MY_MAIN","$domain, $message")   }
                LogLevel.Warning -> {Log.w(domain, message)
                    Log.w("MY_MAIN","$domain, $message")  }
                LogLevel.Error -> {Log.e(domain, message)
                    Log.e("MY_MAIN","$domain, $message")    }
                LogLevel.Fatal -> Log.wtf(domain, message)
                else -> Log.wtf(domain, message)

            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val callButton=findViewById<Button>(R.id.buttonCall)
        val endButton=findViewById<Button>(R.id.buttonEND)

        callButton.setOnClickListener{
            //start a acall
            mCore.iterate()
            Log.i(MLOG," proxy config list je ${mCore.proxyConfigList.size}")
            Log.i(MLOG," username je ${mCore.defaultProxyConfig.identityAddress.username}")
            Log.i(MLOG,"stanje registracije ${mCore.defaultProxyConfig.state} ")
            val call=mCore.invite("sip:381643292202@45.63.117.19")

            val remoteAdress=call.remoteAddressAsString
            Log.i("MY_MAIN","nat adress ${mCore.natAddress} ")
            Log.i("MY_MAIN","remote adress $remoteAdress ")
            Log.i("MY_MAIN","call objekat je $call ")
            mCore.iterate()
        }

        endButton.setOnClickListener{

            mCore.currentCall.terminate()
        }

        val mContext=applicationContext
         mBasePath = mContext.getFilesDir().getAbsolutePath()
        Log.i("MY_MAIN","base path je $mBasePath")

        mCore =
            Factory.instance()
                .createCore(
                    null,
                    null,
                    mContext)

        Log.i("MY_MAIN","core je $mCore")
        Log.i("MY_MAIN","audio ports range ${mCore.audioPortsRange.max},${mCore.audioPortsRange.min} ")

        lateinit var fromAdress:Address
        //evo ga, moj broj je 381652771372
        fromAdress=mCore.createAddress("sip:381646408513@45.63.117.19")
        Log.i("MY_MAIN","from je $fromAdress")
        fromAdress.password="0b860db1d0ee1"

        var proxyConfig= mCore.createProxyConfig()

        proxyConfig.setIdentityAddress(fromAdress)
        proxyConfig.serverAddr="45.63.117.19"
        proxyConfig.expires=90
        //proxyConfig.enablePublish(true)
        Log.i(MLOG," proxy config username${proxyConfig.identityAddress.username}")
        Log.i(MLOG," proxy config server ${proxyConfig.serverAddr}")

        // Log.i(MLOG,"auth info class, ${authInfo.domain},${authInfo.username}")
        proxyConfig.enableRegister(true)

        mListener = object : CoreListenerStub() {

            override fun onRegistrationStateChanged(
                lc: Core?,
                cfg: ProxyConfig?,
                cstate: RegistrationState?,
                message: String?
            ) {
                when (cstate){

                    RegistrationState.None->{
                        Toast.makeText(
                            this@MainActivity,
                            "registration state NONE",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(MLOG," registration state NONE, $message,$cstate")
                    }

                    RegistrationState.Cleared->{
                    Toast.makeText(
                        this@MainActivity,
                        "registration state Cleared",
                        Toast.LENGTH_SHORT
                        ).show()
                        Log.i(MLOG," registration state Cleared, $message,$cstate")
                    }

                    RegistrationState.Failed->{
                        Toast.makeText(
                            this@MainActivity,
                            "registration state FAILED",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(MLOG," registration state FAILED,$message,$cstate ")
                    }

                    RegistrationState.Ok->{
                        Toast.makeText(
                            this@MainActivity,
                            "registration state OK",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(MLOG," registration state OK,$message, $cstate")
                    }

                    RegistrationState.Progress->{
                        Toast.makeText(
                            this@MainActivity,
                            "registration state PROGRESS",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(MLOG," registration state PROGRESS,$message, $cstate")
                    }


                }
                //super.onRegistrationStateChanged(lc, cfg, cstate, message)
            }

            override fun onCallStateChanged(
                lc: Core?,
                call: Call?,
                cstate: Call.State?,
                message: String?
            ) {
                when (cstate) {

                    Call.State.OutgoingEarlyMedia->{
                        Log.i(MLOG,"outgoing early media $message")
                        Toast.makeText(
                            this@MainActivity,
                            "outgoing early media",
                            Toast.LENGTH_SHORT
                        ).show()


                    }

                    Call.State.Error -> {
                        Log.i(MLOG,"call state error $message")
                        if (call?.getErrorInfo()?.getReason() == Reason.Declined) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_call_declined",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (call?.getErrorInfo()?.getReason() == Reason.NotFound) {

                            Toast.makeText(this@MainActivity, "error not found", Toast.LENGTH_SHORT)
                                .show()
                        } else if (call?.getErrorInfo()?.getReason() == Reason.NotAcceptable) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_not acceptable",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else if (call?.getErrorInfo()?.getReason() == Reason.Busy) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_user busy",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else if (message != null) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_user busy",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    Call.State.End-> {
                        // Convert Core message for internalization
                        Log.i(MLOG,"call state end $message")
                        if (call?.getErrorInfo()?.getReason() == Reason.Declined) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_call_declined",
                                Toast.LENGTH_SHORT)
                                .show();
                            Log.i(MLOG,"error_call_declined $message")

                        }
                    }
                    Call.State.Connected->{
                        // Convert Core message for internalization

                            Log.i(MLOG,"connected $message")
                    }

                    Call.State.OutgoingInit->{

                        Toast.makeText(
                            this@MainActivity,
                            "outgoing init, $message",
                            Toast.LENGTH_SHORT)
                            .show();
                        Log.i(MLOG,"outgoing init $message")
                    }

                    Call.State.OutgoingProgress->{
                        Toast.makeText(
                            this@MainActivity,
                            "outgoing progress, $message",
                            Toast.LENGTH_SHORT)
                            .show();
                        Log.i(MLOG,"outgoing progress $message")

                    }

                    Call.State.OutgoingRinging->{
                            Toast.makeText(
                                this@MainActivity,
                                "outgoing ringing, $message",
                                Toast.LENGTH_SHORT)
                                .show();
                        Log.i(MLOG,"outgoing ringing $message")
                    }




                    }
                }

        }

        var myAuthInfo=Factory.instance().createAuthInfo("381646408513","381646408513","0b860db1d0ee1",null,null,null)
        mCore.addAuthInfo(myAuthInfo)
        mCore.addListener(mListener)
        mCore.enableIpv6(true)
        mCore.setAudioPortRange(10000,20000)
        val array=mCore.audioPayloadTypes
        for(item in array.toList()){

            Log.i(MLOG,"audio payload types : ${item.mimeType},${item.enabled()}" +
                    " ${item.description},${item.isUsable}")

        }

        mCore.addProxyConfig(proxyConfig)
        // Log.i(MLOG,"auth info class, ${authInfo.domain},${authInfo.username}")
        Log.i(MLOG," proxy config list posle add je ${mCore.proxyConfigList.size}")


        mCore.defaultProxyConfig=proxyConfig
        Log.i(MLOG," username posle dodavanja proxy config je ${mCore.defaultProxyConfig.identityAddress.username}")

        mCore.enableLogCollection(LogCollectionState.Enabled)

        Factory.instance().setDebugMode(true, "MY_LIN");
        val loggingService=Factory.instance().loggingService
        Log.i("MY_MAIN","logging service params ${loggingService}")
        Factory.instance().loggingService.addListener(myLoggingServiceListener)
        Log.i("MY_MAIN"," avpf mode ${mCore.avpfMode.toString()}")
        mCore.avpfMode.toString()
        mCore.start()
        //mCore.iterate()
        Log.i("MY_MAIN","log collection enabled ${mCore.logCollectionEnabled().toString()} ")

        val mIterateRunnable = Runnable {
            if (mCore != null) {
                mCore.iterate()
                //Toast.makeText(this, "runnable posel iterate", Toast.LENGTH_SHORT).show()
                //Log.i(MLOG," u iterate")

            }
        }

        val lTask: TimerTask = object : TimerTask() {
            override fun run() {
                dispatchOnUIThread(mIterateRunnable)
            }
        }
        /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
        /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
        val mTimer =
            Timer("Linphone scheduler")
        mTimer.schedule(lTask, 0, 20)

    }

    override fun onDestroy() {

        mCore.stop()
        super.onDestroy()
    }
    private fun getLinphoneDefaultConfig():String {
        return mBasePath + LINPHONE_DEFAULT_RC
    }

    private fun getLinphoneFactoryConfig() :String{
        return mBasePath + LINPHONE_FACTORY_RC;
    }

    fun dispatchOnUIThread(r: Runnable) {
        //Log.i(MLOG,"usao u runnable")
        sHandler.post(r)
    }
}




