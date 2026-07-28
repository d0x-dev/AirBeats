/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.darkxvenom.airbeats.kizzy.repository

import com.darkxvenom.airbeats.kizzy.remote.ApiService
import com.darkxvenom.airbeats.kizzy.utils.toImageAsset

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return api.getImage(url).getOrNull()?.toImageAsset()
    }
}
