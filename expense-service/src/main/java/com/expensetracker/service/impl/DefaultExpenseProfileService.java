package com.expensetracker.service.impl;

import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.repositories.ExpenseProfileRepository;
import com.expensetracker.services.ExpenseProfileService;

/**
 * Default implementation of {@link ExpenseProfileService}.
 *
 * <p>
 * Provides application-level expense profile management operations
 * by delegating persistence to {@link ExpenseProfileRepository}.
 */
public class DefaultExpenseProfileService implements ExpenseProfileService {
    private final ExpenseProfileRepository expenseProfileRepository;

    /**
     * Creates an expense profile service backed by the specified repository.
     *
     * @param expenseProfileRepository the repository used to manage profiles
     * @throws NullPointerException if expenseProfileRepository is null
     */
    public DefaultExpenseProfileService(ExpenseProfileRepository expenseProfileRepository) {
        this.expenseProfileRepository = Objects.requireNonNull(expenseProfileRepository,
                "expenseProfileRepository must not be null");
    }

    @Override
    public ExpenseProfile createProfile(ExpenseProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.getId() != null) {
            throw new IllegalArgumentException("New profile must not have an id");
        }
        return this.expenseProfileRepository.save(profile);
    }

    @Override
    public Optional<ExpenseProfile> getProfileByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        return this.expenseProfileRepository.findByUserId(userId);
    }

    @Override
    public ExpenseProfile updateProfile(ExpenseProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.getId() == null) {
            throw new IllegalArgumentException("profileId must not be null");
        }
        return this.expenseProfileRepository.update(profile);
    }

    @Override
    public boolean deleteProfile(long profileId) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        return this.expenseProfileRepository.deleteById(profileId);
    }
}
