import crypto from "k6/crypto";
import encoding from "k6/encoding";

export const csrfToken = __ENV.CSRF_TOKEN || "perf-csrf-token";

const jwtSecret =
    __ENV.JWT_SECRET || "local-development-jwt-secret-key-must-be-at-least-32-bytes";
const tokenVersion = Number(__ENV.TOKEN_VERSION || "0");

export function authParams(userId, tags = {}) {
    return {
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken,
            Cookie: `accessToken=${accessToken(userId)}; XSRF-TOKEN=${csrfToken}`,
        },
        tags,
    };
}

export function accessToken(userId) {
    const now = Math.floor(Date.now() / 1000);
    const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
    const payload = base64Url(JSON.stringify({
        sub: String(userId),
        jti: `${userId}-${now}`,
        email: `perf-${userId}@sisibibi.test`,
        role: "USER",
        tokenType: "ACCESS",
        tokenVersion,
        iat: now,
        exp: now + Number(__ENV.ACCESS_TOKEN_TTL_SECONDS || "1800"),
    }));
    const unsignedToken = `${header}.${payload}`;
    const signature = crypto.hmac("sha256", jwtSecret, unsignedToken, "base64rawurl");
    return `${unsignedToken}.${signature}`;
}

function base64Url(value) {
    return encoding.b64encode(value, "rawurl");
}
