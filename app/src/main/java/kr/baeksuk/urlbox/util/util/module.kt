package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val module = module {

    viewModel { MainViewModel(androidApplication()) }
    viewModel { UrlViewModel(androidApplication()) }
    viewModel { AddLinkViewModel(androidApplication()) }
    viewModel { CaptureViewModel(androidApplication()) }
    viewModel { UrlDetailViewModel(androidApplication()) }

}