package com.enazamusic.smsapp.model

import java.io.Serializable

class LastTransferAttempt(
    var date: Long,
    var httpResponseCode: Int
) : Serializable