package com.brainserve.appointment.architecture;

import com.brainserve.appointment.BrainServeApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleBoundaryTest {

    @Test
    void modularMonolithBoundariesAreValid() {
        ApplicationModules.of(BrainServeApplication.class).verify();
    }
}
