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

    private lateinit var mCore:Core
    private lateinit var mProxyConfig:ProxyConfig
    private var mListener: CoreListenerStub? = null
    private lateinit var mTimer:Timer
    private var mRingSoundFile: String? = null

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val callButton=findViewById<Button>(R.id.buttonCall)
        val endButton=findViewById<Button>(R.id.buttonEND)

       // mRingSoundFile = mBasePath + "/share/sounds/linphone/rings/notes_of_the_optimistic.mkv";

        callButton.setOnClickListener{
            //start a call
            Log.i(MLOG," proxy config list je ${mCore.proxyConfigList.size}")
            Log.i(MLOG," username je ${mCore.defaultProxyConfig.identityAddress.username}")
            Log.i(MLOG,"stanje registracije ${mCore.defaultProxyConfig.state} ")
            Log.i(MLOG," avpf mode iz Call ${mCore.defaultProxyConfig.avpfMode}, enabled ${mCore.defaultProxyConfig.avpfEnabled()} ")
            Log.i(MLOG," media encryption iz Call je ${mCore.mediaEncryption}")
            //Evo ga broj: +381655761851

             mCall=mCore.invite("sip:+38163352717@45.63.117.19")
            var callParams=mCore.createCallParams(mCall)
            Log.i(MLOG," call params ${callParams.usedAudioPayloadType},${callParams.audioEnabled()}, early media enabled ${callParams.earlyMediaSendingEnabled()} ")
            callParams.enableEarlyMediaSending(true)
            Log.i(MLOG," call params posle setovanja ${callParams.usedAudioPayloadType},${callParams.audioEnabled()}, early media enabled ${callParams.earlyMediaSendingEnabled()} ")
            var callParams2=mCore.createCallParams(mCall)
            Log.i(MLOG," call params2 posle setovanja ${callParams.usedAudioPayloadType},${callParams.audioEnabled()}, early media enabled ${callParams.earlyMediaSendingEnabled()} ")


            //ok, evo ja sam spreman +381652771372

        }

        endButton.setOnClickListener {

            if (mCall != null) {
                if (mCore.currentCall != null) mCore.currentCall.terminate()
            }
        }

        val mContext=applicationContext

        mCore =
            Factory.instance()
                .createCore(
                    null,
                    null,
                    mContext)

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

                    Call.State.Idle->{
                        Log.i(MLOG,"idle, $message")
                        updateCallStatus("Idle")
                    }

                    Call.State.OutgoingInit->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)

                    }

                    Call.State.OutgoingProgress->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                    Call.State.OutgoingRinging->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                    Call.State.OutgoingEarlyMedia->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                    Call.State.Connected->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                    Call.State.Error -> {
                        Log.i(MLOG,"call state error $message")
                        if (call?.getErrorInfo()?.getReason() == Reason.Declined) {
                            updateCallStatus("call declined")

                        } else if (call?.getErrorInfo()?.getReason() == Reason.NotFound) {
                            updateCallStatus("Not Found")

                        } else if (call?.getErrorInfo()?.getReason() == Reason.NotAcceptable) {
                            updateCallStatus("Not Acceptable")

                        } else if (call?.getErrorInfo()?.getReason() == Reason.Busy) {
                            updateCallStatus("Busy")

                        } else if (message != null) {
                            updateCallStatus(message)
                        }
                    }

                    Call.State.End-> {
                        // Convert Core message for internalization
                        Log.i(MLOG," $message")
                        if (call?.getErrorInfo()?.getReason() == Reason.Declined) {
                            Toast.makeText(
                                this@MainActivity,
                                "error_call_declined",
                                Toast.LENGTH_SHORT)
                                .show();
                            Log.i(MLOG,"error_call_declined $message")

                        }
                        updateCallStatus(message)

                    }

                    Call.State.Paused->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                    Call.State.Resuming->{
                        Log.i(MLOG, "$message")
                        updateCallStatus(message)
                    }

                }

                if (cstate == Call.State.End || cstate == Call.State.Released) {
                    //finish();
                }
            }

        }

        configureCore()
        configureLogging()

        mProxyConfig= mCore.createProxyConfig()
        configureProxy(mProxyConfig)
        mCore.addProxyConfig(mProxyConfig)
        mCore.defaultProxyConfig=mProxyConfig

        /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
        mTimer = Timer("Linphone scheduler")
       // mTimer.schedule(lTask, 0, 20)

    }

    override fun onStart() {
       mCore.start()
       mTimer.schedule(lTask, 0, 20)
        super.onStart()

    }

    override fun onStop() {
        mTimer.cancel()
        mCore.stop()
        mCore.removeListener(mListener)

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

    }


    fun dispatchOnUIThread(r: Runnable) {

        sHandler.post(r)
    }

    private fun configureCore(){
        val myAuthInfo=Factory.instance().createAuthInfo("381646408513","381646408513","1dd1c051df981",null,null,null)

        mCore.apply {
            addListener(mListener)
            enableIpv6(true)
            setAudioPortRange(10000,20000)
            mediaEncryption=MediaEncryption.None
            isMediaEncryptionMandatory=false
            avpfMode=AVPFMode.Enabled
            enableWifiOnly(false)
            addAuthInfo(myAuthInfo)
            enableLogCollection(LogCollectionState.Enabled)
            ringDuringIncomingEarlyMedia=true
        }

        // set only GSM codec
        for(item in mCore.audioPayloadTypes.toList()){

            if(item.mimeType=="GSM") item.enable(true)
            else item.enable(false)
            Log.i(MLOG,"audio payload types : ${item.mimeType},${item.enabled()}" +
                    " ${item.description},${item.isUsable}")
        }

        Log.i(MLOG,"is ringDuringIncomingEarlyMedia, ${mCore.ringDuringIncomingEarlyMedia}")
    }

    private fun configureLogging(){
        Factory.instance().setDebugMode(true, "MY_LIN");
        //val loggingService=Factory.instance().loggingService
        Factory.instance().loggingService.addListener(myLoggingServiceListener)
    }

    private fun configureProxy(myProxyConfig: ProxyConfig){

        var fromAdress=mCore.createAddress("sip:381646408513@45.63.117.19")
        fromAdress.password="1dd1c051df981"

        myProxyConfig.apply {
            serverAddr="45.63.117.19"
            expires=90
            setIdentityAddress(fromAdress)
            //enableRegister(true)
         }


    }

    private fun updateCallStatus(status: String?) {
        Toast.makeText(
            this@MainActivity,
            status?:"nothing",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*private fun toggleMic() {
        mCore.enableMic(!mCore.micEnabled())
        mMicro.setSelected(!mCore.micEnabled())
    }

    private fun toggleSpeaker() {
        if (mAudioManager.isAudioRoutedToSpeaker()) {
            mAudioManager.routeAudioToEarPiece()
        } else {
            mAudioManager.routeAudioToSpeaker()
        }
        mSpeaker.setSelected(mAudioManager.isAudioRoutedToSpeaker())
    }*/
}




