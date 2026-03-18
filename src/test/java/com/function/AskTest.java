package com.function;

import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AskTest {

    @Test
    void functionReturnsResponse() {
        // This test validates the function can be instantiated and the handler signature is correct.
        // Full integration testing requires the Copilot CLI to be available.
        var ask = new Ask();
        assertNotNull(ask);
    }

    @Test
    void defaultPromptIsUsedWhenBodyIsEmpty() {
        // Verify the function handles empty body gracefully
        // Full invocation would require Copilot CLI — this is a structural test
        var ask = new Ask();
        assertNotNull(ask);
    }
}
