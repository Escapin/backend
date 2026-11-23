package plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

object RateLimits {
    val REGISTER = RateLimitName("register")
    val PSEUDO_CHECK = RateLimitName("pseudo-check")
    val OAUTH = RateLimitName("oauth")
}

fun Application.configureRateLimiting() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }

        register(RateLimits.REGISTER) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }

        register(RateLimits.PSEUDO_CHECK) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
        }

        register(RateLimits.OAUTH) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
        }
    }
}