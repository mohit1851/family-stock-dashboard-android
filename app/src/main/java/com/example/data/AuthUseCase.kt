package com.example.data

import java.util.UUID

sealed class AuthActionResult {
    data class Success(
        val user: User,
        val group: FamilyGroup?,
        val message: String,
        val refreshPortfolioSummary: Boolean = false
    ) : AuthActionResult()

    data class Error(val message: String) : AuthActionResult()
}

class AuthUseCase(private val repository: StockRepository) {
    suspend fun seedSandboxData() {
        val existingGroup = repository.getGroupById(SANDBOX_GROUP_ID)
        if (existingGroup == null) {
            repository.insertGroup(
                FamilyGroup(
                    groupId = SANDBOX_GROUP_ID,
                    name = "Sharma Family Portfolio",
                    inviteCode = "SHARMA-123",
                    ownerUsername = "sharma"
                )
            )
        }

        if (repository.getUserByUsername("sharma") == null) {
            insertSandboxUser("sharma", "Mohit Sharma")
            insertSandboxUser("dad_sharma", "Raj Kumar Sharma (Dad)")
            insertSandboxUser("mom_sharma", "Sarita Sharma (Mom)")
        }
    }

    suspend fun registerUser(username: String, fullName: String, passcode: String): AuthActionResult {
        val cleanUser = username.lowercase().trim()
        if (cleanUser.length < 3 || fullName.isBlank() || passcode.length < 4) {
            return AuthActionResult.Error("Username: min 3 letters. Passcode: min 4 numbers.")
        }
        if (repository.getUserByUsername(cleanUser) != null) {
            return AuthActionResult.Error("Username already exists.")
        }

        val credential = PasscodeHasher.hash(passcode)
        val newUser = User(
            username = cleanUser,
            fullName = fullName.trim(),
            passwordHash = credential.passwordHash,
            salt = credential.salt,
            groupId = null
        )
        repository.insertUser(newUser)
        return AuthActionResult.Success(
            user = newUser,
            group = null,
            message = "Successfully registered! Set up/join a family group next."
        )
    }

    suspend fun loginUser(username: String, passcode: String): AuthActionResult {
        val cleanUser = username.lowercase().trim()
        if (cleanUser.isBlank()) {
            return AuthActionResult.Error("Please enter a valid username.")
        }

        val user = repository.getUserByUsername(cleanUser)
            ?: return AuthActionResult.Error("Incorrect username or passcode.")

        if (!PasscodeHasher.verify(passcode, user.passwordHash, user.salt)) {
            return AuthActionResult.Error("Incorrect passcode.")
        }

        val authenticatedUser = if (PasscodeHasher.needsRehash(user.passwordHash)) {
            val credential = PasscodeHasher.hash(passcode)
            user.copy(passwordHash = credential.passwordHash, salt = credential.salt).also {
                repository.updateUser(it)
            }
        } else {
            user
        }

        val group = authenticatedUser.groupId?.takeIf { it.isNotBlank() }?.let {
            repository.getGroupById(it)
        }
        return AuthActionResult.Success(
            user = authenticatedUser,
            group = group,
            message = "Authenticated! Welcome back, ${authenticatedUser.fullName}.",
            refreshPortfolioSummary = true
        )
    }

    suspend fun createFamilyGroup(user: User?, groupName: String): AuthActionResult {
        if (user == null) return AuthActionResult.Error("Authentication required.")
        if (groupName.isBlank()) return AuthActionResult.Error("Group name cannot be blank.")

        val groupId = UUID.randomUUID().toString().take(8).uppercase()
        val inviteCode = "SHA-$groupId"
        val group = FamilyGroup(
            groupId = groupId,
            name = groupName.trim(),
            inviteCode = inviteCode,
            ownerUsername = user.username
        )
        repository.insertGroup(group)

        val updatedUser = user.copy(groupId = groupId)
        repository.updateUser(updatedUser)
        repository.insertChatMessage(
            sender = "System",
            message = "Secure group formed by ${user.fullName}. Use code $inviteCode to invite your family members!",
            groupId = groupId
        )

        return AuthActionResult.Success(
            user = updatedUser,
            group = group,
            message = "Family group formed! Code: $inviteCode"
        )
    }

    suspend fun joinFamilyGroup(user: User?, inviteCode: String): AuthActionResult {
        if (user == null) return AuthActionResult.Error("Authentication required.")
        val cleanCode = inviteCode.uppercase().trim()
        if (cleanCode.isBlank()) return AuthActionResult.Error("Please insert an invitation code.")

        val group = repository.getGroupByInviteCode(cleanCode)
            ?: return AuthActionResult.Error("Invalid group invitation code.")

        val updatedUser = user.copy(groupId = group.groupId)
        repository.updateUser(updatedUser)
        repository.insertChatMessage(
            sender = "System",
            message = "${user.fullName} joined the family circle.",
            groupId = group.groupId
        )

        return AuthActionResult.Success(
            user = updatedUser,
            group = group,
            message = "Success! Linked to ${group.name} portfolio stream.",
            refreshPortfolioSummary = true
        )
    }

    suspend fun removeMember(username: String): String? {
        val user = repository.getUserByUsername(username) ?: return null
        repository.updateUser(user.copy(groupId = null))
        return "Member @$username removed from the family circle."
    }

    private suspend fun insertSandboxUser(username: String, fullName: String) {
        val credential = PasscodeHasher.hash("1234")
        repository.insertUser(
            User(
                username = username,
                fullName = fullName,
                passwordHash = credential.passwordHash,
                salt = credential.salt,
                groupId = SANDBOX_GROUP_ID
            )
        )
    }

    companion object {
        const val SANDBOX_GROUP_ID = "SHARMA_GROUP"
    }
}
