package com.example.aiassistant.data.model

data class User(
    val id: String,
    val phoneE164: String? = null,
    val phoneMasked: String = "",
    val displayName: String? = null,
    val avatar: Avatar = Avatar(),
)

data class Avatar(
    val type: AvatarType = AvatarType.Default,
    val value: String? = null,
)

enum class AvatarType {
    Default,
    LocalUri,
    RemoteUrl,
}

data class UserPreferences(
    val appearance: AppearancePreference = AppearancePreference.System,
    val fontSize: FontSizePreference = FontSizePreference(),
    val language: LanguagePreference = LanguagePreference.System,
)

enum class AppearancePreference {
    System,
    Light,
    Dark,
}

data class FontSizePreference(
    val followSystem: Boolean = true,
    val scale: Float = 1f,
)

enum class LanguagePreference {
    System,
    ZhHans,
    En,
}

data class Session(
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
)

data class UserState(
    val user: User? = null,
    val preferences: UserPreferences = UserPreferences(),
    val session: Session = Session(),
)
