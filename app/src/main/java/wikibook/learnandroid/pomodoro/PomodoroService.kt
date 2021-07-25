package wikibook.learnandroid.pomodoro

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class PomodoroService : Service() {


    companion object { // 서비스에 사용할 상수를 정의합니다. 이후 앱 상단의 상태바를 이용해 공지를 띄울때 사용할 채널 이름 사수와
        // 인텐트에 포함할 작업 관련 문자열을 정의합니다.
        // 알람 발생과 알람 취소 작업과 관련된 두 개의 문자열 상수를 정의합니다. 이상수는 리시버를 통해서 전달받은 알람 관련 작업의 종류를 구분하는데 사용됩니다.
        val ALARM_CHANNEL_NAME = "뽀모도로 알람"
        val ACTION_ALARM_CANCEL = "wikibook.learnandroid.pomodoro.ACTION_ALARM_CANCEL"
        val ACTION_ALARM = "wikibook.learnandroid.pomodoro.ACTION_ALARM"
        // 남은 시간을 공지해야 할 때마다 브로드캐스트 할 인텐트 메시지 객체의 작업 문자열 상수를 추가했습니다.
        val ACTION_REMAIN_TIME_NOTIFY = "wikibook.learnandroid.pomodoro.ACTION_SEND_COUNT"
    }

    // (2) 서비스 내부에서 사용할 Timer 객체를 정의합니다.
    lateinit var timer: Timer
    // 알람 작동과 관련되 상황을 상태바를 통해 보여주는 데 필요한 정보 속성을 선언합니다.
    var delayTimeInSec: Int = 0
    var startTime: Long = 0
    var endTime: Long = 0

    lateinit var vibrator: Vibrator
    lateinit var receiver: BroadcastReceiver
    lateinit var alarmBroadcastIntent: PendingIntent

    // (1)
    lateinit var builder : NotificationCompat.Builder

    // (2)
    val dateFormatter = SimpleDateFormat("h:mm:ss")

    // 효과음을 출력하기 위해 사용할 SoundPool 객체를 추가
    lateinit var soundPool : SoundPool
    var soundId = 0

    // 배경음악을 출력하기 위해 사용할 MediaPlayer 객체를 추가
    lateinit var mediaPlayer : MediaPlayer

    // 볼륨 정보 저장용 속성을 추가
    var volume : Int = 0

    override fun onBind(intent: Intent): IBinder? = null

    // 서비스에서 필요한 여러 초기화 작업을 진행 할 수 있도록 onStartCommand 메서드를 재정의합니다.
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // (5) 서비스를 시작하는 측(액티비티)에서 타이머 시작 시각과 종료시각 정보를 인텐트 메시지를 통해 전달하도록 구현할 것이므로 인텐트 객체를 통해 해당 정보를 얻습니다.
        delayTimeInSec = intent.getIntExtra("delayTimeInSec", 0)
        startTime = intent.getLongExtra("startTime", 0)
        endTime = startTime + (delayTimeInSec * 1000)

        // (1) 인텐트를 통해 알람 방식을 전달받아 속성값을 초기화 합니다.
        val notifyMethod = intent.getStringExtra("notifyMethod")

        // 인텐트를 통해 전달받은 볼륨값을 이용해 volume 속성을 초기화
        volume = intent.getIntExtra("volume", 50)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationTimeInMS: Long = 1000 * 3

        // (2) 빌더 객체를 통해 SoundPool 객체를 생성하고 load 메서드를 통해 필요한 음악 리소스를 불러옵니다.
        soundPool = SoundPool.Builder().build()
        soundId = soundPool.load(this, R.raw.beep, 1)
        // (3)
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_music)

        // 미디어 플레이어를 생성한 후 볼륨을 설정
        mediaPlayer.setVolume((volume * 0.01).toFloat(), (volume * 0.01).toFloat())

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmBroadcastIntent =
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_ALARM), PendingIntent.FLAG_ONE_SHOT)

        // (9)
        val delay = 1000 * delayTimeInSec
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delay,
            alarmBroadcastIntent
        )

        // (10)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    // (11)
                    ACTION_ALARM -> { // 알람 시작 신호를 받았으므로 진동을 발생시키도록 코드를 작성합니다.
                        // Log.d("mytag", "Vibrating...")
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(vibrationTimeInMS, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(vibrationTimeInMS)
                        }
                        stopSelf()
                    }
                    // (2)
                    "beep" -> {
                        soundPool.play(soundId, (volume * 0.01).toFloat(), (volume * 0.01).toFloat(), 1, 0, 1f)
                        stopSelf()
                    }
                    // (3)
                    "music" -> {
                        mediaPlayer.start()
                        cancelRemainTimeNotifyTimer()
                        // (4)
                        mediaPlayer.setOnCompletionListener {
                            stopSelf()
                        }
                    }
                    // (12) 알람 취소 신호를 받았으므로, 알람을 취소하기 위해 서비스를 종료합니다.
                    ACTION_ALARM_CANCEL -> stopSelf()
                    // 화면 상태가 변경될 때 타이머의 작동 여부를 조정할 분기 코드
                    Intent.ACTION_SCREEN_ON -> startRemainTimeNotifyTimer()
                    Intent.ACTION_SCREEN_OFF -> cancelRemainTimeNotifyTimer()
                }
            }
        }

        // (13) 리시버 객체에서 받을 인텐트 메시지의 종류를 제한하기 위해 사용할 필터 객에를 생성하고 addAction 메서드를 호출해서 수신받을 작업 문자열을 등록합니다.
        val filter = IntentFilter()
        filter.addAction(ACTION_ALARM)
        filter.addAction(ACTION_ALARM_CANCEL)

        // 단말기의 화면 상태 변경과 관련된 시스템 브로드캐스트 메시지 수신
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)

        // (14) 메서드를 호출해서 브로드캐시트 리스버 객체를 동록하고, 인텐트 필터 객체도 함께 전달합니다.
        registerReceiver(receiver, filter)

        // (15) 상태바 매니저(notificationManager)를 통해 상태바를 통해 보여줄 알림 객체를 생성합니다.
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // (16) API 26버전 이상일 경우에만 알림 채널을 생성
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                ALARM_CHANNEL_NAME,
                "뽀모도로 상태 알림 채널",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
        // 빌더 객체를 생성한 후 클래스에 추가한 속성에 저장하게 했습니다. 이후 1ㅗ마다 내용을 갱신한 새로운 알림 메시지 객체를 생성해서 교체하는 데 해당 빌더 객체를 사용하겠습니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = NotificationCompat.Builder(this, ALARM_CHANNEL_NAME)
        } else {
            builder = NotificationCompat.Builder(this)
        }

        // (2) 시작 액티비티로 이동할 인텐트 객체를 생성
        val activityStartIntent = Intent(this, PomodoroActivity::class.java)
        // 알림 메시지를 터치했을 때 수행할 ,PendingIntent 객체를 생성하는 과정에서 액티비티 이동용 인텐트 객체를 전달
        val activityStartPendingIntent = PendingIntent.getActivity(this, 1, activityStartIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = builder
            // (3)
            .setContentTitle("${dateFormatter.format(Date(startTime))}부터 ${dateFormatter.format(Date(endTime))}까지")
            .setContentText("시작됨")
            .setSmallIcon(R.drawable.ic_tomato)
            .setOnlyAlertOnce(true)
            // (4) 상태바의 알림 메시지를 터치하는 시점에 사용할 PendingIntent 객체를 전달합니다.
            .setContentIntent(activityStartPendingIntent)
            .build()

        // (19)
        startForeground(1, notification)
        // (6)
        startRemainTimeNotifyTimer()

        // (20)
        return Service.START_NOT_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        // 음악이 재생 중이라면 종료
        if(mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        // 리소스 해제
        mediaPlayer.release()
        // 서비스가 종료되는 시점에 내부 타이머도 함께 종료 될 수 있게 합니다.
        cancelRemainTimeNotifyTimer()
        // (21)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmBroadcastIntent)
        // (22)
        unregisterReceiver(receiver)
    }

    // 내부적으로 사용할 타미거 객체를 생성하고 타이머를 시작하기 위해 호출하는 메서드
    fun startRemainTimeNotifyTimer() {
        // (3)
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                // (4) 밀리초 단위의 남은 시간을 계산
                val diff = ((endTime - System.currentTimeMillis()) / 1000) * 1000
                val i = Intent(ACTION_REMAIN_TIME_NOTIFY) // 인텐트 메시지 정보에 추가하고 브로드캐스트 방식으로 전송합니다.
                i.putExtra("count", diff)
                // 알람 설정 시간 정보를 포함해서 전달하도록 수정
                i.putExtra("delay", delayTimeInSec)
                sendBroadcast(i)

                // 1초마다 알림 메시지의 내용을 갱신
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if(diff <= 0) {
                    // (1) 알람까지 남은 시간을 계산해서 알람 시간이 되면 내부 타이머 작동이 완료되었음을 공지할 알림 객체를 생성
                    val notification = builder.setContentTitle("완료").setContentText("-").build()
                    notificationManager.notify(1, notification)
                    cancel()
                } else {
                    // (2) 아직 알람까지 시간이 남은 상황이므로 남은 시간을 초 단위 시간으로 변환 후 남은 시간을 보여줄 새 알림 메시지 객체를 생성해 notify 메서드를 호출하며 전달합니다.
                    val remainInSec = diff / 1000
                    val notification = builder.setContentText("남은 시간 : ${remainInSec / 60}:${String.format("%02d", remainInSec % 60)}").build()
                    notificationManager.notify(1, notification)
                }
            }
        }, 0, 1000)
    }

    // (5)
    fun cancelRemainTimeNotifyTimer() {
        timer?.cancel()
    }
}
