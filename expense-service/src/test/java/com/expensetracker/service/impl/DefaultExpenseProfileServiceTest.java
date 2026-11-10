package com.expensetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.repositories.ExpenseProfileRepository;

/**
 * Unit tests for {@link DefaultExpenseProfileService}.
 *
 * <p>
 * Uses a mocked {@link ExpenseProfileRepository} to verify input
 * validation, repository delegation, return values, and exception
 * propagation without accessing a database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultExpenseProfileService Tests")
class DefaultExpenseProfileServiceTest {

    @Mock
    private ExpenseProfileRepository expenseProfileRepository;

    private DefaultExpenseProfileService expenseProfileService;

    @BeforeEach
    void setUp() {
        expenseProfileService = new DefaultExpenseProfileService(expenseProfileRepository);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates service with valid repository")
        void createsServiceWithValidRepository() {
            DefaultExpenseProfileService service = new DefaultExpenseProfileService(expenseProfileRepository);

            assertNotNull(service);
        }

        @Test
        @DisplayName("Throws NullPointerException when repository is null")
        void throwsNullPointerExceptionWhenRepositoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new DefaultExpenseProfileService(null));

            assertEquals(
                    "expenseProfileRepository must not be null",
                    exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Create Profile Tests")
    class CreateProfileTests {

        @Test
        @DisplayName("Creates profile and returns persisted profile")
        void createsProfileAndReturnsPersistedProfile() {
            ExpenseProfile newProfile = ExpenseProfile.builder()
                    .userId(1L)
                    .profileName("Household")
                    .build();

            ExpenseProfile persistedProfile = ExpenseProfile.reconstitute(10L, 1L, "Household");

            when(expenseProfileRepository.save(newProfile))
                    .thenReturn(persistedProfile);

            ExpenseProfile result = expenseProfileService.createProfile(newProfile);

            assertSame(persistedProfile, result);
            verify(expenseProfileRepository).save(newProfile);
        }

        @Test
        @DisplayName("Throws NullPointerException when profile is null")
        void throwsNullPointerExceptionWhenProfileIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> expenseProfileService.createProfile(null));

            assertEquals("profile must not be null", exception.getMessage());
            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile already has ID")
        void throwsIllegalArgumentExceptionWhenProfileAlreadyHasId() {
            ExpenseProfile existingProfile = ExpenseProfile.reconstitute(10L, 1L, "Household");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.createProfile(existingProfile));

            assertEquals(
                    "New profile must not have an id",
                    exception.getMessage());

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when creation fails")
        void propagatesRepositoryExceptionWhenCreationFails() {
            ExpenseProfile newProfile = ExpenseProfile.builder()
                    .userId(1L)
                    .profileName("Household")
                    .build();

            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(expenseProfileRepository.save(newProfile))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> expenseProfileService.createProfile(newProfile));

            assertSame(repositoryException, exception);
            verify(expenseProfileRepository).save(newProfile);
        }
    }

    @Nested
    @DisplayName("Get Profile By User ID Tests")
    class GetProfileByUserIdTests {

        @Test
        @DisplayName("Returns profile when user has one")
        void returnsProfileWhenUserHasOne() {
            ExpenseProfile profile = ExpenseProfile.reconstitute(10L, 1L, "Household");

            when(expenseProfileRepository.findByUserId(1L))
                    .thenReturn(Optional.of(profile));

            Optional<ExpenseProfile> result = expenseProfileService.getProfileByUserId(1L);

            assertTrue(result.isPresent());
            assertSame(profile, result.get());
            verify(expenseProfileRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("Returns empty Optional when user has no profile")
        void returnsEmptyOptionalWhenUserHasNoProfile() {
            when(expenseProfileRepository.findByUserId(2L))
                    .thenReturn(Optional.empty());

            Optional<ExpenseProfile> result = expenseProfileService.getProfileByUserId(2L);

            assertTrue(result.isEmpty());
            verify(expenseProfileRepository).findByUserId(2L);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when user ID is zero")
        void throwsIllegalArgumentExceptionWhenUserIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.getProfileByUserId(0L));

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when user ID is negative")
        void throwsIllegalArgumentExceptionWhenUserIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.getProfileByUserId(-1L));

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when lookup fails")
        void propagatesRepositoryExceptionWhenLookupFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(expenseProfileRepository.findByUserId(1L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> expenseProfileService.getProfileByUserId(1L));

            assertSame(repositoryException, exception);
            verify(expenseProfileRepository).findByUserId(1L);
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Updates profile and returns updated profile")
        void updatesProfileAndReturnsUpdatedProfile() {
            ExpenseProfile updatedProfile = ExpenseProfile.reconstitute(10L, 1L, "Updated Household");

            when(expenseProfileRepository.update(updatedProfile))
                    .thenReturn(updatedProfile);

            ExpenseProfile result = expenseProfileService.updateProfile(updatedProfile);

            assertSame(updatedProfile, result);
            verify(expenseProfileRepository).update(updatedProfile);
        }

        @Test
        @DisplayName("Throws NullPointerException when profile is null")
        void throwsNullPointerExceptionWhenProfileIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> expenseProfileService.updateProfile(null));

            assertEquals("profile must not be null", exception.getMessage());
            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile has no ID")
        void throwsIllegalArgumentExceptionWhenProfileHasNoId() {
            ExpenseProfile newProfile = ExpenseProfile.builder()
                    .userId(1L)
                    .profileName("Household")
                    .build();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.updateProfile(newProfile));

            assertEquals(
                    "profileId must not be null",
                    exception.getMessage());

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when update fails")
        void propagatesRepositoryExceptionWhenUpdateFails() {
            ExpenseProfile profile = ExpenseProfile.reconstitute(10L, 1L, "Household");

            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(expenseProfileRepository.update(profile))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> expenseProfileService.updateProfile(profile));

            assertSame(repositoryException, exception);
            verify(expenseProfileRepository).update(profile);
        }
    }

    @Nested
    @DisplayName("Delete Profile Tests")
    class DeleteProfileTests {

        @Test
        @DisplayName("Returns true when profile is deleted")
        void returnsTrueWhenProfileIsDeleted() {
            when(expenseProfileRepository.deleteById(10L))
                    .thenReturn(true);

            boolean result = expenseProfileService.deleteProfile(10L);

            assertTrue(result);
            verify(expenseProfileRepository).deleteById(10L);
        }

        @Test
        @DisplayName("Returns false when profile does not exist")
        void returnsFalseWhenProfileDoesNotExist() {
            when(expenseProfileRepository.deleteById(99L))
                    .thenReturn(false);

            boolean result = expenseProfileService.deleteProfile(99L);

            assertFalse(result);
            verify(expenseProfileRepository).deleteById(99L);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is zero")
        void throwsIllegalArgumentExceptionWhenProfileIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.deleteProfile(0L));

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is negative")
        void throwsIllegalArgumentExceptionWhenProfileIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> expenseProfileService.deleteProfile(-1L));

            verifyNoInteractions(expenseProfileRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when deletion fails")
        void propagatesRepositoryExceptionWhenDeletionFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(expenseProfileRepository.deleteById(10L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> expenseProfileService.deleteProfile(10L));

            assertSame(repositoryException, exception);
            verify(expenseProfileRepository).deleteById(10L);
        }
    }
}
