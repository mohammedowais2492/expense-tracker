package com.expensetracker.ui.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.ui.context.UserContext;

/**
 * Tests profile resolution and selection for the current UI session.
 */
class ProfileServiceTest {

    private ExpenseProfileService expenseProfileService;
    private UserContext userContext;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        expenseProfileService = mock(ExpenseProfileService.class);
        userContext = new UserContext();
        profileService = new ProfileService(
                expenseProfileService,
                userContext);
    }

    @Test
    void throwsIllegalStateExceptionWhenNoUserIsSelected() {
        assertThrows(
                IllegalStateException.class,
                () -> profileService.selectDefaultProfile());

        verifyNoInteractions(expenseProfileService);
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    void selectsExistingProfileWithoutCreatingAnother() {
        userContext.selectUser(1L);

        ExpenseProfile existingProfile = mock(ExpenseProfile.class);
        when(existingProfile.getId()).thenReturn(10L);

        when(expenseProfileService.getProfileByUserId(1L))
                .thenReturn(Optional.of(existingProfile));

        profileService.selectDefaultProfile();

        assertEquals(10L, userContext.getSelectedProfileId());

        verify(expenseProfileService).getProfileByUserId(1L);
        verify(expenseProfileService, never())
                .createProfile(any(ExpenseProfile.class));
    }

    @Test
    void createsAndSelectsDefaultProfileWhenNoneExists() {
        userContext.selectUser(1L);

        ExpenseProfile persistedProfile = mock(ExpenseProfile.class);
        when(persistedProfile.getId()).thenReturn(10L);

        when(expenseProfileService.getProfileByUserId(1L))
                .thenReturn(Optional.empty());

        when(expenseProfileService.createProfile(any(ExpenseProfile.class)))
                .thenReturn(persistedProfile);

        profileService.selectDefaultProfile();

        ArgumentCaptor<ExpenseProfile> profileCaptor = ArgumentCaptor.forClass(ExpenseProfile.class);

        verify(expenseProfileService)
                .createProfile(profileCaptor.capture());

        ExpenseProfile submittedProfile = profileCaptor.getValue();

        assertEquals("Default", submittedProfile.getProfileName());
        assertEquals(1L, submittedProfile.getUserId());
        assertNull(submittedProfile.getId());

        assertEquals(10L, userContext.getSelectedProfileId());
    }

    @Test
    void propagatesExceptionWhenProfileLookupFails() {
        userContext.selectUser(1L);

        when(expenseProfileService.getProfileByUserId(1L))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> profileService.selectDefaultProfile());

        assertNull(userContext.getSelectedProfileId());

        verify(expenseProfileService, never())
                .createProfile(any(ExpenseProfile.class));
    }

    @Test
    void propagatesExceptionWhenProfileCreationFails() {
        userContext.selectUser(1L);

        when(expenseProfileService.getProfileByUserId(1L))
                .thenReturn(Optional.empty());

        when(expenseProfileService.createProfile(any(ExpenseProfile.class)))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> profileService.selectDefaultProfile());

        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    void throwsNullPointerExceptionWhenExpenseProfileServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> new ProfileService(null, userContext));
    }

    @Test
    void throwsNullPointerExceptionWhenUserContextIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> new ProfileService(expenseProfileService, null));
    }
}
