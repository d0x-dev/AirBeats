package com.darkxvenom.airbeats.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackAuthStateTest {
    private val signedIn = PlaybackAuthState(
        cookie = "SAPISID=same-session; LOGIN_INFO=same-login; SIDCC=old",
        visitorData = "visitor",
        dataSyncId = "account",
        poToken = "manual-token",
    )

    @Test
    fun refreshKeepsTheAccountAndOtherAuthenticationFields() {
        val cookie = "SAPISID=same-session; LOGIN_INFO=same-login; SIDCC=refreshed"
        assertEquals(signedIn.copy(cookie = cookie), signedIn.withRefreshedCookie(cookie))
    }

    @Test
    fun browserCookiesCannotRestoreALoggedOutAccount() {
        assertSame(PlaybackAuthState.EMPTY, PlaybackAuthState.EMPTY.withRefreshedCookie(signedIn.cookie))
    }

    @Test
    fun browserCookiesCannotSelectAnotherSessionOrLogin() {
        assertSame(signedIn, signedIn.withRefreshedCookie("SAPISID=other; LOGIN_INFO=same-login"))
        assertSame(signedIn, signedIn.withRefreshedCookie("SAPISID=same-session; LOGIN_INFO=other-login"))
        assertSame(signedIn, signedIn.withRefreshedCookie("SAPISID=same-session"))
    }

    @Test
    fun missingBrowserSessionDoesNotDiscardTheSavedLogin() {
        assertSame(signedIn, signedIn.withRefreshedCookie(null))
        assertSame(signedIn, signedIn.withRefreshedCookie(""))
        assertSame(signedIn, signedIn.withRefreshedCookie("PREF=hl=en"))
    }
}
