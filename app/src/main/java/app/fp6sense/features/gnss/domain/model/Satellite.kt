package app.fp6sense.features.gnss.domain.model

data class Satellite(
    val svid: Int,
    val constellation: String,
    val cn0DbHz: Float,
    val elevationDegrees: Float,
    val azimuthDegrees: Float,
    val usedInFix: Boolean
)
