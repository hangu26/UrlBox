package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.FirebaseAdminAccessRepository
import kr.baeksuk.urlbox.data.repository.FirebaseFeedbackRepository
import kr.baeksuk.urlbox.domain.LoginUseCase
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.domain.CaptureLoginStateUseCase
import kr.baeksuk.urlbox.domain.CaptureSaveUseCase
import kr.baeksuk.urlbox.domain.DeleteImageUseCase
import kr.baeksuk.urlbox.domain.feedback.CheckAdminAccessUseCase
import kr.baeksuk.urlbox.domain.feedback.ObserveFeedbackReportsUseCase
import kr.baeksuk.urlbox.domain.feedback.UpdateFeedbackStatusUseCase
import kr.baeksuk.urlbox.domain.LoadDetailDataUseCase
import kr.baeksuk.urlbox.domain.LoadThumbnailDataUseCase
import kr.baeksuk.urlbox.domain.LoadUserHomeDataUseCase
import kr.baeksuk.urlbox.domain.ToggleFavoriteUseCase
import kr.baeksuk.urlbox.domain.UpdateUrlMemoUseCase
import kr.baeksuk.urlbox.domain.UpdateUrlNameUseCase
import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import kr.baeksuk.urlbox.viewmodel.editurl.EditUrlViewModel
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import kr.baeksuk.urlbox.viewmodel.favorite.FavoriteViewModel
import kr.baeksuk.urlbox.viewmodel.imgdetail.ImgDetailViewModel
import kr.baeksuk.urlbox.viewmodel.language.LanguageViewModel
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import kr.baeksuk.urlbox.viewmodel.admin.FeedbackAdminViewModel
import kr.baeksuk.urlbox.viewmodel.myfolder.MyFolderViewModel
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import kr.baeksuk.urlbox.viewmodel.nav.ThumbnailViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import kr.baeksuk.urlbox.viewmodel.share.ShareReceiverViewModel
import kr.baeksuk.urlbox.viewmodel.privacy.PrivacyViewModel
import kr.baeksuk.urlbox.viewmodel.savedlink.SavedLinkViewModel
import kr.baeksuk.urlbox.viewmodel.setting.SettingViewModel
import kr.baeksuk.urlbox.viewmodel.tag.TagViewModel
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import kr.baeksuk.urlbox.viewmodel.useterms.UseTermsViewModel
import kr.baeksuk.urlbox.domain.feedback.AdminAccessRepository
import kr.baeksuk.urlbox.domain.feedback.FeedbackRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val module = module {
    single<AdminAccessRepository> { FirebaseAdminAccessRepository() }
    single<FeedbackRepository> { FirebaseFeedbackRepository() }
    single { CheckAdminAccessUseCase(get()) }
    single { ObserveFeedbackReportsUseCase(get()) }
    single { UpdateFeedbackStatusUseCase(get()) }
    single { LoadDetailDataUseCase(get(), get()) }
    single { UpdateUrlMemoUseCase(get(), get()) }
    single { UpdateUrlNameUseCase(get(), get()) }
    single { ToggleFavoriteUseCase(get(), get()) }
    single { DeleteImageUseCase(get(), get()) }
    single { LoadThumbnailDataUseCase(get(), get()) }
    single { UrlRepository(get()) }
    single { LoadUserHomeDataUseCase(get(), get()) }
    single { UserRepository(get()) }
    single { CaptureLoginStateUseCase(get()) }
    single { CaptureSaveUseCase(get(), get()) }
    single { LoginUseCase(get(), get()) }
    single { UserSessionManager(androidContext()) }
    viewModel { MainViewModel(androidApplication()) }
    viewModel { UrlViewModel(androidApplication(), get(), get(), get()) }
    viewModel { AddLinkViewModel(androidApplication()) }
    viewModel { CaptureViewModel(androidApplication(), get(), get()) }
    viewModel { UrlDetailViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ThumbnailViewModel(androidApplication(), get()) }
    viewModel { ShareReceiverViewModel(androidApplication(), get()) }
    viewModel { MyPageViewModel(androidApplication(), get()) }
    viewModel { UrlDataViewModel() }
    viewModel { SavedLinkViewModel(androidApplication(),get()) }
    viewModel { FavoriteViewModel(androidApplication()) }
    viewModel { ImgDetailViewModel(androidApplication(), get(), get(), get() ,get()) }
    viewModel { LoginViewModel(androidApplication(), get()) }
    viewModel { SettingViewModel(androidApplication(), get()) }
    viewModel { LanguageViewModel(androidApplication()) }
    viewModel { MyFolderViewModel(androidApplication()) }
    viewModel { EditUrlViewModel(androidApplication(), get()) }
    viewModel { TagViewModel(androidApplication(), get()) }
    viewModel { SetTagViewModel(androidApplication(), get()) }
    viewModel { PrivacyViewModel(androidApplication()) }
    viewModel { UseTermsViewModel(androidApplication()) }
    viewModel { FeedbackAdminViewModel(get(), get(), get()) }

}