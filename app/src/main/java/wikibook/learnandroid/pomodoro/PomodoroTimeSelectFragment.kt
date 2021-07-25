package wikibook.learnandroid.pomodoro

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

// (1) 대화 상자를 보여주기 위해 사용할 DialogFragment 를 상속받는 프래그먼트 클래스를 정의합니다.
class PomodoroTimeSelectFragment : DialogFragment() {
    lateinit var timeSelectView : View
    // 대화상자에 필요한 레이아웃을 구성하고, 필요한 기능을 설정한 후 대화상자 객체를 반환하는 역할을 수행
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // (2) AlertDialog 내부 클래스에 정의된 Builder 객체를 생성합니다. 이후 빌더 객체에서 제공하는 다양한 메서드를 호출해 대화상자 객체를 생성한 후 반환할 것입니다.
        val builder = AlertDialog.Builder(requireContext())

        // (3)
        timeSelectView = LayoutInflater.from(requireContext()).inflate(R.layout.pomodoro_time_select_dialog, null)
        val timeSelect = timeSelectView.findViewById<LinearLayout>(R.id.time_select)

        // (4) 클릭 시점에 전달받은 버튼 뷰 객체(it)의 tag 속성에 저장된 문자열값을 숫자로 변환합니다.
        val listener = View.OnClickListener {
            // tag 속성에 담긴 초 단위 시간값을 숫자로 변환
            val sec = it.tag.toString().toLong()
            // 서비스를 시작하기 위해 startPomodoro 메서드를 호출
            startPomodoro(sec)
        }

        val times = activity?.getSharedPreferences(SettingFragment.SETTING_PREF_FILENAME, Context.MODE_PRIVATE)?.getString("preset_times", "5,10,15,20,25,30")

        // (5)

        times?.split(",")?.forEach {
            val time = it.trim()
            // (6) 버튼 객체를 동적으로 생성한 후 버튼에 표시될 레이블 문자열을 설정합니다.
            val btn = Button(activity)
            btn.setText("${time}분")
            // (7) 버튼에 설정한리스너의 내부에서 tag 정보를 활용 할 수 있또록 tag 속성값을 분 단위의 시간에서 초 단위의 시간으로 변환한 후 저장합니다.
            btn.tag = "${time.toInt() * 60}"
            // (8) 버튼의 layoutParams 속성을 초기화하며 뷰의 크기를 설정합니다.
            btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // (9) 클릭 리스너를 설정하고 addView 메서드를 호출해서 최종적으로 LinearLayout 뷰그룹에 설정이 완료된 버튼을 최종적으로 추가합니다.
            btn.setOnClickListener(listener)
            timeSelect.addView(btn)
        }

        // (10)
        builder.setView(timeSelectView)
            .setPositiveButton("시작"){ _, _ ->
                // (11) 시작(긍정) 버튼을 누르면 직접 입력한 초 단위 시간 정보를 불러와 startPomodoro 메서드를 호출하며 알람 서비스를 시작합니다.
                var time = timeSelectView.findViewById<EditText>(R.id.manual_time_select).text.toString().toLong()
                startPomodoro(time)
            }
            .setNegativeButton("취소"){ _, _ ->
                // (12) // 부정(취소) 버튼을 누르면 dismiss 메서드를 호출해서 대화상자를 닫습니다.
                dismiss()
            }

        // (13) 최종적으로 빌더 객체의 create 메서드를 호출해서 알림 대화상자를 표시할대화상자 타입(Dialog 타입)의 객체를 생성한 후 반환 합니다.
        return builder.create()
    }

    // (14) 알람 서비스를 시작하는 작업을 진행할 startPomodoro 메서드를 추가합니다.
    private fun startPomodoro(delay : Long) {
        if(!(delay <= 0)) {
            activity?.let {
                // 프래그먼트의 호스트 액티비티에 접근해 인텐트를 생성하고 서비스 실행에 필요한 정보를 추가합니다.
                val i = Intent(it, PomodoroService::class.java)
                i.putExtra("delayTimeInSec", delay.toInt())
                i.putExtra("startTime", System.currentTimeMillis())
                // 서비스로 보낼 인텐트에 알람 방식에 대한 추가 정보를 추가
                i.putExtra("notifyMethod","beep")

                // 프리퍼런스를 통해 받아온 알람 방식과 볼륨값을 인텐트에 추가
                i.putExtra("notifyMethod", it.getSharedPreferences(SettingFragment.SETTING_PREF_FILENAME, Context.MODE_PRIVATE)?.getString("notify_method", "vibration"))
                i.putExtra("volume", it.getSharedPreferences(SettingFragment.SETTING_PREF_FILENAME, Context.MODE_PRIVATE)?.getInt("volume", 50))

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.startForegroundService(i)
                } else {
                    it.startService(i)
                }

                dismiss()
            }
        }
    }
}
