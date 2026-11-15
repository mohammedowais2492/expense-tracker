package com.expensetracker.ui.navigation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.expensetracker.services.UserService;
import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.bootstrap.ApplicationContext;
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.profile.ProfileService;
import com.expensetracker.ui.screens.landing.LandingController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests controller construction and dependency wiring.
 */
class ControllerFactoryTest {

    private ControllerFactory factory;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        UserContext userContext = new UserContext();
        AlertService alertService = mock(AlertService.class);
        ProfileService profileService = mock(ProfileService.class);

        factory = new ControllerFactory(
                applicationContext,
                userContext,
                alertService,
                profileService);
    }

    @Test
    void createsLandingControllerWhenRequested() {
        // ApplicationContext is a Lombok-generated immutable class,
        // so use a mock UserService rather than initializing SQLite.
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        UserService userService = mock(UserService.class);

        // Rebuild the factory with the configured dependency.
        org.mockito.Mockito.when(applicationContext.getUserService())
                .thenReturn(userService);

        ProfileService profileService = mock(ProfileService.class);

        factory = new ControllerFactory(
                applicationContext,
                new UserContext(),
                mock(AlertService.class),
                profileService);

        Object controller = factory.call(LandingController.class);

        assertNotNull(controller);
        assertInstanceOf(LandingController.class, controller);
    }

    @Test
    void throwsIllegalArgumentExceptionWhenControllerIsUnsupported() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.call(String.class));
    }
}
