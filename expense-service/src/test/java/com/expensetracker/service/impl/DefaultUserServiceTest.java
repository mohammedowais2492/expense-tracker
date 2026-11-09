package com.expensetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.models.User;
import com.expensetracker.repositories.UserRepository;

/**
 * Unit tests for {@link DefaultUserService}.
 *
 * <p>
 * Uses a mocked {@link UserRepository} to verify service-level
 * validation, repository delegation, return values, and exception
 * propagation without accessing a database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUserService Tests")
class DefaultUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private DefaultUserService userService;

    @BeforeEach
    void setUp() {
        userService = new DefaultUserService(userRepository);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates service with valid repository")
        void createsServiceWithValidRepository() {
            DefaultUserService service = new DefaultUserService(userRepository);

            assertNotNull(service);
        }

        @Test
        @DisplayName("Throws NullPointerException when repository is null")
        void throwsNullPointerExceptionWhenRepositoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new DefaultUserService(null));

            assertEquals(
                    "userRepository must not be null",
                    exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Add User Tests")
    class AddUserTests {

        @Test
        @DisplayName("Adds user and returns persisted user")
        void addsUserAndReturnsPersistedUser() {
            User newUser = User.builder()
                    .firstName("firstName")
                    .lastName("lastName")
                    .build();

            User persistedUser = User.reconstitute(
                    1L, "firstName", "lastName");

            when(userRepository.save(newUser))
                    .thenReturn(persistedUser);

            User result = userService.addUser(newUser);

            assertSame(persistedUser, result);
            verify(userRepository).save(newUser);
        }

        @Test
        @DisplayName("Throws NullPointerException when user is null")
        void throwsNullPointerExceptionWhenUserIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> userService.addUser(null));

            assertEquals(
                    "user must not be null",
                    exception.getMessage());

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when user already has an ID")
        void throwsIllegalArgumentExceptionWhenUserAlreadyHasId() {
            User existingUser = User.reconstitute(
                    1L, "firstName", "lastName");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.addUser(existingUser));

            assertEquals(
                    "New user must not have an id",
                    exception.getMessage());

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when save fails")
        void propagatesRepositoryExceptionWhenSaveFails() {
            User newUser = User.builder()
                    .firstName("firstName")
                    .build();

            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(userRepository.save(newUser))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.addUser(newUser));

            assertSame(repositoryException, exception);
            verify(userRepository).save(newUser);
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Returns user when ID exists")
        void returnsUserWhenIdExists() {
            User user = User.reconstitute(
                    1L, "firstName", "lastName");

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(user));

            Optional<User> result = userService.getUserById(1L);

            assertTrue(result.isPresent());
            assertSame(user, result.get());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Returns empty Optional when user does not exist")
        void returnsEmptyOptionalWhenUserDoesNotExist() {
            when(userRepository.findById(99L))
                    .thenReturn(Optional.empty());

            Optional<User> result = userService.getUserById(99L);

            assertTrue(result.isEmpty());
            verify(userRepository).findById(99L);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is zero")
        void throwsIllegalArgumentExceptionWhenIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUserById(0L));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is negative")
        void throwsIllegalArgumentExceptionWhenIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUserById(-1L));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when lookup fails")
        void propagatesRepositoryExceptionWhenLookupFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(userRepository.findById(1L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.getUserById(1L));

            assertSame(repositoryException, exception);
            verify(userRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Returns all users from repository")
        void returnsAllUsersFromRepository() {
            User firstUser = User.reconstitute(
                    1L, "firstName", "lastName");

            User secondUser = User.reconstitute(
                    2L, "anotherFirstName", "anotherLastName");

            List<User> users = List.of(firstUser, secondUser);

            when(userRepository.findAll()).thenReturn(users);

            List<User> result = userService.getAllUsers();

            assertSame(users, result);
            assertEquals(2, result.size());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Returns empty list when no users exist")
        void returnsEmptyListWhenNoUsersExist() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<User> result = userService.getAllUsers();

            assertTrue(result.isEmpty());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Propagates repository exception when retrieval fails")
        void propagatesRepositoryExceptionWhenRetrievalFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(userRepository.findAll())
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.getAllUsers());

            assertSame(repositoryException, exception);
            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Updates user and returns updated user")
        void updatesUserAndReturnsUpdatedUser() {
            User user = User.reconstitute(
                    1L, "updatedFirstName", "updatedLastName");

            when(userRepository.update(user)).thenReturn(user);

            User result = userService.updateUser(user);

            assertSame(user, result);
            verify(userRepository).update(user);
        }

        @Test
        @DisplayName("Throws NullPointerException when user is null")
        void throwsNullPointerExceptionWhenUserIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> userService.updateUser(null));

            assertEquals(
                    "user must not be null",
                    exception.getMessage());

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when user has no ID")
        void throwsIllegalArgumentExceptionWhenUserHasNoId() {
            User user = User.builder()
                    .firstName("firstName")
                    .build();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.updateUser(user));

            assertEquals(
                    "userId must not be null",
                    exception.getMessage());

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when update fails")
        void propagatesRepositoryExceptionWhenUpdateFails() {
            User user = User.reconstitute(
                    1L, "firstName", "lastName");

            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(userRepository.update(user))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.updateUser(user));

            assertSame(repositoryException, exception);
            verify(userRepository).update(user);
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Returns true when user is deleted")
        void returnsTrueWhenUserIsDeleted() {
            when(userRepository.deleteById(1L))
                    .thenReturn(true);

            boolean result = userService.deleteUser(1L);

            assertTrue(result);
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Returns false when user does not exist")
        void returnsFalseWhenUserDoesNotExist() {
            when(userRepository.deleteById(99L))
                    .thenReturn(false);

            boolean result = userService.deleteUser(99L);

            assertFalse(result);
            verify(userRepository).deleteById(99L);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is zero")
        void throwsIllegalArgumentExceptionWhenIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.deleteUser(0L));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is negative")
        void throwsIllegalArgumentExceptionWhenIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.deleteUser(-1L));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when deletion fails")
        void propagatesRepositoryExceptionWhenDeletionFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(userRepository.deleteById(1L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.deleteUser(1L));

            assertSame(repositoryException, exception);
            verify(userRepository).deleteById(1L);
        }
    }
}
