package com.example

import com.example.data.PasscodeHasher
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun passcodeHashVerifiesCorrectPasscode() {
    val credential = PasscodeHasher.hash("1234")

    assertTrue(PasscodeHasher.verify("1234", credential.passwordHash, credential.salt))
    assertFalse(PasscodeHasher.verify("4321", credential.passwordHash, credential.salt))
    assertFalse(PasscodeHasher.needsRehash(credential.passwordHash))
  }

  @Test
  fun passcodeHashUsesUniqueSalts() {
    val first = PasscodeHasher.hash("1234")
    val second = PasscodeHasher.hash("1234")

    assertNotEquals(first.salt, second.salt)
    assertNotEquals(first.passwordHash, second.passwordHash)
  }

  @Test
  fun legacySha256HashStillVerifiesForMigration() {
    val legacySalt = "sharma_salt"
    val legacyHash = PasscodeHasher.legacySha256("1234", legacySalt)

    assertTrue(PasscodeHasher.verify("1234", legacyHash, legacySalt))
    assertTrue(PasscodeHasher.needsRehash(legacyHash))
  }
}
