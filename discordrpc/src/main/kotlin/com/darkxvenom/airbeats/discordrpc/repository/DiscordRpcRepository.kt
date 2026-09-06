/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * DiscordRpcRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.darkxvenom.airbeats.discordrpc.repository

import com.darkxvenom.airbeats.discordrpc.remote.ApiService
import com.darkxvenom.airbeats.discordrpc.utils.toImageAsset

/**
 * Modified by Zion Huang
 */
class DiscordRpcRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return api.getImage(url).getOrNull()?.toImageAsset()
    }
}
