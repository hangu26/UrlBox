package kr.baeksuk.urlbox.view.nav

import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

sealed class ThumbnailState {

    data class Guest(
        val urls : List<UrlEntity>
    ) : ThumbnailState()

    data class Login(
        val urls : List<UrlBackupEntity>,
        val tags : List<TagBackupEntity>
    ) : ThumbnailState()

}

