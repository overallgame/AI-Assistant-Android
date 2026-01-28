package com.example.aiassistant.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.AvatarType
import com.example.aiassistant.data.model.FontSizePreference
import com.example.aiassistant.data.model.LanguagePreference
import com.example.aiassistant.data.model.Session
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.model.UserState

@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val id: Int = 0,
    val sessionIsLoggedIn: Boolean = false,
    val sessionUserId: String? = null,
    val userId: String? = null,
    val phoneE164: String? = null,
    val phoneMasked: String? = null,
    val displayName: String? = null,
    val avatarType: String = "Default",
    val avatarValue: String? = null,
    val appearance: String = "System",
    val fontFollowSystem: Boolean = true,
    val fontScale: Float = 1f,
    val language: String = "System",
)

fun UserStateEntity.toModel(): UserState {
    val appearancePref = runCatching {
        AppearancePreference.valueOf(appearance)
    }.getOrDefault(AppearancePreference.System)

    val languagePref = runCatching {
        LanguagePreference.valueOf(language)
    }.getOrDefault(LanguagePreference.System)

    val avatarTypeValue = runCatching {
        AvatarType.valueOf(avatarType)
    }.getOrDefault(AvatarType.Default)

    val user = if (userId.isNullOrBlank()) {
        null
    } else {
        User(
            id = userId,
            phoneE164 = phoneE164,
            phoneMasked = phoneMasked.orEmpty(),
            displayName = displayName,
            avatar = Avatar(
                type = avatarTypeValue,
                value = avatarValue,
            ),
        )
    }

    return UserState(
        user = user,
        preferences = UserPreferences(
            appearance = appearancePref,
            fontSize = FontSizePreference(
                followSystem = fontFollowSystem,
                scale = fontScale,
            ),
            language = languagePref,
        ),
        session = Session(
            isLoggedIn = sessionIsLoggedIn,
            currentUserId = sessionUserId,
        ),
    )
}

fun UserState.toEntity(id: Int = 0): UserStateEntity {
    return UserStateEntity(
        id = id,
        sessionIsLoggedIn = session.isLoggedIn,
        sessionUserId = session.currentUserId,
        userId = user?.id,
        phoneE164 = user?.phoneE164,
        phoneMasked = user?.phoneMasked,
        displayName = user?.displayName,
        avatarType = user?.avatar?.type?.name ?: AvatarType.Default.name,
        avatarValue = user?.avatar?.value,
        appearance = preferences.appearance.name,
        fontFollowSystem = preferences.fontSize.followSystem,
        fontScale = preferences.fontSize.scale,
        language = preferences.language.name,
    )
}
