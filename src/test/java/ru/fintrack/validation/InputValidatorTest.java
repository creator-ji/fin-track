package ru.fintrack.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputValidatorTest {

    @Test
    void requireNonBlankRejectsEmpty() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> InputValidator.requireNonBlank("  ", "логин")
        );
        assertTrue(error.getMessage().contains("логин"));
    }

    @Test
    void parsePositiveAmountAcceptsComma() {
        assertEquals(12.5, InputValidator.parsePositiveAmount("12,5"));
    }

    @Test
    void parsePositiveAmountRejectsZeroAndInvalid() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parsePositiveAmount("0"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parsePositiveAmount("abc"));
    }
}
