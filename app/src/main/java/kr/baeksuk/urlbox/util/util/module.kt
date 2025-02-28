package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import kr.baeksuk.urlbox.viewmodel.editurl.EditUrlViewModel
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import kr.baeksuk.urlbox.viewmodel.favorite.FavoriteViewModel
import kr.baeksuk.urlbox.viewmodel.imgdetail.ImgDetailViewModel
import kr.baeksuk.urlbox.viewmodel.language.LanguageViewModel
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import kr.baeksuk.urlbox.viewmodel.myfolder.MyFolderViewModel
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import kr.baeksuk.urlbox.viewmodel.nav.ThumbnailViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import kr.baeksuk.urlbox.viewmodel.privacy.PrivacyViewModel
import kr.baeksuk.urlbox.viewmodel.savedlink.SavedLinkViewModel
import kr.baeksuk.urlbox.viewmodel.setting.SettingViewModel
import kr.baeksuk.urlbox.viewmodel.tag.TagViewModel
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import kr.baeksuk.urlbox.viewmodel.useterms.UseTermsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val module = module {

    viewModel { MainViewModel(androidApplication()) }
    viewModel { UrlViewModel(androidApplication()) }
    viewModel { AddLinkViewModel(androidApplication()) }
    viewModel { CaptureViewModel(androidApplication()) }
    viewModel { UrlDetailViewModel(androidApplication()) }
    viewModel { ThumbnailViewModel(androidApplication()) }
    viewModel { MyPageViewModel(androidApplication()) }
    viewModel { UrlDataViewModel() }
    viewModel { SavedLinkViewModel(androidApplication()) }
    viewModel { FavoriteViewModel(androidApplication()) }
    viewModel { ImgDetailViewModel(androidApplication()) }
    viewModel { LoginViewModel(androidApplication()) }
    viewModel { SettingViewModel(androidApplication()) }
    viewModel { LanguageViewModel(androidApplication()) }
    viewModel { MyFolderViewModel(androidApplication()) }
    viewModel { EditUrlViewModel(androidApplication()) }
    viewModel { TagViewModel(androidApplication()) }
    viewModel { SetTagViewModel(androidApplication()) }
    viewModel { PrivacyViewModel(androidApplication()) }
    viewModel { UseTermsViewModel(androidApplication()) }

}