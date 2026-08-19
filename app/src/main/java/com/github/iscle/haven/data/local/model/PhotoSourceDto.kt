package com.github.iscle.haven.data.local.model

import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.PhotoSourceType
import kotlinx.serialization.Serializable

@Serializable
data class PhotoSourceDto(
    val id: String,
    val type: String,
    val label: String,
    val value: String = "",
    val enabled: Boolean = true,
    val subtitle: String? = null,
    val previewUrl: String? = null
) {
    fun toDomain(): PhotoSource = PhotoSource(
        id = id,
        type = runCatching { PhotoSourceType.valueOf(type) }
            .getOrDefault(PhotoSourceType.UNSPLASH_QUERY),
        label = label,
        value = value,
        enabled = enabled,
        subtitle = subtitle,
        previewUrl = previewUrl
    )

    companion object {
        fun fromDomain(source: PhotoSource): PhotoSourceDto = PhotoSourceDto(
            id = source.id,
            type = source.type.name,
            label = source.label,
            value = source.value,
            enabled = source.enabled,
            subtitle = source.subtitle,
            previewUrl = source.previewUrl
        )
    }
}
