package com.shipal.shipal.Model

import java.time.LocalDateTime


data class PostInfo (
    /* 게시물 PK */
    var postSeq : Int? = null,

    /* 작성자 시퀀스 */
    var userSeq : Int,

    /* 게시글 제목 */
    var title: String,

    /* 내용 */
    var contents: String,

    /* 출발지 (시/도) 시퀀스 */
    var fromCountrySeq: Int,

    /* 출발지 (시/군/구) 시퀀스 */
    var fromCitySeq: Int,

    /* 출발지 (읍/면/동) 시퀀스 */
    var fromTownSeq: Int,

    /* 도착지 (시/도) 시퀀스 */
    var toCountrySeq: Int,

    /* 도착지 (시/군/구) 시퀀스 */
    var toCitySeq: Int,

    /* 도착지 (읍/면/동) 시퀀스 */
    var toTownSeq: Int,

    var createDt: LocalDateTime,



)
