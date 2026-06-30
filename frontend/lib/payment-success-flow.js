export function getPaymentSuccessRedirectPath(payment, roomId) {
  if (payment?.targetType === "CUSTOM_AI_REPORT") {
    return null
  }

  return roomId ? `/rooms/${roomId}` : "/rooms"
}
