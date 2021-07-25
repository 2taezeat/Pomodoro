package wikibook.learnandroid.pomodoro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingFragment : PreferenceFragmentCompat() {
    // (1) 설정 프래그먼트를 통해 사용자가 설정한 정보는 내부적으로 모두 SharedPreferences에 저장됩니다. 그런데 프리퍼런스 객체를 생성하려면 저장할 파일의 이름을 지정해야 하므로 클래스 상수의 형태로 파일명을 정으ㅏㅣ합니다.
    companion object {
        val SETTING_PREF_FILENAME = "pomodoro_setting"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (2) 설정 화면에서는 액션바를 숨김.
        (activity as AppCompatActivity).supportActionBar?.hide()

        // (3)
        preferenceManager.sharedPreferencesName = SETTING_PREF_FILENAME

        // (4) 메서드를 호출하며 XML 리소스 식별자를 전달해서 설정화면과 관련된 구성 정보를 불러옵니다.
        addPreferencesFromResource(R.xml.pomodoro_preferences)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}
}
