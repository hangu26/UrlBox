package kr.baeksuk.urlbox.viewmodel.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.util.base.NavigationMenu

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _menu: MutableLiveData<NavigationMenu> =
        MutableLiveData(NavigationMenu.URL) // 단어 화면으로 초기화
    val menu: LiveData<NavigationMenu> = _menu

    private val _pageLoaded = MutableLiveData<Boolean>()
    val pageLoaded: LiveData<Boolean> = _pageLoaded

    fun changeMenu(menu: NavigationMenu) {
        _menu.value = menu
    } // 네비게이션 메뉴 클릭 시, 메인 액티비티에서 해당 네비게이션 메뉴로 변경

    fun btnSetting(){

    }

}